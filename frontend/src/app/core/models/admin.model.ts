export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UserSummary {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  role: string;
  active: boolean;
  createdAt: string;
}

export interface UserSearchParams {
  search?: string;
  role?: string;
  active?: boolean;
  page?: number;
  size?: number;
}

export interface CreateUserRequest {
  role: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  username?: string;
  password?: string;
}

export interface CreatedUser {
  user: UserSummary;
  username: string;
  temporaryPassword: string | null;
}

export interface UpdateUserRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
}

export interface DoctorDetail {
  doctorId: number;
  userId: number;
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  specialtyId: number;
  specialtyName: string;
  licenseNumber: string;
  biography: string | null;
  active: boolean;
}

export interface UpdateDoctorRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  specialtyId: number;
  licenseNumber: string;
  biography: string | null;
}

export interface CountItem {
  label: string;
  count: number;
}

export interface DoctorStat {
  doctorId: number;
  doctorName: string;
  total: number;
  completed: number;
  cancelled: number;
}

export interface DayCount {
  date: string;
  count: number;
}

export interface ReportSummary {
  startDate: string;
  endDate: string;
  totalAppointments: number;
  pending: number;
  completed: number;
  cancelled: number;
  noShow: number;
  unattended: number;
  activeDoctors: number;
  activePatients: number;
  byStatus: CountItem[];
  bySpecialty: CountItem[];
  byDoctor: DoctorStat[];
  byDay: DayCount[];
}