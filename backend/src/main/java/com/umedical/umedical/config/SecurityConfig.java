package com.umedical.umedical.config;

import com.umedical.umedical.security.CustomUserDetailsService;
import com.umedical.umedical.security.JwtAuthFilter;
import com.umedical.umedical.security.RestAccessDeniedHandler;
import com.umedical.umedical.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Seguridad central: JWT sin estado, RBAC por ruta y respuestas 401/403 en JSON.
 * Spring evalua los requestMatchers de arriba hacia abajo y gana el primero que coincide.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          CustomUserDetailsService userDetailsService,
                          CorsConfigurationSource corsConfigurationSource,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.corsConfigurationSource = corsConfigurationSource;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // Publicos: login, registro de pacientes y Swagger
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()

                        // Catalogos
                        .requestMatchers(HttpMethod.GET, "/api/v1/doctors/**", "/api/v1/specialties/**").hasAnyAuthority(
                                "ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT", "ROLE_SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/api/v1/doctors/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/specialties").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/specialties/*").hasAuthority("ROLE_ADMIN")

                        // Horarios
                        .requestMatchers("/api/v1/schedules/available/**").hasAnyAuthority(
                                "ROLE_PATIENT", "ROLE_ADMIN", "ROLE_SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/api/v1/schedules").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/schedules").hasAnyAuthority("ROLE_ADMIN", "ROLE_DOCTOR")

                        // Citas: doctor
                        .requestMatchers(HttpMethod.GET, "/api/v1/appointments/my-agenda").hasAuthority("ROLE_DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/appointments/my-agenda-range").hasAuthority("ROLE_DOCTOR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/appointments/*/diagnosis").hasAuthority("ROLE_DOCTOR")

                        // Citas: documentos (doctor dueno o paciente dueno; el servicio verifica la propiedad)
                        .requestMatchers(HttpMethod.GET, "/api/v1/appointments/*/documents").hasAnyAuthority(
                                "ROLE_DOCTOR", "ROLE_PATIENT")

                        // Citas: paciente
                        .requestMatchers(HttpMethod.POST, "/api/v1/appointments").hasAuthority("ROLE_PATIENT")
                        .requestMatchers(HttpMethod.GET, "/api/v1/appointments/my-history").hasAuthority("ROLE_PATIENT")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/appointments/*/reschedule").hasAnyAuthority(
                                "ROLE_PATIENT", "ROLE_ADMIN", "ROLE_SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/api/v1/appointments/*/cancel").hasAnyAuthority(
                                "ROLE_PATIENT", "ROLE_DOCTOR", "ROLE_ADMIN", "ROLE_SECRETARY")

                        // Archivos PDF de los documentos
                        .requestMatchers(HttpMethod.POST, "/api/v1/documents/*/file").hasAuthority("ROLE_DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/documents/*/file").hasAnyAuthority(
                                "ROLE_DOCTOR", "ROLE_PATIENT")

                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

// package com.umedical.umedical.config;

// import com.umedical.umedical.security.CustomUserDetailsService;
// import com.umedical.umedical.security.JwtAuthFilter;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.authentication.AuthenticationManager;
// import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
// import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// import org.springframework.security.config.http.SessionCreationPolicy;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
// import org.springframework.web.cors.CorsConfigurationSource;
// import org.springframework.http.HttpMethod;

// /**
//  * Configuracion central de seguridad. Define:
//  * - El PasswordEncoder (BCrypt) usado para comparar contrasenas.
//  * - Que rutas son publicas (login, swagger) y cuales requieren
//  *    autenticacion + que rol especifico (RBAC).
//  * - Que la app es 100% stateless (sin sesiones de servidor).
//  * - Que el JwtAuthFilter corra ANTES del filtro estandar de login por
//  *    usuario/password, para poder autenticar via token en cada request.
//  */

// @Configuration
// @EnableWebSecurity
// public class SecurityConfig {

