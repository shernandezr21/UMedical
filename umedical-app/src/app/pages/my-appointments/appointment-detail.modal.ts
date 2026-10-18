import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  IonHeader, IonToolbar, IonTitle, IonButtons, IonButton, IonIcon,
  IonContent, IonSpinner, IonDatetime, IonSelect, IonSelectOption,
  IonText, ModalController, AlertController, ToastController
} from '@ionic/angular';
import { AppointmentsService } from '../../core/appointments.service';
import { Appointment, DocumentResponse, AvailableSlot } from '../../core/models';
import { initials, avatarColor } from '../../core/avatar.util';

@Component({
  selector: 'app-appointment-detail-modal',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DatePipe, IonHeader, IonToolbar, IonTitle, IonButtons,
    IonButton, IonIcon, IonContent, IonSpinner, IonDatetime, IonSelect, IonSelectOption, IonText,
  ],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-title>Detalle de la cita</ion-title>
        <ion-buttons slot="end">
          <ion-button (click)="dismiss()"><ion-icon name="close-outline"></ion-icon></ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding">
      <div class="detail-card" [ngClass]="'detail-card--' + appointment.status.toLowerCase()">
        <div class="detail-card__doctor-row">
          <span class="avatar" [style.background]="avatarColor(appointment.doctorName)">
            {{ initials(appointment.doctorName) }}
          </span>
          <div>
            <h2 class="detail-card__doctor">{{ appointment.doctorName }}</h2>
            <p class="detail-card__specialty">{{ appointment.specialty }}</p>
          </div>
          <span class="status-badge" [ngClass]="'status-badge--' + appointment.status.toLowerCase()">
            {{ appointment.status }}
          </span>
        </div>
      </div>

      <div class="info-row">
        <ion-icon name="calendar-outline"></ion-icon>
        <span>{{ appointment.appointmentDate | date:'fullDate' }}</span>
      </div>
      <div class="info-row">
        <ion-icon name="time-outline"></ion-icon>
        <span>{{ appointment.appointmentTime.substring(0,5) }}</span>
      </div>

      <p class="section-label">Motivo de la consulta</p>
      <div class="card">
        <p class="reason-text">{{ appointment.reason }}</p>
      </div>

      @if (appointment.status === 'CANCELADA' && appointment.cancellationReason) {
        <p class="section-label">Motivo de cancelación</p>
        <div class="card">
          <p class="reason-text">{{ appointment.cancellationReason }}</p>
        </div>
      }

      @if (appointment.status !== 'PENDIENTE') {
        <p class="section-label">Diagnóstico y receta</p>

        @if (loadingDocs()) {
          <div class="ion-text-center ion-padding"><ion-spinner name="dots"></ion-spinner></div>
        } @else if (docsError()) {
          <div class="notice">
            <ion-icon name="information-circle-outline"></ion-icon>
            <p>{{ docsError() }}</p>
          </div>
        } @else if (documents().length) {
          @for (doc of documents(); track doc.id) {
            <div class="card ion-margin-bottom">
              <p class="doc-type">{{ doc.type === 'DIAGNOSTICO' ? 'Diagnóstico' : 'Receta' }}</p>
              <p class="reason-text">{{ doc.content }}</p>
              @if (doc.fileName) {
                <ion-button fill="outline" size="small" class="ion-margin-top"
                            [disabled]="openingPdf() === doc.id" (click)="viewPdf(doc)">
                  @if (openingPdf() === doc.id) {
                    <ion-spinner name="dots"></ion-spinner>
                  } @else {
                    <ion-icon name="document-text-outline" slot="start"></ion-icon> Ver PDF
                  }
                </ion-button>
              }
            </div>
          }
        } @else {
          <div class="notice">
            <ion-icon name="information-circle-outline"></ion-icon>
            <p>Aún no hay diagnóstico registrado para esta cita.</p>
          </div>
        }
      }

      @if (appointment.status === 'PENDIENTE') {
        @if (!rescheduling()) {
          <div class="actions">
            <ion-button expand="block" fill="outline" (click)="rescheduling.set(true)">
              <ion-icon name="swap-horizontal-outline" slot="start"></ion-icon>
              Reagendar
            </ion-button>
            <ion-button expand="block" fill="outline" color="danger" (click)="confirmCancel()">
              <ion-icon name="trash-outline" slot="start"></ion-icon>
              Cancelar cita
            </ion-button>
          </div>
        } @else {
          <p class="section-label">Nueva fecha y horario</p>
          <ion-datetime presentation="date" locale="es-ES" [(ngModel)]="newDate"
                        (ionChange)="onNewDateChange()" [min]="minDate"></ion-datetime>

          @if (loadingSlots()) {
            <ion-spinner name="dots"></ion-spinner>
          } @else if (newDate) {
            <ion-select label="Horario" placeholder="Elige un horario" [(ngModel)]="newTime"
                        interface="action-sheet" [disabled]="!newSlots().length" class="ion-margin-top">
              @for (slot of newSlots(); track slot.time) {
                <ion-select-option [value]="slot.time">{{ slot.time.substring(0,5) }}</ion-select-option>
              }
            </ion-select>
            @if (!newSlots().length) {
              <ion-text color="medium"><p>No hay horarios disponibles ese día.</p></ion-text>
            }
          }

          @if (rescheduleError()) {
            <ion-text color="danger"><p>{{ rescheduleError() }}</p></ion-text>
          }

          <div class="actions ion-margin-top">
            <ion-button expand="block" [disabled]="!newDate || !newTime || saving()" (click)="confirmReschedule()">
              @if (saving()) { <ion-spinner name="dots"></ion-spinner> } @else { Confirmar nuevo horario }
            </ion-button>
            <ion-button expand="block" fill="clear" (click)="rescheduling.set(false)">Cancelar</ion-button>
          </div>
        }
      }
    </ion-content>
  `,
  styles: [`
    .detail-card {
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-left: 4px solid var(--color-muted);
      border-radius: var(--radius-md);
      padding: 1rem 1.1rem;
      margin-bottom: 1.1rem;
    }
    .detail-card--pendiente { border-left-color: var(--color-pendiente); }
    .detail-card--completada { border-left-color: var(--color-completada); }
    .detail-card--cancelada { border-left-color: var(--color-cancelada); }

    .detail-card__doctor-row { display: flex; align-items: center; gap: 0.75rem; }
    .detail-card__doctor-row > div { flex: 1; }
    .avatar {
      flex-shrink: 0; width: 44px; height: 44px; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      color: #fff; font-family: var(--font-display); font-size: 0.9rem; font-weight: 600;
    }
    .detail-card__doctor { font-family: var(--font-display); font-size: 1.1rem; margin: 0 0 0.1rem; color: var(--color-ink); }
    .detail-card__specialty { margin: 0; font-size: 0.85rem; color: var(--color-muted); }

    .info-row { display: flex; align-items: center; gap: 0.5rem; font-size: 0.92rem; color: var(--color-ink); margin-bottom: 0.5rem; }
    .info-row ion-icon { color: var(--color-primary); font-size: 1.1rem; }

    .section-label { font-size: 0.8rem; color: var(--color-muted); text-transform: uppercase; letter-spacing: 0.04em; margin: 1.3rem 0 0.5rem; }

    .reason-text { margin: 0; font-size: 0.92rem; color: var(--color-ink); line-height: 1.5; }

    .doc-type { margin: 0 0 0.35rem; font-size: 0.8rem; font-weight: 600; color: var(--color-primary); text-transform: uppercase; letter-spacing: 0.03em; }

    .notice {
      display: flex; gap: 0.6rem; align-items: flex-start;
      background: var(--color-bg); border: 1px solid var(--color-border); border-radius: var(--radius-md);
      padding: 0.8rem 0.9rem; color: var(--color-muted); font-size: 0.85rem;
    }
    .notice ion-icon { font-size: 1.2rem; flex-shrink: 0; margin-top: 0.1rem; }
    .notice p { margin: 0; }

    .actions { display: flex; flex-direction: column; gap: 0.6rem; margin-top: 1.5rem; }
  `],
})
export class AppointmentDetailModal implements OnInit {
  @Input() appointment!: Appointment;

  private modalCtrl = inject(ModalController);
  private alertCtrl = inject(AlertController);
  private toastCtrl = inject(ToastController);
  private appointments = inject(AppointmentsService);

  documents = signal<DocumentResponse[]>([]);
  loadingDocs = signal(false);
  docsError = signal('');
  openingPdf = signal<number | null>(null);

  rescheduling = signal(false);
  newDate?: string;
  newTime?: string;
  newSlots = signal<AvailableSlot[]>([]);
  loadingSlots = signal(false);
  rescheduleError = signal('');
  saving = signal(false);
  minDate = new Date().toISOString();

  initials = initials;
  avatarColor = avatarColor;

  ngOnInit() {
    if (this.appointment.status === 'PENDIENTE') return;
    this.loadingDocs.set(true);
    this.appointments.getDocuments(this.appointment.id).subscribe({
      next: (docs) => { this.documents.set(docs); this.loadingDocs.set(false); },
      error: () => {
        this.docsError.set('El diagnóstico aún no está disponible para consulta desde la app.');
        this.loadingDocs.set(false);
      },
    });
  }

  viewPdf(doc: DocumentResponse) {
    this.openingPdf.set(doc.id);
    this.appointments.getDocumentFile(doc.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
        this.openingPdf.set(null);
      },
      error: async () => {
        this.openingPdf.set(null);
        await this.showToast('No se pudo abrir el PDF', 'danger');
      },
    });
  }

  onNewDateChange() {
    this.newTime = undefined;
    this.newSlots.set([]);
    if (!this.newDate) return;
    this.loadingSlots.set(true);
    const date = this.newDate.substring(0, 10);
    this.appointments.getAvailableSlots(this.appointment.doctorId, date).subscribe({
      next: (slots) => { this.newSlots.set(slots); this.loadingSlots.set(false); },
      error: () => { this.rescheduleError.set('No se pudieron cargar los horarios'); this.loadingSlots.set(false); },
    });
  }

  confirmReschedule() {
    if (!this.newDate || !this.newTime) return;
    this.saving.set(true);
    this.rescheduleError.set('');
    this.appointments.rescheduleAppointment(this.appointment.id, {
      appointmentDate: this.newDate.substring(0, 10),
      appointmentTime: this.newTime,
    }).subscribe({
      next: async () => {
        this.saving.set(false);
        await this.showToast('Cita reagendada con éxito', 'success');
        this.modalCtrl.dismiss({ refreshed: true });
      },
      error: (e) => {
        this.saving.set(false);
        this.rescheduleError.set(e.error?.message || 'No se pudo reagendar la cita');
      },
    });
  }

  async confirmCancel() {
    const alert = await this.alertCtrl.create({
      header: 'Cancelar cita',
      message: `¿Seguro que quieres cancelar la cita con ${this.appointment.doctorName}?`,
      inputs: [{ name: 'reason', type: 'text', placeholder: 'Motivo (opcional)' }],
      buttons: [
        { text: 'Volver', role: 'cancel' },
        {
          text: 'Sí, cancelar',
          role: 'destructive',
          handler: (data) => this.doCancel(data.reason),
        },
      ],
    });
    await alert.present();
  }

  private doCancel(reason?: string) {
    this.appointments.cancelAppointment(this.appointment.id, reason ? { reason } : undefined).subscribe({
      next: async () => {
        await this.showToast('Cita cancelada', 'success');
        this.modalCtrl.dismiss({ refreshed: true });
      },
      error: async (e) => {
        await this.showToast(e.error?.message || 'No se pudo cancelar la cita', 'danger');
      },
    });
  }

  private async showToast(message: string, color: 'success' | 'danger') {
    const toast = await this.toastCtrl.create({ message, duration: 2500, color, position: 'bottom' });
    await toast.present();
  }

  dismiss() { this.modalCtrl.dismiss(); }
}