import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import {
  IonHeader, IonToolbar, IonTitle, IonContent, IonButton,
  IonIcon, IonSpinner, ViewWillEnter
} from '@ionic/angular';
import { AuthService } from '../core/auth.service';
import { AppointmentsService } from '../core/appointments.service';
import { Appointment, LoginResponse } from '../core/models';
import { initials, avatarColor } from '../core/avatar.util';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, DatePipe, RouterLink, IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonIcon, IonSpinner],
  template: `
    <ion-header>
      <ion-toolbar><ion-title>U-Medical</ion-title></ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <h1 class="page-title">Hola, {{ firstName() }}</h1>
      <p class="ion-margin-bottom" style="color: var(--color-muted)">
        Bienvenido a tu portal de salud.
      </p>

      @if (loading()) {
        <ion-spinner name="dots"></ion-spinner>
      } @else if (nextAppointment()) {
        <div class="next-card">
          <p class="next-card__label">Próxima cita</p>
          <div class="next-card__doctor-row">
            <span class="avatar" [style.background]="avatarColor(nextAppointment()!.doctorName)">
              {{ initials(nextAppointment()!.doctorName) }}
            </span>
            <div>
              <h2 class="next-card__doctor">{{ nextAppointment()!.doctorName }}</h2>
              <p class="next-card__specialty">{{ nextAppointment()!.specialty }}</p>
            </div>
          </div>
          <div class="next-card__meta">
            <span><ion-icon name="calendar-outline"></ion-icon> {{ nextAppointment()!.appointmentDate | date:'d MMM y' }}</span>
            <span><ion-icon name="time-outline"></ion-icon> {{ nextAppointment()!.appointmentTime.substring(0,5) }}</span>
          </div>
        </div>
      } @else {
        <div class="empty-state">
          <ion-icon name="checkmark-circle-outline" class="empty-state__icon"></ion-icon>
          <p class="empty-state__title">Todo al día</p>
          <p class="empty-state__subtitle">No tienes citas pendientes por el momento.</p>
        </div>
      }

      <ion-button expand="block" class="ion-margin-top" routerLink="/tabs/booking">Agendar nueva cita</ion-button>
    </ion-content>
  `,
  styles: [`
    .next-card {
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-left: 4px solid var(--color-pendiente);
      border-radius: var(--radius-md);
      padding: 1.1rem 1.2rem;
      margin-bottom: 1rem;
    }
    .next-card__label {
      margin: 0 0 0.6rem;
      font-size: 0.8rem;
      color: var(--color-muted);
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }
    .next-card__doctor-row {
      display: flex;
      align-items: center;
      gap: 0.65rem;
    }
    .avatar {
      flex-shrink: 0;
      width: 42px;
      height: 42px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-family: var(--font-display);
      font-size: 0.9rem;
      font-weight: 600;
    }
    .next-card__doctor {
      font-family: var(--font-display);
      font-size: 1.1rem;
      margin: 0 0 0.1rem;
      color: var(--color-ink);
    }
    .next-card__specialty {
      margin: 0;
      font-size: 0.85rem;
      color: var(--color-muted);
    }
    .next-card__meta {
      display: flex;
      gap: 1.25rem;
      margin-top: 0.85rem;
      padding-top: 0.7rem;
      border-top: 1px solid var(--color-border);
      font-size: 0.85rem;
      color: var(--color-ink);
    }
    .next-card__meta span {
      display: inline-flex;
      align-items: center;
      gap: 0.3rem;
    }
    .next-card__meta ion-icon {
      color: var(--color-primary);
    }

    .empty-state {
      text-align: center;
      padding: 2.5rem 1rem;
      color: var(--color-muted);
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
    }
    .empty-state__icon {
      font-size: 2.8rem;
      color: var(--color-primary-light);
      margin-bottom: 0.6rem;
    }
    .empty-state__title {
      font-family: var(--font-display);
      font-size: 1.05rem;
      color: var(--color-ink);
      margin: 0 0 0.25rem;
    }
    .empty-state__subtitle {
      font-size: 0.86rem;
      margin: 0;
    }
  `],
})
export class HomePage implements OnInit, ViewWillEnter {
  private auth = inject(AuthService);
  private appointments = inject(AppointmentsService);

  user = signal<LoginResponse | null>(null);
  nextAppointment = signal<Appointment | null>(null);
  loading = signal(false);

  initials = initials;
  avatarColor = avatarColor;

  async ngOnInit() {
    this.user.set(await this.auth.getUser());
    this.loadNext();
  }

  ionViewWillEnter() { this.loadNext(); }

  firstName() { return this.user()?.fullName?.split(' ')[0] ?? ''; }

  loadNext() {
    this.loading.set(true);
    this.appointments.getMyHistory().subscribe({
      next: (data) => {
        const pending = data
          .filter((a) => a.status === 'PENDIENTE')
          .sort((a, b) => (a.appointmentDate + a.appointmentTime).localeCompare(b.appointmentDate + b.appointmentTime));
        this.nextAppointment.set(pending[0] ?? null);
        this.loading.set(false);
      },
      error: () => { this.loading.set(false); },
    });
  }
}