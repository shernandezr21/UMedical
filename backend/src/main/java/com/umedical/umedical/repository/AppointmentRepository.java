package com.umedical.umedical.repository;

import com.umedical.umedical.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Acceso a datos para las citas medicas.
 * findByPatientUserIdOrderBy...() alimenta GET /appointments/my-history:
 * trae el historial completo de UN paciente (identificado por su userId
 * del JWT), mas reciente primero. findByDoctorIdAndAppointmentDate() se
 * usa para saber que horarios ya estan ocupados en un dia
 * especifico y asi calcular la disponibilidad real de un medico.
 */

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientUserIdOrderByAppointmentDateDescAppointmentTimeDesc(Long patientUserId);
    List<Appointment> findByDoctorIdAndAppointmentDate(Long doctorId, LocalDate date);
    List<Appointment> findByDoctorIdAndAppointmentDateBetween(Long doctorId, LocalDate start, LocalDate end);
}