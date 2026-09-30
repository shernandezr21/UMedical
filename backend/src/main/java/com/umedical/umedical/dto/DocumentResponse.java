package com.umedical.umedical.dto;

import com.umedical.umedical.entity.enumns.DocumentType;

public class DocumentResponse {

    private Long id;
    private DocumentType type;
    private String content;
    private Long appointmentId;

    public DocumentResponse(Long id, DocumentType type, String content, Long appointmentId) {
        this.id = id;
        this.type = type;
        this.content = content;
        this.appointmentId = appointmentId;
    }

    public Long getId() { return id; }
    public DocumentType getType() { return type; }
    public String getContent() { return content; }
    public Long getAppointmentId() { return appointmentId; }
}