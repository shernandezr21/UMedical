package com.umedical.umedical.repository;

import com.umedical.umedical.entity.Patient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUserId(Long userId);

    /** Busca por nombre completo, usuario, correo o telefono, sin distinguir mayusculas. Solo cuentas activas. */
    @Query("""
            SELECT p FROM Patient p JOIN FETCH p.user u
            WHERE u.active = true
              AND (LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR u.phone LIKE CONCAT('%', :query, '%'))
            ORDER BY u.lastName, u.firstName
            """)
    List<Patient> search(@Param("query") String query, Pageable pageable);
}