import { Component, inject } from '@angular/core';
import { IonHeader, IonToolbar, IonTitle, IonContent, IonButton } from '@ionic/angular';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton],
  template: `
    <ion-header>
      <ion-toolbar><ion-title>Perfil</ion-title></ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      @if (user()) {
        <div class="card ion-margin-bottom">
          <h2>{{ user()?.fullName }}</h2>
          <p style="color: var(--color-muted)">{{ user()?.username }}</p>
          <p style="color: var(--color-muted)">Paciente</p>
        </div>
      }
      <ion-button expand="block" color="danger" fill="outline" (click)="logout()">Cerrar sesión</ion-button>
    </ion-content>
  `,
})
export class ProfilePage {
  private auth = inject(AuthService);
  user = this.auth.currentUser;

  logout() { this.auth.logout(); }
}