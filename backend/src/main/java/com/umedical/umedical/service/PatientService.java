package com.umedical.umedical.service;

import com.umedical.umedical.dto.PatientProfileResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro publico de pacientes y gestion de su propio perfil. El rol lo fija el servidor
 * (siempre ROLE_PATIENT): nadie puede registrarse a si mismo como admin o doctor.
 */
@Service
public class PatientService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    public PatientService(UserRepository userRepository,
                          RoleRepository roleRepository,
                          PatientRepository patientRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
    }

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

        Role patientRole = roleRepository.findByName("ROLE_PATIENT")
                .orElseThrow(() -> new ResourceNotFoundException("Rol ROLE_PATIENT no existe"));

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPhone(request.getPhone());
        user.setRole(patientRole);
        user.setActive(true);
        user = userRepository.save(user);

        Patient patient = new Patient();
        patient.setUser(user);
        patient.setBirthDate(request.getBirthDate());
        patient.setAddress(request.getAddress());
        patient.setBloodType(request.getBloodType());
        patientRepository.save(patient);

        return user;
    }

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
}

// package com.umedical.umedical.service;

// import com.umedical.umedical.dto.RegisterPatientRequest;
// import com.umedical.umedical.entity.Patient;
// import com.umedical.umedical.entity.Role;
// import com.umedical.umedical.entity.User;
// import com.umedical.umedical.exception.ConflictException;
// import com.umedical.umedical.exception.ResourceNotFoundException;
// import com.umedical.umedical.repository.PatientRepository;
// import com.umedical.umedical.repository.RoleRepository;
// import com.umedical.umedical.repository.UserRepository;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// /**
//  * Registro publico de pacientes. El rol lo fija el servidor (siempre ROLE_PATIENT):
//  * nadie puede registrarse a si mismo como admin o doctor.
//  */
// @Service
// public class PatientService {

//     private final UserRepository userRepository;
//     private final RoleRepository roleRepository;
//     private final PatientRepository patientRepository;
//     private final PasswordEncoder passwordEncoder;

//     public PatientService(UserRepository userRepository,
//                           RoleRepository roleRepository,
//                           PatientRepository patientRepository,
//                           PasswordEncoder passwordEncoder) {
//         this.userRepository = userRepository;
//         this.roleRepository = roleRepository;
//         this.patientRepository = patientRepository;
//         this.passwordEncoder = passwordEncoder;
//     }

//     @Transactional
//     public User register(RegisterPatientRequest request) {
//         String username = request.getUsername().trim();
//         String email = request.getEmail().trim().toLowerCase();

//         if (userRepository.existsByUsername(username)) {
//             throw new ConflictException("Ese username ya esta en uso");
//         }
//         if (userRepository.existsByEmail(email)) {
//             throw new ConflictException("Ese email ya esta en uso");
//         }

//         Role patientRole = roleRepository.findByName("ROLE_PATIENT")
//                 .orElseThrow(() -> new ResourceNotFoundException("Rol ROLE_PATIENT no existe"));

//         User user = new User();
//         user.setUsername(username);
//         user.setEmail(email);
//         user.setPassword(passwordEncoder.encode(request.getPassword()));
//         user.setFirstName(request.getFirstName().trim());
//         user.setLastName(request.getLastName().trim());
//         user.setPhone(request.getPhone());
//         user.setRole(patientRole);
//         user.setActive(true);
//         user = userRepository.save(user);

//         Patient patient = new Patient();
//         patient.setUser(user);
//         patient.setBirthDate(request.getBirthDate());
//         patient.setAddress(request.getAddress());
//         patient.setBloodType(request.getBloodType());
//         patientRepository.save(patient);

//         return user;
//     }
// }