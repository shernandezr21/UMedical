import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { PageResponse } from '../models/admin.model';
import {
  DoctorPatient, DoctorStats, PatientRecord, TimeOff, TimeOffRequest
} from '../models/doctor-portal.model';
import { ScheduleBlock, ScheduleResponse } from '../models/schedule.model';

/** Todo lo que el medico autenticado hace sobre lo suyo (/doctors/me). */
@Injectable({ providedIn: 'root' })
export class DoctorPortalService {
  private http = inject(HttpClient);
  private base = `${API_BASE_URL}/doctors/me`;

  // ---- Pacientes y expediente
  getMyPatients(search: string, page: number, size: number): Observable<PageResponse<DoctorPatient>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search) {
      params = params.set('search', search);
    }
    return this.http.get<PageResponse<DoctorPatient>>(`${this.base}/patients`, { params });
  }

  getPatientRecord(patientId: number): Observable<PatientRecord> {
    return this.http.get<PatientRecord>(`${this.base}/patients/${patientId}/record`);
  }

  // ---- Estadisticas
  getStats(startDate: string, endDate: string): Observable<DoctorStats> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate);
    return this.http.get<DoctorStats>(`${this.base}/stats`, { params });
  }

  // ---- Mis horarios
  getMySchedules(): Observable<ScheduleResponse[]> {
    return this.http.get<ScheduleResponse[]>(`${this.base}/schedules`);
  }

  createMySchedule(block: ScheduleBlock): Observable<ScheduleResponse> {
    return this.http.post<ScheduleResponse>(`${this.base}/schedules`, block);
  }

  updateMySchedule(id: number, block: ScheduleBlock): Observable<ScheduleResponse> {
    return this.http.put<ScheduleResponse>(`${this.base}/schedules/${id}`, block);
  }

  deleteMySchedule(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/schedules/${id}`);
  }

  // ---- Dias no disponibles
  getMyTimeOff(): Observable<TimeOff[]> {
    return this.http.get<TimeOff[]>(`${this.base}/time-off`);
  }

  createTimeOff(request: TimeOffRequest): Observable<TimeOff> {
    return this.http.post<TimeOff>(`${this.base}/time-off`, request);
  }

  deleteTimeOff(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/time-off/${id}`);
  }
}