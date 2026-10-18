package com.umedical.umedical.service;

import com.umedical.umedical.dto.CreatePatientByStaffRequest;
import com.umedical.umedical.dto.CreateUserRequest;
import com.umedical.umedical.dto.CreatedPatientResponse;
import com.umedical.umedical.dto.CreatedUserResponse;
import com.umedical.umedical.dto.PageResponse;
import com.umedical.umedical.dto.ResetPasswordResponse;
import com.umedical.umedical.dto.UpdateUserRequest;
import com.umedical.umedical.dto.UserSummaryResponse;
import com.umedical.umedical.entity.Role;
import com.umedical.umedical.entity.User;
import com.umedical.umedical.exception.ConflictException;
import com.umedical.umedical.exception.ResourceNotFoundException;
import com.umedical.umedical.repository.RoleRepository;
import com.umedical.umedical.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Gestion de usuarios por el administrador. Los medicos NO se crean ni se activan desde aqui:
 * llevan especialidad, colegiado y horarios, asi que viven en DoctorService / DoctorAdminService.
 */
@Service
public class UserAdminService {

    private static final Set<String> CREATABLE_ROLES = Set.of("ROLE_ADMIN", "ROLE_SECRETARY", "ROLE_PATIENT");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CredentialService credentialService;
    private final PatientService patientService;

    public UserAdminService(UserRepository userRepository,
                            RoleRepository roleRepository,
                            PasswordEncoder passwordEncoder,
                            CredentialService credentialService,
                            PatientService patientService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.credentialService = credentialService;
        this.patientService = patientService;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> search(String query, String role, Boolean active, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);

        Page<User> result = userRepository.search(
                query == null ? "" : query.trim(),
                role == null || role.isBlank() ? null : role,
                active,
                PageRequest.of(safePage, safeSize));

        return new PageResponse<>(
                result.getContent().stream().map(this::toSummary).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public CreatedUserResponse create(CreateUserRequest request) {
        String roleName = request.getRole();

        if ("ROLE_DOCTOR".equals(roleName)) {
            throw new IllegalArgumentException(
                    "Los medicos se crean desde la seccion Medicos (llevan especialidad, colegiado y horarios)");
        }
        if (!CREATABLE_ROLES.contains(roleName)) {
            throw new IllegalArgumentException("Rol invalido. Roles permitidos: ROLE_ADMIN, ROLE_SECRETARY, ROLE_PATIENT");
        }

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Ese email ya esta en uso");
        }

        // Un paciente se da de alta igual que en recepcion (usuario y contrasena temporal generados)
        if ("ROLE_PATIENT".equals(roleName)) {
            CreatePatientByStaffRequest patientRequest = new CreatePatientByStaffRequest();
            patientRequest.setEmail(email);
            patientRequest.setFirstName(request.getFirstName());
            patientRequest.setLastName(request.getLastName());
            patientRequest.setPhone(request.getPhone());

            CreatedPatientResponse created = patientService.createByStaff(patientRequest);
            User user = findUser(created.getPatient().getUserId());
            return new CreatedUserResponse(toSummary(user), created.getUsername(), created.getTemporaryPassword());
        }

        String username = request.getUsername() == null || request.getUsername().isBlank()
                ? credentialService.generateUsername(email)
                : request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Ese username ya esta en uso");
        }

        boolean generated = request.getPassword() == null || request.getPassword().isBlank();
        String password = generated ? credentialService.generateTemporaryPassword() : request.getPassword();

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + roleName));

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPhone(blankToNull(request.getPhone()));
        user.setRole(role);
        user.setActive(true);
        user = userRepository.save(user);

        return new CreatedUserResponse(toSummary(user), username, generated ? password : null);
    }

    @Transactional
    public UserSummaryResponse update(Long id, UpdateUserRequest request) {
        User user = findUser(id);

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new ConflictException("Ese email ya esta en uso");
        }

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(email);
        user.setPhone(blankToNull(request.getPhone()));
        return toSummary(userRepository.save(user));
    }

    @Transactional
    public UserSummaryResponse setActive(Long actorUserId, Long targetId, boolean active) {
        User target = findUser(targetId);
        String role = target.getRole().getName();

        if ("ROLE_DOCTOR".equals(role)) {
            throw new ConflictException("Los medicos se activan o desactivan desde la seccion Medicos");
        }
        if (!active) {
            if (target.getId().equals(actorUserId)) {
                throw new ConflictException("No puedes desactivar tu propia cuenta");
            }
            if ("ROLE_ADMIN".equals(role) && target.isActive()
                    && userRepository.countByRoleNameAndActiveTrue("ROLE_ADMIN") <= 1) {
                throw new ConflictException("No se puede desactivar al ultimo administrador activo");
            }
        }

        target.setActive(active);
        return toSummary(userRepository.save(target));
    }

    /** Genera una contrasena temporal nueva. Se devuelve una sola vez. */
    @Transactional
    public ResetPasswordResponse resetPassword(Long id) {
        User user = findUser(id);
        String temporaryPassword = credentialService.generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        userRepository.save(user);
        return new ResetPasswordResponse(temporaryPassword);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private UserSummaryResponse toSummary(User u) {
        return new UserSummaryResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFirstName(), u.getLastName(),
                u.getPhone(), u.getRole().getName(), u.isActive(), u.getCreatedAt());
    }
}