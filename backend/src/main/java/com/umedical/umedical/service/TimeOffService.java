package com.umedical.umedical.service;

import com.umedical.umedical.dto.TimeOffRequest;
import com.umedical.umedical.dto.TimeOffResponse;
import com.umedical.umedical.entity.Appointment;
import com.umedical.umedical.entity.Doctor;
import com.umedical.umedical.entity.DoctorTimeOff;
import com.umedical.umedical.entity.enumns.AppointmentStatus;
import com.umedical.umedical.exception.ConflictException;
import com.umedical.umedical.exception.ForbiddenOperationException;
import com.umedical.umedical.exception.ResourceNotFoundException;
import com.umedical.umedical.repository.AppointmentRepository;
import com.umedical.umedical.repository.DoctorRepository;
import com.umedical.umedical.repository.DoctorTimeOffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Dias en los que el medico no atiende. Mientras un bloqueo esta vigente, el motor de disponibilidad no
 * ofrece horarios en esas fechas. Si ya hay citas pendientes, no se bloquea en silencio: el medico debe
 * autorizar su cancelacion (con un motivo, que ve el paciente) o reagendarlas antes.
 */
@Service
public class TimeOffService {

    private static final long MAX_RANGE_DAYS = 366;

    private final DoctorTimeOffRepository timeOffRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    public TimeOffService(DoctorTimeOffRepository timeOffRepository,
                          DoctorRepository doctorRepository,
                          AppointmentRepository appointmentRepository,
                          Clock clock) {
        this.timeOffRepository = timeOffRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.clock = clock;
    }

    /** Bloqueos vigentes o futuros del medico. */
    @Transactional(readOnly = true)
    public List<TimeOffResponse> listMine(Long doctorUserId) {
        Doctor doctor = findDoctor(doctorUserId);
        return timeOffRepository
                .findByDoctorIdAndEndDateGreaterThanEqualOrderByStartDate(doctor.getId(), LocalDate.now(clock)).stream()
                .map(t -> toResponse(t, 0))
                .toList();
    }

    @Transactional
    public TimeOffResponse create(Long doctorUserId, TimeOffRequest request) {
        Doctor doctor = findDoctor(doctorUserId);
        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        if (start.isBefore(LocalDate.now(clock))) {
            throw new IllegalArgumentException("No se pueden bloquear fechas pasadas");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("La fecha final debe ser igual o posterior a la inicial");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("El rango maximo es de " + MAX_RANGE_DAYS + " dias");
        }

        List<DoctorTimeOff> overlapping = timeOffRepository
                .findByDoctorIdAndEndDateGreaterThanEqualAndStartDateLessThanEqual(doctor.getId(), start, end);
        if (!overlapping.isEmpty()) {
            DoctorTimeOff existing = overlapping.get(0);
            throw new ConflictException("Ya tienes un bloqueo en esas fechas (del "
                    + existing.getStartDate() + " al " + existing.getEndDate() + ")");
        }

        // Citas pendientes que todavia no ocurren dentro del rango
        LocalDateTime now = LocalDateTime.now(clock);
        List<Appointment> affected = appointmentRepository
                .findByDoctorIdAndStatusAndAppointmentDateBetween(doctor.getId(), AppointmentStatus.PENDIENTE, start, end)
                .stream()
                .filter(a -> LocalDateTime.of(a.getAppointmentDate(), a.getAppointmentTime()).isAfter(now))
                .toList();

        String reason = blankToNull(request.getReason());

        if (!affected.isEmpty()) {
            if (!Boolean.TRUE.equals(request.getCancelExisting())) {
                throw new ConflictException("Hay " + affected.size() + " cita(s) pendiente(s) en esas fechas. "
                        + "Reagendalas o cancelalas, o marca la opcion de cancelarlas al bloquear");
            }
            if (reason == null) {
                throw new IllegalArgumentException(
                        "Indica el motivo del bloqueo: se le muestra a los pacientes cuyas citas se cancelan");
            }

            String cancellationReason = truncate("El medico no estara disponible: " + reason, 255);
            for (Appointment appointment : affected) {
                appointment.setStatus(AppointmentStatus.CANCELADA);
                appointment.setCancellationReason(cancellationReason);
            }
            appointmentRepository.saveAll(affected);
        }

        DoctorTimeOff timeOff = new DoctorTimeOff();
        timeOff.setDoctor(doctor);
        timeOff.setStartDate(start);
        timeOff.setEndDate(end);
        timeOff.setReason(reason);
        timeOff = timeOffRepository.save(timeOff);

        return toResponse(timeOff, affected.size());
    }

    @Transactional
    public void delete(Long doctorUserId, Long timeOffId) {
        DoctorTimeOff timeOff = timeOffRepository.findById(timeOffId)
                .orElseThrow(() -> new ResourceNotFoundException("Bloqueo no encontrado"));

        if (!timeOff.getDoctor().getUser().getId().equals(doctorUserId)) {
            throw new ForbiddenOperationException("Este bloqueo no es tuyo");
        }
        if (timeOff.getEndDate().isBefore(LocalDate.now(clock))) {
            throw new ConflictException("Solo se pueden eliminar bloqueos vigentes o futuros");
        }

        timeOffRepository.delete(timeOff);
    }

    private Doctor findDoctor(Long doctorUserId) {
        return doctorRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de medico no encontrado"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private TimeOffResponse toResponse(DoctorTimeOff t, int cancelled) {
        return new TimeOffResponse(t.getId(), t.getStartDate(), t.getEndDate(), t.getReason(), cancelled);
    }
}