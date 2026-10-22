package com.umedical.umedical.config;

import com.umedical.umedical.security.CustomUserDetailsService;
import com.umedical.umedical.security.JwtAuthFilter;
import com.umedical.umedical.security.RateLimitFilter;
import com.umedical.umedical.security.RateLimiter;
import com.umedical.umedical.security.RestAccessDeniedHandler;
import com.umedical.umedical.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.filter.CorsFilter;

/**
 * Seguridad central: JWT sin estado, RBAC por ruta, limite de intentos y respuestas 401/403 en JSON.
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
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          CustomUserDetailsService userDetailsService,
                          CorsConfigurationSource corsConfigurationSource,
                          RestAuthenticationEntryPoint authenticationEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler,
                          RateLimiter rateLimiter,
                          @Value("${app.rate-limit.enabled:true}") boolean rateLimitEnabled,
                          @Value("${app.rate-limit.register-per-hour:10}") int registerPerHour,
                          @Value("${app.rate-limit.login-per-minute:20}") int loginPerMinute) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.corsConfigurationSource = corsConfigurationSource;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        // No es un @Bean a proposito: asi no se registra tambien como filtro de servlet (correria antes que CORS).
        this.rateLimitFilter = new RateLimitFilter(rateLimiter, rateLimitEnabled, registerPerHour, loginPerMinute);
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

                        // Medicos y catalogos (el listado de administracion va ANTES del listado general)
                        .requestMatchers("/api/v1/doctors/me/**").hasAuthority("ROLE_DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/doctors/management").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/doctors/**", "/api/v1/specialties/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT", "ROLE_SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/api/v1/doctors/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/doctors/*").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/doctors/*/active").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/specialties").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/specialties/*").hasAuthority("ROLE_ADMIN")

                        // Horarios
                        .requestMatchers("/api/v1/schedules/available/**").hasAnyAuthority("ROLE_PATIENT", "ROLE_ADMIN", "ROLE_SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/api/v1/schedules").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/schedules/*").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/schedules/*").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/schedules").hasAnyAuthority("ROLE_ADMIN", "ROLE_DOCTOR")

                        // Reportes del panel
                        .requestMatchers("/api/v1/reports/**").hasAuthority("ROLE_ADMIN")

                        // Citas: doctor
                        .requestMatchers(HttpMethod.GET, "/api/v1/appointments/my-agenda").hasAuthority("ROLE_DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/appointments/my-agenda-range").hasAuthority("ROLE_DOCTOR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/appointments/*/diagnosis").hasAuthority("ROLE_DOCTOR")

                        // Citas: documentos (doctor dueno o paciente dueno; el servicio verifica la propiedad)
                        .requestMatchers(HttpMethod.GET, "/api/v1/appointments/*/documents").hasAnyAuthority(
                                "ROLE_DOCTOR", "ROLE_PATIENT")

                        // Citas: paciente y personal de la clinica
                        .requestMatchers(HttpMethod.POST, "/api/v1/appointments").hasAnyAuthority(
                                "ROLE_PATIENT", "ROLE_SECRETARY", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/appointments").hasAnyAuthority(
                                "ROLE_ADMIN", "ROLE_SECRETARY")
                        .requestMatchers(HttpMethod.GET, "/api/v1/appointments/my-history").hasAuthority("ROLE_PATIENT")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/appointments/*/reschedule").hasAnyAuthority(
                                "ROLE_PATIENT", "ROLE_ADMIN", "ROLE_SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/api/v1/appointments/*/cancel").hasAnyAuthority(
                                "ROLE_PATIENT", "ROLE_DOCTOR", "ROLE_ADMIN", "ROLE_SECRETARY")

                        // Archivos PDF de los documentos
                        .requestMatchers(HttpMethod.POST, "/api/v1/documents/*/file").hasAuthority("ROLE_DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/documents/*/file").hasAnyAuthority(
                                "ROLE_DOCTOR", "ROLE_PATIENT")

                        // Perfil del paciente y cambio de contrasena (ANTES de /patients/* y de /users/**)
                        .requestMatchers(HttpMethod.GET, "/api/v1/patients/me").hasAuthority("ROLE_PATIENT")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/patients/me").hasAuthority("ROLE_PATIENT")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/me/password").authenticated()

                        // Usuarios: solo el administrador
                        .requestMatchers("/api/v1/users/**").hasAuthority("ROLE_ADMIN")

                        // Pacientes: busqueda y alta por la secretaria o el admin
                        .requestMatchers(HttpMethod.GET, "/api/v1/patients").hasAnyAuthority("ROLE_ADMIN", "ROLE_SECRETARY")
                        .requestMatchers(HttpMethod.GET, "/api/v1/patients/*").hasAnyAuthority("ROLE_ADMIN", "ROLE_SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/api/v1/patients").hasAnyAuthority("ROLE_ADMIN", "ROLE_SECRETARY")

                        .requestMatchers(HttpMethod.POST, "/api/v1/appointments/*/no-show").hasAnyAuthority(   "ROLE_DOCTOR", "ROLE_ADMIN", "ROLE_SECRETARY")
                        .requestMatchers("/api/v1/audit/**").hasAuthority("ROLE_ADMIN")

                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterAfter(rateLimitFilter, CorsFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}