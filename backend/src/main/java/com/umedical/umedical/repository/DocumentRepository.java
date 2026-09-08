package com.umedical.umedical.repository;

import com.umedical.umedical.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


/**
 * Acceso a datos para los documentos clinicos (diagnostico, receta)
 * generados a partir de una cita. findByAppointmentId() trae todos los
 * documentos asociados a UNA cita puntual - se usa para mostrarle al
 * paciente el detalle de diagnostico/receta de una cita ya completada.
 */

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByAppointmentId(Long appointmentId);
}