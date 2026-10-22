export type AppointmentStatus = 'PENDIENTE' | 'COMPLETADA' | 'CANCELADA' | 'NO_ASISTIO'

export interface AppointmentResponse {
  id: number;
  doctorId: number;
  doctorName: string;
  specialty: string;
  patientId: number;
  patientName: string;
  patientPhone?: string | null;
  appointmentDate: string;
  appointmentTime: string;
  status: AppointmentStatus;
  reason?: string;
  cancellationReason?: string;
}

export interface AppointmentRequest {
  doctorId: number;
  /** Solo lo envia la secretaria o el admin al agendar a nombre de un paciente. */
  patientId?: number;
  appointmentDate: string;
  appointmentTime: string;
  reason?: string;
}

export interface RescheduleRequest {
  appointmentDate: string;
  appointmentTime: string;
}