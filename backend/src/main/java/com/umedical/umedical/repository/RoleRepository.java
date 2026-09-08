package com.umedical.umedical.repository;

import com.umedical.umedical.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


/**
 * Acceso a datos para el catalogo de roles (ROLE_ADMIN, ROLE_DOCTOR, etc).
 * findByName() se usa al momento de registrar un usuario, para asignarle
 * el rol correcto sin tener que conocer su ID de antemano.
 */

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}