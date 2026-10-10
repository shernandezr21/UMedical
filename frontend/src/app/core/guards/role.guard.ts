import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Factory de guard: recibe la lista de roles permitidos para una ruta
 * y devuelve un CanActivateFn. Se usa en las rutas asi:
 * canActivate: [roleGuard(['ROLE_ADMIN'])]
 * Si el rol del usuario logueado no esta en la lista, lo redirige a
 * una pagina de "no autorizado" en vez de dejarlo entrar.
 */
export const roleGuard = (allowedRoles: string[]): CanActivateFn => {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const userRole = authService.getRole();

    if (userRole && allowedRoles.includes(userRole)) {
      return true;
    }

    router.navigate(['/unauthorized']);
    return false;
  };
};