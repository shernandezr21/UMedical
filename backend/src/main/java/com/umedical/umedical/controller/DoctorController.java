package com.umedical.umedical.controller;

import com.umedical.umedical.dto.CreateDoctorRequest;
import com.umedical.umedical.dto.DoctorDetailResponse;
import com.umedical.umedical.dto.DoctorResponse;
import com.umedical.umedical.dto.SetActiveRequest;
import com.umedical.umedical.dto.UpdateDoctorRequest;
import com.umedical.umedical.entity.Doctor;
import com.umedical.umedical.repository.DoctorRepository;
import com.umedical.umedical.service.AuditActions;
import com.umedical.umedical.service.AuditService;
import com.umedical.umedical.service.DoctorAdminService;
import com.umedical.umedical.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors")
@Tag(name = "Medicos", description = "Listado de medicos activos y administracion de medicos (admin)")
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final DoctorService doctorService;
    private final DoctorAdminService doctorAdminService;
    private final AuditService auditService;

    public DoctorController(DoctorRepository doctorRepository,
                            DoctorService doctorService,
                            DoctorAdminService doctorAdminService,
                            AuditService auditService) {
        this.doctorRepository = doctorRepository;
        this.doctorService = doctorService;
        this.doctorAdminService = doctorAdminService;
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Lista los medicos ACTIVOS, filtrable por especialidad")
    public ResponseEntity<List<DoctorResponse>> listDoctors(@RequestParam(required = false) Long specialtyId) {

        List<Doctor> doctors = (specialtyId != null)
                ? doctorRepository.findBySpecialtyIdAndUserActiveTrue(specialtyId)
                : doctorRepository.findByUserActiveTrue();

        List<DoctorResponse> response = doctors.stream()
                .map(d -> new DoctorResponse(
                        d.getId(),
                        d.getUser().getFirstName() + " " + d.getUser().getLastName(),
                        d.getSpecialty().getName(),
                        d.getLicenseNumber(),
                        d.getBiography()))
                .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Crea un nuevo medico con sus horarios (solo ADMIN)")
    public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.createDoctor(request));
    }

    @GetMapping("/management")
    @Operation(summary = "Listado de administracion: todos los medicos, activos e inactivos (solo ADMIN)")
    public ResponseEntity<List<DoctorDetailResponse>> listForAdmin() {
        return ResponseEntity.ok(doctorAdminService.listAll());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita los datos de un medico (solo ADMIN)",
            description = "409 si el correo o el numero de colegiado ya los usa otro.")
    public ResponseEntity<DoctorDetailResponse> updateDoctor(@PathVariable Long id,
                                                             @Valid @RequestBody UpdateDoctorRequest request) {
        return ResponseEntity.ok(doctorAdminService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Activa o desactiva a un medico (solo ADMIN)",
            description = "409 si se intenta desactivar a un medico con citas pendientes desde hoy.")
    public ResponseEntity<DoctorDetailResponse> setActive(@PathVariable Long id,
                                                          @Valid @RequestBody SetActiveRequest request) {
        DoctorDetailResponse updated = doctorAdminService.setActive(id, request.getActive());
        auditService.record(request.getActive() ? AuditActions.DOCTOR_ACTIVATED : AuditActions.DOCTOR_DEACTIVATED,
                "DOCTOR", id, null, updated.getUsername());
        return ResponseEntity.ok(updated);
    }
}
