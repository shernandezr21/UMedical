import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppointmentResponse } from '../../../core/models/appointment.model';
import { DocumentResponse } from '../../../core/models/document.model';
import { AppointmentService } from '../../../core/services/appointment.service';
import { SelectedSlot, SlotPicker } from '../../../shared/slot-picker/slot-picker';

// Debe coincidir con app.appointments.min-cancel-hours del backend
const MIN_NOTICE_HOURS = 2;

@Component({
  selector: 'app-my-appointments',
  standalone: true,
  imports: [SlotPicker, RouterLink],
  templateUrl: './my-appointments.html'
})
export class MyAppointments {
  private appointmentService = inject(AppointmentService);

  appointments = signal<AppointmentResponse[]>([]);
  tab = signal<'proximas' | 'historial'>('proximas');
  isLoading = signal(true);
  isBusy = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  cancelingId = signal<number | null>(null);
  cancelReason = signal('');
  reschedulingId = signal<number | null>(null);
  newSlot = signal<SelectedSlot | null>(null);

  openDocsId = signal<number | null>(null);
  documents = signal<DocumentResponse[]>([]);
  docsLoading = signal(false);

  upcoming = computed(() =>
    this.appointments()
      .filter((a) => this.isUpcoming(a))
      .sort((a, b) => this.startOf(a).getTime() - this.startOf(b).getTime())
  );

  history = computed(() =>
    this.appointments()
      .filter((a) => !this.isUpcoming(a))
      .sort((a, b) => this.startOf(b).getTime() - this.startOf(a).getTime())
  );

  visible = computed(() => (this.tab() === 'proximas' ? this.upcoming() : this.history()));

  constructor() {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.appointmentService.getMyHistory().subscribe({
      next: (data) => {
        this.appointments.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('No se pudieron cargar tus citas.');
      }
    });
  }

  // ---------------------------------------------------------------- utilidades de presentacion

  startOf(a: AppointmentResponse): Date {
    return new Date(`${a.appointmentDate}T${a.appointmentTime}`);
  }

  isUpcoming(a: AppointmentResponse): boolean {
    return a.status === 'PENDIENTE' && this.startOf(a).getTime() >= Date.now();
  }

  /** Se puede cancelar o reagendar si faltan al menos MIN_NOTICE_HOURS. */
  canModify(a: AppointmentResponse): boolean {
    return this.startOf(a).getTime() - Date.now() >= MIN_NOTICE_HOURS * 3600 * 1000;
  }

  statusLabel(a: AppointmentResponse): string {
    return a.status === 'PENDIENTE' && !this.isUpcoming(a) ? 'SIN ATENDER' : a.status;
  }

  badgeClass(a: AppointmentResponse): string {
    return {
      PENDIENTE: 'badge badge-pendiente',
      COMPLETADA: 'badge badge-completada',
      CANCELADA: 'badge badge-cancelada'
    }[a.status] ?? 'badge';
  }

  dateLabel(a: AppointmentResponse): string {
    const label = new Date(`${a.appointmentDate}T00:00:00`).toLocaleDateString('es', {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
    });
    return label.charAt(0).toUpperCase() + label.slice(1);
  }

  initials(name: string): string {
    return name.split(' ').filter(Boolean).slice(0, 2).map((part) => part[0].toUpperCase()).join('');
  }

  // ---------------------------------------------------------------- cancelar

  startCancel(id: number): void {
    this.closePanels();
    this.cancelingId.set(id);
  }

  onCancelReasonInput(event: Event): void {
    this.cancelReason.set((event.target as HTMLTextAreaElement).value);
  }

  confirmCancel(a: AppointmentResponse): void {
    this.isBusy.set(true);
    this.errorMessage.set(null);

    this.appointmentService.cancelAppointment(a.id, this.cancelReason().trim() || undefined).subscribe({
      next: () => {
        this.isBusy.set(false);
        this.closePanels();
        this.successMessage.set('Tu cita fue cancelada.');
        this.load();
      },
      error: (err) => {
        this.isBusy.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo cancelar la cita.');
      }
    });
  }

  // ---------------------------------------------------------------- reagendar

  startReschedule(id: number): void {
    this.closePanels();
    this.reschedulingId.set(id);
  }

  confirmReschedule(a: AppointmentResponse): void {
    const slot = this.newSlot();
    if (!slot) {
      return;
    }

    this.isBusy.set(true);
    this.errorMessage.set(null);

    this.appointmentService.rescheduleAppointment(a.id, {
      appointmentDate: slot.date,
      appointmentTime: slot.time
    }).subscribe({
      next: () => {
        this.isBusy.set(false);
        this.closePanels();
        this.successMessage.set('Tu cita fue reagendada.');
        this.load();
      },
      error: (err) => {
        this.isBusy.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo reagendar la cita.');
      }
    });
  }

  closePanels(): void {
    this.cancelingId.set(null);
    this.cancelReason.set('');
    this.reschedulingId.set(null);
    this.newSlot.set(null);
    this.successMessage.set(null);
    this.errorMessage.set(null);
  }

  // ---------------------------------------------------------------- documentos

  toggleDocs(a: AppointmentResponse): void {
    if (this.openDocsId() === a.id) {
      this.openDocsId.set(null);
      return;
    }

    this.openDocsId.set(a.id);
    this.documents.set([]);
    this.docsLoading.set(true);

    this.appointmentService.getDocuments(a.id).subscribe({
      next: (docs) => {
        this.documents.set(docs);
        this.docsLoading.set(false);
      },
      error: () => {
        this.docsLoading.set(false);
        this.errorMessage.set('No se pudieron cargar los documentos.');
      }
    });
  }

  viewFile(documentId: number): void {
    // Se abre la pestana en el clic (para que el navegador no la bloquee) y luego se le asigna el PDF
    const win = window.open('', '_blank');
    this.appointmentService.downloadDocumentFile(documentId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        if (win) {
          win.location.href = url;
        } else {
          window.open(url, '_blank');
        }
      },
      error: () => {
        win?.close();
        this.errorMessage.set('No se pudo abrir el PDF.');
      }
    });
  }
}