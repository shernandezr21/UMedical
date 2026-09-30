package com.umedical.umedical.dto;

import com.umedical.umedical.entity.enumns.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DocumentRequest {

    @NotNull(message = "El tipo de documento es obligatorio (DIAGNOSTICO o RECETA)")
    private DocumentType type;

    @NotBlank(message = "El contenido del documento es obligatorio")
    private String content;

    public DocumentType getType() { return type; }
    public void setType(DocumentType type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}