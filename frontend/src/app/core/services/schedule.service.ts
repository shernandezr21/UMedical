import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ScheduleRequest, ScheduleResponse } from '../models/schedule.model';

const API_BASE_URL = 'http://localhost:8080/api/v1';

@Injectable({ providedIn: 'root' })
export class ScheduleService {
  private http = inject(HttpClient);

  createSchedule(request: ScheduleRequest): Observable<ScheduleResponse> {
    return this.http.post<ScheduleResponse>(`${API_BASE_URL}/schedules`, request);
  }
}