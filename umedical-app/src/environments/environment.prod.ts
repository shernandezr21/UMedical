import { Capacitor } from '@capacitor/core';
// Cambiar valor siempre que se cambie de red, a IP que asigne router a dispositivo. 
const LAN_IP = '192.168.0.89';

export const environment = {
  production: true,
  apiUrl: Capacitor.isNativePlatform()
    ? `http://${LAN_IP}:8080/api/v1`
    : 'http://localhost:8080/api/v1',
};