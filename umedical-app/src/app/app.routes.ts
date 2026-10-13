import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login.page').then(m => m.LoginPage) },
  { path: 'booking', canActivate: [authGuard], loadComponent: () => import('./pages/booking/booking.page').then(m => m.BookingPage) },
  { path: 'my-appointments', canActivate: [authGuard], loadComponent: () => import('./pages/my-appointments/my-appointments.page').then(m => m.MyAppointmentsPage) },
  { path: '', redirectTo: 'booking', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.page').then( m => m.LoginPage)
  },
  {
    path: 'booking',
    loadComponent: () => import('./pages/booking/booking.page').then( m => m.BookingPage)
  },
  {
    path: 'my-appointments',
    loadComponent: () => import('./pages/my-appointments/my-appointments.page').then( m => m.MyAppointmentsPage)
  },
];
