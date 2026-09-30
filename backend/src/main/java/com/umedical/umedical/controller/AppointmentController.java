package com.umedical.umedical.controller;

import com.umedical.umedical.dto.AppointmentRequest;
import com.umedical.umedical.dto.AppointmentResponse;
import com.umedical.umedical.dto.DocumentRequest;
import com.umedical.umedical.dto.DocumentResponse;
import com.umedical.umedical.dto.DocumentRequest;
import com.umedical.umedical.dto.DocumentResponse;
import com.umedical.umedical.security.CustomUserDetails;
import com.umedical.umedical.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Citas", description = "Reserva y consulta de citas medicas")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @Operation(summary = "Crea una nueva cita para el paciente autenticado")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody AppointmentRequest request) {

        AppointmentResponse response = appointmentService.createAppointment(currentUser.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-history")
    @Operation(summary = "Historial de citas del paciente autenticado")
    public ResponseEntity<List<AppointmentResponse>> getMyHistory(
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(appointmentService.getMyHistory(currentUser.getId()));
    }

        @PutMapping("/{id}/diagnosis")
    @Operation(summary = "El doctor registra un diagnostico o receta para una cita propia")
    public ResponseEntity<DocumentResponse> addDiagnosis(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody DocumentRequest request) {

        DocumentResponse response = appointmentService.addDiagnosis(currentUser.getId(), id, request);
        return ResponseEntity.ok(response);
    }



}