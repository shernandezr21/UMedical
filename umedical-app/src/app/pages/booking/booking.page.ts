import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  IonContent, IonHeader, IonToolbar, IonTitle, IonItem, IonLabel,
  IonSelect, IonSelectOption, IonDatetime, IonButton, IonText,
  IonTextarea, IonSpinner, ToastController
} from '@ionic/angular';
import { AppointmentsService } from '../../core/appointments.service';
import { Specialty, Doctor, AvailableSlot } from '../../core/models';

@Component({
  selector: 'app-booking',
  standalone: true,
  imports: [
    CommonModule, FormsModule, IonContent, IonHeader, IonToolbar, IonTitle,
    IonItem, IonLabel, IonSelect, IonSelectOption, IonDatetime, IonButton,
    IonText, IonTextarea, IonSpinner,
  ],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-title>Agendar cita</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding">
      <ion-item>
        <ion-label>Especialidad</ion-label>
        <ion-select [(ngModel)]="specialtyId" (ionChange)="onSpecialtyChange()" interface="action-sheet">
          @for (s of specialties; track s.id) {
            <ion-select-option [value]="s.id">{{ s.name }}</ion-select-option>
          }
        </ion-select>
      </ion-item>

      <ion-item>
        <ion-label>Médico</ion-label>
        <ion-select [(ngModel)]="doctorId" (ionChange)="onDoctorChange()" interface="action-sheet"
                    [disabled]="!specialtyId">
          @for (d of doctors; track d.id) {
            <ion-select-option [value]="d.id">{{ d.fullName }}</ion-select-option>
          }
        </ion-select>
      </ion-item>

      <ion-item>
        <ion-label>Fecha</ion-label>
        <ion-datetime presentation="date" [(ngModel)]="selectedDate"
                      (ionChange)="onDateChange()" [min]="minDate"></ion-datetime>
      </ion-item>

      @if (loadingSlots) {
        <ion-spinner name="dots"></ion-spinner>
      } @else if (doctorId && selectedDate) {
        <ion-item>
          <ion-label>Horario disponible</ion-label>
          <ion-select [(ngModel)]="selectedTime" interface="action-sheet"
                      [disabled]="!availableSlots.length">
            @for (slot of availableSlots; track slot.time) {
              <ion-select-option [value]="slot.time">{{ slot.time.substring(0,5) }}</ion-select-option>
            }
          </ion-select>
        </ion-item>
        @if (!availableSlots.length) {
          <ion-text color="medium"><p>No hay horarios disponibles ese día.</p></ion-text>
        }
      }

      <ion-item>
        <ion-label position="floating">Motivo de la consulta</ion-label>
        <ion-textarea [(ngModel)]="reason" rows="3"></ion-textarea>
      </ion-item>

      @if (error) { <ion-text color="danger"><p>{{ error }}</p></ion-text> }
      @if (success) { <ion-text color="success"><p>{{ success }}</p></ion-text> }

      <ion-button expand="block" class="ion-margin-top"
                  [disabled]="!canSubmit() || booking"
                  (click)="submit()">
        @if (booking) { <ion-spinner name="dots"></ion-spinner> } @else { Reservar cita }
      </ion-button>
    </ion-content>
  `,
})
export class BookingPage implements OnInit {
  private appointments = inject(AppointmentsService);
  private toastCtrl = inject(ToastController);
  private cdr = inject(ChangeDetectorRef);

  specialties: Specialty[] = [];
  doctors: Doctor[] = [];
  availableSlots: AvailableSlot[] = [];

  specialtyId?: number;
  doctorId?: number;
  selectedDate?: string;
  selectedTime?: string;
  reason = '';

  loadingSlots = false;
  booking = false;
  error = '';
  success = '';
  minDate = new Date().toISOString();

  ngOnInit() {
    this.appointments.getSpecialties().subscribe({
      next: (data) => {
        this.specialties = data;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'No se pudieron cargar las especialidades';
        this.cdr.markForCheck();
      },
    });
  }

  onSpecialtyChange() {
    this.doctorId = undefined;
    this.doctors = [];
    this.resetSlots();
    if (!this.specialtyId) return;
    this.appointments.getDoctors(this.specialtyId).subscribe({
      next: (data) => {
        this.doctors = data;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'No se pudieron cargar los médicos';
        this.cdr.markForCheck();
      },
    });
  }

  onDoctorChange() { this.resetSlots(); this.fetchSlots(); }
  onDateChange() { this.resetSlots(); this.fetchSlots(); }

  private resetSlots() {
    this.availableSlots = [];
    this.selectedTime = undefined;
    this.cdr.markForCheck();
  }

  private fetchSlots() {
    if (!this.doctorId || !this.selectedDate) return;
    this.loadingSlots = true;
    this.cdr.markForCheck();
    const date = this.selectedDate.substring(0, 10);
    this.appointments.getAvailableSlots(this.doctorId, date).subscribe({
      next: (slots) => {
        this.availableSlots = slots;
        this.loadingSlots = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'No se pudieron cargar los horarios';
        this.loadingSlots = false;
        this.cdr.markForCheck();
      },
    });
  }

  canSubmit() {
    return Boolean(this.doctorId && this.selectedDate && this.selectedTime && this.reason.trim());
  }

  submit() {
    if (!this.canSubmit()) return;
    this.booking = true;
    this.error = '';
    this.success = '';
    this.cdr.markForCheck();

    this.appointments.bookAppointment({
      doctorId: this.doctorId!,
      appointmentDate: this.selectedDate!.substring(0, 10),
      appointmentTime: this.selectedTime!,
      reason: this.reason.trim(),
    }).subscribe({
      next: async () => {
        this.booking = false;
        this.success = '¡Cita reservada con éxito!';
        this.doctorId = undefined;
        this.selectedDate = undefined;
        this.selectedTime = undefined;
        this.reason = '';
        this.resetSlots();
        this.cdr.markForCheck();

        const toast = await this.toastCtrl.create({
          message: '¡Cita reservada con éxito!',
          duration: 3500,
          color: 'success',
          position: 'top',
        });
        await toast.present();
      },
      error: (e) => {
        this.booking = false;
        this.error = e.error?.message || 'No se pudo reservar la cita';
        this.cdr.markForCheck();
      },
    });
  }
}