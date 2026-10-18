export interface LoginResponse {
  token: string; tokenType: string; userId: number;
  username: string; role: string; fullName: string;
}

export interface Specialty { id: number; name: string; description: string; }

export interface Doctor {
  id: number; fullName: string; specialty: string;
  licenseNumber: string; biography?: string;
}

export interface AvailableSlot { date: string; time: string; } // "2026-10-15", "08:00:00"

export type AppointmentStatus = 'PENDIENTE' | 'COMPLETADA' | 'CANCELADA';

export interface Appointment {
  id: number;
  doctorId: number;
  doctorName: string;
  specialty: string;
  patientName: string;
  appointmentDate: string;
  appointmentTime: string;
  status: AppointmentStatus;
  reason: string;
  cancellationReason: string | null;
}

export interface AppointmentRequest {
  doctorId: number;
  appointmentDate: string;
  appointmentTime: string;
  reason: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  birthDate?: string;
  address?: string;
  bloodType?: string;
}

export interface RescheduleRequest {
  appointmentDate: string;
  appointmentTime: string;
}

export interface CancelRequest {
  reason?: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  details: string[] | null;
}

export type DocumentType = 'DIAGNOSTICO' | 'RECETA';
export interface DocumentResponse {
  id: number;
  type: DocumentType;
  content: string;
  appointmentId: number;
  fileName: string | null;
}