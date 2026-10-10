package com.umedical.umedical.controller;

import com.umedical.umedical.entity.Document;
import com.umedical.umedical.exception.ResourceNotFoundException;
import com.umedical.umedical.repository.DocumentRepository;
import com.umedical.umedical.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Adjuntar y descargar PDFs de un Document (diagnostico/receta).
 * Separado de AppointmentController porque opera sobre el documento
 * directamente, no sobre la cita. El PDF se guarda en disco (no en la
 * base de datos) - solo guardamos la ruta en la tabla documents.
 */
@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documentos", description = "Adjuntar y descargar archivos PDF de diagnosticos/recetas")
public class DocumentController {

    private final DocumentRepository documentRepository;

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    public DocumentController(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @PostMapping(value = "/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Adjunta un PDF a un documento existente (solo el doctor dueno de la cita)")
    public ResponseEntity<Void> uploadFile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));

        if (!document.getAppointment().getDoctor().getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("No puedes adjuntar archivos a un documento que no es tuyo");
        }

        if (document.getFilePath() != null) {
            throw new IllegalArgumentException("Este documento ya tiene un PDF adjunto");
        }

        if (!MediaType.APPLICATION_PDF_VALUE.equals(file.getContentType())) {
            throw new IllegalArgumentException("Solo se permiten archivos PDF");
        }

        Path dir = Paths.get(uploadsDir);
        Files.createDirectories(dir);

        String storedName = UUID.randomUUID() + ".pdf";
        Path destination = dir.resolve(storedName);
        file.transferTo(destination);

        document.setFileName(file.getOriginalFilename());
        document.setFilePath(destination.toString());
        documentRepository.save(document);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/file")
    @Operation(summary = "Descarga el PDF adjunto (doctor dueno o paciente dueno de la cita)")
    public ResponseEntity<Resource> downloadFile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));

        boolean esDoctorDueno = document.getAppointment().getDoctor().getUser().getId().equals(currentUser.getId());
        boolean esPacienteDueno = document.getAppointment().getPatient().getUser().getId().equals(currentUser.getId());

        if (!esDoctorDueno && !esPacienteDueno) {
            throw new IllegalArgumentException("No tienes acceso a este archivo");
        }

        if (document.getFilePath() == null) {
            throw new ResourceNotFoundException("Este documento no tiene archivo adjunto");
        }

        Resource resource = new FileSystemResource(document.getFilePath());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}