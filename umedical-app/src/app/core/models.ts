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
  id: number; doctorName: string; specialty: string; patientName: string;
  appointmentDate: string; appointmentTime: string; status: AppointmentStatus; reason: string;
}
export interface AppointmentRequest {
  doctorId: number; appointmentDate: string; appointmentTime: string; reason: string;
}