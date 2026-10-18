import { Component, inject, signal, viewChild } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CAPTCHA_SITE_KEY } from '../../../core/api.config';
import { controlErrorText } from '../../../core/form-errors';
import { RegisterRequest } from '../../../core/models/auth.model';
import { AuthService } from '../../../core/services/auth.service';
import { Captcha } from '../../../shared/captcha/captcha';

const passwordsMatch = (group: AbstractControl): ValidationErrors | null => {
  const password = group.get('password')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { mismatch: true } : null;
};

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, Captcha],
  templateUrl: './register.html',
  styleUrl: '../login/login.scss'
})
export class Register {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  readonly bloodTypes = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
  readonly today = this.toIso(new Date());
  readonly captchaSiteKey = CAPTCHA_SITE_KEY;

  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  captchaToken = signal<string | null>(null);
  private captcha = viewChild(Captcha);

  form = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(50),
                    Validators.pattern(/^[A-Za-z0-9._-]+$/)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    confirmPassword: ['', Validators.required],
    firstName: ['', [Validators.required, Validators.maxLength(80)]],
    lastName: ['', [Validators.required, Validators.maxLength(80)]],
    phone: ['', Validators.maxLength(20)],
    birthDate: [''],
    bloodType: [''],
    address: ['', Validators.maxLength(255)]
  }, { validators: [passwordsMatch] });

  errorText(name: string): string | null {
    return controlErrorText(this.form, name);
  }

  private toIso(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    if (raw.birthDate && raw.birthDate > this.today) {
      this.errorMessage.set('La fecha de nacimiento no puede ser futura.');
      return;
    }

    if (this.captchaSiteKey && !this.captchaToken()) {
      this.errorMessage.set('Completa la verificacion anti-bots.');
      return;
    }

    const request: RegisterRequest = {
      username: raw.username!.trim(),
      email: raw.email!.trim(),
      password: raw.password!,
      firstName: raw.firstName!.trim(),
      lastName: raw.lastName!.trim(),
      phone: raw.phone?.trim() || undefined,
      birthDate: raw.birthDate || undefined,
      address: raw.address?.trim() || undefined,
      bloodType: raw.bloodType || undefined,
      captchaToken: this.captchaToken() ?? undefined
    };

    this.errorMessage.set(null);
    this.isLoading.set(true);

    this.authService.register(request).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.router.navigate([this.authService.landingRoute()]);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.captcha()?.reset();   // el token del captcha es de un solo uso
        const details: string[] | undefined = err?.error?.details;
        if (err?.status === 0) {
          this.errorMessage.set('No se pudo conectar con el servidor.');
        } else if (details?.length) {
          this.errorMessage.set(`${err.error.message}: ${details.join(' | ')}`);
        } else {
          this.errorMessage.set(err?.error?.message ?? 'No se pudo crear la cuenta.');
        }
      }
    });
  }
}