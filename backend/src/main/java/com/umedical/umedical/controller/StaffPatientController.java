package com.umedical.umedical.controller;

import com.umedical.umedical.dto.CreatePatientByStaffRequest;
import com.umedical.umedical.dto.CreatedPatientResponse;
import com.umedical.umedical.dto.PatientSummaryResponse;
import com.umedical.umedical.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Pacientes (personal)", description = "Busqueda y alta de pacientes por secretaria o administrador")
public class StaffPatientController {

    private final PatientService patientService;

    public StaffPatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    @Operation(summary = "Busca pacientes por nombre, usuario, correo o telefono",
            description = "Maximo 20 resultados. Sin texto devuelve los primeros 20 por apellido.")
    public ResponseEntity<List<PatientSummaryResponse>> search(
            @RequestParam(required = false, defaultValue = "") String search) {
        return ResponseEntity.ok(patientService.search(search));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Datos basicos de un paciente por su id")
    public ResponseEntity<PatientSummaryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getSummary(id));
    }

    @PostMapping
    @Operation(summary = "Registra un paciente (alta en recepcion)",
            description = "Genera el usuario a partir del correo y una contrasena temporal que se devuelve UNA sola vez. "
                    + "409 si el correo ya existe.")
    public ResponseEntity<CreatedPatientResponse> create(@Valid @RequestBody CreatePatientByStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.createByStaff(request));
    }
}