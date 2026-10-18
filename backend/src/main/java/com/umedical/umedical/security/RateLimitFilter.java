package com.umedical.umedical.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

/** Limita por IP el registro y el login. Responde 429 con el header Retry-After. */
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final boolean enabled;
    private final int registerPerHour;
    private final int loginPerMinute;

    public RateLimitFilter(RateLimiter rateLimiter, boolean enabled, int registerPerHour, int loginPerMinute) {
        this.rateLimiter = rateLimiter;
        this.enabled = enabled;
        this.registerPerHour = registerPerHour;
        this.loginPerMinute = loginPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (enabled && "POST".equalsIgnoreCase(request.getMethod())) {
            String path = request.getRequestURI();
            String ip = request.getRemoteAddr();
            long retryAfter = 0;

            if ("/api/v1/auth/register".equals(path)) {
                retryAfter = rateLimiter.check("register:" + ip, registerPerHour, Duration.ofHours(1));
            } else if ("/api/v1/auth/login".equals(path)) {
                retryAfter = rateLimiter.check("login:" + ip, loginPerMinute, Duration.ofMinutes(1));
            }

            if (retryAfter > 0) {
                response.setStatus(429);
                response.setHeader("Retry-After", String.valueOf(retryAfter));
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now()
                        + "\",\"status\":429,\"error\":\"Too Many Requests\","
                        + "\"message\":\"Demasiados intentos. Intenta de nuevo en " + retryAfter + " segundos\","
                        + "\"details\":null}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}