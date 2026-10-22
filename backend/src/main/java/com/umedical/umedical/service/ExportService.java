package com.umedical.umedical.service;

import com.umedical.umedical.entity.Appointment;
import com.umedical.umedical.entity.AuditLog;
import com.umedical.umedical.repository.AppointmentRepository;
import com.umedical.umedical.util.CsvWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Exportaciones a CSV: citas de la clinica y bitacora de auditoria. */
@Service
public class ExportService {

    private static final long MAX_RANGE_DAYS = 366;

    private final AppointmentRepository appointmentRepository;
    private final AuditService auditService;

    public ExportService(AppointmentRepository appointmentRepository, AuditService auditService) {
        this.appointmentRepository = appointmentRepository;
        this.auditService = auditService;
    }

    /** Todas las citas del rango (opcionalmente de un solo medico). Maximo 366 dias. */
    @Transactional(readOnly = true)
    public CsvExport appointmentsCsv(LocalDate start, LocalDate end, Long doctorId) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("La fecha final debe ser igual o posterior a la inicial");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("El rango maximo es de " + MAX_RANGE_DAYS + " dias");
        }

        List<Appointment> appointments = appointmentRepository.findClinicAgenda(start, end, doctorId);

        CsvWriter csv = new CsvWriter()
                .row("ID", "Fecha", "Hora", "Médico", "Especialidad", "Paciente", "Teléfono",
                        "Estado", "Motivo", "Motivo de cancelación");

        for (Appointment a : appointments) {
            csv.row(
                    a.getId(),
                    a.getAppointmentDate(),
                    a.getAppointmentTime(),
                    a.getDoctor().getUser().getFirstName() + " " + a.getDoctor().getUser().getLastName(),
                    a.getDoctor().getSpecialty().getName(),
                    a.getPatient().getUser().getFirstName() + " " + a.getPatient().getUser().getLastName(),
                    a.getPatient().getUser().getPhone(),
                    a.getStatus().name(),
                    a.getReason(),
                    a.getCancellationReason());
        }

        return new CsvExport(csv.toBytes(), appointments.size());
    }

    /** La bitacora con los mismos filtros de la pantalla (hasta 10.000 filas). */
    @Transactional(readOnly = true)
    public CsvExport auditCsv(String action, String actor, Long patientId, LocalDate start, LocalDate end) {
        List<AuditLog> entries = auditService.searchForExport(action, actor, patientId, start, end);

        CsvWriter csv = new CsvWriter()
                .row("Fecha y hora", "Usuario", "Rol", "Acción", "Entidad", "ID entidad", "ID paciente", "Detalle", "IP");

        for (AuditLog a : entries) {
            csv.row(a.getOccurredAt(), a.getActorUsername(), a.getActorRole(), a.getAction(),
                    a.getEntityType(), a.getEntityId(), a.getPatientId(), a.getDetails(), a.getIpAddress());
        }

        return new CsvExport(csv.toBytes(), entries.size());
    }
}
