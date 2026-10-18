import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function toMinutes(time: string): number {
  const [hours, minutes] = time.split(':').map(Number);
  return hours * 60 + minutes;
}

/** Valida UN bloque: fin posterior a inicio, y que el slot quepa dentro del bloque. */
export const timeRangeValidator: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const start = group.get('startTime')?.value;
  const end = group.get('endTime')?.value;
  const slot = group.get('slotDurationMinutes')?.value;

  if (!start || !end) {
    return null;
  }

  const length = toMinutes(end) - toMinutes(start);
  if (length <= 0) {
    return { endBeforeStart: true };
  }
  if (slot && slot > length) {
    return { slotTooLong: true };
  }
  return null;
};