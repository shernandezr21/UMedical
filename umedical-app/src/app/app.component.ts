import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonApp, IonRouterOutlet, IonBadge } from '@ionic/angular';
import { NetworkService } from './core/network.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, IonApp, IonRouterOutlet, IonBadge],
  template: `
    <ion-app>
      @if (!network.online()) {
        <ion-badge color="warning" class="offline-banner">Sin conexión a internet</ion-badge>
      }
      <ion-router-outlet></ion-router-outlet>
    </ion-app>
  `,
  styles: [`
    .offline-banner {
      display: block;
      width: 100%;
      text-align: center;
      border-radius: 0;
      padding: 6px;
      font-size: 0.9rem;
      padding-top: calc(6px + env(safe-area-inset-top, 0px));
    }
  `],
})
export class AppComponent {
  network = inject(NetworkService);
}