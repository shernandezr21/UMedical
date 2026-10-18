import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators
} from '@angular/forms';
import { controlErrorText } from '../../../core/form-errors';
import { AuthService } from '../../../core/services/auth.service';
import { PatientService } from '../../../core/services/patient.service';

const newPasswordRules: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const current = group.get('currentPassword')?.value;
  const next = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;

  if (next && confirm && next !== confirm) {
    return { mismatch: true };
  }
  if (current && next && current === next) {
    return { sameAsCurrent: true };
  }
  return null;
};

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './profile.html'
})
export class Profile {
  private fb = inject(FormBuilder);
  private patientService = inject(PatientService);
  private authService = inject(AuthService);

  readonly bloodTypes = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
  readonly today = this.toIso(new Date());

  username = signal('');
  isLoadingProfile = signal(true);
  isSavingProfile = signal(false);
  profileError = signal<string | null>(null);
  profileSuccess = signal<string | null>(null);

  isSavingPassword = signal(false);
  passwordError = signal<string | null>(null);
  passwordSuccess = signal<string | null>(null);

  profileForm = this.fb.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    firstName: ['', [Validators.required, Validators.maxLength(80)]],
    lastName: ['', [Validators.required, Validators.maxLength(80)]],
    phone: ['', Validators.maxLength(20)],
    birthDate: [''],
    bloodType: [''],
    address: ['', Validators.maxLength(255)]
  });

  passwordForm = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    confirmPassword: ['', Validators.required]
  }, { validators: [newPasswordRules] });

  constructor() {
    this.patientService.getProfile().subscribe({
      next: (profile) => {
        this.username.set(profile.username);
        this.profileForm.patchValue({
          email: profile.email,
          firstName: profile.firstName,
          lastName: profile.lastName,
          phone: profile.phone ?? '',
          birthDate: profile.birthDate ?? '',
          bloodType: profile.bloodType ?? '',
          address: profile.address ?? ''
        });
        this.isLoadingProfile.set(false);
      },
      error: () => {
        this.isLoadingProfile.set(false);
        this.profileError.set('No se pudo cargar tu perfil.');
      }
    });
  }

  profileErrorText(name: string): string | null {
    return controlErrorText(this.profileForm, name);
  }

  passwordErrorText(name: string): string | null {
    return controlErrorText(this.passwordForm, name);
  }

  private toIso(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const raw = this.profileForm.getRawValue();
    if (raw.birthDate && raw.birthDate > this.today) {
      this.profileError.set('La fecha de nacimiento no puede ser futura.');
      return;
    }

    this.profileError.set(null);
    this.profileSuccess.set(null);
    this.isSavingProfile.set(true);

    this.patientService.updateProfile({
      email: raw.email!.trim(),
      firstName: raw.firstName!.trim(),
      lastName: raw.lastName!.trim(),
      phone: raw.phone?.trim() || null,
      birthDate: raw.birthDate || null,
      bloodType: raw.bloodType || null,
      address: raw.address?.trim() || null
    }).subscribe({
      next: (updated) => {
        this.isSavingProfile.set(false);
        this.authService.setFullName(`${updated.firstName} ${updated.lastName}`);
        this.profileSuccess.set('Tu perfil se actualizo.');
      },
      error: (err) => {
        this.isSavingProfile.set(false);
        const details: string[] | undefined = err?.error?.details;
        this.profileError.set(
          details?.length
            ? `${err.error.message}: ${details.join(' | ')}`
            : err?.error?.message ?? 'No se pudo actualizar el perfil.'
        );
      }
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.passwordError.set(null);
    this.passwordSuccess.set(null);
    this.isSavingPassword.set(true);

    const raw = this.passwordForm.getRawValue();

    this.authService.changePassword({
      currentPassword: raw.currentPassword!,
      newPassword: raw.newPassword!
    }).subscribe({
      next: () => {
        this.isSavingPassword.set(false);
        this.passwordForm.reset();
        this.passwordSuccess.set('Tu contrasena se cambio correctamente.');
      },
      error: (err) => {
        this.isSavingPassword.set(false);
        this.passwordError.set(err?.error?.message ?? 'No se pudo cambiar la contrasena.');
      }
    });
  }
}