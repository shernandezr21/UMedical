import { Component, OnInit, inject, signal } from '@angular/core';
import { IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonIcon, AlertController } from '@ionic/angular';
import { AuthService } from '../../core/auth.service';
import { AppointmentsService } from '../../core/appointments.service';
import { LoginResponse } from '../../core/models';
import { initials, avatarColor } from '../../core/avatar.util';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonIcon],
  template: `
    <ion-header>
      <ion-toolbar><ion-title>Perfil</ion-title></ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding">
      @if (user()) {
        <div class="profile-header">
          <span class="avatar" [style.background]="avatarColor(user()!.fullName)">
            {{ initials(user()!.fullName) }}
          </span>
          <h1 class="profile-header__name">{{ user()!.fullName }}</h1>
          <span class="profile-header__badge">Paciente</span>
        </div>

        <div class="stats-row">
          <div class="stat-card">
            <span class="stat-card__value">{{ pendingCount() }}</span>
            <span class="stat-card__label">Pendientes</span>
          </div>
          <div class="stat-card">
            <span class="stat-card__value">{{ completedCount() }}</span>
            <span class="stat-card__label">Completadas</span>
          </div>
        </div>

        <p class="section-label">Datos de la cuenta</p>
        <div class="card">
          <div class="info-row">
            <ion-icon name="person-outline"></ion-icon>
            <div>
              <p class="info-row__label">Usuario</p>
              <p class="info-row__value">{{ user()!.username }}</p>
            </div>
          </div>
        </div>

        <div class="notice">
          <ion-icon name="information-circle-outline"></ion-icon>
          <p>La edición de perfil y el cambio de contraseña estarán disponibles próximamente.</p>
        </div>
      }

      <ion-button expand="block" color="danger" fill="outline" class="ion-margin-top" (click)="confirmLogout()">
        <ion-icon name="log-out-outline" slot="start"></ion-icon>
        Cerrar sesión
      </ion-button>

      <p class="app-version">U-Medical · Portal de pacientes</p>
    </ion-content>
  `,
  styles: [`
    .profile-header {
      text-align: center;
      padding: 1rem 0 1.5rem;
    }
    .avatar {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 76px;
      height: 76px;
      border-radius: 50%;
      color: #fff;
      font-family: var(--font-display);
      font-size: 1.6rem;
      font-weight: 600;
      margin-bottom: 0.75rem;
    }
    .profile-header__name {
      font-family: var(--font-display);
      font-size: 1.3rem;
      color: var(--color-ink);
      margin: 0 0 0.4rem;
    }
    .profile-header__badge {
      display: inline-block;
      background: var(--color-pendiente-bg);
      color: var(--color-pendiente);
      font-size: 0.78rem;
      font-weight: 600;
      padding: 0.25rem 0.7rem;
      border-radius: 999px;
    }

    .stats-row {
      display: flex;
      gap: 0.75rem;
      margin-bottom: 1.4rem;
    }
    .stat-card {
      flex: 1;
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 0.9rem;
      text-align: center;
    }
    .stat-card__value {
      display: block;
      font-family: var(--font-display);
      font-size: 1.6rem;
      color: var(--color-primary);
    }
    .stat-card__label {
      display: block;
      font-size: 0.78rem;
      color: var(--color-muted);
      margin-top: 0.1rem;
    }

    .section-label {
      font-size: 0.8rem;
      color: var(--color-muted);
      text-transform: uppercase;
      letter-spacing: 0.04em;
      margin: 0 0 0.5rem;
    }

    .info-row {
      display: flex;
      align-items: center;
      gap: 0.7rem;
    }
    .info-row ion-icon {
      font-size: 1.2rem;
      color: var(--color-primary);
    }
    .info-row__label {
      margin: 0;
      font-size: 0.78rem;
      color: var(--color-muted);
    }
    .info-row__value {
      margin: 0.1rem 0 0;
      font-size: 0.95rem;
      color: var(--color-ink);
    }

    .notice {
      display: flex;
      gap: 0.6rem;
      align-items: flex-start;
      background: var(--color-bg);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 0.8rem 0.9rem;
      color: var(--color-muted);
      font-size: 0.85rem;
      margin-top: 1rem;
    }
    .notice ion-icon { font-size: 1.2rem; flex-shrink: 0; margin-top: 0.1rem; }
    .notice p { margin: 0; }

    .app-version {
      text-align: center;
      font-size: 0.78rem;
      color: var(--color-muted);
      margin-top: 2rem;
    }
  `],
})
export class ProfilePage implements OnInit {
  private auth = inject(AuthService);
  private appointments = inject(AppointmentsService);
  private alertCtrl = inject(AlertController);

  user = signal<LoginResponse | null>(null);
  pendingCount = signal(0);
  completedCount = signal(0);

  initials = initials;
  avatarColor = avatarColor;

  async ngOnInit() {
    this.user.set(await this.auth.getUser());
    this.appointments.getMyHistory().subscribe({
      next: (data) => {
        this.pendingCount.set(data.filter((a) => a.status === 'PENDIENTE').length);
        this.completedCount.set(data.filter((a) => a.status === 'COMPLETADA').length);
      },
    });
  }

  async confirmLogout() {
    const alert = await this.alertCtrl.create({
      header: 'Cerrar sesión',
      message: '¿Seguro que quieres salir de tu cuenta?',
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        { text: 'Cerrar sesión', role: 'destructive', handler: () => this.auth.logout() },
      ],
    });
    await alert.present();
  }
}