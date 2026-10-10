import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl, FormArray, FormBuilder, ReactiveFormsModule,
  ValidationErrors, ValidatorFn, Validators
} from '@angular/forms';
import { DoctorService } from '../../../core/services/doctor.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { CreateDoctorRequest } from '../../../core/models/doctor.model';
import { Specialty } from '../../../core/models/specialty.model';

function toMinutes(time: string): number {
  const [hours, minutes] = time.split(':').map(Number);
  return hours * 60 + minutes;
}

/** Valida UN bloque: fin posterior a inicio, y que el slot quepa dentro del bloque. */
const timeRangeValidator: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const start = group.get('startTime')?.value;
  const end = group.get('endTime')?.value;
  const slot = group.get('slotDurationMinutes')?.value;

  if (!start || !end) {
    return null;
  }

  const length = toMinutes(end) - toMinutes(start);
  if (length <= 0) {
    return { endBeforeStart: true };
  }
  if (slot && slot > length) {
    return { slotTooLong: true };
  }
  return null;
};

/** Valida TODOS los bloques juntos: dos bloques del mismo dia no pueden cruzarse. */
const noOverlapValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const blocks = (control as FormArray).controls.map((c) => c.value);

  for (let i = 0; i < blocks.length; i++) {
    for (let j = i + 1; j < blocks.length; j++) {
      const a = blocks[i];
      const b = blocks[j];
      const comparable = a.dayOfWeek != null && a.dayOfWeek === b.dayOfWeek
        && a.startTime && a.endTime && b.startTime && b.endTime;

      if (comparable
          && toMinutes(a.startTime) < toMinutes(b.endTime)
          && toMinutes(b.startTime) < toMinutes(a.endTime)) {
        return { overlap: true };
      }
    }
  }
  return null;
};

@Component({
  selector: 'app-create-doctor',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './create-doctor.html',
  styleUrl: './create-doctor.scss'
})
export class CreateDoctor {
  private fb = inject(FormBuilder);
  private doctorService = inject(DoctorService);
  private specialtyService = inject(SpecialtyService);

  specialties = signal<Specialty[]>([]);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  readonly diasSemana = [
    { value: 1, label: 'Lunes' },
    { value: 2, label: 'Martes' },
    { value: 3, label: 'Miercoles' },
    { value: 4, label: 'Jueves' },
    { value: 5, label: 'Viernes' },
    { value: 6, label: 'Sabado' },
    { value: 7, label: 'Domingo' }
  ];

  form = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(4)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    phone: [''],
    specialtyId: [null as number | null, Validators.required],
    licenseNumber: ['', Validators.required],
    biography: [''],
    schedules: this.fb.array([this.buildScheduleGroup()], { validators: [noOverlapValidator] })
  });

  constructor() {
    this.specialtyService.getSpecialties().subscribe((data) => this.specialties.set(data));
  }

  get schedules(): FormArray {
    return this.form.get('schedules') as FormArray;
  }

  buildScheduleGroup() {
    return this.fb.group({
      dayOfWeek: [null as number | null, Validators.required],
      startTime: ['', Validators.required],
      endTime: ['', Validators.required],
      slotDurationMinutes: [30, [Validators.required, Validators.min(5)]]
    }, { validators: [timeRangeValidator] });
  }

  addSchedule(): void {
    this.schedules.push(this.buildScheduleGroup());
  }

  removeSchedule(index: number): void {
    if (this.schedules.length > 1) {
      this.schedules.removeAt(index);
    }
  }

  /** Mensaje de error de un campo simple, o null si no hay que mostrar nada. */
  errorText(name: string): string | null {
    const control = this.form.get(name);
    if (!control || !control.invalid || !(control.touched || control.dirty)) {
      return null;
    }
    if (control.errors?.['required']) return 'Este campo es obligatorio.';
    if (control.errors?.['email']) return 'Ingresa un correo valido.';
    if (control.errors?.['minlength']) {
      return `Minimo ${control.errors['minlength'].requiredLength} caracteres.`;
    }
    return 'Valor invalido.';
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.isLoading.set(true);

    const raw = this.form.getRawValue();

    const request: CreateDoctorRequest = {
      username: raw.username!.trim(),
      email: raw.email!.trim(),
      password: raw.password!,
      firstName: raw.firstName!.trim(),
      lastName: raw.lastName!.trim(),
      phone: raw.phone?.trim() || undefined,
      specialtyId: raw.specialtyId!,
      licenseNumber: raw.licenseNumber!.trim(),
      biography: raw.biography?.trim() || undefined,
      schedules: raw.schedules.map((s) => ({
        dayOfWeek: s.dayOfWeek!,
        startTime: s.startTime!,
        endTime: s.endTime!,
        slotDurationMinutes: s.slotDurationMinutes!
      }))
    };

    // Una sola peticion: el backend crea medico y horarios en una transaccion
    this.doctorService.createDoctor(request).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.successMessage.set(
          `Medico creado con ${request.schedules.length} bloque(s) de horario.`
        );
        this.form.reset();
        this.schedules.clear();
        this.schedules.push(this.buildScheduleGroup());
      },
      error: (err) => {
        this.isLoading.set(false);
        const details: string[] | undefined = err?.error?.details;
        this.errorMessage.set(
          details?.length
            ? `${err.error.message}: ${details.join(' | ')}`
            : err?.error?.message ?? 'Ocurrio un error al crear el medico.'
        );
      }
    });
  }
}

