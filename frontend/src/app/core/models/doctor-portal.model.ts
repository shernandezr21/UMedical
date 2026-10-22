import { CountItem, DayCount } from './admin.model';
import { AppointmentStatus } from './appointment.model';
import { DocumentResponse } from './document.model';

export interface DoctorPatient {
  patientId: number;
  fullName: string;
  email: string | null;
  phone: string | null;
  totalAppointments: number;
  completedAppointments: number;
  lastCompletedDate: string | null;
}

export interface PatientInfo {
  patientId: number;
  fullName: string;
  email: string | null;
  phone: string | null;
  birthDate: string | null;
  bloodType: string | null;
  address: string | null;
}

export interface Visit {
  appointmentId: number;
  appointmentDate: string;
  appointmentTime: string;
  status: AppointmentStatus;
  reason: string | null;
  documents: DocumentResponse[];
}

export interface PatientRecord {
  patient: PatientInfo;
  visits: Visit[];
}

export interface DoctorStats {
  startDate: string;
  endDate: string;
  totalAppointments: number;
  pending: number;
  completed: number;
  cancelled: number;
  noShow: number;
  unattended: number;
  uniquePatients: number;
  documentsRegistered: number;
  byDay: DayCount[];
  byWeekday: CountItem[];
}

export interface TimeOff {
  id: number;
  startDate: string;
  endDate: string;
  reason: string | null;
  cancelledAppointments: number;
}

export interface TimeOffRequest {
  startDate: string;
  endDate: string;
  reason?: string;
  cancelExisting?: boolean;
}