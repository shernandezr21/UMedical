import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { DoctorResponse } from '../../../core/models/doctor.model';
import { Specialty } from '../../../core/models/specialty.model';
import { DoctorService } from '../../../core/services/doctor.service';
import { SpecialtyService } from '../../../core/services/specialty.service';

@Component({
  selector: 'app-doctor-directory',
  standalone: true,
  templateUrl: './doctors.html'
})
export class DoctorDirectory {
  private doctorService = inject(DoctorService);
  private specialtyService = inject(SpecialtyService);
  private router = inject(Router);

  specialties = signal<Specialty[]>([]);
  doctors = signal<DoctorResponse[]>([]);
  specialtyFilter = signal<string>('');
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  filtered = computed(() => {
    const filter = this.specialtyFilter();
    return filter ? this.doctors().filter((d) => d.specialty === filter) : this.doctors();
  });

  constructor() {
    this.specialtyService.getSpecialties().subscribe((data) => this.specialties.set(data));
    this.doctorService.getDoctors().subscribe({
      next: (data) => {
        this.doctors.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('No se pudo cargar el directorio de medicos.');
      }
    });
  }

  onFilter(event: Event): void {
    this.specialtyFilter.set((event.target as HTMLSelectElement).value);
  }

  initials(name: string): string {
    return name.split(' ').filter(Boolean).slice(0, 2).map((part) => part[0].toUpperCase()).join('');
  }

  book(doctor: DoctorResponse): void {
    this.router.navigate(['/dashboard/agendar'], { queryParams: { doctorId: doctor.id } });
  }
}