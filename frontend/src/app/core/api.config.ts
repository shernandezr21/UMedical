export const API_BASE_URL = 'http://localhost:8080/api/v1';

/**
 * Clave PUBLICA de Cloudflare Turnstile. Vacia = sin captcha (desarrollo).
 * Para probarlo: '1x00000000000000000000AA' (clave de prueba) y en el backend
 * CAPTCHA_ENABLED=true con CAPTCHA_SECRET=1x0000000000000000000000000000000AA.
 * En produccion van las claves reales de nuestro panel de Cloudflare.
 */

export const CAPTCHA_SITE_KEY = '';

// Clave de prueba de Cloudflare Turnstile: 1x00000000000000000000AA
