package com.umedical.umedical.service;

import com.umedical.umedical.dto.AppointmentRequest;
import com.umedical.umedical.dto.AppointmentResponse;
import com.umedical.umedical.dto.DocumentRequest;
import com.umedical.umedical.dto.DocumentResponse;
import com.umedical.umedical.dto.RescheduleRequest;
import com.umedical.umedical.entity.Appointment;
import com.umedical.umedical.entity.Doctor;
import com.umedical.umedical.entity.Document;
import com.umedical.umedical.entity.Patient;
import com.umedical.umedical.entity.enumns.AppointmentStatus;
import com.umedical.umedical.exception.ConflictException;
import com.umedical.umedical.exception.ForbiddenOperationException;
import com.umedical.umedical.exception.ResourceNotFoundException;
import com.umedical.umedical.repository.AppointmentRepository;
import com.umedical.umedical.repository.DoctorRepository;
import com.umedical.umedical.repository.DocumentRepository;
import com.umedical.umedical.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Reglas de negocio de las citas. Capas de proteccion contra horarios cruzados:
 * (1) el horario debe existir en la agenda del medico y estar libre (AvailabilityService),
 * (2) el paciente no puede tener otra cita activa a la misma hora,
 * (3) la UNIQUE KEY (doctor, fecha, hora, active_slot) de la BD es la ultima linea de defensa
 *     ante dos peticiones simultaneas.
 */
@Service
public class AppointmentService {

