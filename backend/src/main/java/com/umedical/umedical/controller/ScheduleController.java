package com.umedical.umedical.controller;

import com.umedical.umedical.dto.AvailableSlotResponse;
import com.umedical.umedical.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@Tag(name = "Horarios", description = "Consulta de disponibilidad de medicos")
public class ScheduleController {

    private final AvailabilityService availabilityService;

    public ScheduleController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/available")
    @Operation(summary = "Consulta horarios disponibles de un medico en una fecha")
    public ResponseEntity<List<AvailableSlotResponse>> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(availabilityService.getAvailableSlots(doctorId, date));
    }
}