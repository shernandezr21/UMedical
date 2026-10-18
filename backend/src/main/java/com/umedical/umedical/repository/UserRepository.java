package com.umedical.umedical.repository;

import com.umedical.umedical.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    long countByRoleNameAndActiveTrue(String roleName);

    /** Busqueda para el admin: texto libre (nombre, usuario, correo), rol y estado opcionales. */
    @Query("""
            SELECT u FROM User u
            WHERE (:role IS NULL OR u.role.name = :role)
              AND (:active IS NULL OR u.active = :active)
              AND (:query = ''
                   OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY u.lastName, u.firstName
            """)
    Page<User> search(@Param("query") String query,
                      @Param("role") String role,
                      @Param("active") Boolean active,
                      Pageable pageable);
}