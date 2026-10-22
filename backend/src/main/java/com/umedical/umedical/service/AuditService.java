package com.umedical.umedical.service;

import com.umedical.umedical.dto.AuditLogResponse;
import com.umedical.umedical.dto.PageResponse;
import com.umedical.umedical.entity.AuditLog;
import com.umedical.umedical.repository.AuditLogRepository;
import com.umedical.umedical.security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Bitacora de auditoria: quien hizo que y cuando. Los registros se escriben en una transaccion PROPIA:
 * asi un fallo de la bitacora nunca deshace la operacion de negocio, y un registro de "consulto" se
 * conserva aunque la operacion principal despues falle.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final long MAX_RANGE_DAYS = 366;
    private static final int MAX_EXPORT_ROWS = 10_000;

    private final AuditLogRepository repository;
    private final Clock clock;
    private final TransactionTemplate ownTransaction;

    public AuditService(AuditLogRepository repository, Clock clock, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.clock = clock;
        this.ownTransaction = new TransactionTemplate(transactionManager);
        this.ownTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Registra una accion del usuario autenticado. Nunca lanza excepcion: si falla, queda en el log del servidor. */
    public void record(String action, String entityType, Long entityId, Long patientId, String details) {
        try {
            AuditLog entry = new AuditLog();
            entry.setOccurredAt(LocalDateTime.now(clock));
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setPatientId(patientId);
            entry.setDetails(truncate(details, 500));
            entry.setIpAddress(currentIp());

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails user) {
                entry.setActorUserId(user.getId());
                entry.setActorUsername(user.getUsername());
                entry.setActorRole(user.getRoleName());
            } else {
                entry.setActorUsername("desconocido");
                entry.setActorRole("N/A");
            }

            ownTransaction.executeWithoutResult(status -> repository.save(entry));
        } catch (Exception e) {
            log.error("No se pudo registrar la auditoria de {}", action, e);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(String action, String actor, Long patientId,
                                                 LocalDate start, LocalDate end, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);

        Page<AuditLog> result = query(action, actor, patientId, start, end, PageRequest.of(safePage, safeSize));

        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    /** Hasta 10.000 filas, para exportar a CSV. */
    @Transactional(readOnly = true)
    public List<AuditLog> searchForExport(String action, String actor, Long patientId, LocalDate start, LocalDate end) {
        return query(action, actor, patientId, start, end, PageRequest.of(0, MAX_EXPORT_ROWS)).getContent();
    }

    private Page<AuditLog> query(String action, String actor, Long patientId,
                                 LocalDate start, LocalDate end, PageRequest pageRequest) {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = start != null ? start : today.minusDays(6);
        LocalDate to = end != null ? end : today;

        if (to.isBefore(from)) {
            throw new IllegalArgumentException("La fecha final debe ser igual o posterior a la inicial");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("El rango maximo es de " + MAX_RANGE_DAYS + " dias");
        }

        return repository.search(
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                action == null || action.isBlank() ? null : action.trim(),
                actor == null ? "" : actor.trim(),
                patientId,
                pageRequest);
    }

    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getRemoteAddr();
        }
        return null;
    }

    private String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return new AuditLogResponse(a.getId(), a.getOccurredAt(), a.getActorUsername(), a.getActorRole(),
                a.getAction(), a.getEntityType(), a.getEntityId(), a.getPatientId(), a.getDetails(), a.getIpAddress());
    }
}
