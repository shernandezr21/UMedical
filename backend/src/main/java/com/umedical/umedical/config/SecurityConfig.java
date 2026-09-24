package com.umedical.umedical.config;

import com.umedical.umedical.security.CustomUserDetailsService;
import com.umedical.umedical.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * Configuracion central de seguridad. Define:
 * - El PasswordEncoder (BCrypt) usado para comparar contrasenas.
 * - Que rutas son publicas (login, swagger) y cuales requieren
 *    autenticacion + que rol especifico (RBAC).
 * - Que la app es 100% stateless (sin sesiones de servidor).
 * - Que el JwtAuthFilter corra ANTES del filtro estandar de login por
 *    usuario/password, para poder autenticar via token en cada request.
 */

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                           CustomUserDetailsService userDetailsService,
                           CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.corsConfigurationSource = corsConfigurationSource;
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
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publicos
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()

                        // RBAC por rol (endpoints minimos de Fase 1)
                        .requestMatchers("/api/v1/doctors/**", "/api/v1/specialties/**").hasAnyAuthority(
                                "ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_PATIENT", "ROLE_SECRETARY")
                        .requestMatchers("/api/v1/schedules/available/**").hasAnyAuthority(
                                "ROLE_PATIENT", "ROLE_ADMIN", "ROLE_SECRETARY")
                        .requestMatchers("/api/v1/appointments/my-history").hasAuthority("ROLE_PATIENT")
                        .requestMatchers("/api/v1/appointments/*/diagnosis").hasAuthority("ROLE_DOCTOR")
                        .requestMatchers("/api/v1/appointments/**").hasAnyAuthority(
                                "ROLE_PATIENT", "ROLE_ADMIN", "ROLE_SECRETARY")

                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}