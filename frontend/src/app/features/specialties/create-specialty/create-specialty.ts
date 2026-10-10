import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { Specialty } from '../../../core/models/specialty.model';

@Component({
  selector: 'app-create-specialty',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './create-specialty.html',
  styleUrl: './create-specialty.scss'
})
export class CreateSpecialty {
  private fb = inject(FormBuilder);
  private specialtyService = inject(SpecialtyService);

  specialties = signal<Specialty[]>([]);
  editingId = signal<number | null>(null);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', Validators.maxLength(255)]
  });

  constructor() {
    this.loadSpecialties();
  }

  loadSpecialties(): void {
    this.specialtyService.getSpecialties().subscribe((data) => this.specialties.set(data));
  }

  startEdit(specialty: Specialty): void {
    this.editingId.set(specialty.id);
    this.form.reset({ name: specialty.name, description: specialty.description ?? '' });
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  cancelEdit(): void {
    this.editingId.set(null);
    this.form.reset({ name: '', description: '' });
  }

  errorText(): string | null {
    const control = this.form.controls.name;
    if (!control.invalid || !(control.touched || control.dirty)) {
      return null;
    }
    if (control.errors?.['required']) return 'El nombre es obligatorio.';
    if (control.errors?.['maxlength']) return 'Maximo 100 caracteres.';
    if (control.errors?.['duplicate']) return 'Ya existe una especialidad con ese nombre.';
    return null;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const name = this.form.value.name!.trim();
    const nameControl = this.form.controls.name;

    if (!name) {
      nameControl.setErrors({ required: true });
      nameControl.markAsTouched();
      return;
    }

    const id = this.editingId();
    const duplicate = this.specialties().some(
      (s) => s.id !== id && s.name.trim().toLowerCase() === name.toLowerCase()
    );
    if (duplicate) {
      nameControl.setErrors({ duplicate: true });
      nameControl.markAsTouched();
      return;
    }

    const request = { name, description: this.form.value.description?.trim() || undefined };
    const call$ = id === null
      ? this.specialtyService.createSpecialty(request)
      : this.specialtyService.updateSpecialty(id, request);

    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.isLoading.set(true);

    call$.subscribe({
      next: () => {
        this.isLoading.set(false);
        this.successMessage.set(id === null ? 'Especialidad creada.' : 'Especialidad actualizada.');
        this.cancelEdit();
        this.loadSpecialties();
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo guardar la especialidad.');
      }
    });
  }
}

// import { Component, inject, signal } from '@angular/core';
// import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
// import { SpecialtyService } from '../../../core/services/specialty.service';
// import { Specialty } from '../../../core/models/specialty.model';

// @Component({
//   selector: 'app-create-specialty',
//   standalone: true,
//   imports: [ReactiveFormsModule],
//   templateUrl: './create-specialty.html',
//   styleUrl: './create-specialty.scss'
// })
// export class CreateSpecialty {
//   private fb = inject(FormBuilder);
//   private specialtyService = inject(SpecialtyService);

//   specialties = signal<Specialty[]>([]);
//   isLoading = signal(false);
//   errorMessage = signal<string | null>(null);

//   form = this.fb.group({
//     name: ['', Validators.required],
//     description: ['']
//   });

//   constructor() {
//     this.loadSpecialties();
//   }

//   loadSpecialties(): void {
//     this.specialtyService.getSpecialties().subscribe((data) => this.specialties.set(data));
//   }

//   submit(): void {
//     if (this.form.invalid) {
//       this.form.markAllAsTouched();
//       return;
//     }

//     this.errorMessage.set(null);
//     this.isLoading.set(true);

//     this.specialtyService.createSpecialty({
//       name: this.form.value.name!,
//       description: this.form.value.description ?? undefined
//     }).subscribe({
//       next: () => {
//         this.isLoading.set(false);
//         this.form.reset();
//         this.loadSpecialties();
//       },
//       error: (err) => {
//         this.isLoading.set(false);
//         this.errorMessage.set(err?.error?.message ?? 'No se pudo crear la especialidad.');
//       }
//     });
//   }
// }