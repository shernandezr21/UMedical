export interface ScheduleBlock {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number;
}

export interface ScheduleRequest extends ScheduleBlock {
  doctorId: number;
}

export interface ScheduleResponse {
  id: number;
  doctorId: number;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number;
  active: boolean;
}

export interface AvailableSlot {
  date: string;
  time: string;
}

export interface DayAvailability {
  date: string;
  availableSlots: number;
}