import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PatientRecord, Visit } from '../../../core/models/doctor-portal.model';
import { AppointmentService } from '../../../core/services/appointment.service';
import { DoctorPortalService } from '../../../core/services/doctor-portal.service';

@Component({
  selector: 'app-patient-record',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './patient-record.html'
})
export class PatientRecordPage {
  private route = inject(ActivatedRoute);
  private portal = inject(DoctorPortalService);
  private appointmentService = inject(AppointmentService);

  record = signal<PatientRecord | null>(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  age = computed(() => {
    const birthDate = this.record()?.patient.birthDate;
    if (!birthDate) {
      return null;
    }
    const birth = new Date(`${birthDate}T00:00:00`);
    const now = new Date();
    let years = now.getFullYear() - birth.getFullYear();
    const monthDiff = now.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && now.getDate() < birth.getDate())) {
      years--;
    }
    return years;
  });

  constructor() {
    const patientId = Number(this.route.snapshot.paramMap.get('id'));

    this.portal.getPatientRecord(patientId).subscribe({
      next: (record) => {
        this.record.set(record);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(
          err?.status === 403
            ? 'Este paciente no tiene citas contigo, asi que no puedes ver su expediente.'
            : 'No se pudo cargar el expediente.'
        );
      }
    });
  }

  initials(name: string): string {
    return name.split(' ').filter(Boolean).slice(0, 2).map((part) => part[0].toUpperCase()).join('');
  }

  private startOf(visit: Visit): Date {
    return new Date(`${visit.appointmentDate}T${visit.appointmentTime}`);
  }

  statusLabel(visit: Visit): string {
    if (visit.status === 'NO_ASISTIO') { return 'NO ASISTIO'; }
    return visit.status === 'PENDIENTE' && this.startOf(visit).getTime() < Date.now() ? 'SIN ATENDER' : visit.status;
  }

  badgeClass(visit: Visit): string {
    return {
      PENDIENTE: 'badge badge-pendiente',
      COMPLETADA: 'badge badge-completada',
      CANCELADA: 'badge badge-cancelada',
      NO_ASISTIO: 'badge badge-noasistio'
    }[visit.status] ?? 'badge';
  }

  dateLabel(visit: Visit): string {
    const label = new Date(`${visit.appointmentDate}T00:00:00`).toLocaleDateString('es', {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
    });
    return label.charAt(0).toUpperCase() + label.slice(1);
  }

  viewFile(documentId: number): void {
    // Se abre la pestana en el clic (para que el navegador no la bloquee) y luego se le asigna el PDF
    const win = window.open('', '_blank');
    this.appointmentService.downloadDocumentFile(documentId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        if (win) {
          win.location.href = url;
        } else {
          window.open(url, '_blank');
        }
      },
      error: () => {
        win?.close();
        this.errorMessage.set('No se pudo abrir el PDF.');
      }
    });
  }
}