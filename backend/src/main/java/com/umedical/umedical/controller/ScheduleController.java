package com.umedical.umedical.controller;

import com.umedical.umedical.dto.AvailableSlotResponse;
import com.umedical.umedical.dto.ScheduleRequest;
import com.umedical.umedical.dto.ScheduleResponse;
import com.umedical.umedical.service.AvailabilityService;
import com.umedical.umedical.service.ScheduleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@Tag(name = "Horarios", description = "Consulta de disponibilidad de medicos")
public class ScheduleController {

    private final AvailabilityService availabilityService;

    @GetMapping("/available")
    @Operation(summary = "Consulta horarios disponibles de un medico en una fecha")
    public ResponseEntity<List<AvailableSlotResponse>> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(availabilityService.getAvailableSlots(doctorId, date));
    }

        private final ScheduleService scheduleService;

    public ScheduleController(AvailabilityService availabilityService, ScheduleService scheduleService) {
        this.availabilityService = availabilityService;
        this.scheduleService = scheduleService;
    }

    @PostMapping
    @Operation(summary = "Crea un bloque de horario para un medico")
    public ResponseEntity<ScheduleResponse> createSchedule(@Valid @RequestBody ScheduleRequest request) {
        return ResponseEntity.ok(scheduleService.createSchedule(request));
    }

    @GetMapping
    @Operation(summary = "Lista los bloques de horario de un medico")
    public ResponseEntity<List<ScheduleResponse>> getSchedulesByDoctor(@RequestParam Long doctorId) {
        return ResponseEntity.ok(scheduleService.getSchedulesByDoctor(doctorId));
    }
}