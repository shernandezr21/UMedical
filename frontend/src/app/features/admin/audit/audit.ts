import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { blobErrorMessage, saveBlob } from '../../../core/download';
import { PageResponse } from '../../../core/models/admin.model';
import { AuditEntry, AuditFilters } from '../../../core/models/audit.model';
import { AuditService } from '../../../core/services/audit.service';

const ACTION_LABELS: Record<string, string> = {
  DOCUMENTS_VIEWED: 'Consulto los documentos de una cita',
  DOCUMENT_CREATED: 'Registro un documento clinico',
  DOCUMENT_FILE_UPLOADED: 'Adjunto un PDF',
  DOCUMENT_FILE_DOWNLOADED: 'Abrio un PDF',
  PATIENT_RECORD_VIEWED: 'Abrio el expediente de un paciente',
  APPOINTMENT_NO_SHOW: 'Marco una inasistencia',
  USER_CREATED: 'Creo un usuario',
  USER_ACTIVATED: 'Activo una cuenta',
  USER_DEACTIVATED: 'Desactivo una cuenta',
  PASSWORD_RESET: 'Restablecio una contrasena',
  DOCTOR_ACTIVATED: 'Activo a un medico',
  DOCTOR_DEACTIVATED: 'Desactivo a un medico',
  REPORT_EXPORTED: 'Exporto un reporte'
};

const ROLE_LABELS: Record<string, string> = {
  ROLE_ADMIN: 'Administrador',
  ROLE_DOCTOR: 'Medico',
  ROLE_SECRETARY: 'Secretaria',
  ROLE_PATIENT: 'Paciente'
};

const ENTITY_LABELS: Record<string, string> = {
  APPOINTMENT: 'Cita',
  DOCUMENT: 'Documento',
  PATIENT: 'Paciente',
  USER: 'Usuario',
  DOCTOR: 'Medico',
  REPORT: 'Reporte'
};

@Component({
  selector: 'app-audit',
  standalone: true,
  templateUrl: './audit.html',
  styleUrl: '../dashboard/admin-dashboard.scss'
})
export class AuditPage {
  private auditService = inject(AuditService);
  private actor$ = new Subject<string>();

  readonly pageSize = 20;
  readonly actions = Object.entries(ACTION_LABELS).map(([value, label]) => ({ value, label }));

  actionFilter = signal('');
  actorFilter = signal('');
  startDate = signal(this.toIso(this.daysAgo(6)));
  endDate = signal(this.toIso(new Date()));
  page = signal(0);

  result = signal<PageResponse<AuditEntry> | null>(null);
  isLoading = signal(true);
  isExporting = signal(false);
  errorMessage = signal<string | null>(null);

  constructor() {
    this.actor$.pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed()).subscribe((text) => {
      this.actorFilter.set(text);
      this.page.set(0);
      this.load();
    });
    this.load();
  }

  private daysAgo(days: number): Date {
    const date = new Date();
    date.setDate(date.getDate() - days);
    return date;
  }

  private toIso(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }

  private filters(): AuditFilters {
    return {
      action: this.actionFilter(),
      actor: this.actorFilter(),
      startDate: this.startDate(),
      endDate: this.endDate()
    };
  }

  load(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.auditService.search(this.filters(), this.page(), this.pageSize).subscribe({
      next: (result) => {
        this.result.set(result);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo cargar la bitacora.');
      }
    });
  }

  onAction(event: Event): void {
    this.actionFilter.set((event.target as HTMLSelectElement).value);
    this.page.set(0);
    this.load();
  }

  onActor(event: Event): void {
    this.actor$.next((event.target as HTMLInputElement).value);
  }

  onStart(event: Event): void {
    this.startDate.set((event.target as HTMLInputElement).value);
    this.page.set(0);
    this.load();
  }

  onEnd(event: Event): void {
    this.endDate.set((event.target as HTMLInputElement).value);
    this.page.set(0);
    this.load();
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.page.update((p) => p - 1);
      this.load();
    }
  }

  nextPage(): void {
    if (this.page() + 1 < (this.result()?.totalPages ?? 0)) {
      this.page.update((p) => p + 1);
      this.load();
    }
  }

  exportCsv(): void {
    this.isExporting.set(true);
    this.errorMessage.set(null);

    this.auditService.downloadCsv(this.filters()).subscribe({
      next: (blob) => {
        saveBlob(blob, `bitacora_${this.startDate()}_${this.endDate()}.csv`);
        this.isExporting.set(false);
      },
      error: async (err) => {
        this.isExporting.set(false);
        this.errorMessage.set(await blobErrorMessage(err, 'No se pudo exportar la bitacora.'));
      }
    });
  }

  actionLabel(action: string): string {
    return ACTION_LABELS[action] ?? action;
  }

  roleLabel(role: string): string {
    return ROLE_LABELS[role] ?? role;
  }

  entityLabel(entry: AuditEntry): string {
    if (!entry.entityType) {
      return '';
    }
    const name = ENTITY_LABELS[entry.entityType] ?? entry.entityType;
    return entry.entityId !== null ? `${name} #${entry.entityId}` : name;
  }

  timeLabel(occurredAt: string): string {
    return occurredAt.replace('T', ' ').slice(0, 16);
  }
}
