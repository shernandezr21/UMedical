import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { controlErrorText } from '../../../core/form-errors';
import { PageResponse } from '../../../core/models/admin.model';
import { CreatedPatient, PatientSummary } from '../../../core/models/patient.model';
import { PatientService } from '../../../core/services/patient.service';

@Component({
  selector: 'app-patients',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './patients.html'
})
export class Patients {
  private fb = inject(FormBuilder);
  private patientService = inject(PatientService);
  private search$ = new Subject<string>();
  private searchText = '';

  readonly pageSize = 10;
  readonly bloodTypes = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
  readonly today = this.toIso(new Date());

  result = signal<PageResponse<PatientSummary> | null>(null);
  page = signal(0);
  isSearching = signal(true);
  searchError = signal<string | null>(null);

  showForm = signal(false);
  isSaving = signal(false);
  formError = signal<string | null>(null);
  created = signal<CreatedPatient | null>(null);
  copied = signal(false);

  form = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(80)]],
    lastName: ['', [Validators.required, Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    phone: ['', Validators.maxLength(20)],
    birthDate: [''],
    bloodType: [''],
    address: ['', Validators.maxLength(255)]
  });

  constructor() {
    this.search$.pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed()).subscribe((text) => {
      this.searchText = text;
      this.page.set(0);
      this.load();
    });
    this.load();
  }

  load(): void {
    this.isSearching.set(true);
    this.searchError.set(null);

    this.patientService.search(this.searchText, this.page(), this.pageSize).subscribe({
      next: (result) => {
        this.result.set(result);
        this.isSearching.set(false);
      },
      error: () => {
        this.isSearching.set(false);
        this.searchError.set('No se pudo buscar pacientes.');
      }
    });
  }

  onSearchInput(event: Event): void {
    this.search$.next((event.target as HTMLInputElement).value);
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.page.update((p) => p - 1);
      this.load();
    }
  }

  nextPage(): void {
    if (this.page() + 1 < (this.result()?.totalPages ?? 0)) {
      this.page.update((p) => p + 1);
      this.load();
    }
  }

  errorText(name: string): string | null {
    return controlErrorText(this.form, name);
  }

  private toIso(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }

  toggleForm(): void {
    this.showForm.update((open) => !open);
    this.formError.set(null);
  }

  savePatient(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    if (raw.birthDate && raw.birthDate > this.today) {
      this.formError.set('La fecha de nacimiento no puede ser futura.');
      return;
    }

    this.formError.set(null);
    this.isSaving.set(true);
    this.copied.set(false);

    this.patientService.create({
      email: raw.email!.trim(),
      firstName: raw.firstName!.trim(),
      lastName: raw.lastName!.trim(),
      phone: raw.phone?.trim() || null,
      birthDate: raw.birthDate || null,
      address: raw.address?.trim() || null,
      bloodType: raw.bloodType || null
    }).subscribe({
      next: (result) => {
        this.isSaving.set(false);
        this.created.set(result);
        this.showForm.set(false);
        this.form.reset();
        this.page.set(0);
        this.load();
      },
      error: (err) => {
        this.isSaving.set(false);
        const details: string[] | undefined = err?.error?.details;
        this.formError.set(
          details?.length
            ? `${err.error.message}: ${details.join(' | ')}`
            : err?.error?.message ?? 'No se pudo registrar al paciente.'
        );
      }
    });
  }

  copyCredentials(): void {
    const result = this.created();
    if (!result) {
      return;
    }
    navigator.clipboard
      .writeText(`Usuario: ${result.username}\nContrasena temporal: ${result.temporaryPassword}`)
      .then(() => this.copied.set(true));
  }

  dismissCreated(): void {
    this.created.set(null);
  }
}