export type AppointmentStatus = 'PENDIENTE' | 'COMPLETADA' | 'CANCELADA';

export interface AppointmentResponse {
  id: number;
  doctorId: number;
  doctorName: string;
  specialty: string;
  patientName: string;
  appointmentDate: string;
  appointmentTime: string;
  status: AppointmentStatus;
  reason?: string;
  cancellationReason?: string;
}

export interface AppointmentRequest {
  doctorId: number;
  appointmentDate: string;
  appointmentTime: string;
  reason?: string;
}

export interface RescheduleRequest {
  appointmentDate: string;
  appointmentTime: string;
}