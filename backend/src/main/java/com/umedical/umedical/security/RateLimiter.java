package com.umedical.umedical.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitador de peticiones en memoria (ventana fija por clave). Sirve para una sola instancia del backend:
 * si algun dia corren varias detras de un balanceador, el contador debe moverse a un almacen compartido (Redis).
 */
@Component
public class RateLimiter {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private static final class Window {
        final long startMillis;
        final long lengthMillis;
        int count;

        Window(long startMillis, long lengthMillis) {
            this.startMillis = startMillis;
            this.lengthMillis = lengthMillis;
        }
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /** Devuelve 0 si la peticion esta permitida; si no, los segundos que faltan para que se reinicie la ventana. */
    public long check(String key, int limit, Duration windowLength) {
        long now = System.currentTimeMillis();
        long lengthMillis = windowLength.toMillis();
        long[] retryAfterSeconds = {0};

        windows.compute(key, (k, window) -> {
            if (window == null || now - window.startMillis >= window.lengthMillis) {
                Window fresh = new Window(now, lengthMillis);
                fresh.count = 1;
                return fresh;
            }
            if (window.count >= limit) {
                long remainingMillis = window.startMillis + window.lengthMillis - now;
                retryAfterSeconds[0] = Math.max(1, (remainingMillis + 999) / 1000);
                return window;
            }
            window.count++;
            return window;
        });

        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(w -> now - w.startMillis >= w.lengthMillis);
        }
        return retryAfterSeconds[0];
    }
}