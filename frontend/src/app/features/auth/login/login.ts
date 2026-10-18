import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  sessionExpired = this.route.snapshot.queryParamMap.get('expired') === '1';
  errorMessage = signal<string | null>(null);
  isLoading = signal(false);

  form = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set(null);
    this.isLoading.set(true);

    this.authService.login({
      username: this.form.value.username!,
      password: this.form.value.password!
    }).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.router.navigate([this.authService.landingRoute()]);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(
          err?.status === 0
            ? 'No se pudo conectar con el servidor.'
            : 'Usuario o contrasena incorrectos.'
        );
      }
    });
  }
}