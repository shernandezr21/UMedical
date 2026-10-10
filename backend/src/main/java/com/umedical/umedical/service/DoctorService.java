package com.umedical.umedical.service;

import com.umedical.umedical.dto.CreateDoctorRequest;
import com.umedical.umedical.dto.DoctorResponse;
import com.umedical.umedical.dto.ScheduleBlockRequest;
import com.umedical.umedical.entity.Doctor;
import com.umedical.umedical.entity.Role;
import com.umedical.umedical.entity.Specialty;
import com.umedical.umedical.entity.User;
import com.umedical.umedical.exception.ResourceNotFoundException;
import com.umedical.umedical.repository.DoctorRepository;
import com.umedical.umedical.repository.RoleRepository;
import com.umedical.umedical.repository.SpecialtyRepository;
import com.umedical.umedical.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Alta completa de un medico en UNA sola transaccion: usuario (ROLE_DOCTOR),
 * perfil profesional y bloques de horario. Si cualquier paso falla se hace
 * rollback de todo, asi nunca quedan usuarios ni horarios huerfanos.
 */
@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PasswordEncoder passwordEncoder;
    private final ScheduleService scheduleService;

    public DoctorService(DoctorRepository doctorRepository,
                          UserRepository userRepository,
                          RoleRepository roleRepository,
                          SpecialtyRepository specialtyRepository,
                          PasswordEncoder passwordEncoder,
                          ScheduleService scheduleService) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.specialtyRepository = specialtyRepository;
        this.passwordEncoder = passwordEncoder;
        this.scheduleService = scheduleService;
    }

    @Transactional
    public DoctorResponse createDoctor(CreateDoctorRequest request) {

        List<ScheduleBlockRequest> blocks = request.getSchedules() == null ? List.of() : request.getSchedules();

        // 1) Validar todo antes de escribir
        scheduleService.validateBlocks(blocks);

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Ese username ya esta en uso");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Ese email ya esta en uso");
        }

        Role doctorRole = roleRepository.findByName("ROLE_DOCTOR")
                .orElseThrow(() -> new ResourceNotFoundException("Rol ROLE_DOCTOR no existe"));

        Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada"));

        // 2) Usuario + perfil de medico
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setRole(doctorRole);
        user.setActive(true);
        user = userRepository.save(user);

        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setSpecialty(specialty);
        doctor.setLicenseNumber(request.getLicenseNumber());
        doctor.setBiography(request.getBiography());
        doctor = doctorRepository.save(doctor);

        // 3) Horarios, en la misma transaccion
        for (ScheduleBlockRequest block : blocks) {
            scheduleService.saveBlock(doctor, block);
        }

        return new DoctorResponse(
                doctor.getId(),
                user.getFirstName() + " " + user.getLastName(),
                specialty.getName(),
                doctor.getLicenseNumber(),
                doctor.getBiography());
    }
}

// package com.umedical.umedical.service;

// import com.umedical.umedical.dto.CreateDoctorRequest;
// import com.umedical.umedical.dto.DoctorResponse;
// import com.umedical.umedical.entity.Doctor;
// import com.umedical.umedical.entity.Role;
// import com.umedical.umedical.entity.Specialty;
// import com.umedical.umedical.entity.User;
// import com.umedical.umedical.exception.ResourceNotFoundException;
// import com.umedical.umedical.repository.DoctorRepository;
// import com.umedical.umedical.repository.RoleRepository;
// import com.umedical.umedical.repository.SpecialtyRepository;
// import com.umedical.umedical.repository.UserRepository;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// /**
//  * Crea el perfil completo de un medico: primero el User (con rol
//  * ROLE_DOCTOR y password encriptado con BCrypt), luego el Doctor que
//  * extiende ese usuario con datos profesionales. Ambos pasos van en una
//  * sola transaccion: si falla crear el Doctor, tampoco queda guardado
//  * el User huerfano.
//  */
// @Service
// public class DoctorService {

//     private final DoctorRepository doctorRepository;
//     private final UserRepository userRepository;
//     private final RoleRepository roleRepository;
//     private final SpecialtyRepository specialtyRepository;
//     private final PasswordEncoder passwordEncoder;

//     public DoctorService(DoctorRepository doctorRepository,
//                           UserRepository userRepository,
//                           RoleRepository roleRepository,
//                           SpecialtyRepository specialtyRepository,
//                           PasswordEncoder passwordEncoder) {
//         this.doctorRepository = doctorRepository;
//         this.userRepository = userRepository;
//         this.roleRepository = roleRepository;
//         this.specialtyRepository = specialtyRepository;
//         this.passwordEncoder = passwordEncoder;
//     }

//     @Transactional
//     public DoctorResponse createDoctor(CreateDoctorRequest request) {

//         if (userRepository.existsByUsername(request.getUsername())) {
//             throw new IllegalArgumentException("Ese username ya esta en uso");
//         }
//         if (userRepository.existsByEmail(request.getEmail())) {
//             throw new IllegalArgumentException("Ese email ya esta en uso");
//         }

//         Role doctorRole = roleRepository.findByName("ROLE_DOCTOR")
//                 .orElseThrow(() -> new ResourceNotFoundException("Rol ROLE_DOCTOR no existe"));

//         Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
//                 .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada"));

//         User user = new User();
//         user.setUsername(request.getUsername());
//         user.setEmail(request.getEmail());
//         user.setPassword(passwordEncoder.encode(request.getPassword()));
//         user.setFirstName(request.getFirstName());
//         user.setLastName(request.getLastName());
//         user.setPhone(request.getPhone());
//         user.setRole(doctorRole);
//         user.setActive(true);
//         user = userRepository.save(user);

//         Doctor doctor = new Doctor();
//         doctor.setUser(user);
//         doctor.setSpecialty(specialty);
//         doctor.setLicenseNumber(request.getLicenseNumber());
//         doctor.setBiography(request.getBiography());
//         doctor = doctorRepository.save(doctor);

//         return new DoctorResponse(
//                 doctor.getId(),
//                 user.getFirstName() + " " + user.getLastName(),
//                 specialty.getName(),
//                 doctor.getLicenseNumber(),
//                 doctor.getBiography());
//     }
// }