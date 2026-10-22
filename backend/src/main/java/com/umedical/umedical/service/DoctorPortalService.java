package com.umedical.umedical.service;

import com.umedical.umedical.dto.DoctorPatientResponse;
import com.umedical.umedical.dto.DoctorStatsResponse;
import com.umedical.umedical.dto.DocumentResponse;
import com.umedical.umedical.dto.PageResponse;
import com.umedical.umedical.dto.PatientRecordResponse;
import com.umedical.umedical.dto.PatientRecordResponse.PatientInfo;
import com.umedical.umedical.dto.PatientRecordResponse.Visit;
import com.umedical.umedical.dto.ReportSummaryResponse.CountItem;
import com.umedical.umedical.dto.ReportSummaryResponse.DayCount;
import com.umedical.umedical.entity.Appointment;
import com.umedical.umedical.entity.Doctor;
import com.umedical.umedical.entity.Document;
import com.umedical.umedical.entity.Patient;
import com.umedical.umedical.entity.User;
import com.umedical.umedical.entity.enumns.AppointmentStatus;
import com.umedical.umedical.exception.ForbiddenOperationException;
import com.umedical.umedical.exception.ResourceNotFoundException;
import com.umedical.umedical.repository.AppointmentRepository;
import com.umedical.umedical.repository.DoctorRepository;
import com.umedical.umedical.repository.DocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Area personal del medico: sus pacientes, el expediente de cada uno y sus estadisticas.
 * Un medico solo ve pacientes con los que tiene (o tuvo) al menos una cita no cancelada, y de ellos solo
 * las consultas que tuvo con el. Cada apertura de un expediente queda en la bitacora de auditoria.
 */
@Service
public class DoctorPortalService {

    private static final long MAX_RANGE_DAYS = 366;
    private static final String[] WEEKDAYS =
            {"Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"};

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final DocumentRepository documentRepository;
    private final AuditService auditService;
    private final Clock clock;

    public DoctorPortalService(DoctorRepository doctorRepository,
                               AppointmentRepository appointmentRepository,
                               DocumentRepository documentRepository,
                               AuditService auditService,
                               Clock clock) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.documentRepository = documentRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<DoctorPatientResponse> listMyPatients(Long doctorUserId, String search, int page, int size) {
        Doctor doctor = findDoctor(doctorUserId);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);

        Page<Object[]> result = appointmentRepository.findPatientsOfDoctor(
                doctor.getId(),
                search == null ? "" : search.trim(),
                AppointmentStatus.COMPLETADA,
                AppointmentStatus.CANCELADA,
                PageRequest.of(safePage, safeSize));

        List<DoctorPatientResponse> content = result.getContent().stream()
                .map(r -> new DoctorPatientResponse(
                        ((Number) r[0]).longValue(),
                        r[1] + " " + r[2],
                        (String) r[3],
                        (String) r[4],
                        ((Number) r[5]).longValue(),
                        ((Number) r[6]).longValue(),
                        (LocalDate) r[7]))
                .toList();

        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PatientRecordResponse getPatientRecord(Long doctorUserId, Long patientId) {
        Doctor doctor = findDoctor(doctorUserId);

        // Se responde 403 tambien si el paciente no existe, para no revelar que ids existen
        if (!appointmentRepository.existsByDoctorIdAndPatientIdAndStatusNot(
                doctor.getId(), patientId, AppointmentStatus.CANCELADA)) {
            throw new ForbiddenOperationException("Este paciente no tiene citas contigo");
        }

        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndPatientIdAndStatusNotOrderByAppointmentDateDescAppointmentTimeDesc(
                        doctor.getId(), patientId, AppointmentStatus.CANCELADA);

        Patient patient = appointments.get(0).getPatient();
        User user = patient.getUser();
        PatientInfo info = new PatientInfo(
                patient.getId(),
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                patient.getBirthDate(),
                patient.getBloodType(),
                patient.getAddress());

        List<Visit> visits = appointments.stream()
                .map(a -> new Visit(
                        a.getId(),
                        a.getAppointmentDate(),
                        a.getAppointmentTime(),
                        a.getStatus(),
                        a.getReason(),
                        documentRepository.findByAppointmentId(a.getId()).stream()
                                .sorted(Comparator.comparing(Document::getId))
                                .map(d -> new DocumentResponse(
                                        d.getId(), d.getType(), d.getContent(), a.getId(), d.getFileName()))
                                .toList()))
                .toList();

        auditService.record(AuditActions.PATIENT_RECORD_VIEWED, "PATIENT", patient.getId(), patient.getId(), null);

        return new PatientRecordResponse(info, visits);
    }

    @Transactional(readOnly = true)
    public DoctorStatsResponse getStats(Long doctorUserId, LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("La fecha final debe ser igual o posterior a la inicial");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("El rango maximo es de " + MAX_RANGE_DAYS + " dias");
        }

        Long doctorId = findDoctor(doctorUserId).getId();

        Map<String, Long> byStatus = new HashMap<>();
        for (Object[] row : appointmentRepository.countByStatusForDoctor(doctorId, start, end)) {
            byStatus.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        // "Pendiente" en la base incluye las citas pasadas que nadie atendio ni marco: aqui se separan
        long unattended = appointmentRepository.countUnattendedForDoctor(
                doctorId, start, end, LocalDate.now(clock), AppointmentStatus.PENDIENTE);
        long pending = byStatus.getOrDefault("PENDIENTE", 0L) - unattended;
        long completed = byStatus.getOrDefault("COMPLETADA", 0L);
        long cancelled = byStatus.getOrDefault("CANCELADA", 0L);
        long noShow = byStatus.getOrDefault("NO_ASISTIO", 0L);
        long total = pending + unattended + completed + cancelled + noShow;

        List<DayCount> byDay = appointmentRepository
                .countByDayForDoctor(doctorId, start, end, AppointmentStatus.CANCELADA).stream()
                .map(r -> new DayCount((LocalDate) r[0], ((Number) r[1]).longValue()))
                .toList();

        long[] perWeekday = new long[7];
        for (DayCount day : byDay) {
            perWeekday[day.getDate().getDayOfWeek().getValue() - 1] += day.getCount();
        }
        List<CountItem> byWeekday = IntStream.range(0, 7)
                .mapToObj(i -> new CountItem(WEEKDAYS[i], perWeekday[i]))
                .toList();

        return new DoctorStatsResponse(
                start, end, total, pending, completed, cancelled, noShow, unattended,
                appointmentRepository.countDistinctPatientsForDoctor(doctorId, start, end, AppointmentStatus.CANCELADA),
                appointmentRepository.countDocumentsForDoctor(doctorId, start, end),
                byDay, byWeekday);
    }

    private Doctor findDoctor(Long doctorUserId) {
        return doctorRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de medico no encontrado"));
    }
}
