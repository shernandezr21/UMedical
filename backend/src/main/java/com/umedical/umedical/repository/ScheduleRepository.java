package com.umedical.umedical.repository;

import com.umedical.umedical.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a datos para los bloques de horario semanal que cada medico
 * configura. findByDoctorIdAndActiveTrue() trae solo los horarios
 * vigentes de un medico (ignora los que el medico desactivo)
 */

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByDoctorIdAndActiveTrue(Long doctorId);
}