    private static final long MAX_AGENDA_RANGE_DAYS = 62;

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DocumentRepository documentRepository;
    private final AvailabilityService availabilityService;
    private final Clock clock;
    private final long minCancelHours;
    private final long minBookingLeadMinutes;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorRepository doctorRepository,
                              PatientRepository patientRepository,
                              DocumentRepository documentRepository,
                              AvailabilityService availabilityService,
                              Clock clock,
                              @Value("${app.appointments.min-cancel-hours:2}") long minCancelHours,
                              @Value("${app.appointments.min-booking-lead-minutes:60}") long minBookingLeadMinutes) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.documentRepository = documentRepository;
        this.availabilityService = availabilityService;
        this.clock = clock;
        this.minCancelHours = minCancelHours;
        this.minBookingLeadMinutes = minBookingLeadMinutes;
    }

    // ------------------------------------------------------------------ Reservar

    /** Un paciente agenda para si mismo; ADMIN y SECRETARY agendan a nombre de un paciente (patientId obligatorio). */
    @Transactional
    public AppointmentResponse createAppointment(Long userId, String role, AppointmentRequest request) {
        Patient patient = resolvePatient(userId, role, request.getPatientId());

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Medico no encontrado"));

        if (!doctor.getUser().isActive()) {
            throw new ConflictException("Este medico no esta disponible para nuevas citas");
        }

        LocalDate date = request.getAppointmentDate();
        LocalTime time = request.getAppointmentTime();

        assertNotTooSoon(date, time);
        assertSlotAvailable(doctor.getId(), date, time);
        assertPatientFree(patient.getId(), date, time);

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(date);
        appointment.setAppointmentTime(time);
        appointment.setReason(request.getReason());
        appointment.setStatus(AppointmentStatus.PENDIENTE);

        try {
            appointment = appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Ese horario se acaba de ocupar. Elige otro horario disponible.");
        }

        return toResponse(appointment);
    }

    private Patient resolvePatient(Long userId, String role, Long requestedPatientId) {
        return switch (role) {
            case "ROLE_PATIENT" -> {
                Patient own = patientRepository.findByUserId(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Perfil de paciente no encontrado"));
                if (requestedPatientId != null && !requestedPatientId.equals(own.getId())) {
                    throw new ForbiddenOperationException("No puedes agendar citas a nombre de otro paciente");
                }
                yield own;
            }
            case "ROLE_ADMIN", "ROLE_SECRETARY" -> {
                if (requestedPatientId == null) {
                    throw new IllegalArgumentException("patientId es obligatorio al agendar a nombre de un paciente");
                }
                yield patientRepository.findById(requestedPatientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
            }
            default -> throw new ForbiddenOperationException("No tienes permisos para agendar citas");
        };
    }

    public List<AppointmentResponse> getMyHistory(Long patientUserId) {
        return appointmentRepository
                .findByPatientUserIdOrderByAppointmentDateDescAppointmentTimeDesc(patientUserId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Reagendar: paciente dueno de la cita, o ADMIN/SECRETARY. */
    @Transactional
    public AppointmentResponse rescheduleAppointment(Long userId, String role, Long appointmentId,
                                                     RescheduleRequest request) {
        Appointment appointment = findAppointment(appointmentId);

        switch (role) {
            case "ROLE_PATIENT" -> assertPatientOwner(appointment, userId, "Esta cita no te pertenece");
            case "ROLE_ADMIN", "ROLE_SECRETARY" -> { }
            default -> throw new ForbiddenOperationException("No tienes permisos para reagendar citas");
        }

        if (appointment.getStatus() != AppointmentStatus.PENDIENTE) {
            throw new ConflictException("Solo se pueden reagendar citas pendientes");
        }
        if ("ROLE_PATIENT".equals(role)) {
            assertEnoughNotice(appointment, "reagendar");
        }

        LocalDate newDate = request.getAppointmentDate();
        LocalTime newTime = request.getAppointmentTime();

        if (newDate.equals(appointment.getAppointmentDate()) && newTime.equals(appointment.getAppointmentTime())) {
            throw new ConflictException("La cita ya esta agendada en ese horario");
        }

        assertNotTooSoon(newDate, newTime);
        assertSlotAvailable(appointment.getDoctor().getId(), newDate, newTime);
        assertPatientFree(appointment.getPatient().getId(), newDate, newTime);

        appointment.setAppointmentDate(newDate);
        appointment.setAppointmentTime(newTime);

        try {
            appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Ese horario se acaba de ocupar. Elige otro horario disponible.");
        }

        return toResponse(appointment);
    }

    /** Cancelar: paciente dueno, doctor dueno, o ADMIN/SECRETARY. */
    @Transactional
    public AppointmentResponse cancelAppointment(Long userId, String role, Long appointmentId, String reason) {
        Appointment appointment = findAppointment(appointmentId);

        switch (role) {
            case "ROLE_PATIENT" -> assertPatientOwner(appointment, userId, "Esta cita no te pertenece");
            case "ROLE_DOCTOR" -> assertDoctorOwner(appointment, userId, "Esta cita no es tuya");
            case "ROLE_ADMIN", "ROLE_SECRETARY" -> { }
            default -> throw new ForbiddenOperationException("No tienes permisos para cancelar citas");
        }

        if (appointment.getStatus() != AppointmentStatus.PENDIENTE) {
            throw new ConflictException("Solo se pueden cancelar citas pendientes");
        }
        if ("ROLE_PATIENT".equals(role)) {
            assertEnoughNotice(appointment, "cancelar");
        }

        appointment.setStatus(AppointmentStatus.CANCELADA);
        appointment.setCancellationReason(reason == null || reason.isBlank() ? null : reason.trim());

        return toResponse(appointmentRepository.save(appointment));
    }

    // ------------------------------------------------------------------ Personal de la clinica

    /** Agenda general de la clinica en un rango de fechas, opcionalmente de un solo medico. */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getClinicAgenda(LocalDate start, LocalDate end, Long doctorId) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("La fecha final debe ser igual o posterior a la inicial");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_AGENDA_RANGE_DAYS) {
            throw new IllegalArgumentException("El rango maximo es de " + MAX_AGENDA_RANGE_DAYS + " dias");
        }

        return appointmentRepository.findClinicAgenda(start, end, doctorId).stream()
                .map(this::toResponse)
                .toList();
    }

    // ------------------------------------------------------------------ Doctor

    public List<AppointmentResponse> getMyAgenda(Long doctorUserId, LocalDate date) {
        Doctor doctor = doctorRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de medico no encontrado"));

        return appointmentRepository.findByDoctorIdAndAppointmentDate(doctor.getId(), date).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AppointmentResponse> getMyAgendaRange(Long doctorUserId, LocalDate start, LocalDate end) {
        Doctor doctor = doctorRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de medico no encontrado"));

        return appointmentRepository.findByDoctorIdAndAppointmentDateBetween(doctor.getId(), start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DocumentResponse addDiagnosis(Long doctorUserId, Long appointmentId, DocumentRequest request) {
        Appointment appointment = findAppointment(appointmentId);

        assertDoctorOwner(appointment, doctorUserId, "No puedes registrar diagnostico en una cita que no es tuya");

        if (appointment.getStatus() == AppointmentStatus.CANCELADA) {
            throw new ConflictException("No se pueden registrar documentos en una cita cancelada");
        }
        if (appointment.getAppointmentDate().isAfter(LocalDate.now(clock))) {
            throw new ConflictException("No se pueden registrar documentos en una cita que aun no ocurre ("
                    + appointment.getAppointmentDate() + ")");
        }

        Document document = new Document();
        document.setAppointment(appointment);
        document.setType(request.getType());
        document.setContent(request.getContent());
        document = documentRepository.save(document);

        appointment.setStatus(AppointmentStatus.COMPLETADA);
        appointmentRepository.save(appointment);

        return new DocumentResponse(document.getId(), document.getType(), document.getContent(),
                appointment.getId(), document.getFileName());
    }

    /** Documentos de una cita: los puede ver el doctor dueno y el paciente dueno. */
    public List<DocumentResponse> getDocumentsForAppointment(Long userId, Long appointmentId) {
        Appointment appointment = findAppointment(appointmentId);

        boolean isDoctorOwner = appointment.getDoctor().getUser().getId().equals(userId);
        boolean isPatientOwner = appointment.getPatient().getUser().getId().equals(userId);
        if (!isDoctorOwner && !isPatientOwner) {
            throw new ForbiddenOperationException("No tienes acceso a los documentos de esta cita");
        }

        return documentRepository.findByAppointmentId(appointmentId).stream()
                .map(d -> new DocumentResponse(d.getId(), d.getType(), d.getContent(), appointmentId, d.getFileName()))
                .toList();
    }

    // ------------------------------------------------------------------ Reglas privadas

    private Appointment findAppointment(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));
    }

    private void assertPatientOwner(Appointment appointment, Long userId, String message) {
        if (!appointment.getPatient().getUser().getId().equals(userId)) {
            throw new ForbiddenOperationException(message);
        }
    }

    private void assertDoctorOwner(Appointment appointment, Long userId, String message) {
        if (!appointment.getDoctor().getUser().getId().equals(userId)) {
            throw new ForbiddenOperationException(message);
        }
    }

    /** No se agenda en el pasado ni con menos de X minutos de anticipacion. */
    private void assertNotTooSoon(LocalDate date, LocalTime time) {
        LocalDateTime earliest = LocalDateTime.now(clock).plusMinutes(minBookingLeadMinutes);
        if (LocalDateTime.of(date, time).isBefore(earliest)) {
            throw new IllegalArgumentException(
                    "La cita debe agendarse con al menos " + minBookingLeadMinutes + " minutos de anticipacion");
        }
    }

    /** La hora debe ser uno de los horarios libres que calcula el motor de disponibilidad. */
    private void assertSlotAvailable(Long doctorId, LocalDate date, LocalTime time) {
        boolean available = availabilityService.getAvailableSlots(doctorId, date).stream()
                .anyMatch(slot -> slot.getTime().equals(time));
        if (!available) {
            throw new ConflictException(
                    "Ese horario no esta disponible: esta fuera del horario del medico o ya fue reservado");
        }
    }

    private void assertPatientFree(Long patientId, LocalDate date, LocalTime time) {
        boolean busy = appointmentRepository
                .existsByPatientIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
                        patientId, date, time, AppointmentStatus.CANCELADA);
        if (busy) {
            throw new ConflictException("Ya tienes otra cita a esa misma fecha y hora");
        }
    }

    /** El paciente solo puede cancelar/reagendar con X horas de anticipacion. */
    private void assertEnoughNotice(Appointment appointment, String action) {
        LocalDateTime start = LocalDateTime.of(appointment.getAppointmentDate(), appointment.getAppointmentTime());
        if (LocalDateTime.now(clock).plusHours(minCancelHours).isAfter(start)) {
            throw new ConflictException(
                    "Solo puedes " + action + " la cita con al menos " + minCancelHours + " horas de anticipacion");
        }
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getDoctor().getId(),
                a.getDoctor().getUser().getFirstName() + " " + a.getDoctor().getUser().getLastName(),
                a.getDoctor().getSpecialty().getName(),
                a.getPatient().getId(),
                a.getPatient().getUser().getFirstName() + " " + a.getPatient().getUser().getLastName(),
                a.getPatient().getUser().getPhone(),
                a.getAppointmentDate(),
                a.getAppointmentTime(),
                a.getStatus(),
                a.getReason(),
                a.getCancellationReason());
    }
}