import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TimeOff } from '../../../core/models/doctor-portal.model';
import { ScheduleResponse } from '../../../core/models/schedule.model';
import { DoctorPortalService } from '../../../core/services/doctor-portal.service';
import { timeRangeValidator, toMinutes } from '../../../core/schedule-validators';

@Component({
  selector: 'app-my-schedule',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './my-schedule.html'
})
export class MySchedule {
  private fb = inject(FormBuilder);
  private portal = inject(DoctorPortalService);

  readonly days = [
    { value: 1, label: 'Lunes' },
    { value: 2, label: 'Martes' },
    { value: 3, label: 'Miercoles' },
    { value: 4, label: 'Jueves' },
    { value: 5, label: 'Viernes' },
    { value: 6, label: 'Sabado' },
    { value: 7, label: 'Domingo' }
  ];
  readonly today = this.toIso(new Date());

  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  // ---------------------------------------------------------------- bloques de atencion
  blocks = signal<ScheduleResponse[]>([]);
  blocksLoading = signal(true);
  formOpen = signal(false);
  editingId = signal<number | null>(null);
  isSaving = signal(false);
  formError = signal<string | null>(null);

  sortedBlocks = computed(() =>
    [...this.blocks()].sort((a, b) => a.dayOfWeek - b.dayOfWeek || a.startTime.localeCompare(b.startTime))
  );

  blockForm = this.fb.group({
    dayOfWeek: [null as number | null, Validators.required],
    startTime: ['', Validators.required],
    endTime: ['', Validators.required],
    slotDurationMinutes: [30, [Validators.required, Validators.min(5)]]
  }, { validators: [timeRangeValidator] });

  // ---------------------------------------------------------------- dias no disponibles
  timeOffs = signal<TimeOff[]>([]);
  timeOffLoading = signal(true);
  offFormOpen = signal(false);
  offSaving = signal(false);
  offError = signal<string | null>(null);

  offForm = this.fb.group({
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    reason: ['', Validators.maxLength(200)],
    cancelExisting: [false]
  });

  constructor() {
    this.loadBlocks();
    this.loadTimeOff();
  }

  private toIso(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }

  dayLabel(day: number): string {
    return this.days.find((d) => d.value === day)?.label ?? String(day);
  }

  rangeLabel(t: TimeOff): string {
    return t.startDate === t.endDate ? t.startDate : `${t.startDate} al ${t.endDate}`;
  }

  // ================================================================ bloques de atencion

  loadBlocks(): void {
    this.blocksLoading.set(true);
    this.portal.getMySchedules().subscribe({
      next: (data) => {
        this.blocks.set(data);
        this.blocksLoading.set(false);
      },
      error: () => {
        this.blocksLoading.set(false);
        this.errorMessage.set('No se pudieron cargar tus horarios.');
      }
    });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.formError.set(null);
    this.successMessage.set(null);
    this.blockForm.reset({ dayOfWeek: null, startTime: '', endTime: '', slotDurationMinutes: 30 });
    this.formOpen.set(true);
  }

  openEdit(block: ScheduleResponse): void {
    this.editingId.set(block.id);
    this.formError.set(null);
    this.successMessage.set(null);
    this.blockForm.reset({
      dayOfWeek: block.dayOfWeek,
      startTime: block.startTime.slice(0, 5),
      endTime: block.endTime.slice(0, 5),
      slotDurationMinutes: block.slotDurationMinutes
    });
    this.formOpen.set(true);
  }

  closeForm(): void {
    this.formOpen.set(false);
    this.editingId.set(null);
  }

