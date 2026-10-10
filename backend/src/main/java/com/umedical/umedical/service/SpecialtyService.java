package com.umedical.umedical.service;

import com.umedical.umedical.dto.CreateSpecialtyRequest;
import com.umedical.umedical.entity.Specialty;
import com.umedical.umedical.exception.ResourceNotFoundException;
import com.umedical.umedical.repository.SpecialtyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SpecialtyService {

    private final SpecialtyRepository specialtyRepository;

    public SpecialtyService(SpecialtyRepository specialtyRepository) {
        this.specialtyRepository = specialtyRepository;
    }

    public List<Specialty> findAll() {
        return specialtyRepository.findAll();
    }

    @Transactional
    public Specialty create(CreateSpecialtyRequest request) {
        String name = request.getName().trim();
        if (specialtyRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Ya existe una especialidad con ese nombre");
        }
        Specialty specialty = new Specialty();
        specialty.setName(name);
        specialty.setDescription(cleanDescription(request.getDescription()));
        return specialtyRepository.save(specialty);
    }

    @Transactional
    public Specialty update(Long id, CreateSpecialtyRequest request) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada"));

        String name = request.getName().trim();
        if (specialtyRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("Ya existe otra especialidad con ese nombre");
        }
        specialty.setName(name);
        specialty.setDescription(cleanDescription(request.getDescription()));
        return specialtyRepository.save(specialty);
    }

    private String cleanDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}