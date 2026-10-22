package com.umedical.umedical.controller;

import com.umedical.umedical.dto.ReportSummaryResponse;
import com.umedical.umedical.service.AuditActions;
import com.umedical.umedical.service.AuditService;
import com.umedical.umedical.service.CsvExport;
import com.umedical.umedical.service.ExportService;
import com.umedical.umedical.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reportes (admin)", description = "Datos del panel de administracion y exportaciones")
public class ReportController {

    private final ReportService reportService;
    private final ExportService exportService;
    private final AuditService auditService;

    public ReportController(ReportService reportService, ExportService exportService, AuditService auditService) {
        this.reportService = reportService;
        this.exportService = exportService;
        this.auditService = auditService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Resumen de citas en un rango: por estado, especialidad, medico y dia",
            description = "Maximo 366 dias. 400 si el rango es mayor o la fecha final es anterior a la inicial.")
    public ResponseEntity<ReportSummaryResponse> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(reportService.getSummary(startDate, endDate));
    }

    @GetMapping("/appointments.csv")
    @Operation(summary = "Exporta las citas de un rango a CSV (solo ADMIN)",
            description = "Columnas: ID, fecha, hora, medico, especialidad, paciente, telefono, estado, motivo y motivo de "
                    + "cancelacion. Opcionalmente de un solo medico (doctorId). Maximo 366 dias. UTF-8 con BOM para Excel. "
                    + "Cada exportacion queda registrada en la bitacora.")
    public ResponseEntity<byte[]> exportAppointments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long doctorId) {

        CsvExport export = exportService.appointmentsCsv(startDate, endDate, doctorId);
        auditService.record(AuditActions.REPORT_EXPORTED, "REPORT", null, null,
                "citas " + startDate + " a " + endDate + " (" + export.rows() + " filas)");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("citas_" + startDate + "_" + endDate + ".csv", StandardCharsets.UTF_8).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(export.content());
    }
}