  saveBlock(): void {
    if (this.blockForm.invalid) {
      this.blockForm.markAllAsTouched();
      return;
    }

    const raw = this.blockForm.getRawValue();
    const block = {
      dayOfWeek: raw.dayOfWeek!,
      startTime: raw.startTime!,
      endTime: raw.endTime!,
      slotDurationMinutes: raw.slotDurationMinutes!
    };

    // Cruce con mis otros bloques del mismo dia (el servidor tambien lo valida)
    const editing = this.editingId();
    const overlaps = this.blocks().some((b) =>
      b.id !== editing
      && b.dayOfWeek === block.dayOfWeek
      && toMinutes(b.startTime.slice(0, 5)) < toMinutes(block.endTime)
      && toMinutes(block.startTime) < toMinutes(b.endTime.slice(0, 5))
    );
    if (overlaps) {
      this.formError.set('Ese bloque se cruza con otro horario tuyo del mismo dia.');
      return;
    }

    this.formError.set(null);
    this.isSaving.set(true);

    const request$ = editing === null
      ? this.portal.createMySchedule(block)
      : this.portal.updateMySchedule(editing, block);

    request$.subscribe({
      next: () => {
        this.isSaving.set(false);
        this.successMessage.set(editing === null ? 'Bloque agregado.' : 'Bloque actualizado.');
        this.closeForm();
        this.loadBlocks();
      },
      error: (err) => {
        this.isSaving.set(false);
        this.formError.set(err?.error?.message ?? 'No se pudo guardar el bloque.');
      }
    });
  }

  removeBlock(block: ScheduleResponse): void {
    const label = `${this.dayLabel(block.dayOfWeek)} ${block.startTime.slice(0, 5)} - ${block.endTime.slice(0, 5)}`;
    if (!window.confirm(`¿Eliminar el bloque ${label}?`)) {
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.portal.deleteMySchedule(block.id).subscribe({
      next: () => {
        this.successMessage.set('Bloque eliminado.');
        this.loadBlocks();
      },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'No se pudo eliminar el bloque.')
    });
  }

  // ================================================================ dias no disponibles

  loadTimeOff(): void {
    this.timeOffLoading.set(true);
    this.portal.getMyTimeOff().subscribe({
      next: (data) => {
        this.timeOffs.set(data);
        this.timeOffLoading.set(false);
      },
      error: () => {
        this.timeOffLoading.set(false);
        this.errorMessage.set('No se pudieron cargar tus dias no disponibles.');
      }
    });
  }

  openOffForm(): void {
    this.offError.set(null);
    this.successMessage.set(null);
    this.offForm.reset({ startDate: '', endDate: '', reason: '', cancelExisting: false });
    this.offFormOpen.set(true);
  }

  closeOffForm(): void {
    this.offFormOpen.set(false);
  }

  saveTimeOff(): void {
    if (this.offForm.invalid) {
      this.offForm.markAllAsTouched();
      return;
    }

    const raw = this.offForm.getRawValue();
    if (raw.startDate! < this.today) {
      this.offError.set('No se pueden bloquear fechas pasadas.');
      return;
    }
    if (raw.endDate! < raw.startDate!) {
      this.offError.set('La fecha final debe ser igual o posterior a la inicial.');
      return;
    }

    this.offError.set(null);
    this.offSaving.set(true);

    this.portal.createTimeOff({
      startDate: raw.startDate!,
      endDate: raw.endDate!,
      reason: raw.reason?.trim() || undefined,
      cancelExisting: raw.cancelExisting || undefined
    }).subscribe({
      next: (created) => {
        this.offSaving.set(false);
        this.offFormOpen.set(false);
        this.successMessage.set(
          created.cancelledAppointments > 0
            ? `Dias bloqueados. Se cancelaron ${created.cancelledAppointments} cita(s) pendiente(s).`
            : 'Dias bloqueados. Ya no se ofreceran horarios en esas fechas.'
        );
        this.loadTimeOff();
      },
      error: (err) => {
        this.offSaving.set(false);
        this.offError.set(err?.error?.message ?? 'No se pudo bloquear esos dias.');
      }
    });
  }

  removeTimeOff(t: TimeOff): void {
    if (!window.confirm(`¿Quitar el bloqueo del ${this.rangeLabel(t)}? Volveran a ofrecerse horarios en esas fechas.`)) {
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.portal.deleteTimeOff(t.id).subscribe({
      next: () => {
        this.successMessage.set('Bloqueo eliminado.');
        this.loadTimeOff();
      },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'No se pudo eliminar el bloqueo.')
    });
  }
}