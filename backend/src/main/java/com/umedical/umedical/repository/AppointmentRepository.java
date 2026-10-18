package com.umedical.umedical.repository;

import com.umedical.umedical.entity.Appointment;
import com.umedical.umedical.entity.enumns.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientUserIdOrderByAppointmentDateDescAppointmentTimeDesc(Long patientUserId);

    List<Appointment> findByDoctorIdAndAppointmentDate(Long doctorId, LocalDate date);

    List<Appointment> findByDoctorIdAndAppointmentDateBetween(Long doctorId, LocalDate start, LocalDate end);

    /** Para impedir que un paciente tenga dos citas activas a la misma hora (con medicos distintos). */
    boolean existsByPatientIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
            Long patientId, LocalDate date, LocalTime time, AppointmentStatus status);

    /** Citas de un medico con cierto estado desde una fecha (p. ej. pendientes desde hoy). */
    List<Appointment> findByDoctorIdAndStatusAndAppointmentDateGreaterThanEqual(
            Long doctorId, AppointmentStatus status, LocalDate date);

    long countByDoctorIdAndStatusAndAppointmentDateGreaterThanEqual(
            Long doctorId, AppointmentStatus status, LocalDate date);

    /** Agenda general: todas las citas del rango, opcionalmente de un solo medico. */
    @Query("""
            SELECT a FROM Appointment a
              JOIN FETCH a.doctor d JOIN FETCH d.user JOIN FETCH d.specialty
              JOIN FETCH a.patient p JOIN FETCH p.user
            WHERE a.appointmentDate BETWEEN :start AND :end
              AND (:doctorId IS NULL OR d.id = :doctorId)
            ORDER BY a.appointmentDate, a.appointmentTime
            """)
    List<Appointment> findClinicAgenda(@Param("start") LocalDate start,
                                       @Param("end") LocalDate end,
                                       @Param("doctorId") Long doctorId);

    // ------------------------------------------------------------------ Reportes del panel

    @Query("""
            SELECT a.status, COUNT(a) FROM Appointment a
            WHERE a.appointmentDate BETWEEN :start AND :end
            GROUP BY a.status
            """)
    List<Object[]> countByStatus(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            SELECT sp.name, COUNT(a) FROM Appointment a JOIN a.doctor d JOIN d.specialty sp
            WHERE a.appointmentDate BETWEEN :start AND :end
            GROUP BY sp.name
            ORDER BY COUNT(a) DESC
            """)
    List<Object[]> countBySpecialty(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            SELECT d.id, u.firstName, u.lastName, COUNT(a),
                   SUM(CASE WHEN a.status = :completed THEN 1 ELSE 0 END),
                   SUM(CASE WHEN a.status = :cancelled THEN 1 ELSE 0 END)
            FROM Appointment a JOIN a.doctor d JOIN d.user u
            WHERE a.appointmentDate BETWEEN :start AND :end
            GROUP BY d.id, u.firstName, u.lastName
            ORDER BY COUNT(a) DESC
            """)
    List<Object[]> statsByDoctor(@Param("start") LocalDate start, @Param("end") LocalDate end,
                                 @Param("completed") AppointmentStatus completed,
                                 @Param("cancelled") AppointmentStatus cancelled);

    @Query("""
            SELECT a.appointmentDate, COUNT(a) FROM Appointment a
            WHERE a.appointmentDate BETWEEN :start AND :end
            GROUP BY a.appointmentDate
            ORDER BY a.appointmentDate
            """)
    List<Object[]> countByDay(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** Citas pendientes cuya fecha ya paso: nadie las atendio. */
    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.status = :pending
              AND a.appointmentDate BETWEEN :start AND :end
              AND a.appointmentDate < :today
            """)
    long countUnattended(@Param("start") LocalDate start, @Param("end") LocalDate end,
                         @Param("today") LocalDate today, @Param("pending") AppointmentStatus pending);
}