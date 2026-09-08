package com.umedical.umedical.repository;

import com.umedical.umedical.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acceso a datos para la tabla unificada de usuarios (admins, doctores,
 * pacientes, secretarias). findByUsername() es el que usa el login para
 * validar credenciales. Los existsBy* se usan para validar que un
 * username/email no este duplicado antes de crear un usuario nuevo.
 */

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}