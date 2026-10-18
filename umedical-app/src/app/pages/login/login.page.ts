import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { IonContent, IonInput, IonButton, IonIcon, IonSpinner } from '@ionic/angular';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, IonContent, IonInput, IonButton, IonIcon, IonSpinner],
  template: `
    <ion-content class="login-content" [fullscreen]="true">
      <div class="login-wrap">
        <div class="brand">
          <span class="brand__mark"><ion-icon name="medical-outline"></ion-icon></span>
          <h1 class="brand__title">U-Medical</h1>
          <p class="brand__subtitle">Portal de pacientes</p>
        </div>

        <form [formGroup]="form" (ngSubmit)="submit()" class="login-form">
          <div class="field">
            <ion-icon name="person-outline" class="field__icon"></ion-icon>
            <ion-input
              class="field__input"
              placeholder="Usuario"
              formControlName="username"
              autocapitalize="off"></ion-input>
          </div>

          <div class="field">
            <ion-icon name="lock-closed-outline" class="field__icon"></ion-icon>
            <ion-input
              class="field__input"
              placeholder="Contraseña"
              [type]="showPassword() ? 'text' : 'password'"
              formControlName="password"></ion-input>
            <ion-icon
              [name]="showPassword() ? 'eye-off-outline' : 'eye-outline'"
              class="field__toggle"
              (click)="showPassword.set(!showPassword())"></ion-icon>
          </div>

          @if (error()) {
            <p class="error-banner">{{ error() }}</p>
          }

          <ion-button expand="block" type="submit" class="login-btn"
                      [disabled]="form.invalid || loading()">
            @if (loading()) { <ion-spinner name="dots"></ion-spinner> } @else { Iniciar sesión }
          </ion-button>
        </form>

        <p class="login-footer">¿No tienes cuenta? <a routerLink="/register">Regístrate</a></p>
      </div>
    </ion-content>
  `,
  styles: [`
    .login-content { --background: var(--color-bg); }

    .login-wrap {
      min-height: 100%;
      display: flex;
      flex-direction: column;
      justify-content: center;
      padding: 2rem 1.75rem;
      max-width: 420px;
      margin: 0 auto;
    }

    .brand {
      text-align: center;
      margin-bottom: 2.2rem;
    }
    .brand__mark {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 60px;
      height: 60px;
      border-radius: 50%;
      background: var(--color-primary);
      color: #fff;
      font-size: 1.7rem;
      margin-bottom: 0.9rem;
    }
    .brand__title {
      font-family: var(--font-display);
      font-size: 1.9rem;
      color: var(--color-ink);
      margin: 0;
    }
    .brand__subtitle {
      color: var(--color-muted);
      font-size: 0.9rem;
      margin: 0.25rem 0 0;
    }

    .login-form {
      display: flex;
      flex-direction: column;
      gap: 0.9rem;
    }

    .field {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      padding: 0 0.9rem;
    }
    .field__icon {
      flex-shrink: 0;
      font-size: 1.15rem;
      color: var(--color-primary);
    }
    .field__input {
      --padding-start: 0;
      --padding-top: 0.8rem;
      --padding-bottom: 0.8rem;
      flex: 1;
      font-family: var(--font-body);
    }
    .field__toggle {
      flex-shrink: 0;
      font-size: 1.15rem;
      color: var(--color-muted);
      cursor: pointer;
    }

    .error-banner {
      background: var(--color-cancelada-bg);
      color: var(--color-cancelada);
      border-radius: var(--radius-sm);
      padding: 0.6rem 0.8rem;
      font-size: 0.85rem;
      margin: 0;
    }

    .login-btn {
      margin-top: 0.4rem;
      --border-radius: var(--radius-md);
      font-weight: 600;
    }

    .login-footer {
      text-align: center;
      font-size: 0.8rem;
      color: var(--color-muted);
      margin-top: 1.8rem;
    }
    .login-footer a { color: var(--color-primary); font-weight: 600; text-decoration: none; }
  `],
})
export class LoginPage {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  loading = signal(false);
  error = signal('');
  showPassword = signal(false);

  form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  submit() {
    this.loading.set(true);
    this.error.set('');
    const { username, password } = this.form.getRawValue();
    this.auth.login(username, password).subscribe({
      next: () => this.router.navigateByUrl('/tabs/home', { replaceUrl: true }),
      error: (e) => {
        this.loading.set(false);
        this.error.set(
          e.status === 401 || e.status === 403
            ? 'Usuario o contraseña incorrectos'
            : e.message || 'No se pudo conectar con el servidor'
        );
      },
    });
  }
}