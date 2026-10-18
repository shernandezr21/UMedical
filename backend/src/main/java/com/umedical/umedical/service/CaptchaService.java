package com.umedical.umedical.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Valida el token de Cloudflare Turnstile contra su API (siteverify). Apagado por defecto
 * (app.captcha.enabled=false). Si esta activo y la verificacion falla o no se puede completar,
 * el registro se rechaza.
 */
@Service
public class CaptchaService {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final Pattern SUCCESS = Pattern.compile("\"success\"\\s*:\\s*true");

    private final boolean enabled;
    private final String secret;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public CaptchaService(@Value("${app.captcha.enabled:false}") boolean enabled,
                          @Value("${app.captcha.secret:}") String secret) {
        if (enabled && secret.isBlank()) {
            throw new IllegalStateException("app.captcha.secret es obligatorio cuando app.captcha.enabled=true");
        }
        this.enabled = enabled;
        this.secret = secret;
    }

    public void verify(String token, String clientIp) {
        if (!enabled) {
            return;
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Completa la verificacion anti-bots");
        }

        StringBuilder form = new StringBuilder()
                .append("secret=").append(encode(secret))
                .append("&response=").append(encode(token));
        if (clientIp != null && !clientIp.isBlank()) {
            form.append("&remoteip=").append(encode(clientIp));
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(VERIFY_URL))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 || !SUCCESS.matcher(response.body()).find()) {
                throw new IllegalArgumentException("No se pudo validar la verificacion anti-bots. Intenta de nuevo");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo contactar el servicio de verificacion anti-bots. Intenta de nuevo");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalArgumentException("La verificacion anti-bots fue interrumpida. Intenta de nuevo");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}