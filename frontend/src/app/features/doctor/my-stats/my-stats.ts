import { Component, computed, inject, signal } from '@angular/core';
import { DoctorStats } from '../../../core/models/doctor-portal.model';
import { DoctorPortalService } from '../../../core/services/doctor-portal.service';

// Mismo orden de los datos: Pendientes, Atendidas, Canceladas, No asistieron, Sin atender
const STATUS_COLORS = [
  'var(--color-pendiente)',
  'var(--color-completada)',
  'var(--color-cancelada)',
  '#7A6AA8',
  'var(--color-muted)'
];

type Preset = 'last30' | 'month' | 'next30' | 'last90';

@Component({
  selector: 'app-my-stats',
  standalone: true,
  templateUrl: './my-stats.html',
  styleUrl: '../../admin/dashboard/admin-dashboard.scss'
})
export class MyStats {
  private portal = inject(DoctorPortalService);

  startDate = signal('');
  endDate = signal('');
  preset = signal<Preset | null>(null);
  stats = signal<DoctorStats | null>(null);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  statusSegments = computed(() => {
    const s = this.stats();
    if (!s || s.totalAppointments === 0) {
      return [];
    }
    const items = [
      { label: 'Pendientes', count: s.pending },
      { label: 'Atendidas', count: s.completed },
      { label: 'Canceladas', count: s.cancelled },
      { label: 'No asistieron', count: s.noShow },
      { label: 'Sin atender', count: s.unattended }
    ];
    return items.map((item, i) => ({
      ...item,
      percent: (item.count / s.totalAppointments) * 100,
      color: STATUS_COLORS[i]
    }));
  });

  dayColumns = computed(() => {
    const s = this.stats();
    if (!s) {
      return [];
    }
    const max = Math.max(1, ...s.byDay.map((d) => d.count));
    return s.byDay.map((d) => ({ date: d.date, count: d.count, height: (d.count / max) * 100 }));
  });

  maxWeekday = computed(() => Math.max(1, ...(this.stats()?.byWeekday.map((i) => i.count) ?? [0])));

  cancelRate = computed(() => {
    const s = this.stats();
    return s && s.totalAppointments > 0 ? Math.round((s.cancelled / s.totalAppointments) * 100) : 0;
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

    this.portal.getStats(this.startDate(), this.endDate()).subscribe({
      next: (data) => {
        this.stats.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudieron cargar las estadisticas.');
      }
    });
  }
}