// import { Component, inject, signal } from '@angular/core';
// import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
// import { forkJoin, switchMap } from 'rxjs';
// import { DoctorService } from '../../../core/services/doctor.service';
// import { ScheduleService } from '../../../core/services/schedule.service';
// import { Specialty } from '../../../core/models/specialty.model';
// import { SpecialtyService } from '../../../core/services/specialty.service';

// @Component({
//   selector: 'app-create-doctor',
//   standalone: true,
//   imports: [ReactiveFormsModule],
//   templateUrl: './create-doctor.html',
//   styleUrl: './create-doctor.scss'
// })
// export class CreateDoctor {
//   private fb = inject(FormBuilder);
//   private doctorService = inject(DoctorService);
// private specialtyService = inject(SpecialtyService);
//   private scheduleService = inject(ScheduleService);

//   specialties = signal<Specialty[]>([]);
//   isLoading = signal(false);
//   errorMessage = signal<string | null>(null);
//   successMessage = signal<string | null>(null);

//   readonly diasSemana = [
//     { value: 1, label: 'Lunes' },
//     { value: 2, label: 'Martes' },
//     { value: 3, label: 'Miercoles' },
//     { value: 4, label: 'Jueves' },
//     { value: 5, label: 'Viernes' },
//     { value: 6, label: 'Sabado' },
//     { value: 7, label: 'Domingo' }
//   ];

//   form = this.fb.group({
//     username: ['', [Validators.required, Validators.minLength(4)]],
//     email: ['', [Validators.required, Validators.email]],
//     password: ['', [Validators.required, Validators.minLength(6)]],
//     firstName: ['', Validators.required],
//     lastName: ['', Validators.required],
//     phone: [''],
//     specialtyId: [null as number | null, Validators.required],
//     licenseNumber: ['', Validators.required],
//     biography: [''],
//     schedules: this.fb.array([this.buildScheduleGroup()])
//   });

//   constructor() {
//     this.specialtyService.getSpecialties().subscribe((data) => this.specialties.set(data));
//   }

//   get schedules(): FormArray {
//     return this.form.get('schedules') as FormArray;
//   }

//   buildScheduleGroup() {
//     return this.fb.group({
//       dayOfWeek: [null as number | null, Validators.required],
//       startTime: ['', Validators.required],
//       endTime: ['', Validators.required],
//       slotDurationMinutes: [30, [Validators.required, Validators.min(5)]]
//     });
//   }

//   addSchedule(): void {
//     this.schedules.push(this.buildScheduleGroup());
//   }

//   removeSchedule(index: number): void {
//     if (this.schedules.length > 1) {
//       this.schedules.removeAt(index);
//     }
//   }

//   submit(): void {
//     if (this.form.invalid) {
//       this.form.markAllAsTouched();
//       return;
//     }

//     this.errorMessage.set(null);
//     this.successMessage.set(null);
//     this.isLoading.set(true);

//     const { schedules, ...doctorData } = this.form.getRawValue();

//     this.doctorService.createDoctor(doctorData as any).pipe(
//       switchMap((doctor) => {
//         const scheduleRequests = schedules.map((s) =>
//           this.scheduleService.createSchedule({ ...s, doctorId: doctor.id } as any)
//         );
//         return forkJoin(scheduleRequests);
//       })
//     ).subscribe({
//       next: () => {
//         this.isLoading.set(false);
//         this.successMessage.set('Medico y horarios creados exitosamente.');
//         this.form.reset();
//         this.schedules.clear();
//         this.schedules.push(this.buildScheduleGroup());
//       },
//       error: (err) => {
//         this.isLoading.set(false);
//         this.errorMessage.set(err?.error?.message ?? 'Ocurrio un error al crear el medico.');
//       }
//     });
//   }
// }