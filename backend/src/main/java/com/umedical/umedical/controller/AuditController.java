package com.umedical.umedical.controller;

import com.umedical.umedical.dto.AuditLogResponse;
import com.umedical.umedical.dto.PageResponse;
import com.umedical.umedical.service.AuditActions;
import com.umedical.umedical.service.AuditService;
import com.umedical.umedical.service.CsvExport;
import com.umedical.umedical.service.ExportService;
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
@RequestMapping("/api/v1/audit")
@Tag(name = "Auditoria (admin)", description = "Bitacora de quien hizo que: acceso a informacion clinica y cambios de cuentas")
public class AuditController {

    private final AuditService auditService;
    private final ExportService exportService;

    public AuditController(AuditService auditService, ExportService exportService) {
        this.auditService = auditService;
        this.exportService = exportService;
    }

    @GetMapping
    @Operation(summary = "Consulta la bitacora de auditoria (solo ADMIN)",
            description = "Filtros opcionales: action (p. ej. DOCUMENTS_VIEWED), actor (usuario), patientId y rango de fechas "
                    + "(por defecto los ultimos 7 dias, maximo 366). Paginado: maximo 100 por pagina. Lo mas reciente primero. "
                    + "La bitacora es de solo lectura: no se puede editar ni borrar.")
    public ResponseEntity<PageResponse<AuditLogResponse>> search(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(auditService.search(action, actor, patientId, startDate, endDate, page, size));
    }

    @GetMapping("/export.csv")
    @Operation(summary = "Exporta la bitacora a CSV con los mismos filtros (solo ADMIN, hasta 10.000 filas)")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        CsvExport export = exportService.auditCsv(action, actor, patientId, startDate, endDate);
        auditService.record(AuditActions.REPORT_EXPORTED, "REPORT", null, null,
                "bitacora (" + export.rows() + " filas)");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("bitacora.csv", StandardCharsets.UTF_8).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(export.content());
    }
}
