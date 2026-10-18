import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  IonContent, IonHeader, IonToolbar, IonTitle, IonItem, IonLabel,
  IonSelect, IonSelectOption, IonDatetime, IonButton, IonText,
  IonTextarea, IonSpinner, IonIcon, ToastController
} from '@ionic/angular';
import { AppointmentsService } from '../../core/appointments.service';
import { Specialty, Doctor, AvailableSlot } from '../../core/models';
import { initials, avatarColor } from '../../core/avatar.util';
import { specialtyIcon } from '../../core/specialty-icon.util';

@Component({
  selector: 'app-booking',
  standalone: true,
  imports: [
    CommonModule, FormsModule, IonContent, IonHeader, IonToolbar, IonTitle,
    IonItem, IonLabel, IonSelect, IonSelectOption, IonDatetime, IonButton,
    IonText, IonTextarea, IonSpinner, IonIcon,
  ],
  template: `
    <ion-header>
      <ion-toolbar><ion-title>Agendar cita</ion-title></ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding">
      <p class="section-label">Especialidad</p>
      @if (specialties().length) {
        <div class="specialty-grid">
          @for (s of specialties(); track s.id) {
            <button type="button" class="specialty-card" [class.specialty-card--selected]="specialtyId === s.id"
                    (click)="selectSpecialty(s.id)">
              <span class="specialty-card__icon">
                <ion-icon [name]="specialtyIcon(s.name)"></ion-icon>
              </span>
              <span class="specialty-card__name">{{ s.name }}</span>
            </button>
          }
        </div>
      } @else {
        <ion-spinner name="dots"></ion-spinner>
      }

      @if (specialtyId) {
        <p class="section-label">Médico</p>
        @if (doctors().length) {
          <div class="doctor-list">
            @for (d of doctors(); track d.id) {
              <button type="button" class="doctor-card" [class.doctor-card--selected]="doctorId === d.id"
                      (click)="selectDoctor(d.id)">
                <span class="avatar" [style.background]="avatarColor(d.fullName)">{{ initials(d.fullName) }}</span>
                <div class="doctor-card__info">
                  <span class="doctor-card__name">{{ d.fullName }}</span>
                  @if (d.biography) { <span class="doctor-card__bio">{{ d.biography }}</span> }
                </div>
                @if (doctorId === d.id) {
                  <ion-icon name="checkmark-circle-outline" class="doctor-card__check"></ion-icon>
                }
              </button>
            }
          </div>
        } @else {
          <ion-text color="medium"><p>No hay médicos para esta especialidad.</p></ion-text>
        }
      }

      @if (doctorId) {
        <ion-item class="ion-margin-top">
          <ion-label>Fecha</ion-label>
          <ion-datetime presentation="date" locale="es-ES" [(ngModel)]="selectedDate"
                        (ionChange)="onDateChange()" [min]="minDate"></ion-datetime>
        </ion-item>
      }

      @if (loadingSlots()) {
        <ion-spinner name="dots"></ion-spinner>
      } @else if (doctorId && selectedDate) {
        <ion-item>
          <ion-label>Horario disponible</ion-label>
          <ion-select [(ngModel)]="selectedTime" interface="action-sheet"
                      [disabled]="!availableSlots().length">
            @for (slot of availableSlots(); track slot.time) {
              <ion-select-option [value]="slot.time">{{ slot.time.substring(0,5) }}</ion-select-option>
            }
          </ion-select>
        </ion-item>
        @if (!availableSlots().length) {
          <ion-text color="medium"><p>No hay horarios disponibles ese día.</p></ion-text>
        }
      }

      @if (doctorId) {
        <ion-item>
          <ion-label position="floating">Motivo de la consulta</ion-label>
          <ion-textarea [(ngModel)]="reason" rows="3"></ion-textarea>
        </ion-item>
      }

      @if (loadError()) { <ion-text color="danger"><p>{{ loadError() }}</p></ion-text> }

      <ion-button expand="block" class="ion-margin-top"
                  [disabled]="!canSubmit() || booking()"
                  (click)="submit()">
        @if (booking()) { <ion-spinner name="dots"></ion-spinner> } @else { Reservar cita }
      </ion-button>
    </ion-content>
  `,
  styles: [`
    .section-label {
      font-size: 0.8rem;
      color: var(--color-muted);
      text-transform: uppercase;
      letter-spacing: 0.04em;
      margin: 0 0 0.5rem;
    }
    .section-label:not(:first-child) { margin-top: 1.1rem; }

    .specialty-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.6rem;
    }
    .specialty-card {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 1rem 0.6rem;
      cursor: pointer;
      transition: border-color 0.15s ease, background 0.15s ease;
    }
    .specialty-card--selected {
      border-color: var(--color-primary);
      background: var(--color-pendiente-bg);
    }
    .specialty-card__icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 44px;
      height: 44px;
      border-radius: 50%;
      background: var(--color-primary);
      color: #fff;
      font-size: 1.3rem;
    }
    .specialty-card__name {
      font-family: var(--font-display);
      font-size: 0.92rem;
      color: var(--color-ink);
      text-align: center;
    }

    .doctor-list {
      display: flex;
      flex-direction: column;
      gap: 0.6rem;
    }
    .doctor-card {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      width: 100%;
      text-align: left;
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 0.75rem 0.9rem;
      font-family: var(--font-body);
      cursor: pointer;
      transition: border-color 0.15s ease, background 0.15s ease;
    }
    .doctor-card--selected {
      border-color: var(--color-primary);
      background: var(--color-pendiente-bg);
    }
    .avatar {
      flex-shrink: 0;
      width: 40px;
      height: 40px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-family: var(--font-display);
      font-size: 0.85rem;
      font-weight: 600;
    }
    .doctor-card__info {
      display: flex;
      flex-direction: column;
      flex: 1;
      min-width: 0;
    }
    .doctor-card__name {
      font-family: var(--font-display);
      font-size: 1rem;
      color: var(--color-ink);
    }
    .doctor-card__bio {
      font-size: 0.8rem;
      color: var(--color-muted);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .doctor-card__check {
      flex-shrink: 0;
      font-size: 1.3rem;
      color: var(--color-primary);
    }
  `],
})
export class BookingPage implements OnInit {
  private appointments = inject(AppointmentsService);
  private toastCtrl = inject(ToastController);

