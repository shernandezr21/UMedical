import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { IonContent, IonInput, IonButton, IonIcon, IonSpinner } from '@ionic/angular';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, IonContent, IonInput, IonButton, IonIcon, IonSpinner],
  template: `
    <ion-content class="register-content" [fullscreen]="true">
      <div class="register-wrap">
        <div class="brand">
          <span class="brand__mark"><ion-icon name="medical-outline"></ion-icon></span>
          <h1 class="brand__title">Crear cuenta</h1>
          <p class="brand__subtitle">Regístrate para agendar tus citas</p>
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()" class="register-form">
          <div class="field">
            <ion-icon name="person-outline" class="field__icon"></ion-icon>
            <ion-input class="field__input" placeholder="Nombre" formControlName="firstName"></ion-input>
          </div>
          <div class="field">
            <ion-icon name="person-outline" class="field__icon"></ion-icon>
            <ion-input class="field__input" placeholder="Apellido" formControlName="lastName"></ion-input>
          </div>
          <div class="field">
            <ion-icon name="person-outline" class="field__icon"></ion-icon>
            <ion-input class="field__input" placeholder="Usuario" formControlName="username" autocapitalize="off"></ion-input>
          </div>
          <div class="field">
            <ion-icon name="lock-closed-outline" class="field__icon"></ion-icon>
            <ion-input class="field__input" placeholder="Correo electrónico" type="email"
                       formControlName="email" autocapitalize="off"></ion-input>
          </div>
          <div class="field">
            <ion-icon name="lock-closed-outline" class="field__icon"></ion-icon>
            <ion-input class="field__input" placeholder="Contraseña (mínimo 8 caracteres)"
                       [type]="showPassword() ? 'text' : 'password'" formControlName="password"></ion-input>
            <ion-icon
              [name]="showPassword() ? 'eye-off-outline' : 'eye-outline'"
              class="field__toggle"
              (click)="showPassword.set(!showPassword())"></ion-icon>
          </div>
          <div class="field">
            <ion-icon name="calendar-outline" class="field__icon"></ion-icon>
            <ion-input class="field__input" placeholder="Teléfono (opcional)" formControlName="phone"></ion-input>
          </div>

          @if (error()) {
            <p class="error-banner">{{ error() }}</p>
          }

          <ion-button expand="block" type="submit" class="register-btn"
                      [disabled]="form.invalid || loading()">
            @if (loading()) { <ion-spinner name="dots"></ion-spinner> } @else { Crear cuenta }
          </ion-button>
        </form>

        <p class="register-footer">
          ¿Ya tienes cuenta? <a routerLink="/login">Inicia sesión</a>
        </p>
      </div>
    </ion-content>
  `,
  styles: [`
    .register-content { --background: var(--color-bg); }
    .register-wrap {
      min-height: 100%;
      display: flex; flex-direction: column; justify-content: center;
      padding: 2rem 1.75rem; max-width: 420px; margin: 0 auto;
    }
    .brand { text-align: center; margin-bottom: 1.8rem; }
    .brand__mark {
      display: inline-flex; align-items: center; justify-content: center;
      width: 56px; height: 56px; border-radius: 50%;
      background: var(--color-primary); color: #fff; font-size: 1.5rem; margin-bottom: 0.8rem;
    }
    .brand__title { font-family: var(--font-display); font-size: 1.6rem; color: var(--color-ink); margin: 0; }
    .brand__subtitle { color: var(--color-muted); font-size: 0.88rem; margin: 0.25rem 0 0; }

    .register-form { display: flex; flex-direction: column; gap: 0.75rem; }

    .field {
      display: flex; align-items: center; gap: 0.6rem;
      background: var(--color-surface); border: 1px solid var(--color-border);
      border-radius: var(--radius-md); padding: 0 0.9rem;
    }
    .field__icon { flex-shrink: 0; font-size: 1.1rem; color: var(--color-primary); }
    .field__input { --padding-start: 0; --padding-top: 0.7rem; --padding-bottom: 0.7rem; flex: 1; font-family: var(--font-body); }
    .field__toggle { flex-shrink: 0; font-size: 1.1rem; color: var(--color-muted); cursor: pointer; }

    .error-banner {
      background: var(--color-cancelada-bg); color: var(--color-cancelada);
      border-radius: var(--radius-sm); padding: 0.6rem 0.8rem; font-size: 0.85rem; margin: 0;
    }

    .register-btn { margin-top: 0.4rem; --border-radius: var(--radius-md); font-weight: 600; }

    .register-footer { text-align: center; font-size: 0.85rem; color: var(--color-muted); margin-top: 1.5rem; }
    .register-footer a { color: var(--color-primary); font-weight: 600; text-decoration: none; }
  `],
})
export class RegisterPage {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  loading = signal(false);
  error = signal('');
  showPassword = signal(false);

  form = this.fb.nonNullable.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    username: ['', [Validators.required, Validators.minLength(4)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    phone: [''],
  });

  submit() {
    this.loading.set(true);
    this.error.set('');
    const raw = this.form.getRawValue();
    this.auth.register({ ...raw, phone: raw.phone || undefined }).subscribe({
      next: () => this.router.navigateByUrl('/tabs/home', { replaceUrl: true }),
      error: (e) => {
        this.loading.set(false);
        this.error.set(
          e.status === 409
            ? 'Ese usuario o correo ya está registrado'
            : e.error?.message || 'No se pudo crear la cuenta'
        );
      },
    });
  }
}