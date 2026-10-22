package com.umedical.umedical.service;

import com.umedical.umedical.dto.CreatePatientByStaffRequest;
import com.umedical.umedical.dto.CreatedPatientResponse;
import com.umedical.umedical.dto.PageResponse;
import com.umedical.umedical.dto.PatientProfileResponse;
import com.umedical.umedical.dto.PatientSummaryResponse;
import com.umedical.umedical.dto.RegisterPatientRequest;
import com.umedical.umedical.dto.UpdatePatientProfileRequest;
import com.umedical.umedical.entity.Patient;
import com.umedical.umedical.entity.Role;
import com.umedical.umedical.entity.User;
import com.umedical.umedical.exception.ConflictException;
import com.umedical.umedical.exception.ResourceNotFoundException;
import com.umedical.umedical.repository.PatientRepository;
import com.umedical.umedical.repository.RoleRepository;
import com.umedical.umedical.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Pacientes: registro publico, perfil propio, y operaciones de la secretaria (buscar, ver y dar de alta).
 * El rol lo fija siempre el servidor (ROLE_PATIENT). Los usuarios y contrasenas temporales los genera
 * CredentialService (una sola implementacion para todo el sistema).
 */
@Service
public class PatientService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final CredentialService credentialService;

    public PatientService(UserRepository userRepository,
                          RoleRepository roleRepository,
                          PatientRepository patientRepository,
                          PasswordEncoder passwordEncoder,
                          CredentialService credentialService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.credentialService = credentialService;
    }

    // ------------------------------------------------------------------ Registro publico

    @Transactional
    public User register(RegisterPatientRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Ese username ya esta en uso");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Ese email ya esta en uso");
        }

        User user = newPatientUser(username, email, passwordEncoder.encode(request.getPassword()),
                request.getFirstName(), request.getLastName(), request.getPhone());
        savePatientProfile(user, request.getBirthDate(), request.getAddress(), request.getBloodType());
        return user;
    }

    // ------------------------------------------------------------------ Perfil propio

    @Transactional(readOnly = true)
    public PatientProfileResponse getProfile(Long userId) {
        return toProfile(findByUserId(userId));
    }

    @Transactional
    public PatientProfileResponse updateProfile(Long userId, UpdatePatientProfileRequest request) {
        Patient patient = findByUserId(userId);
        User user = patient.getUser();

        String email = request.getEmail().trim().toLowerCase();
        if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new ConflictException("Ese email ya esta en uso");
        }

        user.setEmail(email);
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPhone(blankToNull(request.getPhone()));
        patient.setBirthDate(request.getBirthDate());
        patient.setAddress(blankToNull(request.getAddress()));
        patient.setBloodType(blankToNull(request.getBloodType()));

        userRepository.save(user);
        patientRepository.save(patient);
        return toProfile(patient);
    }

    // ------------------------------------------------------------------ Secretaria / admin

    @Transactional(readOnly = true)
    public PageResponse<PatientSummaryResponse> search(String query, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);

        Page<Patient> result = patientRepository.search(
                query == null ? "" : query.trim(), PageRequest.of(safePage, safeSize));

        return new PageResponse<>(
                result.getContent().stream().map(this::toSummary).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PatientSummaryResponse getSummary(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        return toSummary(patient);
    }

    /** Alta en recepcion: usuario generado a partir del correo y contrasena temporal que se devuelve una sola vez. */
    @Transactional
    public CreatedPatientResponse createByStaff(CreatePatientByStaffRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Ese email ya esta en uso");
        }

        String username = credentialService.generateUsername(email);
        String temporaryPassword = credentialService.generateTemporaryPassword();

        User user = newPatientUser(username, email, passwordEncoder.encode(temporaryPassword),
                request.getFirstName(), request.getLastName(), request.getPhone());
        Patient patient = savePatientProfile(user, request.getBirthDate(), request.getAddress(), request.getBloodType());

        return new CreatedPatientResponse(toSummary(patient), username, temporaryPassword);
    }

    // ------------------------------------------------------------------ Privados

    private User newPatientUser(String username, String email, String passwordHash,
                                String firstName, String lastName, String phone) {
        Role patientRole = roleRepository.findByName("ROLE_PATIENT")
                .orElseThrow(() -> new ResourceNotFoundException("Rol ROLE_PATIENT no existe"));

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordHash);
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setPhone(blankToNull(phone));
        user.setRole(patientRole);
        user.setActive(true);
        return userRepository.save(user);
    }

    private Patient savePatientProfile(User user, LocalDate birthDate, String address, String bloodType) {
        Patient patient = new Patient();
        patient.setUser(user);
        patient.setBirthDate(birthDate);
        patient.setAddress(blankToNull(address));
        patient.setBloodType(blankToNull(bloodType));
        return patientRepository.save(patient);
    }

    private Patient findByUserId(Long userId) {
        return patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de paciente no encontrado"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PatientProfileResponse toProfile(Patient patient) {
        User user = patient.getUser();
        return new PatientProfileResponse(
                patient.getId(), user.getId(), user.getUsername(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getPhone(),
                patient.getBirthDate(), patient.getAddress(), patient.getBloodType());
    }

    private PatientSummaryResponse toSummary(Patient patient) {
        User user = patient.getUser();
        return new PatientSummaryResponse(
                patient.getId(), user.getId(), user.getUsername(),
                user.getFirstName() + " " + user.getLastName(), user.getEmail(), user.getPhone(),
                patient.getBirthDate(), patient.getBloodType());
    }
}