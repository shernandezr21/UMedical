package com.umedical.umedical.controller;

import com.umedical.umedical.entity.Specialty;
import com.umedical.umedical.repository.SpecialtyRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specialties")
@Tag(name = "Especialidades", description = "Catalogo de especialidades medicas")
public class SpecialtyController {

    private final SpecialtyRepository specialtyRepository;

    public SpecialtyController(SpecialtyRepository specialtyRepository) {
        this.specialtyRepository = specialtyRepository;
    }

    @GetMapping
    @Operation(summary = "Lista todas las especialidades disponibles")
    public ResponseEntity<List<Specialty>> listSpecialties() {
        return ResponseEntity.ok(specialtyRepository.findAll());
    }
}