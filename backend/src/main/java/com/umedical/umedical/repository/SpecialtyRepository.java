package com.umedical.umedical.repository;

import com.umedical.umedical.entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}

// package com.umedical.umedical.repository;

// import com.umedical.umedical.entity.Specialty;
// import org.springframework.data.jpa.repository.JpaRepository;

// /**
//  * Acceso a datos para el catalogo de especialidades medicas.
//  * No necesita metodos propios: el CRUD basico que trae JpaRepository
//  * (findAll, findById, save, delete) es suficiente para un catalogo simple.
//  */

// public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {
// }