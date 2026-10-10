export interface ScheduleRequest {
  doctorId: number;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number;
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

export interface ScheduleBlock {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number;
}