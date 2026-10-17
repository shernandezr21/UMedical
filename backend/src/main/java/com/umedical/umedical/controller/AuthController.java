package com.umedical.umedical.controller;

import com.umedical.umedical.dto.LoginRequest;
import com.umedical.umedical.dto.LoginResponse;
import com.umedical.umedical.dto.RegisterPatientRequest;
import com.umedical.umedical.entity.User;
import com.umedical.umedical.repository.UserRepository;
import com.umedical.umedical.security.CustomUserDetails;
import com.umedical.umedical.security.JwtUtil;
import com.umedical.umedical.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticacion", description = "Registro de pacientes, login y generacion de token JWT")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PatientService patientService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UserRepository userRepository,
                          PatientService patientService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.patientService = patientService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica un usuario y devuelve un token JWT",
            description = "401 si las credenciales son incorrectas.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));

        return ResponseEntity.ok(buildLoginResponse(user));
    }

    @PostMapping("/register")
    @Operation(summary = "Registro publico de un paciente",
            description = "Crea el usuario (siempre ROLE_PATIENT) y su perfil, y devuelve un token listo para usar, "
                    + "igual que el login. 400 si hay datos invalidos, 409 si el username o el email ya existen.")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterPatientRequest request) {
        User user = patientService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildLoginResponse(user));
    }

    private LoginResponse buildLoginResponse(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtUtil.generateToken(userDetails, user.getId(), user.getRole().getName());

        return new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getRole().getName(),
                user.getFirstName() + " " + user.getLastName());
    }
}

// package com.umedical.umedical.controller;

// import com.umedical.umedical.dto.LoginRequest;
// import com.umedical.umedical.dto.LoginResponse;
// import com.umedical.umedical.entity.User;
// import com.umedical.umedical.repository.UserRepository;
// import com.umedical.umedical.security.CustomUserDetails;
// import com.umedical.umedical.security.JwtUtil;
// import io.swagger.v3.oas.annotations.Operation;
// import io.swagger.v3.oas.annotations.tags.Tag;
// import jakarta.validation.Valid;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.authentication.AuthenticationManager;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.web.bind.annotation.*;

// @RestController
// @RequestMapping("/api/v1/auth")
// @Tag(name = "Autenticacion", description = "Login y generacion de token JWT")
// public class AuthController {

//     private final AuthenticationManager authenticationManager;
//     private final JwtUtil jwtUtil;
//     private final UserRepository userRepository;

//     public AuthController(AuthenticationManager authenticationManager,
//                            JwtUtil jwtUtil,
//                            UserRepository userRepository) {
//         this.authenticationManager = authenticationManager;
//         this.jwtUtil = jwtUtil;
//         this.userRepository = userRepository;
//     }

//     @PostMapping("/login")
//     @Operation(summary = "Autentica un usuario y devuelve un token JWT")
//     public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

//         authenticationManager.authenticate(
//                 new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

//         User user = userRepository.findByUsername(request.getUsername())
//                 .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));

//         CustomUserDetails userDetails = new CustomUserDetails(user);
//         String token = jwtUtil.generateToken(userDetails, user.getId(), user.getRole().getName());

//         LoginResponse response = new LoginResponse(
//                 token,
//                 user.getId(),
//                 user.getUsername(),
//                 user.getRole().getName(),
//                 user.getFirstName() + " " + user.getLastName());

//         return ResponseEntity.ok(response);
//     }
// }