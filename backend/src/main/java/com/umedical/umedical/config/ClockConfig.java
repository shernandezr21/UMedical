package com.umedical.umedical.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Reloj con la zona horaria de la clinica (el servidor puede estar en otra zona, p. ej. Docker en UTC). */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clinicClock(@Value("${app.clinic-timezone:America/Guatemala}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}