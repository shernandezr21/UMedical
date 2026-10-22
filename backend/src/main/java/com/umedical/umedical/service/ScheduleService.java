package com.umedical.umedical.service;

import com.umedical.umedical.dto.ScheduleBlockRequest;
import com.umedical.umedical.dto.ScheduleRequest;
import com.umedical.umedical.dto.ScheduleResponse;
import com.umedical.umedical.entity.Appointment;
import com.umedical.umedical.entity.Doctor;
import com.umedical.umedical.entity.Schedule;
import com.umedical.umedical.entity.enumns.AppointmentStatus;
import com.umedical.umedical.exception.ConflictException;
import com.umedical.umedical.exception.ForbiddenOperationException;
import com.umedical.umedical.exception.ResourceNotFoundException;
import com.umedical.umedical.repository.AppointmentRepository;
import com.umedical.umedical.repository.DoctorRepository;
import com.umedical.umedical.repository.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Reglas de los bloques de horario: la hora de fin debe ser posterior a la de inicio, el slot debe caber
 * dentro del bloque, dos bloques activos del mismo medico no pueden cruzarse el mismo dia, y no se puede
 * editar ni eliminar un bloque si eso dejaria sin horario a una cita pendiente.
 * El administrador gestiona los bloques de cualquier medico; cada medico gestiona los suyos (metodos "My").
 */
