package com.umedical.umedical.repository;

import com.umedical.umedical.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findBySpecialtyId(Long specialtyId);

    Optional<Doctor> findByUserId(Long userId);

    // Listado publico: solo medicos con la cuenta activa
    List<Doctor> findByUserActiveTrue();

    List<Doctor> findBySpecialtyIdAndUserActiveTrue(Long specialtyId);

    boolean existsByLicenseNumberAndIdNot(String licenseNumber, Long id);

    /** Listado de administracion: todos los medicos (activos e inactivos) con su usuario y especialidad. */
    @Query("""
            SELECT d FROM Doctor d JOIN FETCH d.user u JOIN FETCH d.specialty
            ORDER BY u.lastName, u.firstName
            """)
    List<Doctor> findAllWithDetails();
}