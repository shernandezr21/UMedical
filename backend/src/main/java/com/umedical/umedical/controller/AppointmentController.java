package com.umedical.umedical.controller;

import com.umedical.umedical.dto.AppointmentRequest;
import com.umedical.umedical.dto.AppointmentResponse;
import com.umedical.umedical.dto.CancelAppointmentRequest;
import com.umedical.umedical.dto.DocumentRequest;
import com.umedical.umedical.dto.DocumentResponse;
import com.umedical.umedical.dto.RescheduleRequest;
import com.umedical.umedical.security.CustomUserDetails;
import com.umedical.umedical.service.AppointmentService;
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
@RequestMapping("/api/v1/appointments")
@Tag(name = "Citas", description = "Reserva, reagendado, cancelacion y consulta de citas medicas")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // ---------------------------------------------------------------- Paciente y personal

    @PostMapping
    @Operation(summary = "Crea una cita",
            description = "El paciente agenda para si mismo. ADMIN y SECRETARY agendan a nombre de un paciente enviando patientId. "
                    + "201 si se crea. 400 si los datos son invalidos o hay menos anticipacion de la permitida. "
                    + "403 si un paciente intenta agendar a nombre de otro. "
                    + "409 si el horario no esta disponible o el paciente ya tiene otra cita a esa hora.")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody AppointmentRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.createAppointment(currentUser.getId(), currentUser.getRoleName(), request));
    }

    @GetMapping("/my-history")
    @Operation(summary = "Historial de citas del paciente autenticado (todos los estados, la mas reciente primero)")
    public ResponseEntity<List<AppointmentResponse>> getMyHistory(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(appointmentService.getMyHistory(currentUser.getId()));
    }

    @PutMapping("/{id}/reschedule")
    @Operation(summary = "Reagenda una cita pendiente a otro horario disponible",
            description = "Paciente dueno de la cita (con al menos 2 h de anticipacion), ADMIN o SECRETARY. "
                    + "403 si la cita no es tuya, 409 si no esta pendiente o el nuevo horario no esta disponible.")
    public ResponseEntity<AppointmentResponse> reschedule(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody RescheduleRequest request) {

        return ResponseEntity.ok(
                appointmentService.rescheduleAppointment(currentUser.getId(), currentUser.getRoleName(), id, request));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancela una cita pendiente",
            description = "Paciente dueno (con al menos 2 h de anticipacion), doctor dueno, ADMIN o SECRETARY. "
                    + "El cuerpo es opcional: {\"reason\": \"...\"}. 403 si la cita no es tuya, 409 si no esta pendiente.")
    public ResponseEntity<AppointmentResponse> cancel(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelAppointmentRequest request) {

        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(
                appointmentService.cancelAppointment(currentUser.getId(), currentUser.getRoleName(), id, reason));
    }

    // ---------------------------------------------------------------- Personal de la clinica

    @GetMapping
    @Operation(summary = "Agenda general de la clinica en un rango de fechas (ADMIN y SECRETARY)",
            description = "Todas las citas del rango, opcionalmente de un solo medico (doctorId). Maximo 62 dias.")
    public ResponseEntity<List<AppointmentResponse>> getClinicAgenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long doctorId) {

        return ResponseEntity.ok(appointmentService.getClinicAgenda(startDate, endDate, doctorId));
    }

    // ---------------------------------------------------------------- Doctor

    @GetMapping("/my-agenda")
    @Operation(summary = "Agenda del doctor autenticado en una fecha")
    public ResponseEntity<List<AppointmentResponse>> getMyAgenda(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(appointmentService.getMyAgenda(currentUser.getId(), date));
    }

    @GetMapping("/my-agenda-range")
    @Operation(summary = "Agenda del doctor autenticado en un rango de fechas")
    public ResponseEntity<List<AppointmentResponse>> getMyAgendaRange(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(appointmentService.getMyAgendaRange(currentUser.getId(), startDate, endDate));
    }

    @PutMapping("/{id}/diagnosis")
    @Operation(summary = "El doctor registra un diagnostico o receta en una cita propia",
            description = "Cada llamada agrega un documento nuevo (historial, no se sobrescribe) y marca la cita COMPLETADA. "
                    + "403 si la cita no es tuya, 409 si esta cancelada o aun no ocurre.")
    public ResponseEntity<DocumentResponse> addDiagnosis(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody DocumentRequest request) {

        return ResponseEntity.ok(appointmentService.addDiagnosis(currentUser.getId(), id, request));
    }

    // ---------------------------------------------------------------- Doctor y paciente

    @GetMapping("/{id}/documents")
    @Operation(summary = "Documentos (diagnostico/receta) de una cita",
            description = "Los puede ver el doctor dueno y el paciente dueno de la cita. 403 para cualquier otro usuario.")
    public ResponseEntity<List<DocumentResponse>> getDocuments(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {

        return ResponseEntity.ok(appointmentService.getDocumentsForAppointment(currentUser.getId(), id));
    }

    @PostMapping("/{id}/no-show")
    @Operation(summary = "Marca una cita pendiente que ya empezo como NO_ASISTIO",
            description = "Medico dueno de la cita, SECRETARY o ADMIN. 409 si no esta pendiente o aun no empieza; 403 si no es tuya.")
    public ResponseEntity<AppointmentResponse> markNoShow(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {

        return ResponseEntity.ok(appointmentService.markNoShow(currentUser.getId(), currentUser.getRoleName(), id));
    }
}