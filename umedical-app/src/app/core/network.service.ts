import { Injectable, signal } from '@angular/core';
import { Network } from '@capacitor/network';

@Injectable({ providedIn: 'root' })
export class NetworkService {
  online = signal(true);

  constructor() {
    Network.getStatus().then((s) => this.online.set(s.connected));
    Network.addListener('networkStatusChange', (s) => this.online.set(s.connected));
  }
}