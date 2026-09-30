package com.umedical.umedical.service;

import com.umedical.umedical.dto.AvailableSlotResponse;
import com.umedical.umedical.entity.Appointment;
import com.umedical.umedical.entity.enumns.AppointmentStatus;
import com.umedical.umedical.entity.Schedule;
import com.umedical.umedical.repository.AppointmentRepository;
import com.umedical.umedical.repository.ScheduleRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calcula los horarios disponibles de un medico en una fecha especifica.
 * Disponibilidad = bloques configurados en schedules para ese dia de la
 * semana, divididos en slots segun slot_duration_minutes, MENOS los slots
 * que ya tienen una cita activa (no cancelada) ese mismo dia.
 */
@Service
public class AvailabilityService {

    private final ScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;

    public AvailabilityService(ScheduleRepository scheduleRepository,
                                AppointmentRepository appointmentRepository) {
        this.scheduleRepository = scheduleRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public List<AvailableSlotResponse> getAvailableSlots(Long doctorId, LocalDate date) {

        // 1. Convertir el dia de la semana de Java (MONDAY=1...SUNDAY=7,
        // que coincide justo con nuestra convencion en la BD) al entero
        // que guardamos en schedules.day_of_week
        int dayOfWeekValue = date.getDayOfWeek().getValue();

        List<Schedule> schedulesForDay = scheduleRepository.findByDoctorIdAndActiveTrue(doctorId).stream()
                .filter(s -> s.getDayOfWeek() == dayOfWeekValue)
                .toList();

        if (schedulesForDay.isEmpty()) {
            return List.of(); // el doctor no atiende ese dia de la semana
        }

        // 2. Horas ya ocupadas ese dia (cualquier cita que no este cancelada)
        Set<LocalTime> occupiedTimes = appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, date).stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELADA)
                .map(Appointment::getAppointmentTime)
                .collect(Collectors.toSet());

        // 3. Generar los slots de cada bloque y descartar los ocupados
        List<AvailableSlotResponse> availableSlots = new ArrayList<>();

        for (Schedule schedule : schedulesForDay) {
            LocalTime current = schedule.getStartTime();
            while (current.plusMinutes(schedule.getSlotDurationMinutes()).compareTo(schedule.getEndTime()) <= 0) {
                if (!occupiedTimes.contains(current)) {
                    availableSlots.add(new AvailableSlotResponse(date, current));
                }
                current = current.plusMinutes(schedule.getSlotDurationMinutes());
            }
        }

        return availableSlots;
    }
}