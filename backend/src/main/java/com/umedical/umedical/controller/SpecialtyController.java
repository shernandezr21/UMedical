package com.umedical.umedical.controller;

import com.umedical.umedical.dto.CreateSpecialtyRequest;
import com.umedical.umedical.entity.Specialty;
import com.umedical.umedical.service.SpecialtyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specialties")
@Tag(name = "Especialidades", description = "Catalogo de especialidades medicas")
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    public SpecialtyController(SpecialtyService specialtyService) {
        this.specialtyService = specialtyService;
    }

    @GetMapping
    @Operation(summary = "Lista todas las especialidades disponibles")
    public ResponseEntity<List<Specialty>> listSpecialties() {
        return ResponseEntity.ok(specialtyService.findAll());
    }

    @PostMapping
    @Operation(summary = "Crea una nueva especialidad (solo ADMIN)")
    public ResponseEntity<Specialty> createSpecialty(@Valid @RequestBody CreateSpecialtyRequest request) {
        return ResponseEntity.ok(specialtyService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita una especialidad existente (solo ADMIN)")
    public ResponseEntity<Specialty> updateSpecialty(@PathVariable Long id,
                                                     @Valid @RequestBody CreateSpecialtyRequest request) {
        return ResponseEntity.ok(specialtyService.update(id, request));
    }
}

// package com.umedical.umedical.controller;

// import com.umedical.umedical.dto.CreateSpecialtyRequest;
// import com.umedical.umedical.entity.Specialty;
// import com.umedical.umedical.repository.SpecialtyRepository;
// import io.swagger.v3.oas.annotations.Operation;
// import io.swagger.v3.oas.annotations.tags.Tag;
// import jakarta.validation.Valid;

// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;
// import org.springframework.web.bind.annotation.RequestBody;

// import java.util.List;

// @RestController
// @RequestMapping("/api/v1/specialties")
// @Tag(name = "Especialidades", description = "Catalogo de especialidades medicas")
// public class SpecialtyController {

//     private final SpecialtyRepository specialtyRepository;

//     public SpecialtyController(SpecialtyRepository specialtyRepository) {
//         this.specialtyRepository = specialtyRepository;
//     }

//     @GetMapping
//     @Operation(summary = "Lista todas las especialidades disponibles")
//     public ResponseEntity<List<Specialty>> listSpecialties() {
//         return ResponseEntity.ok(specialtyRepository.findAll());
//     }

//     @PostMapping
//     @Operation(summary = "Crea una nueva especialidad (solo ADMIN)")
//     public ResponseEntity<Specialty> createSpecialty(@Valid @RequestBody CreateSpecialtyRequest request) {
//         Specialty specialty = new Specialty();
//         specialty.setName(request.getName());
//         specialty.setDescription(request.getDescription());
//         return ResponseEntity.ok(specialtyRepository.save(specialty));
//     }
// }