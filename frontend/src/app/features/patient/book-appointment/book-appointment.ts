import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AppointmentResponse } from '../../../core/models/appointment.model';
import { DoctorResponse } from '../../../core/models/doctor.model';
import { Specialty } from '../../../core/models/specialty.model';
import { AppointmentService } from '../../../core/services/appointment.service';
import { DoctorService } from '../../../core/services/doctor.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { SelectedSlot, SlotPicker } from '../../../shared/slot-picker/slot-picker';

@Component({
  selector: 'app-book-appointment',
  standalone: true,
  imports: [SlotPicker, RouterLink],
  templateUrl: './book-appointment.html'
})
export class BookAppointment {
  private doctorService = inject(DoctorService);
  private specialtyService = inject(SpecialtyService);
  private appointmentService = inject(AppointmentService);
  private route = inject(ActivatedRoute);

  slotPicker = viewChild(SlotPicker);

  specialties = signal<Specialty[]>([]);
  doctors = signal<DoctorResponse[]>([]);
  specialtyFilter = signal<string>('');
  selectedDoctorId = signal<number | null>(null);
  slot = signal<SelectedSlot | null>(null);
  reason = signal('');
  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);
  booked = signal<AppointmentResponse | null>(null);

  filteredDoctors = computed(() => {
    const filter = this.specialtyFilter();
    return filter ? this.doctors().filter((d) => d.specialty === filter) : this.doctors();
  });

  selectedDoctor = computed(() => this.doctors().find((d) => d.id === this.selectedDoctorId()) ?? null);

  constructor() {
    const preselected = Number(this.route.snapshot.queryParamMap.get('doctorId'));

    this.specialtyService.getSpecialties().subscribe((data) => this.specialties.set(data));
    this.doctorService.getDoctors().subscribe((data) => {
      this.doctors.set(data);
      // Si llegaron desde el directorio con ?doctorId=, dejar ese medico ya elegido
      const doctor = preselected ? data.find((d) => d.id === preselected) : undefined;
      if (doctor) {
        this.specialtyFilter.set(doctor.specialty);
        this.selectedDoctorId.set(doctor.id);
      }
    });
  }

  onSpecialtyChange(event: Event): void {
    this.specialtyFilter.set((event.target as HTMLSelectElement).value);
    const selected = this.selectedDoctor();
    if (selected && !this.filteredDoctors().some((d) => d.id === selected.id)) {
      this.selectedDoctorId.set(null);
      this.slot.set(null);
    }
  }

  selectDoctor(id: number): void {
    if (this.selectedDoctorId() !== id) {
      this.selectedDoctorId.set(id);
      this.slot.set(null);
    }
  }

  onReasonInput(event: Event): void {
    this.reason.set((event.target as HTMLTextAreaElement).value);
  }

  confirm(): void {
    const doctor = this.selectedDoctor();
    const slot = this.slot();
    if (!doctor || !slot) {
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    this.appointmentService.createAppointment({
      doctorId: doctor.id,
      appointmentDate: slot.date,
      appointmentTime: slot.time,
      reason: this.reason().trim() || undefined
    }).subscribe({
      next: (appointment) => {
        this.isSubmitting.set(false);
        this.booked.set(appointment);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo reservar la cita.');
        // Si el horario se ocupo mientras decidias, refrescar lo que se muestra
        this.slotPicker()?.reload();
      }
    });
  }

  bookAnother(): void {
    this.booked.set(null);
    this.selectedDoctorId.set(null);
    this.slot.set(null);
    this.reason.set('');
    this.errorMessage.set(null);
  }

  initials(name: string): string {
    return name.split(' ').filter(Boolean).slice(0, 2).map((part) => part[0].toUpperCase()).join('');
  }
}