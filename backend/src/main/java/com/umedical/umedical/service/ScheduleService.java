package com.umedical.umedical.service;

import com.umedical.umedical.dto.ScheduleBlockRequest;
import com.umedical.umedical.dto.ScheduleRequest;
import com.umedical.umedical.dto.ScheduleResponse;
import com.umedical.umedical.entity.Doctor;
import com.umedical.umedical.entity.Schedule;
import com.umedical.umedical.exception.ResourceNotFoundException;
import com.umedical.umedical.repository.DoctorRepository;
import com.umedical.umedical.repository.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

/**
 * Reglas de los bloques de horario: la hora de fin debe ser posterior a la
 * de inicio, el slot debe caber dentro del bloque, y dos bloques activos del
 * mismo medico no pueden cruzarse el mismo dia (si se cruzaran, el motor de
 * disponibilidad generaria slots duplicados).
 */
@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;

    public ScheduleService(ScheduleRepository scheduleRepository, DoctorRepository doctorRepository) {
        this.scheduleRepository = scheduleRepository;
        this.doctorRepository = doctorRepository;
    }

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

// package com.umedical.umedical.service;

// import com.umedical.umedical.dto.ScheduleRequest;
// import com.umedical.umedical.dto.ScheduleResponse;
// import com.umedical.umedical.entity.Doctor;
// import com.umedical.umedical.entity.Schedule;
// import com.umedical.umedical.exception.ResourceNotFoundException;
// import com.umedical.umedical.repository.DoctorRepository;
// import com.umedical.umedical.repository.ScheduleRepository;
// import org.springframework.stereotype.Service;

// import java.util.List;

// @Service
// public class ScheduleService {

//     private final ScheduleRepository scheduleRepository;
//     private final DoctorRepository doctorRepository;

//     public ScheduleService(ScheduleRepository scheduleRepository, DoctorRepository doctorRepository) {
//         this.scheduleRepository = scheduleRepository;
//         this.doctorRepository = doctorRepository;
//     }

//     public ScheduleResponse createSchedule(ScheduleRequest request) {

//         Doctor doctor = doctorRepository.findById(request.getDoctorId())
//                 .orElseThrow(() -> new ResourceNotFoundException("Medico no encontrado"));

//         if (!request.getEndTime().isAfter(request.getStartTime())) {
//             throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
//         }

//         Schedule schedule = new Schedule();
//         schedule.setDoctor(doctor);
//         schedule.setDayOfWeek(request.getDayOfWeek());
//         schedule.setStartTime(request.getStartTime());
//         schedule.setEndTime(request.getEndTime());
//         schedule.setSlotDurationMinutes(request.getSlotDurationMinutes());
//         schedule.setActive(true);
//         schedule = scheduleRepository.save(schedule);

//         return toResponse(schedule);
//     }

//     public List<ScheduleResponse> getSchedulesByDoctor(Long doctorId) {
//         return scheduleRepository.findByDoctorIdAndActiveTrue(doctorId).stream()
//                 .map(this::toResponse)
//                 .toList();
//     }

//     private ScheduleResponse toResponse(Schedule s) {
//         return new ScheduleResponse(
//                 s.getId(), s.getDoctor().getId(), s.getDayOfWeek(),
//                 s.getStartTime(), s.getEndTime(), s.getSlotDurationMinutes(), s.isActive());
//     }
// }