  specialties = signal<Specialty[]>([]);
  doctors = signal<Doctor[]>([]);
  availableSlots = signal<AvailableSlot[]>([]);

  specialtyId?: number;
  doctorId?: number;
  selectedDate?: string;
  selectedTime?: string;
  reason = '';

  loadingSlots = signal(false);
  booking = signal(false);
  loadError = signal('');
  minDate = new Date().toISOString();

  initials = initials;
  avatarColor = avatarColor;
  specialtyIcon = specialtyIcon;

  ngOnInit() {
    this.appointments.getSpecialties().subscribe({
      next: (data) => this.specialties.set(data),
      error: () => this.loadError.set('No se pudieron cargar las especialidades'),
    });
  }

  selectSpecialty(id: number) {
    this.specialtyId = id;
    this.doctorId = undefined;
    this.doctors.set([]);
    this.resetDateAndSlots();
    this.appointments.getDoctors(id).subscribe({
      next: (data) => this.doctors.set(data),
      error: () => this.loadError.set('No se pudieron cargar los médicos'),
    });
  }

  selectDoctor(id: number) {
    this.doctorId = id;
    this.resetDateAndSlots();
  }

  onDateChange() { this.resetSlots(); this.fetchSlots(); }

  private resetDateAndSlots() {
    this.selectedDate = undefined;
    this.resetSlots();
  }

  private resetSlots() {
    this.availableSlots.set([]);
    this.selectedTime = undefined;
  }

  private fetchSlots() {
    if (!this.doctorId || !this.selectedDate) return;
    this.loadingSlots.set(true);
    const date = this.selectedDate.substring(0, 10);
    this.appointments.getAvailableSlots(this.doctorId, date).subscribe({
      next: (slots) => { this.availableSlots.set(slots); this.loadingSlots.set(false); },
      error: () => { this.loadError.set('No se pudieron cargar los horarios'); this.loadingSlots.set(false); },
    });
  }

  canSubmit() {
    return this.doctorId && this.selectedDate && this.selectedTime && this.reason.trim();
  }

  submit() {
    if (!this.canSubmit()) return;
    this.booking.set(true);
    this.appointments.bookAppointment({
      doctorId: this.doctorId!,
      appointmentDate: this.selectedDate!.substring(0, 10),
      appointmentTime: this.selectedTime!,
      reason: this.reason.trim(),
    }).subscribe({
      next: async () => {
        this.booking.set(false);
        this.doctorId = undefined; this.selectedDate = undefined;
        this.selectedTime = undefined; this.reason = '';
        this.specialtyId = undefined;
        this.doctors.set([]);
        this.resetSlots();
        await this.showToast('Cita reservada con éxito', 'success');
      },
      error: async (e) => {
        this.booking.set(false);
        await this.showToast(e.error?.message || 'No se pudo reservar la cita', 'danger');
      },
    });
  }

  private async showToast(message: string, color: 'success' | 'danger') {
    const toast = await this.toastCtrl.create({ message, duration: 2500, color, position: 'bottom' });
    await toast.present();
  }
}