package com.umedical.umedical.repository;

import com.umedical.umedical.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acceso a datos para el perfil de pacientes (fecha de nacimiento,
 * direccion, tipo de sangre). findByUserId() sirve para ir del usuario
 * logueado (via JWT) a su perfil de paciente, por ejemplo al reservar
 * una cita o consultar su propio historial.
 */

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByUserId(Long userId);
}