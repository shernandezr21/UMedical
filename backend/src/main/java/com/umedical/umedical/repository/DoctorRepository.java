package com.umedical.umedical.repository;

import com.umedical.umedical.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos para el perfil profesional de los medicos.
 * findBySpecialtyId() alimenta el endpoint GET /doctors?specialtyId=
 * (filtrar medicos por especialidad). findByUserId() sirve para ir del
 * usuario logueado (via JWT) a su perfil de doctor, por ejemplo cuando
 * un doctor consulta su propia agenda.
 */

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findBySpecialtyId(Long specialtyId);
    Optional<Doctor> findByUserId(Long userId);
}