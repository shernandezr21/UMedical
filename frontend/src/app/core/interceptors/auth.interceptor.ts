import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.getToken();
  const authReq = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      const isAuthCall = req.url.includes('/auth/login') || req.url.includes('/auth/register');
      // 401 = token ausente, invalido o vencido: cerrar sesion y volver al login
      if (error.status === 401 && !isAuthCall && authService.isAuthenticated()) {
        authService.logout();
        router.navigate(['/login'], { queryParams: { expired: '1' } });
      }
      return throwError(() => error);
    })
  );
};