package com.umedical.umedical.service;

import com.umedical.umedical.dto.AvailableSlotResponse;
import com.umedical.umedical.dto.DayAvailabilityResponse;
import com.umedical.umedical.entity.Appointment;
import com.umedical.umedical.entity.DoctorTimeOff;
import com.umedical.umedical.entity.Schedule;
import com.umedical.umedical.entity.enumns.AppointmentStatus;
import com.umedical.umedical.repository.AppointmentRepository;
import com.umedical.umedical.repository.DoctorTimeOffRepository;
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
 * (citas no canceladas), menos los que ya no cumplen la anticipacion minima y menos los dias en que el
 * medico marco que no atiende (vacaciones, congreso...).
 */
@Service
public class AvailabilityService {

    private static final long MAX_RANGE_DAYS = 62;

    private final ScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorTimeOffRepository timeOffRepository;
    private final Clock clock;
    private final long minBookingLeadMinutes;

    public AvailabilityService(ScheduleRepository scheduleRepository,
                               AppointmentRepository appointmentRepository,
                               DoctorTimeOffRepository timeOffRepository,
                               Clock clock,
                               @Value("${app.appointments.min-booking-lead-minutes:60}") long minBookingLeadMinutes) {
        this.scheduleRepository = scheduleRepository;
        this.appointmentRepository = appointmentRepository;
        this.timeOffRepository = timeOffRepository;
        this.clock = clock;
        this.minBookingLeadMinutes = minBookingLeadMinutes;
    }

    public List<AvailableSlotResponse> getAvailableSlots(Long doctorId, LocalDate date) {
        // Dia bloqueado por el medico: no hay horarios
        if (!timeOffRepository
                .findByDoctorIdAndEndDateGreaterThanEqualAndStartDateLessThanEqual(doctorId, date, date).isEmpty()) {
            return List.of();
        }

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
        List<DoctorTimeOff> timeOffs = timeOffRepository
                .findByDoctorIdAndEndDateGreaterThanEqualAndStartDateLessThanEqual(doctorId, start, end);
        Map<LocalDate, Set<LocalTime>> occupiedByDate = appointmentRepository
                .findByDoctorIdAndAppointmentDateBetween(doctorId, start, end).stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELADA)
                .collect(Collectors.groupingBy(Appointment::getAppointmentDate,
                        Collectors.mapping(Appointment::getAppointmentTime, Collectors.toSet())));

        LocalDateTime earliest = earliestBookable();
        List<DayAvailabilityResponse> result = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            final LocalDate current = day;
            boolean blocked = timeOffs.stream()
                    .anyMatch(t -> !current.isBefore(t.getStartDate()) && !current.isAfter(t.getEndDate()));

            int slots = 0;
            if (!blocked) {
                Set<LocalTime> occupied = occupiedByDate.getOrDefault(current, Set.of());
                slots = buildSlots(schedules, occupied, current, earliest).size();
            }
            result.add(new DayAvailabilityResponse(current, slots));
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