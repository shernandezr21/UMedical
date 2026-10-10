package com.umedical.umedical.controller;

import com.umedical.umedical.dto.CreateDoctorRequest;
import com.umedical.umedical.dto.DoctorResponse;
import com.umedical.umedical.entity.Doctor;
import com.umedical.umedical.repository.DoctorRepository;
import com.umedical.umedical.service.DoctorService;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors")
@Tag(name = "Medicos", description = "Listado y filtrado de medicos")
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final DoctorService doctorService;

    public DoctorController(DoctorRepository doctorRepository, DoctorService doctorService) {
        this.doctorRepository = doctorRepository;
        this.doctorService = doctorService;
    }

    @GetMapping
    @Operation(summary = "Lista medicos, filtrable por especialidad")
    public ResponseEntity<List<DoctorResponse>> listDoctors(
            @RequestParam(required = false) Long specialtyId) {

        List<Doctor> doctors = (specialtyId != null)
                ? doctorRepository.findBySpecialtyId(specialtyId)
                : doctorRepository.findAll();

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
    @Operation(summary = "Crea un nuevo medico (usuario + perfil profesional)")
    public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        return ResponseEntity.ok(doctorService.createDoctor(request));
    }
}