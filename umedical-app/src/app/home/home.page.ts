import { Component, OnInit, inject, ChangeDetectorRef, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import {
  IonHeader, IonToolbar, IonTitle, IonContent, IonButton,
  IonSpinner, ViewWillEnter
} from '@ionic/angular';
import { AuthService } from '../core/auth.service';
import { AppointmentsService } from '../core/appointments.service';
import { Appointment } from '../core/models';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, DatePipe, RouterLink, IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonSpinner],
  template: `
    <ion-header>
      <ion-toolbar><ion-title>U-Medical</ion-title></ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <h1 class="page-title">Hola, {{ firstName() }}</h1>
      <p class="ion-margin-bottom" style="color: var(--color-muted)">
        Bienvenido a tu portal de salud.
      </p>

      @if (loading) {
        <ion-spinner name="dots"></ion-spinner>
      } @else if (nextAppointment) {
        <div class="card ion-margin-bottom">
          <p style="color: var(--color-muted); font-size: 0.85rem;">Próxima cita</p>
          <h2>{{ nextAppointment.doctorName }} — {{ nextAppointment.specialty }}</h2>
          <p>{{ nextAppointment.appointmentDate | date:'fullDate' }} · {{ nextAppointment.appointmentTime.substring(0,5) }}</p>
        </div>
      } @else {
        <div class="card ion-margin-bottom">
          <p>No tienes citas pendientes.</p>
        </div>
      }

      <ion-button expand="block" routerLink="/tabs/booking">Agendar nueva cita</ion-button>
    </ion-content>
  `,
})
export class HomePage implements OnInit, ViewWillEnter {
  private auth = inject(AuthService);
  private appointments = inject(AppointmentsService);
  private cdr = inject(ChangeDetectorRef);

  user = this.auth.currentUser;
  firstName = computed(() => this.user()?.fullName?.split(' ')[0] ?? '');
  nextAppointment: Appointment | null = null;
  loading = false;

  ngOnInit() {
    this.loadNext();
  }

  ionViewWillEnter() {
    this.loadNext();
  }

  loadNext() {
    this.loading = true;
    this.cdr.markForCheck();
    this.appointments.getMyHistory().subscribe({
      next: (data) => {
        const pending = data
          .filter((a) => a.status === 'PENDIENTE')
          .sort((a, b) => (a.appointmentDate + a.appointmentTime).localeCompare(b.appointmentDate + b.appointmentTime));
        this.nextAppointment = pending[0] ?? null;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }
}