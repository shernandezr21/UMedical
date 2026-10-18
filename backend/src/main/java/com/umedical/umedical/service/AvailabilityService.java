package com.umedical.umedical.service;

import com.umedical.umedical.dto.AvailableSlotResponse;
import com.umedical.umedical.dto.DayAvailabilityResponse;
import com.umedical.umedical.entity.Appointment;
import com.umedical.umedical.entity.Schedule;
import com.umedical.umedical.entity.enumns.AppointmentStatus;
import com.umedical.umedical.repository.AppointmentRepository;
import com.umedical.umedical.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Horarios que realmente se pueden reservar: bloques del medico divididos en slots, menos los ocupados
 * (citas no canceladas) y menos los que ya no cumplen la anticipacion minima (p. ej. los de hoy que ya pasaron).
 */
@Service
public class AvailabilityService {

    private static final long MAX_RANGE_DAYS = 62;

    private final ScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;
    private final long minBookingLeadMinutes;

    public AvailabilityService(ScheduleRepository scheduleRepository,
                               AppointmentRepository appointmentRepository,
                               Clock clock,
                               @Value("${app.appointments.min-booking-lead-minutes:60}") long minBookingLeadMinutes) {
        this.scheduleRepository = scheduleRepository;
        this.appointmentRepository = appointmentRepository;
        this.clock = clock;
        this.minBookingLeadMinutes = minBookingLeadMinutes;
    }

    public List<AvailableSlotResponse> getAvailableSlots(Long doctorId, LocalDate date) {
        List<Schedule> schedules = scheduleRepository.findByDoctorIdAndActiveTrue(doctorId);
        Set<LocalTime> occupied = occupiedTimes(appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, date));
        return buildSlots(schedules, occupied, date, earliestBookable());
    }

    /** Cuantos horarios libres tiene cada dia del rango (para pintar un calendario). */
    public List<DayAvailabilityResponse> getAvailabilityRange(Long doctorId, LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("La fecha final debe ser igual o posterior a la inicial");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("El rango maximo es de " + MAX_RANGE_DAYS + " dias");
        }

        List<Schedule> schedules = scheduleRepository.findByDoctorIdAndActiveTrue(doctorId);
        Map<LocalDate, Set<LocalTime>> occupiedByDate = appointmentRepository
                .findByDoctorIdAndAppointmentDateBetween(doctorId, start, end).stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELADA)
                .collect(Collectors.groupingBy(Appointment::getAppointmentDate,
                        Collectors.mapping(Appointment::getAppointmentTime, Collectors.toSet())));

        LocalDateTime earliest = earliestBookable();
        List<DayAvailabilityResponse> result = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            Set<LocalTime> occupied = occupiedByDate.getOrDefault(day, Set.of());
            result.add(new DayAvailabilityResponse(day, buildSlots(schedules, occupied, day, earliest).size()));
        }
        return result;
    }

    private List<AvailableSlotResponse> buildSlots(List<Schedule> schedules, Set<LocalTime> occupied,
                                                   LocalDate date, LocalDateTime earliest) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        List<AvailableSlotResponse> slots = new ArrayList<>();

        for (Schedule schedule : schedules) {
            if (schedule.getDayOfWeek() != dayOfWeek) {
                continue;
            }

            int duration = schedule.getSlotDurationMinutes();
            LocalTime current = schedule.getStartTime();

            while (true) {
                LocalTime next = current.plusMinutes(duration);
                // Se acabo el bloque, o la suma dio la vuelta pasada la medianoche
                if (next.isAfter(schedule.getEndTime()) || next.isBefore(current)) {
                    break;
                }
                boolean bookable = !LocalDateTime.of(date, current).isBefore(earliest);
                if (bookable && !occupied.contains(current)) {
                    slots.add(new AvailableSlotResponse(date, current));
                }
                current = next;
            }
        }

        slots.sort(Comparator.comparing(AvailableSlotResponse::getTime));
        return slots;
    }

    private Set<LocalTime> occupiedTimes(List<Appointment> appointments) {
        return appointments.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELADA)
                .map(Appointment::getAppointmentTime)
                .collect(Collectors.toSet());
    }

    private LocalDateTime earliestBookable() {
        return LocalDateTime.now(clock).plusMinutes(minBookingLeadMinutes);
    }
}

// package com.umedical.umedical.service;

// import com.umedical.umedical.dto.AvailableSlotResponse;
// import com.umedical.umedical.entity.Appointment;
// import com.umedical.umedical.entity.enumns.AppointmentStatus;
// import com.umedical.umedical.entity.Schedule;
// import com.umedical.umedical.repository.AppointmentRepository;
// import com.umedical.umedical.repository.ScheduleRepository;
// import org.springframework.stereotype.Service;

// import java.time.DayOfWeek;
// import java.time.LocalDate;
// import java.time.LocalTime;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Set;
// import java.util.stream.Collectors;

// /**
//  * Calcula los horarios disponibles de un medico en una fecha especifica.
//  * Disponibilidad = bloques configurados en schedules para ese dia de la
//  * semana, divididos en slots segun slot_duration_minutes, MENOS los slots
//  * que ya tienen una cita activa (no cancelada) ese mismo dia.
//  */
// @Service
// public class AvailabilityService {

//     private final ScheduleRepository scheduleRepository;
//     private final AppointmentRepository appointmentRepository;

//     public AvailabilityService(ScheduleRepository scheduleRepository,
//                                 AppointmentRepository appointmentRepository) {
//         this.scheduleRepository = scheduleRepository;
//         this.appointmentRepository = appointmentRepository;
//     }

//     public List<AvailableSlotResponse> getAvailableSlots(Long doctorId, LocalDate date) {

//         // 1. Convertir el dia de la semana de Java (MONDAY=1...SUNDAY=7,
//         // que coincide justo con nuestra convencion en la BD) al entero
//         // que guardamos en schedules.day_of_week
//         int dayOfWeekValue = date.getDayOfWeek().getValue();

//         List<Schedule> schedulesForDay = scheduleRepository.findByDoctorIdAndActiveTrue(doctorId).stream()
//                 .filter(s -> s.getDayOfWeek() == dayOfWeekValue)
//                 .toList();

//         if (schedulesForDay.isEmpty()) {
//             return List.of(); // el doctor no atiende ese dia de la semana
//         }

//         // 2. Horas ya ocupadas ese dia (cualquier cita que no este cancelada)
//         Set<LocalTime> occupiedTimes = appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, date).stream()
//                 .filter(a -> a.getStatus() != AppointmentStatus.CANCELADA)
//                 .map(Appointment::getAppointmentTime)
//                 .collect(Collectors.toSet());

//         // 3. Generar los slots de cada bloque y descartar los ocupados
//         List<AvailableSlotResponse> availableSlots = new ArrayList<>();

//         for (Schedule schedule : schedulesForDay) {
//             LocalTime current = schedule.getStartTime();
//             while (current.plusMinutes(schedule.getSlotDurationMinutes()).compareTo(schedule.getEndTime()) <= 0) {
//                 if (!occupiedTimes.contains(current)) {
//                     availableSlots.add(new AvailableSlotResponse(date, current));
//                 }
//                 current = current.plusMinutes(schedule.getSlotDurationMinutes());
//             }
//         }

//         return availableSlots;
//     }
// }