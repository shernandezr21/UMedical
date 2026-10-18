import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-unauthorized',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div style="min-height: 100vh; display: flex; align-items: center; justify-content: center;">
      <div class="card" style="max-width: 420px; text-align: center;">
        <h2 style="margin-bottom: 0.5rem;">No tienes acceso a esta seccion</h2>
        <p class="muted" style="margin-bottom: 1.25rem;">Tu perfil no tiene permisos para ver esta pagina.</p>
        <a class="btn btn-primary" routerLink="/dashboard">Volver al inicio</a>
      </div>
    </div>
  `
})
export class Unauthorized {}