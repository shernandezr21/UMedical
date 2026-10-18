import { Component, computed, effect, inject, input, output, signal, untracked } from '@angular/core';
import { AvailableSlot, DayAvailability } from '../../core/models/schedule.model';
import { ScheduleService } from '../../core/services/schedule.service';

export interface SelectedSlot {
  date: string;
  time: string;
}

interface DayView {
  iso: string;
  dayName: string;
  dayNumber: number;
  month: string;
  available: number;
}

const DAY_NAMES = ['Dom', 'Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab'];
const MONTH_NAMES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
const WINDOW_DAYS = 14;

function toIso(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

function addDays(date: Date, days: number): Date {
  const copy = new Date(date);
  copy.setDate(copy.getDate() + days);
  return copy;
}

@Component({
  selector: 'app-slot-picker',
  standalone: true,
  templateUrl: './slot-picker.html',
  styleUrl: './slot-picker.scss'
})
export class SlotPicker {
  private scheduleService = inject(ScheduleService);

  doctorId = input.required<number>();
  slotSelected = output<SelectedSlot | null>();

  readonly maxWindow = 5;

  windowOffset = signal(0);
  days = signal<DayView[]>([]);
  isLoadingDays = signal(false);
  selectedDate = signal<string | null>(null);
  slots = signal<AvailableSlot[]>([]);
  isLoadingSlots = signal(false);
  selectedTime = signal<string | null>(null);
  errorMessage = signal<string | null>(null);

  private daysRequest = 0;
  private slotsRequest = 0;

  rangeLabel = computed(() => {
    const days = this.days();
    if (!days.length) {
      return '';
    }
    const first = days[0];
    const last = days[days.length - 1];
    return `${first.dayNumber} ${first.month} - ${last.dayNumber} ${last.month}`;
  });

  selectedDayLabel = computed(() => {
    const day = this.days().find((d) => d.iso === this.selectedDate());
    return day ? `${day.dayName} ${day.dayNumber} de ${day.month}` : '';
  });

  constructor() {
    effect(() => {
      const id = this.doctorId();
      untracked(() => {
        this.windowOffset.set(0);
        this.clearSelection();
        this.loadDays(id);
      });
    });
  }

  previousWindow(): void {
    if (this.windowOffset() > 0) {
      this.windowOffset.update((n) => n - 1);
      this.clearSelection();
      this.loadDays();
    }
  }

  nextWindow(): void {
    if (this.windowOffset() < this.maxWindow) {
      this.windowOffset.update((n) => n + 1);
      this.clearSelection();
      this.loadDays();
    }
  }

  selectDay(day: DayView): void {
    if (day.available === 0) {
      return;
    }
    this.selectedDate.set(day.iso);
    this.selectedTime.set(null);
    this.slotSelected.emit(null);
    this.loadSlots(day.iso);
  }

  selectTime(slot: AvailableSlot): void {
    this.selectedTime.set(slot.time);
    this.slotSelected.emit({ date: slot.date, time: slot.time });
  }

  /** Vuelve a pedir la disponibilidad (p. ej. cuando el servidor dice que un horario se acaba de ocupar). */
  reload(): void {
    const date = this.selectedDate();
    this.selectedTime.set(null);
    this.slotSelected.emit(null);
    this.loadDays();
    if (date) {
      this.loadSlots(date);
    }
  }

  private clearSelection(): void {
    this.selectedDate.set(null);
    this.selectedTime.set(null);
    this.slots.set([]);
    this.slotSelected.emit(null);
  }

  private loadDays(doctorId = this.doctorId()): void {
    const request = ++this.daysRequest;
    const start = addDays(new Date(new Date().setHours(0, 0, 0, 0)), this.windowOffset() * WINDOW_DAYS);
    const end = addDays(start, WINDOW_DAYS - 1);

    this.isLoadingDays.set(true);
    this.errorMessage.set(null);

    this.scheduleService.getAvailableRange(doctorId, toIso(start), toIso(end)).subscribe({
      next: (data) => {
        if (request !== this.daysRequest) {
          return; // llego una respuesta vieja (el usuario ya cambio de medico o de pagina)
        }
        this.days.set(data.map((d) => this.toDayView(d)));
        this.isLoadingDays.set(false);
      },
      error: () => {
        if (request !== this.daysRequest) {
          return;
        }
        this.isLoadingDays.set(false);
        this.errorMessage.set('No se pudo cargar la disponibilidad.');
      }
    });
  }

  private loadSlots(date: string): void {
    const request = ++this.slotsRequest;
    this.isLoadingSlots.set(true);

    this.scheduleService.getAvailableSlots(this.doctorId(), date).subscribe({
      next: (data) => {
        if (request !== this.slotsRequest) {
          return;
        }
        this.slots.set(data);
        this.isLoadingSlots.set(false);
      },
      error: () => {
        if (request !== this.slotsRequest) {
          return;
        }
        this.isLoadingSlots.set(false);
        this.errorMessage.set('No se pudieron cargar los horarios.');
      }
    });
  }

  private toDayView(day: DayAvailability): DayView {
    const date = new Date(`${day.date}T00:00:00`);
    return {
      iso: day.date,
      dayName: DAY_NAMES[date.getDay()],
      dayNumber: date.getDate(),
      month: MONTH_NAMES[date.getMonth()],
      available: day.availableSlots
    };
  }
}