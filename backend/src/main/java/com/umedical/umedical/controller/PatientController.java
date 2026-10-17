package com.umedical.umedical.controller;

import com.umedical.umedical.dto.PatientProfileResponse;
import com.umedical.umedical.dto.UpdatePatientProfileRequest;
import com.umedical.umedical.security.CustomUserDetails;
import com.umedical.umedical.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients/me")
@Tag(name = "Perfil del paciente", description = "El paciente autenticado consulta y edita su propio perfil")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    @Operation(summary = "Mi perfil de paciente")
    public ResponseEntity<PatientProfileResponse> getProfile(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(patientService.getProfile(currentUser.getId()));
    }

    @PutMapping
    @Operation(summary = "Edita mi perfil",
            description = "Reemplaza los datos editables (el username no cambia). "
                    + "409 si el email ya lo usa otro usuario.")
    public ResponseEntity<PatientProfileResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody UpdatePatientProfileRequest request) {

        return ResponseEntity.ok(patientService.updateProfile(currentUser.getId(), request));
    }
}