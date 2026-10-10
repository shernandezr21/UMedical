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
    path: 'dashboard',
    loadComponent: () => import('./features/layout/layout').then(m => m.Layout),
    canActivate: [authGuard],
    children: [
      { path: '', loadComponent: () => import('./features/dashboard/home/home').then(m => m.Home) },
      { path: '', loadComponent: () => import('./features/dashboard/home/home').then(m => m.Home) },
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
      {
        path: 'mi-agenda',
        loadComponent: () => import('./features/doctor/my-agenda/my-agenda').then(m => m.MyAgenda),
        canActivate: [roleGuard(['ROLE_DOCTOR'])]
      }
    ]
  }
];