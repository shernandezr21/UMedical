package com.umedical.umedical.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configurable por la propiedad app.cors.allowed-origins (variable CORS_ALLOWED_ORIGINS).
 * Acepta origenes exactos o patrones: "http://localhost:[*]" = cualquier puerto,
 * "http://192.168.*:[*]" = cualquier IP 192.168.x.x. Cubre Angular (4200), Ionic en el navegador (8100)
 * y el WebView de Capacitor (http/https://localhost, capacitor://localhost).
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> patterns = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(patterns);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

// package com.umedical.umedical.config;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.web.cors.CorsConfiguration;
// import org.springframework.web.cors.CorsConfigurationSource;
// import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// import java.util.Arrays;
// import java.util.List;

// /**
//  * Configuracion explicita de CORS para habilitar peticiones desde
//  * el panel Angular (http://localhost:4200) y la app Ionic
//  * (http://localhost:8100 en navegador / capacitor://localhost en dispositivo).
//  * Sin esto, el navegador bloquea las peticiones aunque el backend este
//  * funcionando perfecto y es una proteccion del lado del navegador, no del
//  * servidor, por eso hay que declararlo explicitamente aqui.
//  */
// @Configuration
// public class CorsConfig {

//     @Value("${app.cors.allowed-origins}")
//     private String allowedOrigins;

//     @Bean
//     public CorsConfigurationSource corsConfigurationSource() {
//         CorsConfiguration configuration = new CorsConfiguration();
//         configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
//         configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
//         configuration.setAllowedHeaders(List.of("*"));
//         configuration.setExposedHeaders(List.of("Authorization"));
//         configuration.setAllowCredentials(true);
//         configuration.setMaxAge(3600L);

//         UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//         source.registerCorsConfiguration("/**", configuration);
//         return source;
//     }
// }