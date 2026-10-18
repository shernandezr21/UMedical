import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { controlErrorText } from '../../../core/form-errors';
import { DoctorDetail } from '../../../core/models/admin.model';
import { Specialty } from '../../../core/models/specialty.model';
import { DoctorService } from '../../../core/services/doctor.service';
import { SpecialtyService } from '../../../core/services/specialty.service';

@Component({
  selector: 'app-doctors-admin',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './doctors-admin.html'
})
export class DoctorsAdmin {
  private fb = inject(FormBuilder);
  private doctorService = inject(DoctorService);
  private specialtyService = inject(SpecialtyService);

  doctors = signal<DoctorDetail[]>([]);
  specialties = signal<Specialty[]>([]);
  query = signal('');
  activeFilter = signal<'all' | 'active' | 'inactive'>('all');
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  editingId = signal<number | null>(null);
  editError = signal<string | null>(null);

  filtered = computed(() => {
    const text = this.query().trim().toLowerCase();
    const state = this.activeFilter();
    return this.doctors().filter((d) => {
      const matchesText = !text
        || `${d.firstName} ${d.lastName} ${d.specialtyName} ${d.licenseNumber} ${d.email}`.toLowerCase().includes(text);
      const matchesState = state === 'all' || (state === 'active' ? d.active : !d.active);
      return matchesText && matchesState;
    });
  });

  editForm = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(80)]],
    lastName: ['', [Validators.required, Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    phone: ['', Validators.maxLength(20)],
    specialtyId: [null as number | null, Validators.required],
    licenseNumber: ['', [Validators.required, Validators.maxLength(50)]],
    biography: ['']
  });

  constructor() {
    this.specialtyService.getSpecialties().subscribe((data) => this.specialties.set(data));
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.doctorService.getManagementList().subscribe({
      next: (data) => {
        this.doctors.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('No se pudo cargar la lista de medicos.');
      }
    });
  }

  errorText(name: string): string | null {
    return controlErrorText(this.editForm, name);
  }

  onQuery(event: Event): void {
    this.query.set((event.target as HTMLInputElement).value);
  }

  onActiveFilter(event: Event): void {
    this.activeFilter.set((event.target as HTMLSelectElement).value as 'all' | 'active' | 'inactive');
  }

  initials(doctor: DoctorDetail): string {
    return `${doctor.firstName[0] ?? ''}${doctor.lastName[0] ?? ''}`.toUpperCase();
  }

  startEdit(doctor: DoctorDetail): void {
    this.editingId.set(doctor.doctorId);
    this.editError.set(null);
    this.successMessage.set(null);
    this.editForm.reset({
      firstName: doctor.firstName,
      lastName: doctor.lastName,
      email: doctor.email,
      phone: doctor.phone ?? '',
      specialtyId: doctor.specialtyId,
      licenseNumber: doctor.licenseNumber,
      biography: doctor.biography ?? ''
    });
  }

  cancelEdit(): void {
    this.editingId.set(null);
  }

  saveEdit(doctor: DoctorDetail): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    const raw = this.editForm.getRawValue();
    this.editError.set(null);

    this.doctorService.updateDoctor(doctor.doctorId, {
      firstName: raw.firstName!.trim(),
      lastName: raw.lastName!.trim(),
      email: raw.email!.trim(),
      phone: raw.phone?.trim() || null,
      specialtyId: raw.specialtyId!,
      licenseNumber: raw.licenseNumber!.trim(),
      biography: raw.biography?.trim() || null
    }).subscribe({
      next: () => {
        this.editingId.set(null);
        this.successMessage.set('Los datos del medico se actualizaron.');
        this.load();
      },
      error: (err) => this.editError.set(err?.error?.message ?? 'No se pudo actualizar al medico.')
    });
  }

  toggleActive(doctor: DoctorDetail): void {
    const action = doctor.active ? 'desactivar' : 'activar';
    if (!window.confirm(`¿Seguro que quieres ${action} a ${doctor.firstName} ${doctor.lastName}?`)) {
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.doctorService.setDoctorActive(doctor.doctorId, !doctor.active).subscribe({
      next: () => {
        this.successMessage.set(doctor.active ? 'El medico fue desactivado.' : 'El medico fue activado.');
        this.load();
      },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'No se pudo cambiar el estado del medico.')
    });
  }
}