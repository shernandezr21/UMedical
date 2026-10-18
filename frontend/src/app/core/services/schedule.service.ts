import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { AvailableSlot, DayAvailability, ScheduleRequest, ScheduleResponse } from '../models/schedule.model';

@Injectable({ providedIn: 'root' })
export class ScheduleService {
  private http = inject(HttpClient);

  createSchedule(request: ScheduleRequest): Observable<ScheduleResponse> {
    return this.http.post<ScheduleResponse>(`${API_BASE_URL}/schedules`, request);
  }

  getAvailableSlots(doctorId: number, date: string): Observable<AvailableSlot[]> {
    const params = new HttpParams().set('doctorId', doctorId).set('date', date);
    return this.http.get<AvailableSlot[]>(`${API_BASE_URL}/schedules/available`, { params });
  }

  getAvailableRange(doctorId: number, startDate: string, endDate: string): Observable<DayAvailability[]> {
    const params = new HttpParams().set('doctorId', doctorId).set('startDate', startDate).set('endDate', endDate);
    return this.http.get<DayAvailability[]>(`${API_BASE_URL}/schedules/available/range`, { params });
  }
}