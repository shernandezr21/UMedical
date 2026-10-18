package com.umedical.umedical.controller;

import com.umedical.umedical.dto.AvailableSlotResponse;
import com.umedical.umedical.dto.DayAvailabilityResponse;
import com.umedical.umedical.dto.ScheduleBlockRequest;
import com.umedical.umedical.dto.ScheduleRequest;
import com.umedical.umedical.dto.ScheduleResponse;
import com.umedical.umedical.service.AvailabilityService;
import com.umedical.umedical.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@Tag(name = "Horarios", description = "Disponibilidad de medicos y bloques de horario")
public class ScheduleController {

    private final AvailabilityService availabilityService;
    private final ScheduleService scheduleService;

    public ScheduleController(AvailabilityService availabilityService, ScheduleService scheduleService) {
        this.availabilityService = availabilityService;
        this.scheduleService = scheduleService;
    }

    @GetMapping("/available")
    @Operation(summary = "Horarios libres de un medico en una fecha",
            description = "Solo devuelve horarios que se pueden reservar: no ocupados y con la anticipacion minima.")
    public ResponseEntity<List<AvailableSlotResponse>> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(availabilityService.getAvailableSlots(doctorId, date));
    }

    @GetMapping("/available/range")
    @Operation(summary = "Cuantos horarios libres tiene cada dia de un rango (para un calendario)",
            description = "Maximo 62 dias. 400 si el rango es mayor o la fecha final es anterior a la inicial.")
    public ResponseEntity<List<DayAvailabilityResponse>> getAvailabilityRange(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(availabilityService.getAvailabilityRange(doctorId, startDate, endDate));
    }

    @PostMapping
    @Operation(summary = "Crea un bloque de horario para un medico (solo ADMIN)")
    public ResponseEntity<ScheduleResponse> createSchedule(@Valid @RequestBody ScheduleRequest request) {
        return ResponseEntity.ok(scheduleService.createSchedule(request));
    }

    @GetMapping
    @Operation(summary = "Lista los bloques de horario activos de un medico")
    public ResponseEntity<List<ScheduleResponse>> getSchedulesByDoctor(@RequestParam Long doctorId) {
        return ResponseEntity.ok(scheduleService.getSchedulesByDoctor(doctorId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita un bloque de horario (solo ADMIN)",
            description = "400 si el bloque es invalido o se cruza con otro. "
                    + "409 si dejaria sin horario a citas pendientes (hay que reagendarlas o cancelarlas antes).")
    public ResponseEntity<ScheduleResponse> updateSchedule(@PathVariable Long id,
                                                           @Valid @RequestBody ScheduleBlockRequest request) {
        return ResponseEntity.ok(scheduleService.updateSchedule(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un bloque de horario (solo ADMIN)",
            description = "204 si se elimina. 409 si hay citas pendientes dentro del bloque.")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        scheduleService.deactivateSchedule(id);
        return ResponseEntity.noContent().build();
    }
}