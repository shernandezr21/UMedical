export interface PatientProfile {
  patientId: number;
  userId: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  birthDate: string | null;
  address: string | null;
  bloodType: string | null;
}

export interface UpdatePatientProfileRequest {
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  birthDate: string | null;
  address: string | null;
  bloodType: string | null;
}

export interface PatientSummary {
  patientId: number;
  userId: number;
  username: string;
  fullName: string;
  email: string;
  phone: string | null;
  birthDate: string | null;
  bloodType: string | null;
}

export interface CreatePatientRequest {
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  birthDate: string | null;
  address: string | null;
  bloodType: string | null;
}

export interface CreatedPatient {
  patient: PatientSummary;
  username: string;
  temporaryPassword: string;
}