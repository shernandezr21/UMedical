import { Component, inject } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  template: `
    <h2>Bienvenido, {{ authService.getFullName() }}</h2>
    <p style="color: var(--color-muted); margin-top: 0.5rem;">
      Selecciona una opcion del menu para comenzar.
    </p>
  `
})
export class Home {
  authService = inject(AuthService);
}