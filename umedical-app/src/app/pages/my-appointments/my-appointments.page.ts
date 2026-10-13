import { Component, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { ViewWillEnter } from '@ionic/angular';
import { FormsModule } from '@angular/forms';
import {
  IonHeader, IonToolbar, IonTitle, IonButtons, IonButton, IonContent,
  IonSegment, IonSegmentButton, IonLabel, IonList, IonItem,
  IonBadge, IonSpinner, IonText, IonRefresher, IonRefresherContent
} from '@ionic/angular';
import { AppointmentsService } from '../../core/appointments.service';
import { Appointment } from '../../core/models';

@Component({
  selector: 'app-my-appointments',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DatePipe, IonHeader, IonToolbar, IonTitle, IonButtons, IonButton,
    IonContent, IonSegment, IonSegmentButton, IonLabel, IonList, IonItem,
    IonBadge, IonSpinner, IonText, IonRefresher, IonRefresherContent,
  ],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-button (click)="goBooking()">←</ion-button>
        </ion-buttons>
        <ion-title>Mis citas</ion-title>
      </ion-toolbar>
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

    <ion-content>
      <ion-refresher slot="fixed" (ionRefresh)="load($event)">
        <ion-refresher-content></ion-refresher-content>
      </ion-refresher>

      @if (loading) {
        <div class="ion-text-center ion-padding"><ion-spinner name="dots"></ion-spinner></div>
      } @else if (error) {
        <ion-text color="danger" class="ion-padding"><p>{{ error }}</p></ion-text>
      } @else {
        <ion-list>
          @for (a of filtered(); track a.id) {
            <ion-item>
              <ion-label>
                <h2>{{ a.doctorName }} — {{ a.specialty }}</h2>
                <p>{{ a.appointmentDate | date:'fullDate' }} · {{ a.appointmentTime.substring(0,5) }}</p>
                <p>{{ a.reason }}</p>
              </ion-label>
              <ion-badge slot="end" [color]="statusColor(a.status)">{{ a.status }}</ion-badge>
            </ion-item>
          } @empty {
            <ion-text color="medium" class="ion-padding">
              <p>No hay citas {{ segment === 'pendientes' ? 'pendientes' : 'en tu historial' }}.</p>
            </ion-text>
          }
        </ion-list>
      }
    </ion-content>
  `,
})
export class MyAppointmentsPage implements ViewWillEnter {
  private appointments = inject(AppointmentsService);
  private router = inject(Router);

  all: Appointment[] = [];
  segment: 'pendientes' | 'historial' = 'pendientes';
  loading = false;
  error = '';

  ionViewWillEnter() { this.load(); }

  load(event?: any) {
    this.loading = !event;
    this.error = '';
    this.appointments.getMyHistory().subscribe({
      next: (data) => {
        this.all = data;
        this.loading = false;
        event?.target?.complete();
      },
      error: () => {
        this.error = 'No se pudo cargar tu historial de citas';
        this.loading = false;
        event?.target?.complete();
      },
    });
  }

  filtered() {
    return this.segment === 'pendientes'
      ? this.all.filter((a) => a.status === 'PENDIENTE')
      : this.all.filter((a) => a.status !== 'PENDIENTE');
  }

  statusColor(status: string) {
    return status === 'PENDIENTE' ? 'warning' : status === 'COMPLETADA' ? 'success' : 'medium';
  }

  goBooking() { this.router.navigateByUrl('/booking'); }
}