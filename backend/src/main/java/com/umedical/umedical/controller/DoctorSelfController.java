package com.umedical.umedical.controller;

import com.umedical.umedical.dto.DoctorPatientResponse;
import com.umedical.umedical.dto.DoctorStatsResponse;
import com.umedical.umedical.dto.PageResponse;
import com.umedical.umedical.dto.PatientRecordResponse;
import com.umedical.umedical.dto.ScheduleBlockRequest;
import com.umedical.umedical.dto.ScheduleResponse;
import com.umedical.umedical.dto.TimeOffRequest;
import com.umedical.umedical.dto.TimeOffResponse;
import com.umedical.umedical.security.CustomUserDetails;
import com.umedical.umedical.service.DoctorPortalService;
import com.umedical.umedical.service.ScheduleService;
import com.umedical.umedical.service.TimeOffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors/me")
@Tag(name = "Medico (area personal)",
        description = "Pacientes, expediente, horarios propios, dias no disponibles y estadisticas del medico autenticado")
public class DoctorSelfController {

    private final DoctorPortalService portalService;
    private final ScheduleService scheduleService;
    private final TimeOffService timeOffService;

    public DoctorSelfController(DoctorPortalService portalService,
                                ScheduleService scheduleService,
                                TimeOffService timeOffService) {
        this.portalService = portalService;
        this.scheduleService = scheduleService;
        this.timeOffService = timeOffService;
    }

    // ---------------------------------------------------------------- Pacientes y expediente

    @GetMapping("/patients")
    @Operation(summary = "Mis pacientes: quienes tienen o tuvieron citas conmigo",
            description = "search: nombre, correo o telefono. Paginado (maximo 50 por pagina). "
                    + "Ordenado por la ultima consulta atendida.")
    public ResponseEntity<PageResponse<DoctorPatientResponse>> myPatients(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        return ResponseEntity.ok(portalService.listMyPatients(currentUser.getId(), search, page, size));
    }

    @GetMapping("/patients/{patientId}/record")
    @Operation(summary = "Expediente de un paciente: sus consultas conmigo con diagnosticos, recetas y PDF",
            description = "403 si el paciente no tiene citas (no canceladas) conmigo.")
    public ResponseEntity<PatientRecordResponse> patientRecord(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long patientId) {

        return ResponseEntity.ok(portalService.getPatientRecord(currentUser.getId(), patientId));
    }

    // ---------------------------------------------------------------- Estadisticas

    @GetMapping("/stats")
    @Operation(summary = "Mis estadisticas en un rango de fechas",
            description = "Maximo 366 dias. Totales por estado, pacientes distintos, documentos registrados, "
                    + "citas por dia y por dia de la semana.")
    public ResponseEntity<DoctorStatsResponse> stats(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(portalService.getStats(currentUser.getId(), startDate, endDate));
    }

    // ---------------------------------------------------------------- Mis horarios

    @GetMapping("/schedules")
    @Operation(summary = "Mis bloques de horario activos")
    public ResponseEntity<List<ScheduleResponse>> mySchedules(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(scheduleService.getMySchedules(currentUser.getId()));
    }

    @PostMapping("/schedules")
    @Operation(summary = "Crea un bloque de horario propio",
            description = "400 si el bloque es invalido o se cruza con otro tuyo.")
    public ResponseEntity<ScheduleResponse> createSchedule(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ScheduleBlockRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduleService.createMySchedule(currentUser.getId(), request));
    }

    @PutMapping("/schedules/{id}")
    @Operation(summary = "Edita un bloque de horario propio",
            description = "403 si el bloque es de otro medico. 409 si dejaria sin horario a citas pendientes.")
    public ResponseEntity<ScheduleResponse> updateSchedule(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody ScheduleBlockRequest request) {

        return ResponseEntity.ok(scheduleService.updateMySchedule(currentUser.getId(), id, request));
    }

    @DeleteMapping("/schedules/{id}")
    @Operation(summary = "Elimina un bloque de horario propio",
            description = "403 si es de otro medico. 409 si hay citas pendientes dentro del bloque.")
    public ResponseEntity<Void> deleteSchedule(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {

        scheduleService.deactivateMySchedule(currentUser.getId(), id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- Dias no disponibles

    @GetMapping("/time-off")
    @Operation(summary = "Mis dias no disponibles vigentes o futuros")
    public ResponseEntity<List<TimeOffResponse>> myTimeOff(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(timeOffService.listMine(currentUser.getId()));
    }

    @PostMapping("/time-off")
    @Operation(summary = "Bloquea un rango de fechas (vacaciones, congreso, ausencia)",
            description = "Esos dias dejan de ofrecer horarios. 400 si hay fechas pasadas o el rango es invalido. "
                    + "409 si se cruza con otro bloqueo o si hay citas pendientes y no se autorizo cancelarlas "
                    + "(cancelExisting=true, con un reason).")
    public ResponseEntity<TimeOffResponse> createTimeOff(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody TimeOffRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(timeOffService.create(currentUser.getId(), request));
    }

    @DeleteMapping("/time-off/{id}")
    @Operation(summary = "Elimina un bloqueo de dias propio (vuelven a ofrecerse horarios)")
    public ResponseEntity<Void> deleteTimeOff(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {

        timeOffService.delete(currentUser.getId(), id);
        return ResponseEntity.noContent().build();
    }
}