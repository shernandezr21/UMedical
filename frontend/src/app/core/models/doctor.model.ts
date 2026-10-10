import { Specialty } from './specialty.model';
import { ScheduleBlock } from './schedule.model';

export interface DoctorResponse {
  id: number;
  fullName: string;
  specialty: string;
  licenseNumber: string;
  biography?: string;
}

export interface CreateDoctorRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  specialtyId: number;
  licenseNumber: string;
  biography?: string;
  schedules: ScheduleBlock[];
}