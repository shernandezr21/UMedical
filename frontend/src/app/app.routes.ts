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
        path: 'panel',
        loadComponent: () => import('./features/admin/dashboard/admin-dashboard').then(m => m.AdminDashboard),
        canActivate: [roleGuard(['ROLE_ADMIN'])]
      },
      {
        path: 'usuarios',
        loadComponent: () => import('./features/admin/users/users').then(m => m.UsersAdmin),
        canActivate: [roleGuard(['ROLE_ADMIN'])]
      },
      {
        path: 'doctores',
        loadComponent: () => import('./features/admin/doctors/doctors-admin').then(m => m.DoctorsAdmin),
        canActivate: [roleGuard(['ROLE_ADMIN'])]
      },
      {
        path: 'doctores/nuevo',
        loadComponent: () => import('./features/doctors/create-doctor/create-doctor').then(m => m.CreateDoctor),
        canActivate: [roleGuard(['ROLE_ADMIN'])]
      },
      {
        path: 'doctores/:id/horarios',
        loadComponent: () => import('./features/admin/doctor-schedules/doctor-schedules').then(m => m.DoctorSchedules),
        canActivate: [roleGuard(['ROLE_ADMIN'])]
      },
      {
        path: 'especialidades',
        loadComponent: () => import('./features/specialties/create-specialty/create-specialty').then(m => m.CreateSpecialty),
        canActivate: [roleGuard(['ROLE_ADMIN'])]
      },
      {
        path: 'auditoria',
        loadComponent: () => import('./features/admin/audit/audit').then(m => m.AuditPage),
        canActivate: [roleGuard(['ROLE_ADMIN'])]
      },

      // Secretaria y admin
      {
        path: 'calendario',
        loadComponent: () => import('./features/staff/clinic-calendar/clinic-calendar').then(m => m.ClinicCalendar),
        canActivate: [roleGuard(['ROLE_SECRETARY', 'ROLE_ADMIN'])]
      },
      {
        path: 'pacientes',
        loadComponent: () => import('./features/staff/patients/patients').then(m => m.Patients),
        canActivate: [roleGuard(['ROLE_SECRETARY', 'ROLE_ADMIN'])]
      },

      // Doctor
      {
        path: 'mi-agenda',
        loadComponent: () => import('./features/doctor/my-agenda/my-agenda').then(m => m.MyAgenda),
        canActivate: [roleGuard(['ROLE_DOCTOR'])]
      },
            {
        path: 'mis-pacientes',
        loadComponent: () => import('./features/doctor/my-patients/my-patients').then(m => m.MyPatients),
        canActivate: [roleGuard(['ROLE_DOCTOR'])]
      },
      {
        path: 'mis-pacientes/:id',
        loadComponent: () => import('./features/doctor/patient-record/patient-record').then(m => m.PatientRecordPage),
        canActivate: [roleGuard(['ROLE_DOCTOR'])]
      },
      {
        path: 'mis-horarios',
        loadComponent: () => import('./features/doctor/my-schedule/my-schedule').then(m => m.MySchedule),
        canActivate: [roleGuard(['ROLE_DOCTOR'])]
      },
      {
        path: 'mis-estadisticas',
        loadComponent: () => import('./features/doctor/my-stats/my-stats').then(m => m.MyStats),
        canActivate: [roleGuard(['ROLE_DOCTOR'])]
      },

      // Paciente (y agendar tambien para la secretaria y el admin)
      {
        path: 'agendar',
        loadComponent: () => import('./features/patient/book-appointment/book-appointment').then(m => m.BookAppointment),
        canActivate: [roleGuard(['ROLE_PATIENT', 'ROLE_SECRETARY', 'ROLE_ADMIN'])]
      },
      {
        path: 'mis-citas',
        loadComponent: () => import('./features/patient/my-appointments/my-appointments').then(m => m.MyAppointments),
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