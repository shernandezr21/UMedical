import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { IonContent, IonInput, IonButton, IonText, IonSpinner } from '@ionic/angular';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, IonContent, IonInput, IonButton, IonText, IonSpinner],
  template: `
    <ion-content class="ion-padding">
      <h1>UMedical</h1>
      <form [formGroup]="form" (ngSubmit)="submit()">
        <ion-input label="Usuario" labelPlacement="floating" fill="outline" formControlName="username"></ion-input>
        <ion-input label="Contraseña" labelPlacement="floating" fill="outline" type="password"
                   formControlName="password" class="ion-margin-top"></ion-input>
        @if (error) { <ion-text color="danger"><p>{{ error }}</p></ion-text> }
        <ion-button expand="block" type="submit" class="ion-margin-top" [disabled]="form.invalid || loading">
          @if (loading) { <ion-spinner name="dots"></ion-spinner> } @else { Iniciar sesión }
        </ion-button>
      </form>
    </ion-content>
  `,
})
export class LoginPage {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  loading = false;
  error = '';
  form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  submit() {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    const { username, password } = this.form.getRawValue();
    this.auth.login(username, password).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigateByUrl('/tabs/home', { replaceUrl: true });
      },
      error: (e) => {
        this.loading = false;
        this.error = e.status === 401 || e.status === 403
          ? 'Usuario o contraseña incorrectos' : e.message || 'No se pudo conectar con el servidor';
        this.cdr.markForCheck();
      },
    });
  }
}
