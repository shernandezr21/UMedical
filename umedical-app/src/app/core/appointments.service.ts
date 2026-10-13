import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { AppointmentRequest, Appointment, AvailableSlot, Doctor, Specialty } from './models';

@Injectable({ providedIn: 'root' })
export class AppointmentsService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getSpecialties() {
    return this.http.get<Specialty[]>(`${this.base}/specialties`);
  }
  getDoctors(specialtyId?: number) {
    const params: Record<string, number> = {};
    if (specialtyId) params['specialtyId'] = specialtyId;
    return this.http.get<Doctor[]>(`${this.base}/doctors`, { params });
  }
  getAvailableSlots(doctorId: number, date: string) {
    return this.http.get<AvailableSlot[]>(`${this.base}/schedules/available`, { params: { doctorId, date } });
  }
  bookAppointment(req: AppointmentRequest) {
    return this.http.post<Appointment>(`${this.base}/appointments`, req);
  }
  getMyHistory() {
    return this.http.get<Appointment[]>(`${this.base}/appointments/my-history`);
  }
}