//     private final JwtAuthFilter jwtAuthFilter;
//     private final CustomUserDetailsService userDetailsService;
//     private final CorsConfigurationSource corsConfigurationSource;

//     public SecurityConfig(JwtAuthFilter jwtAuthFilter,
//                            CustomUserDetailsService userDetailsService,
//                            CorsConfigurationSource corsConfigurationSource) {
//         this.jwtAuthFilter = jwtAuthFilter;
//         this.userDetailsService = userDetailsService;
//         this.corsConfigurationSource = corsConfigurationSource;
//     }

//     @Bean
//     public PasswordEncoder passwordEncoder() {
//         return new BCryptPasswordEncoder();
//     }

//     @Bean
//     public DaoAuthenticationProvider authenticationProvider() {
//         DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
//         provider.setPasswordEncoder(passwordEncoder());
//         return provider;
//     }

//     @Bean
//     public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
//         return config.getAuthenticationManager();
//     }

//     @Bean
//     public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//         http
//                 .csrf(csrf -> csrf.disable())
//                 .cors(cors -> cors.configurationSource(corsConfigurationSource))
//                 .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//                 .authorizeHttpRequests(auth -> auth
//                     // Publicos
//                     .requestMatchers("/api/v1/auth/**").permitAll()
//                     .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()

//                     // Catalogos
//                     .requestMatchers(HttpMethod.GET, "/api/v1/doctors/**", "/api/v1/specialties/**").hasAnyAuthority(
//                             "ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT", "ROLE_SECRETARY")
//                     .requestMatchers(HttpMethod.POST, "/api/v1/doctors/**").hasAuthority("ROLE_ADMIN")
//                     .requestMatchers(HttpMethod.POST, "/api/v1/specialties").hasAuthority("ROLE_ADMIN")
//                     .requestMatchers(HttpMethod.PUT, "/api/v1/specialties/*").hasAuthority("ROLE_ADMIN")

//                     // Horarios
//                     .requestMatchers("/api/v1/schedules/available/**").hasAnyAuthority(
//                             "ROLE_PATIENT", "ROLE_ADMIN", "ROLE_SECRETARY")
//                     .requestMatchers(HttpMethod.POST, "/api/v1/schedules").hasAuthority("ROLE_ADMIN")
//                     .requestMatchers(HttpMethod.GET, "/api/v1/schedules").hasAnyAuthority("ROLE_ADMIN", "ROLE_DOCTOR")

//                     // Citas: endpoints especificos del doctor
//                     .requestMatchers("/api/v1/appointments/my-agenda").hasAuthority("ROLE_DOCTOR")
//                     .requestMatchers("/api/v1/appointments/my-agenda-range").hasAuthority("ROLE_DOCTOR")
//                     .requestMatchers("/api/v1/appointments/*/diagnosis").hasAuthority("ROLE_DOCTOR")
//                     .requestMatchers("/api/v1/appointments/*/documents").hasAuthority("ROLE_DOCTOR")

//                     // Citas: endpoint especifico del paciente
//                     .requestMatchers("/api/v1/appointments/my-history").hasAuthority("ROLE_PATIENT")

//                     // Documentos (PDF adjunto)
//                     .requestMatchers(HttpMethod.POST, "/api/v1/documents/*/file").hasAuthority("ROLE_DOCTOR")
//                     .requestMatchers(HttpMethod.GET, "/api/v1/documents/*/file").hasAnyAuthority("ROLE_DOCTOR", "ROLE_PATIENT")

//                     // Citas: regla generica (crear cita). Va al final, despues de las especificas
//                     .requestMatchers("/api/v1/appointments/**").hasAnyAuthority(
//                             "ROLE_PATIENT", "ROLE_ADMIN", "ROLE_SECRETARY")

//                     .anyRequest().authenticated())
//                 .authenticationProvider(authenticationProvider())
//                 .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

//         return http.build();
//     }
// }