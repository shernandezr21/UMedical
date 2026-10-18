import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then(m => m.Login)
  },
  {
    path: 'registro',
    loadComponent: () => import('./features/auth/register/register').then(m => m.Register)
  },
  {
    path: 'unauthorized',
    loadComponent: () => import('./features/shared/unauthorized').then(m => m.Unauthorized)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/layout/layout').then(m => m.Layout),
    canActivate: [authGuard],
    children: [
      { path: '', loadComponent: () => import('./features/dashboard/home/home').then(m => m.Home) },

      // Admin
      {
        path: 'doctores',
        loadComponent: () => import('./features/doctors/create-doctor/create-doctor').then(m => m.CreateDoctor),
        canActivate: [roleGuard(['ROLE_ADMIN'])]
      },
      {
        path: 'especialidades',
        loadComponent: () => import('./features/specialties/create-specialty/create-specialty').then(m => m.CreateSpecialty),
        canActivate: [roleGuard(['ROLE_ADMIN'])]
      },

      // Doctor
      {
        path: 'mi-agenda',
        loadComponent: () => import('./features/doctor/my-agenda/my-agenda').then(m => m.MyAgenda),
        canActivate: [roleGuard(['ROLE_DOCTOR'])]
      },

      // Paciente
      {
        path: 'mis-citas',
        loadComponent: () => import('./features/patient/my-appointments/my-appointments').then(m => m.MyAppointments),
        canActivate: [roleGuard(['ROLE_PATIENT'])]
      },
      {
        path: 'agendar',
        loadComponent: () => import('./features/patient/book-appointment/book-appointment').then(m => m.BookAppointment),
        canActivate: [roleGuard(['ROLE_PATIENT'])]
      },
      {
        path: 'medicos',
        loadComponent: () => import('./features/patient/doctors/doctors').then(m => m.DoctorDirectory),
        canActivate: [roleGuard(['ROLE_PATIENT'])]
      },
      {
        path: 'perfil',
        loadComponent: () => import('./features/patient/profile/profile').then(m => m.Profile),
        canActivate: [roleGuard(['ROLE_PATIENT'])]
      }
    ]
  },
  { path: '**', redirectTo: 'login' }
];