import { Component, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ViewWillEnter, ModalController } from '@ionic/angular';
import {
  IonHeader, IonToolbar, IonTitle,
  IonContent, IonSegment, IonSegmentButton, IonLabel,
  IonIcon, IonSpinner, IonText, IonRefresher, IonRefresherContent
} from '@ionic/angular';
import { AppointmentsService } from '../../core/appointments.service';
import { Appointment } from '../../core/models';
import { initials, avatarColor } from '../../core/avatar.util';
import { AppointmentDetailModal } from './appointment-detail.modal';

@Component({
  selector: 'app-my-appointments',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DatePipe, IonHeader, IonToolbar, IonTitle,
    IonContent, IonSegment, IonSegmentButton, IonLabel, IonIcon,
    IonSpinner, IonText, IonRefresher, IonRefresherContent,
  ],
  template: `
    <ion-header>
      <ion-toolbar><ion-title>Mis citas</ion-title></ion-toolbar>
      <ion-toolbar>
        <ion-segment [(ngModel)]="segment">
          <ion-segment-button value="pendientes">
            <ion-label>Pendientes</ion-label>
          </ion-segment-button>
          <ion-segment-button value="historial">
            <ion-label>Historial</ion-label>
          </ion-segment-button>
        </ion-segment>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding">
      <ion-refresher slot="fixed" (ionRefresh)="load($event)">
        <ion-refresher-content></ion-refresher-content>
      </ion-refresher>

      @if (loading()) {
        <div class="ion-text-center ion-padding"><ion-spinner name="dots"></ion-spinner></div>
      } @else if (error()) {
        <ion-text color="danger" class="ion-padding"><p>{{ error() }}</p></ion-text>
      } @else {
        @for (a of filtered(); track a.id) {
          <div class="appt-card" [ngClass]="'appt-card--' + a.status.toLowerCase()" (click)="openDetail(a)">
            <div class="appt-card__top">
              <div class="appt-card__doctor-row">
                <span class="avatar" [style.background]="avatarColor(a.doctorName)">{{ initials(a.doctorName) }}</span>
                <div>
                  <h2 class="appt-card__doctor">{{ a.doctorName }}</h2>
                  <p class="appt-card__specialty">{{ a.specialty }}</p>
                </div>
              </div>
              <span class="status-badge" [ngClass]="'status-badge--' + a.status.toLowerCase()">{{ a.status }}</span>
            </div>

            <div class="appt-card__meta">
              <span class="appt-card__meta-item">
                <ion-icon name="calendar-outline"></ion-icon>
                {{ a.appointmentDate | date:'d MMM y' }}
              </span>
              <span class="appt-card__meta-item">
                <ion-icon name="time-outline"></ion-icon>
                {{ a.appointmentTime.substring(0,5) }}
              </span>
            </div>

            <p class="appt-card__reason">{{ a.reason }}</p>
          </div>
        } @empty {
          <div class="empty-state">
            <ion-icon
              [name]="segment === 'pendientes' ? 'checkmark-circle-outline' : 'calendar-outline'"
              class="empty-state__icon"></ion-icon>
            <p class="empty-state__title">
              {{ segment === 'pendientes' ? 'Todo al día' : 'Aún no hay historial' }}
            </p>
            <p class="empty-state__subtitle">
              {{ segment === 'pendientes'
                ? 'No tienes citas pendientes por el momento.'
                : 'Aquí verás tus citas completadas y canceladas.' }}
            </p>
          </div>
        }
      }
    </ion-content>
  `,
  styles: [`
    .appt-card {
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-left: 4px solid var(--color-muted);
      border-radius: var(--radius-md);
      padding: 1rem 1.1rem;
      margin-bottom: 0.85rem;
      cursor: pointer;
    }
    .appt-card--pendiente { border-left-color: var(--color-pendiente); }
    .appt-card--completada { border-left-color: var(--color-completada); }
    .appt-card--cancelada { border-left-color: var(--color-cancelada); }

    .appt-card__top {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 0.5rem;
    }
    .appt-card__doctor-row {
      display: flex;
      align-items: center;
      gap: 0.65rem;
    }
    .avatar {
      flex-shrink: 0;
      width: 38px;
      height: 38px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-family: var(--font-display);
      font-size: 0.85rem;
      font-weight: 600;
    }
    .appt-card__doctor {
      font-family: var(--font-display);
      font-size: 1.05rem;
      margin: 0 0 0.15rem;
      color: var(--color-ink);
    }
    .appt-card__specialty {
      margin: 0;
      font-size: 0.85rem;
      color: var(--color-muted);
    }

    .appt-card__meta {
      display: flex;
      gap: 1.25rem;
      margin: 0.75rem 0 0.5rem;
    }
    .appt-card__meta-item {
      display: inline-flex;
      align-items: center;
      gap: 0.3rem;
      font-size: 0.85rem;
      color: var(--color-ink);
    }
    .appt-card__meta-item ion-icon {
      font-size: 1rem;
      color: var(--color-primary);
    }

    .appt-card__reason {
      margin: 0.4rem 0 0;
      font-size: 0.88rem;
      color: var(--color-muted);
      border-top: 1px solid var(--color-border);
      padding-top: 0.6rem;
    }

    .empty-state {
      text-align: center;
      padding: 3rem 1.5rem;
      color: var(--color-muted);
    }
    .empty-state__icon {
      font-size: 3rem;
      color: var(--color-primary-light);
      margin-bottom: 0.75rem;
    }
    .empty-state__title {
      font-family: var(--font-display);
      font-size: 1.1rem;
      color: var(--color-ink);
      margin: 0 0 0.3rem;
    }
    .empty-state__subtitle {
      font-size: 0.88rem;
      margin: 0;
    }
  `],
})
export class MyAppointmentsPage implements ViewWillEnter {
  private appointments = inject(AppointmentsService);
  private modalCtrl = inject(ModalController);

  all = signal<Appointment[]>([]);
  segment: 'pendientes' | 'historial' = 'pendientes';
  loading = signal(false);
  error = signal('');

  initials = initials;
  avatarColor = avatarColor;

  ionViewWillEnter() { this.load(); }

  load(event?: any) {
    this.loading.set(!event);
    this.error.set('');
    this.appointments.getMyHistory().subscribe({
      next: (data) => {
        this.all.set(data);
        this.loading.set(false);
        event?.target?.complete();
      },
      error: () => {
        this.error.set('No se pudo cargar tu historial de citas');
        this.loading.set(false);
        event?.target?.complete();
      },
    });
  }

  filtered() {
    return this.segment === 'pendientes'
      ? this.all().filter((a) => a.status === 'PENDIENTE')
      : this.all().filter((a) => a.status !== 'PENDIENTE');
  }

async openDetail(appointment: Appointment) {
  const modal = await this.modalCtrl.create({
    component: AppointmentDetailModal,
    componentProps: { appointment },
  });
  await modal.present();
  const { data } = await modal.onWillDismiss();
  if (data?.refreshed) this.load();
}
}