@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    public ScheduleService(ScheduleRepository scheduleRepository,
                           DoctorRepository doctorRepository,
                           AppointmentRepository appointmentRepository,
                           Clock clock) {
        this.scheduleRepository = scheduleRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ Administrador

    @Transactional
    public ScheduleResponse createSchedule(ScheduleRequest request) {
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Medico no encontrado"));

        validateBlock(request);

        for (Schedule existing : scheduleRepository.findByDoctorIdAndActiveTrue(doctor.getId())) {
            if (existing.getDayOfWeek().equals(request.getDayOfWeek())
                    && overlaps(existing.getStartTime(), existing.getEndTime(),
                                request.getStartTime(), request.getEndTime())) {
                throw new IllegalArgumentException("El bloque se cruza con un horario ya existente de este medico");
            }
        }

        return toResponse(saveBlock(doctor, request));
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long scheduleId, ScheduleBlockRequest request) {
        Schedule schedule = findActive(scheduleId);
        validateBlock(request);

        for (Schedule other : scheduleRepository.findByDoctorIdAndActiveTrue(schedule.getDoctor().getId())) {
            if (!other.getId().equals(scheduleId)
                    && other.getDayOfWeek().equals(request.getDayOfWeek())
                    && overlaps(other.getStartTime(), other.getEndTime(),
                                request.getStartTime(), request.getEndTime())) {
                throw new IllegalArgumentException("El bloque se cruza con otro horario de este medico");
            }
        }

        long orphaned = countOrphanedAppointments(schedule, request);
        if (orphaned > 0) {
            throw new ConflictException("Hay " + orphaned + " cita(s) pendiente(s) en este horario que quedarian "
                    + "fuera del nuevo bloque. Reagendalas o cancelalas primero");
        }

        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setSlotDurationMinutes(request.getSlotDurationMinutes());
        return toResponse(scheduleRepository.save(schedule));
    }

    /** Elimina el bloque (baja logica: queda inactivo). */
    @Transactional
    public void deactivateSchedule(Long scheduleId) {
        Schedule schedule = findActive(scheduleId);

        long orphaned = countOrphanedAppointments(schedule, null);
        if (orphaned > 0) {
            throw new ConflictException("Hay " + orphaned + " cita(s) pendiente(s) en este horario. "
                    + "Reagendalas o cancelalas antes de eliminarlo");
        }

        schedule.setActive(false);
        scheduleRepository.save(schedule);
    }

    // ------------------------------------------------------------------ Medico: sus propios bloques

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getMySchedules(Long doctorUserId) {
        return getSchedulesByDoctor(findDoctorByUser(doctorUserId).getId());
    }

    @Transactional
    public ScheduleResponse createMySchedule(Long doctorUserId, ScheduleBlockRequest block) {
        Doctor doctor = findDoctorByUser(doctorUserId);

        ScheduleRequest request = new ScheduleRequest();
        request.setDoctorId(doctor.getId());
        request.setDayOfWeek(block.getDayOfWeek());
        request.setStartTime(block.getStartTime());
        request.setEndTime(block.getEndTime());
        request.setSlotDurationMinutes(block.getSlotDurationMinutes());
        return createSchedule(request);
    }

    @Transactional
    public ScheduleResponse updateMySchedule(Long doctorUserId, Long scheduleId, ScheduleBlockRequest request) {
        assertOwnedBy(doctorUserId, findActive(scheduleId));
        return updateSchedule(scheduleId, request);
    }

    @Transactional
    public void deactivateMySchedule(Long doctorUserId, Long scheduleId) {
        assertOwnedBy(doctorUserId, findActive(scheduleId));
        deactivateSchedule(scheduleId);
    }

    // ------------------------------------------------------------------ Compartido con el alta de medicos

    /** Valida una lista de bloques ANTES de escribir nada en la base. */
    public void validateBlocks(List<? extends ScheduleBlockRequest> blocks) {
        for (ScheduleBlockRequest block : blocks) {
            validateBlock(block);
        }
        for (int i = 0; i < blocks.size(); i++) {
            for (int j = i + 1; j < blocks.size(); j++) {
                ScheduleBlockRequest a = blocks.get(i);
                ScheduleBlockRequest b = blocks.get(j);
                if (a.getDayOfWeek().equals(b.getDayOfWeek())
                        && overlaps(a.getStartTime(), a.getEndTime(), b.getStartTime(), b.getEndTime())) {
                    throw new IllegalArgumentException("Dos bloques del mismo dia se cruzan entre si");
                }
            }
        }
    }

    public Schedule saveBlock(Doctor doctor, ScheduleBlockRequest block) {
        Schedule schedule = new Schedule();
        schedule.setDoctor(doctor);
        schedule.setDayOfWeek(block.getDayOfWeek());
        schedule.setStartTime(block.getStartTime());
        schedule.setEndTime(block.getEndTime());
        schedule.setSlotDurationMinutes(block.getSlotDurationMinutes());
        schedule.setActive(true);
        return scheduleRepository.save(schedule);
    }

    public List<ScheduleResponse> getSchedulesByDoctor(Long doctorId) {
        return scheduleRepository.findByDoctorIdAndActiveTrue(doctorId).stream()
                .map(this::toResponse)
                .toList();
    }

    // ------------------------------------------------------------------ Privados

    private Doctor findDoctorByUser(Long doctorUserId) {
        return doctorRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de medico no encontrado"));
    }

    private void assertOwnedBy(Long doctorUserId, Schedule schedule) {
        if (!schedule.getDoctor().getUser().getId().equals(doctorUserId)) {
            throw new ForbiddenOperationException("Este horario no es tuyo");
        }
    }

    private Schedule findActive(Long id) {
        return scheduleRepository.findById(id)
                .filter(Schedule::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado"));
    }

    /**
     * Citas pendientes desde hoy que estan dentro del bloque actual y quedarian fuera del reemplazo
     * (o de cualquier bloque, si se elimina: replacement == null).
     */
    private long countOrphanedAppointments(Schedule current, ScheduleBlockRequest replacement) {
        Long doctorId = current.getDoctor().getId();

        return appointmentRepository.findByDoctorIdAndStatusAndAppointmentDateGreaterThanEqual(
                        doctorId, AppointmentStatus.PENDIENTE, LocalDate.now(clock)).stream()
                .filter(a -> isInside(a, current.getDayOfWeek(), current.getStartTime(), current.getEndTime()))
                .filter(a -> replacement == null
                        || !isInside(a, replacement.getDayOfWeek(), replacement.getStartTime(), replacement.getEndTime()))
                .count();
    }

    private boolean isInside(Appointment appointment, Integer dayOfWeek, LocalTime start, LocalTime end) {
        return appointment.getAppointmentDate().getDayOfWeek().getValue() == dayOfWeek
                && !appointment.getAppointmentTime().isBefore(start)
                && appointment.getAppointmentTime().isBefore(end);
    }

    private void validateBlock(ScheduleBlockRequest block) {
        if (!block.getEndTime().isAfter(block.getStartTime())) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }
        long minutes = Duration.between(block.getStartTime(), block.getEndTime()).toMinutes();
        if (block.getSlotDurationMinutes() > minutes) {
            throw new IllegalArgumentException("La duracion del slot no cabe dentro del bloque de horario");
        }
    }

    private boolean overlaps(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private ScheduleResponse toResponse(Schedule s) {
        return new ScheduleResponse(
                s.getId(), s.getDoctor().getId(), s.getDayOfWeek(),
                s.getStartTime(), s.getEndTime(), s.getSlotDurationMinutes(), s.isActive());
    }
}