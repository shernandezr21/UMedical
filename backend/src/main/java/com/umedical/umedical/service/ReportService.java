package com.umedical.umedical.service;

import com.umedical.umedical.dto.ReportSummaryResponse;
import com.umedical.umedical.dto.ReportSummaryResponse.CountItem;
import com.umedical.umedical.dto.ReportSummaryResponse.DayCount;
import com.umedical.umedical.dto.ReportSummaryResponse.DoctorStat;
import com.umedical.umedical.entity.enumns.AppointmentStatus;
import com.umedical.umedical.repository.AppointmentRepository;
import com.umedical.umedical.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Datos del panel de administracion: citas por estado, especialidad, medico y dia en un rango de fechas. */
@Service
public class ReportService {

    private static final long MAX_RANGE_DAYS = 366;

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public ReportService(AppointmentRepository appointmentRepository, UserRepository userRepository, Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ReportSummaryResponse getSummary(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("La fecha final debe ser igual o posterior a la inicial");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("El rango maximo es de " + MAX_RANGE_DAYS + " dias");
        }

        Map<String, Long> byStatus = new HashMap<>();
        for (Object[] row : appointmentRepository.countByStatus(start, end)) {
            byStatus.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        // "Pendiente" en la base incluye las citas pasadas que nadie atendio ni marco: aqui se separan
        long unattended = appointmentRepository.countUnattended(
                start, end, LocalDate.now(clock), AppointmentStatus.PENDIENTE);
        long pending = byStatus.getOrDefault("PENDIENTE", 0L) - unattended;
        long completed = byStatus.getOrDefault("COMPLETADA", 0L);
        long cancelled = byStatus.getOrDefault("CANCELADA", 0L);
        long noShow = byStatus.getOrDefault("NO_ASISTIO", 0L);
        long total = pending + unattended + completed + cancelled + noShow;

        // Mismo orden que usan las graficas del panel
        List<CountItem> statusItems = List.of(
                new CountItem("Pendientes", pending),
                new CountItem("Completadas", completed),
                new CountItem("Canceladas", cancelled),
                new CountItem("No asistieron", noShow),
                new CountItem("Sin atender", unattended));

        List<CountItem> bySpecialty = appointmentRepository.countBySpecialty(start, end).stream()
                .map(r -> new CountItem((String) r[0], ((Number) r[1]).longValue()))
                .toList();

        List<DoctorStat> byDoctor = appointmentRepository
                .statsByDoctor(start, end, AppointmentStatus.COMPLETADA, AppointmentStatus.CANCELADA).stream()
                .map(r -> new DoctorStat(
                        ((Number) r[0]).longValue(), r[1] + " " + r[2],
                        ((Number) r[3]).longValue(), ((Number) r[4]).longValue(), ((Number) r[5]).longValue()))
                .toList();

        List<DayCount> byDay = appointmentRepository.countByDay(start, end).stream()
                .map(r -> new DayCount((LocalDate) r[0], ((Number) r[1]).longValue()))
                .toList();

        return new ReportSummaryResponse(
                start, end, total, pending, completed, cancelled, noShow, unattended,
                userRepository.countByRoleNameAndActiveTrue("ROLE_DOCTOR"),
                userRepository.countByRoleNameAndActiveTrue("ROLE_PATIENT"),
                statusItems, bySpecialty, byDoctor, byDay);
    }
}
