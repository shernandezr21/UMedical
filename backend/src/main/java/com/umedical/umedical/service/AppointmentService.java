package com.umedical.umedical.service;

import com.umedical.umedical.dto.AppointmentRequest;
import com.umedical.umedical.dto.AppointmentResponse;
import com.umedical.umedical.entity.Appointment;
import com.umedical.umedical.entity.enumns.AppointmentStatus;
import com.umedical.umedical.entity.Doctor;
import com.umedical.umedical.entity.Patient;
import com.umedical.umedical.exception.ResourceNotFoundException;
import com.umedical.umedical.repository.AppointmentRepository;
import com.umedical.umedical.repository.DoctorRepository;
import com.umedical.umedical.repository.PatientRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umedical.umedical.dto.DocumentRequest;
import com.umedical.umedical.dto.DocumentResponse;
import com.umedical.umedical.entity.Document;
import com.umedical.umedical.repository.DocumentRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Logica de negocio para la reserva de citas. La proteccion contra
 * cruces de horario tiene DOS capas: (1) verificamos en la app que el
 * slot siga libre antes de insertar, y (2) la UNIQUE KEY de la tabla
 * appointments actua como ultima linea de defensa si dos peticiones
 * llegan casi al mismo tiempo (condicion de carrera).
 */
@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DocumentRepository documentRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                            DoctorRepository doctorRepository,
                            PatientRepository patientRepository,
                            DocumentRepository documentRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional
    public AppointmentResponse createAppointment(Long patientUserId, AppointmentRequest request) {

        Patient patient = patientRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de paciente no encontrado"));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Medico no encontrado"));

        // Capa 1: verificacion en la aplicacion
        boolean slotOcupado = appointmentRepository
                .findByDoctorIdAndAppointmentDate(doctor.getId(), request.getAppointmentDate()).stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELADA)
                .anyMatch(a -> a.getAppointmentTime().equals(request.getAppointmentTime()));

        if (slotOcupado) {
            throw new IllegalArgumentException("Ese horario ya fue reservado. Elige otro slot disponible.");
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setReason(request.getReason());
        appointment.setStatus(AppointmentStatus.PENDIENTE);

        try {
            // Capa 2: si dos requests pasaron la validacion de arriba casi
            // al mismo tiempo, la UNIQUE KEY de la BD rechaza el segundo INSERT
            appointment = appointmentRepository.save(appointment);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Ese horario se acaba de ocupar. Elige otro slot disponible.");
        }

        return toResponse(appointment);
    }

    public List<AppointmentResponse> getMyHistory(Long patientUserId) {
        return appointmentRepository
                .findByPatientUserIdOrderByAppointmentDateDescAppointmentTimeDesc(patientUserId).stream()
                .map(this::toResponse)
                .toList();
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getDoctor().getUser().getFirstName() + " " + a.getDoctor().getUser().getLastName(),
                a.getDoctor().getSpecialty().getName(),
                a.getPatient().getUser().getFirstName() + " " + a.getPatient().getUser().getLastName(),
                a.getAppointmentDate(),
                a.getAppointmentTime(),
                a.getStatus(),
                a.getReason());
    }

    @Transactional
    public DocumentResponse addDiagnosis(Long doctorUserId, Long appointmentId, DocumentRequest request) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));

        // Regla de seguridad clave: el doctor autenticado debe ser DUENIO de esta cita
        if (!appointment.getDoctor().getUser().getId().equals(doctorUserId)) {
            throw new IllegalArgumentException("No puedes registrar diagnostico en una cita que no es tuya");
    }

        Document document = new Document();
        document.setAppointment(appointment);
        document.setType(request.getType());
        document.setContent(request.getContent());
        document = documentRepository.save(document);

        appointment.setStatus(AppointmentStatus.COMPLETADA);
        appointmentRepository.save(appointment);

        return new DocumentResponse(document.getId(), document.getType(), document.getContent(), appointment.getId(), document.getFileName());
    }

    // public List<DocumentResponse> getDocumentsByAppointment(Long appointmentId) {
    //     return documentRepository.findByAppointmentId(appointmentId).stream()
    //             .map(d -> new DocumentResponse(d.getId(), d.getType(), d.getContent(), appointmentId))
    //             .toList();
    // }

    public List<DocumentResponse> getDocumentsForAppointment(Long doctorUserId, Long appointmentId) {
    Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));

        if (!appointment.getDoctor().getUser().getId().equals(doctorUserId)) {
            throw new IllegalArgumentException("No puedes ver documentos de una cita que no es tuya");
        }

        return documentRepository.findByAppointmentId(appointmentId).stream()
                .map(d -> new DocumentResponse(d.getId(), d.getType(), d.getContent(), appointmentId, d.getFileName()))
                .toList();
    }

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
}