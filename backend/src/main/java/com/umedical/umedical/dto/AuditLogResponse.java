package com.umedical.umedical.dto;

import java.time.LocalDateTime;

public class AuditLogResponse {

    private Long id;
    private LocalDateTime occurredAt;
    private String actorUsername;
    private String actorRole;
    private String action;
    private String entityType;
    private Long entityId;
    private Long patientId;
    private String details;
    private String ipAddress;

    public AuditLogResponse(Long id, LocalDateTime occurredAt, String actorUsername, String actorRole, String action,
                            String entityType, Long entityId, Long patientId, String details, String ipAddress) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.patientId = patientId;
        this.details = details;
        this.ipAddress = ipAddress;
    }

    public Long getId() { return id; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getActorUsername() { return actorUsername; }
    public String getActorRole() { return actorRole; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public Long getPatientId() { return patientId; }
    public String getDetails() { return details; }
    public String getIpAddress() { return ipAddress; }
}
