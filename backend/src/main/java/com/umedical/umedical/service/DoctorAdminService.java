package com.umedical.umedical.service;

import com.umedical.umedical.dto.DoctorDetailResponse;
import com.umedical.umedical.dto.UpdateDoctorRequest;
import com.umedical.umedical.entity.Doctor;
import com.umedical.umedical.entity.Specialty;
import com.umedical.umedical.entity.User;
import com.umedical.umedical.entity.enumns.AppointmentStatus;
import com.umedical.umedical.exception.ConflictException;
import com.umedical.umedical.exception.ResourceNotFoundException;
import com.umedical.umedical.repository.AppointmentRepository;
import com.umedical.umedical.repository.DoctorRepository;
import com.umedical.umedical.repository.SpecialtyRepository;
import com.umedical.umedical.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Administracion de medicos ya creados: listar, editar datos y activar o desactivar. */
@Service
public class DoctorAdminService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    public DoctorAdminService(DoctorRepository doctorRepository,
                              UserRepository userRepository,
                              SpecialtyRepository specialtyRepository,
                              AppointmentRepository appointmentRepository,
                              Clock clock) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.specialtyRepository = specialtyRepository;
        this.appointmentRepository = appointmentRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<DoctorDetailResponse> listAll() {
        return doctorRepository.findAllWithDetails().stream().map(this::toDetail).toList();
    }

    @Transactional
    public DoctorDetailResponse update(Long doctorId, UpdateDoctorRequest request) {
        Doctor doctor = findDoctor(doctorId);
        User user = doctor.getUser();

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailAndIdNot(email, user.getId())) {
            throw new ConflictException("Ese email ya esta en uso");
        }

        String license = request.getLicenseNumber().trim();
        if (doctorRepository.existsByLicenseNumberAndIdNot(license, doctorId)) {
            throw new ConflictException("Ese numero de colegiado ya esta registrado");
        }

        Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada"));

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(email);
        user.setPhone(request.getPhone() == null || request.getPhone().isBlank() ? null : request.getPhone().trim());
        doctor.setSpecialty(specialty);
        doctor.setLicenseNumber(license);
        doctor.setBiography(request.getBiography() == null || request.getBiography().isBlank()
                ? null : request.getBiography().trim());

        userRepository.save(user);
        doctorRepository.save(doctor);
        return toDetail(doctor);
    }

    /** No se desactiva a un medico con citas pendientes desde hoy: primero hay que reagendarlas o cancelarlas. */
    @Transactional
    public DoctorDetailResponse setActive(Long doctorId, boolean active) {
        Doctor doctor = findDoctor(doctorId);

        if (!active) {
            long pending = appointmentRepository.countByDoctorIdAndStatusAndAppointmentDateGreaterThanEqual(
                    doctorId, AppointmentStatus.PENDIENTE, LocalDate.now(clock));
            if (pending > 0) {
                throw new ConflictException("El medico tiene " + pending
                        + " cita(s) pendiente(s) desde hoy. Reagendalas o cancelalas antes de desactivarlo");
            }
        }

        User user = doctor.getUser();
        user.setActive(active);
        userRepository.save(user);
        return toDetail(doctor);
    }

    private Doctor findDoctor(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medico no encontrado"));
    }

    private DoctorDetailResponse toDetail(Doctor d) {
        User u = d.getUser();
        return new DoctorDetailResponse(
                d.getId(), u.getId(), u.getUsername(), u.getFirstName(), u.getLastName(), u.getEmail(), u.getPhone(),
                d.getSpecialty().getId(), d.getSpecialty().getName(), d.getLicenseNumber(), d.getBiography(),
                u.isActive());
    }
}