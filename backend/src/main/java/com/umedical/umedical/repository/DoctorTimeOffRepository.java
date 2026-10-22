package com.umedical.umedical.repository;

import com.umedical.umedical.entity.DoctorTimeOff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DoctorTimeOffRepository extends JpaRepository<DoctorTimeOff, Long> {

    /** Bloqueos del medico que tocan el rango [start, end]. */
    List<DoctorTimeOff> findByDoctorIdAndEndDateGreaterThanEqualAndStartDateLessThanEqual(
            Long doctorId, LocalDate start, LocalDate end);

    /** Bloqueos vigentes o futuros (terminan hoy o despues). */
    List<DoctorTimeOff> findByDoctorIdAndEndDateGreaterThanEqualOrderByStartDate(Long doctorId, LocalDate from);
}