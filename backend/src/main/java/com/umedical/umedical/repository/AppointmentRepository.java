package com.umedical.umedical.repository;

import com.umedical.umedical.entity.Appointment;
import com.umedical.umedical.entity.enumns.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Reportes del panel

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

        //  Area del medico

    boolean existsByDoctorIdAndPatientIdAndStatusNot(Long doctorId, Long patientId, AppointmentStatus status);

    List<Appointment> findByDoctorIdAndPatientIdAndStatusNotOrderByAppointmentDateDescAppointmentTimeDesc(
            Long doctorId, Long patientId, AppointmentStatus status);

    List<Appointment> findByDoctorIdAndStatusAndAppointmentDateBetween(
            Long doctorId, AppointmentStatus status, LocalDate start, LocalDate end);

    /** Pacientes con al menos una cita no cancelada con este medico, con sus totales. */
    @Query(value = """
            SELECT p.id, u.firstName, u.lastName, u.email, u.phone, COUNT(a),
                   SUM(CASE WHEN a.status = :completed THEN 1 ELSE 0 END),
                   MAX(CASE WHEN a.status = :completed THEN a.appointmentDate END)
            FROM Appointment a JOIN a.patient p JOIN p.user u
            WHERE a.doctor.id = :doctorId
              AND a.status <> :cancelled
              AND (:query = ''
                   OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR u.phone LIKE CONCAT('%', :query, '%'))
            GROUP BY p.id, u.firstName, u.lastName, u.email, u.phone
            ORDER BY MAX(CASE WHEN a.status = :completed THEN a.appointmentDate END) DESC, u.lastName, u.firstName
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p.id)
            FROM Appointment a JOIN a.patient p JOIN p.user u
            WHERE a.doctor.id = :doctorId
              AND a.status <> :cancelled
              AND (:query = ''
                   OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR u.phone LIKE CONCAT('%', :query, '%'))
            """)
    Page<Object[]> findPatientsOfDoctor(@Param("doctorId") Long doctorId,
                                        @Param("query") String query,
                                        @Param("completed") AppointmentStatus completed,
                                        @Param("cancelled") AppointmentStatus cancelled,
                                        Pageable pageable);

    // Estadisticas del medico

    @Query("""
            SELECT a.status, COUNT(a) FROM Appointment a
            WHERE a.doctor.id = :doctorId AND a.appointmentDate BETWEEN :start AND :end
            GROUP BY a.status
            """)
    List<Object[]> countByStatusForDoctor(@Param("doctorId") Long doctorId,
                                          @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            SELECT a.appointmentDate, COUNT(a) FROM Appointment a
            WHERE a.doctor.id = :doctorId AND a.status <> :cancelled
              AND a.appointmentDate BETWEEN :start AND :end
            GROUP BY a.appointmentDate
            ORDER BY a.appointmentDate
            """)
    List<Object[]> countByDayForDoctor(@Param("doctorId") Long doctorId,
                                       @Param("start") LocalDate start, @Param("end") LocalDate end,
                                       @Param("cancelled") AppointmentStatus cancelled);

    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.doctor.id = :doctorId AND a.status = :pending
              AND a.appointmentDate BETWEEN :start AND :end AND a.appointmentDate < :today
            """)
    long countUnattendedForDoctor(@Param("doctorId") Long doctorId,
                                  @Param("start") LocalDate start, @Param("end") LocalDate end,
                                  @Param("today") LocalDate today, @Param("pending") AppointmentStatus pending);

    @Query("""
            SELECT COUNT(DISTINCT a.patient.id) FROM Appointment a
            WHERE a.doctor.id = :doctorId AND a.status <> :cancelled
              AND a.appointmentDate BETWEEN :start AND :end
            """)
    long countDistinctPatientsForDoctor(@Param("doctorId") Long doctorId,
                                        @Param("start") LocalDate start, @Param("end") LocalDate end,
                                        @Param("cancelled") AppointmentStatus cancelled);

    @Query("""
            SELECT COUNT(d) FROM Document d
            WHERE d.appointment.doctor.id = :doctorId
              AND d.appointment.appointmentDate BETWEEN :start AND :end
            """)
    long countDocumentsForDoctor(@Param("doctorId") Long doctorId,
                                 @Param("start") LocalDate start, @Param("end") LocalDate end);

}