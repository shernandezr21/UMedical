import { Component, computed, inject, signal } from '@angular/core';
import { ReportSummary } from '../../../core/models/admin.model';
import { ReportService } from '../../../core/services/report.service';

// Mismo orden que devuelve el backend: Pendientes, Completadas, Canceladas, Sin atender
const STATUS_COLORS = [
  'var(--color-pendiente)',
  'var(--color-completada)',
  'var(--color-cancelada)',
  'var(--color-muted)'
];

type Preset = 'last30' | 'month' | 'next30' | 'last90';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss'
})
export class AdminDashboard {
  private reportService = inject(ReportService);

  startDate = signal('');
  endDate = signal('');
  preset = signal<Preset | null>(null);
  report = signal<ReportSummary | null>(null);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  statusSegments = computed(() => {
    const r = this.report();
    if (!r || r.totalAppointments === 0) {
      return [];
    }
    return r.byStatus.map((item, i) => ({
      label: item.label,
      count: item.count,
      percent: (item.count / r.totalAppointments) * 100,
      color: STATUS_COLORS[i] ?? 'var(--color-muted)'
    }));
  });

  maxSpecialty = computed(() => Math.max(1, ...(this.report()?.bySpecialty.map((i) => i.count) ?? [0])));

  dayColumns = computed(() => {
    const r = this.report();
    if (!r) {
      return [];
    }
    const max = Math.max(1, ...r.byDay.map((d) => d.count));
    return r.byDay.map((d) => ({ date: d.date, count: d.count, height: (d.count / max) * 100 }));
  });

  cancelRate = computed(() => {
    const r = this.report();
    return r && r.totalAppointments > 0 ? Math.round((r.cancelled / r.totalAppointments) * 100) : 0;
  });

  constructor() {
    this.applyPreset('last30');
  }

  private toIso(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }

  applyPreset(kind: Preset): void {
    const today = new Date();
    let start = new Date(today);
    let end = new Date(today);

    switch (kind) {
      case 'last30':
        start.setDate(today.getDate() - 29);
        break;
      case 'last90':
        start.setDate(today.getDate() - 89);
        break;
      case 'next30':
        end.setDate(today.getDate() + 30);
        break;
      case 'month':
        start = new Date(today.getFullYear(), today.getMonth(), 1);
        end = new Date(today.getFullYear(), today.getMonth() + 1, 0);
        break;
    }

    this.preset.set(kind);
    this.startDate.set(this.toIso(start));
    this.endDate.set(this.toIso(end));
    this.load();
  }

  onStartChange(event: Event): void {
    this.startDate.set((event.target as HTMLInputElement).value);
    this.preset.set(null);
    this.load();
  }

  onEndChange(event: Event): void {
    this.endDate.set((event.target as HTMLInputElement).value);
    this.preset.set(null);
    this.load();
  }

  load(): void {
    if (!this.startDate() || !this.endDate()) {
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.reportService.getSummary(this.startDate(), this.endDate()).subscribe({
      next: (data) => {
        this.report.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo cargar el resumen.');
      }
    });
  }
}