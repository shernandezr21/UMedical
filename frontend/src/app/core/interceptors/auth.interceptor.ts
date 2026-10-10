import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Agrega automaticamente el header "Authorization: Bearer <token>" a
 * TODA peticion HTTP saliente, sin que cada componente/servicio tenga
 * que acordarse de hacerlo manualmente. Equivalente en el frontend a lo
 * que JwtAuthFilter hace en el backend, pero al reves: aqui ADJUNTAMOS
 * el token, en el backend lo LEEMOS.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  if (!token) {
    return next(req);
  }

  const clonedRequest = req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  });

  return next(clonedRequest);
};