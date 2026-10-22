package com.umedical.umedical.repository;

import com.umedical.umedical.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** Busqueda con filtros opcionales dentro de un rango de fecha y hora [from, to). Lo mas reciente primero. */
    @Query(value = """
            SELECT a FROM AuditLog a
            WHERE a.occurredAt >= :from AND a.occurredAt < :to
              AND (:action IS NULL OR a.action = :action)
              AND (:actor = '' OR LOWER(a.actorUsername) LIKE LOWER(CONCAT('%', :actor, '%')))
              AND (:patientId IS NULL OR a.patientId = :patientId)
            ORDER BY a.occurredAt DESC, a.id DESC
            """,
            countQuery = """
            SELECT COUNT(a) FROM AuditLog a
            WHERE a.occurredAt >= :from AND a.occurredAt < :to
              AND (:action IS NULL OR a.action = :action)
              AND (:actor = '' OR LOWER(a.actorUsername) LIKE LOWER(CONCAT('%', :actor, '%')))
              AND (:patientId IS NULL OR a.patientId = :patientId)
            """)
    Page<AuditLog> search(@Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          @Param("action") String action,
                          @Param("actor") String actor,
                          @Param("patientId") Long patientId,
                          Pageable pageable);
}
