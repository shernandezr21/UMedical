import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DoctorDetail } from '../../../core/models/admin.model';
import { ScheduleResponse } from '../../../core/models/schedule.model';
import { DoctorService } from '../../../core/services/doctor.service';
import { ScheduleService } from '../../../core/services/schedule.service';
import { timeRangeValidator, toMinutes } from '../../../core/schedule-validators';

@Component({
  selector: 'app-doctor-schedules',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './doctor-schedules.html'
})
export class DoctorSchedules {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private doctorService = inject(DoctorService);
  private scheduleService = inject(ScheduleService);

  readonly doctorId = Number(this.route.snapshot.paramMap.get('id'));

  readonly days = [
    { value: 1, label: 'Lunes' },
    { value: 2, label: 'Martes' },
    { value: 3, label: 'Miercoles' },
    { value: 4, label: 'Jueves' },
    { value: 5, label: 'Viernes' },
    { value: 6, label: 'Sabado' },
    { value: 7, label: 'Domingo' }
  ];

  doctor = signal<DoctorDetail | null>(null);
  blocks = signal<ScheduleResponse[]>([]);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  formOpen = signal(false);
  editingId = signal<number | null>(null);
  isSaving = signal(false);
  formError = signal<string | null>(null);

  sortedBlocks = computed(() =>
    [...this.blocks()].sort((a, b) => a.dayOfWeek - b.dayOfWeek || a.startTime.localeCompare(b.startTime))
  );

  form = this.fb.group({
    dayOfWeek: [null as number | null, Validators.required],
    startTime: ['', Validators.required],
    endTime: ['', Validators.required],
    slotDurationMinutes: [30, [Validators.required, Validators.min(5)]]
  }, { validators: [timeRangeValidator] });

  constructor() {
    this.doctorService.getManagementList().subscribe({
      next: (list) => this.doctor.set(list.find((d) => d.doctorId === this.doctorId) ?? null),
      error: () => this.errorMessage.set('No se pudo cargar al medico.')
    });
    this.loadBlocks();
  }

  dayLabel(day: number): string {
    return this.days.find((d) => d.value === day)?.label ?? String(day);
  }

  loadBlocks(): void {
    this.isLoading.set(true);
    this.scheduleService.getByDoctor(this.doctorId).subscribe({
      next: (data) => {
        this.blocks.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('No se pudieron cargar los horarios.');
      }
    });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.formError.set(null);
    this.successMessage.set(null);
    this.form.reset({ dayOfWeek: null, startTime: '', endTime: '', slotDurationMinutes: 30 });
    this.formOpen.set(true);
  }

  openEdit(block: ScheduleResponse): void {
    this.editingId.set(block.id);
    this.formError.set(null);
    this.successMessage.set(null);
    this.form.reset({
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

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const block = {
      dayOfWeek: raw.dayOfWeek!,
      startTime: raw.startTime!,
      endTime: raw.endTime!,
      slotDurationMinutes: raw.slotDurationMinutes!
    };

    // Cruce con los otros bloques del mismo dia (el servidor tambien lo valida)
    const editing = this.editingId();
    const overlaps = this.blocks().some((b) =>
      b.id !== editing
      && b.dayOfWeek === block.dayOfWeek
      && toMinutes(b.startTime.slice(0, 5)) < toMinutes(block.endTime)
      && toMinutes(block.startTime) < toMinutes(b.endTime.slice(0, 5))
    );
    if (overlaps) {
      this.formError.set('Ese bloque se cruza con otro horario del mismo dia.');
      return;
    }

    this.formError.set(null);
    this.isSaving.set(true);

    const request$ = editing === null
      ? this.scheduleService.createSchedule({ doctorId: this.doctorId, ...block })
      : this.scheduleService.updateSchedule(editing, block);

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

  remove(block: ScheduleResponse): void {
    const label = `${this.dayLabel(block.dayOfWeek)} ${block.startTime.slice(0, 5)} - ${block.endTime.slice(0, 5)}`;
    if (!window.confirm(`¿Eliminar el bloque ${label}?`)) {
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.scheduleService.deleteSchedule(block.id).subscribe({
      next: () => {
        this.successMessage.set('Bloque eliminado.');
        this.loadBlocks();
      },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'No se pudo eliminar el bloque.')
    });
  }
}