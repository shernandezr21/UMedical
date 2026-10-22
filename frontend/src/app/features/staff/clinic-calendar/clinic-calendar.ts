import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppointmentResponse } from '../../../core/models/appointment.model';
import { DoctorResponse } from '../../../core/models/doctor.model';
import { AppointmentService } from '../../../core/services/appointment.service';
import { DoctorService } from '../../../core/services/doctor.service';
import { SelectedSlot, SlotPicker } from '../../../shared/slot-picker/slot-picker';

interface WeekDay {
  iso: string;
  label: string;
  isToday: boolean;
}

@Component({
  selector: 'app-clinic-calendar',
  standalone: true,
  imports: [RouterLink, SlotPicker],
  templateUrl: './clinic-calendar.html',
  styleUrls: ['../../doctor/my-agenda/my-agenda.scss', './clinic-calendar.scss']
})
export class ClinicCalendar {
  private appointmentService = inject(AppointmentService);
  private doctorService = inject(DoctorService);

  private readonly DIAS = ['Dom', 'Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab'];

  weekStart = signal<Date>(this.mondayOf(new Date()));
  appointments = signal<AppointmentResponse[]>([]);
  doctors = signal<DoctorResponse[]>([]);
  doctorFilter = signal<number | null>(null);

  isLoading = signal(false);
  isBusy = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  selected = signal<AppointmentResponse | null>(null);
  cancelOpen = signal(false);
  cancelReason = signal('');
  rescheduleOpen = signal(false);
  newSlot = signal<SelectedSlot | null>(null);

  weekDays = computed<WeekDay[]>(() => {
    const start = this.weekStart();
    const todayIso = this.toIso(new Date());
    return Array.from({ length: 7 }, (_, i) => {
      const d = new Date(start);
      d.setDate(d.getDate() + i);
      const iso = this.toIso(d);
      return { iso, label: `${this.DIAS[d.getDay()]} ${d.getDate()}`, isToday: iso === todayIso };
    });
  });

  weekRangeLabel = computed(() => {
    const days = this.weekDays();
    return `${days[0].label} - ${days[6].label}`;
  });

  constructor() {
    this.doctorService.getDoctors().subscribe((data) => this.doctors.set(data));
    this.loadWeek();
  }

  private mondayOf(date: Date): Date {
    const d = new Date(date);
    const day = d.getDay();
    d.setDate(d.getDate() + (day === 0 ? -6 : 1 - day));
    d.setHours(0, 0, 0, 0);
    return d;
  }

  private toIso(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }

  loadWeek(): void {
    this.isLoading.set(true);
    const days = this.weekDays();
    this.appointmentService.getClinicAgenda(days[0].iso, days[6].iso, this.doctorFilter() ?? undefined).subscribe({
      next: (data) => {
        this.appointments.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('No se pudo cargar el calendario.');
      }
    });
  }

  appointmentsForDay(iso: string): AppointmentResponse[] {
    return this.appointments()
      .filter((a) => a.appointmentDate === iso)
      .sort((a, b) => a.appointmentTime.localeCompare(b.appointmentTime));
  }

  previousWeek(): void {
    const d = new Date(this.weekStart());
    d.setDate(d.getDate() - 7);
    this.weekStart.set(d);
    this.clearSelection();
    this.loadWeek();
  }

  nextWeek(): void {
    const d = new Date(this.weekStart());
    d.setDate(d.getDate() + 7);
    this.weekStart.set(d);
    this.clearSelection();
    this.loadWeek();
  }

  goToToday(): void {
    this.weekStart.set(this.mondayOf(new Date()));
    this.clearSelection();
    this.loadWeek();
  }

  onDoctorFilter(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.doctorFilter.set(value ? Number(value) : null);
    this.clearSelection();
    this.loadWeek();
  }

  // ---------------------------------------------------------------- detalle y acciones

  select(appointment: AppointmentResponse): void {
    this.selected.set(appointment);
    this.closeActions();
  }

  clearSelection(): void {
    this.selected.set(null);
    this.closeActions();
    this.successMessage.set(null);
  }

  closeActions(): void {
    this.cancelOpen.set(false);
    this.cancelReason.set('');
    this.rescheduleOpen.set(false);
    this.newSlot.set(null);
    this.errorMessage.set(null);
  }

  startCancel(): void {
    this.closeActions();
    this.cancelOpen.set(true);
  }

  startReschedule(): void {
    this.closeActions();
    this.rescheduleOpen.set(true);
  }

  onCancelReasonInput(event: Event): void {
    this.cancelReason.set((event.target as HTMLTextAreaElement).value);
  }

  confirmCancel(): void {
    const appointment = this.selected();
    if (!appointment) {
      return;
    }
    this.isBusy.set(true);
    this.errorMessage.set(null);

    this.appointmentService.cancelAppointment(appointment.id, this.cancelReason().trim() || undefined).subscribe({
      next: () => {
        this.isBusy.set(false);
        this.clearSelection();
        this.successMessage.set('La cita fue cancelada.');
        this.loadWeek();
      },
      error: (err) => {
        this.isBusy.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo cancelar la cita.');
      }
    });
  }

  confirmReschedule(): void {
    const appointment = this.selected();
    const slot = this.newSlot();
    if (!appointment || !slot) {
      return;
    }
    this.isBusy.set(true);
    this.errorMessage.set(null);

    this.appointmentService.rescheduleAppointment(appointment.id, {
      appointmentDate: slot.date,
      appointmentTime: slot.time
    }).subscribe({
      next: () => {
        this.isBusy.set(false);
        this.clearSelection();
        this.successMessage.set('La cita fue reagendada.');
        this.loadWeek();
      },
      error: (err) => {
        this.isBusy.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo reagendar la cita.');
      }
    });
  }

  // ---------------------------------------------------------------- presentacion

  isPastPending(a: AppointmentResponse): boolean {
    return a.status === 'PENDIENTE' && new Date(`${a.appointmentDate}T${a.appointmentTime}`).getTime() < Date.now();
  }

  statusLabel(a: AppointmentResponse): string {
    if (a.status === 'NO_ASISTIO') { return 'NO ASISTIO'; }
    return this.isPastPending(a) ? 'SIN ATENDER' : a.status;
  }

  badgeClass(a: AppointmentResponse): string {
    return {
      PENDIENTE: 'badge badge-pendiente',
      COMPLETADA: 'badge badge-completada',
      CANCELADA: 'badge badge-cancelada',
      NO_ASISTIO: 'badge badge-noasistio'
    }[a.status] ?? 'badge';
  }

  hasStarted(appt: AppointmentResponse): boolean {
    return new Date(`${appt.appointmentDate}T${appt.appointmentTime}`).getTime() <= Date.now();
  }

  markNoShow(): void {
      const appointment = this.selected();
      if (!appointment || !window.confirm(`¿Marcar que ${appointment.patientName} no asistio a su cita?`)) {
        return;
      }
      this.isBusy.set(true);
      this.errorMessage.set(null);
      this.appointmentService.markNoShow(appointment.id).subscribe({
        next: () => {
          this.isBusy.set(false);
          this.clearSelection();
          this.successMessage.set('La cita quedo marcada como no asistio.');
          this.loadWeek();
        },
        error: (err) => {
          this.isBusy.set(false);
          this.errorMessage.set(err?.error?.message ?? 'No se pudo marcar la inasistencia.');
        }
      });
  }
}