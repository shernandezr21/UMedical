import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { PatientProfile, UpdatePatientProfileRequest } from '../models/patient.model';

@Injectable({ providedIn: 'root' })
export class PatientService {
  private http = inject(HttpClient);

  getProfile(): Observable<PatientProfile> {
    return this.http.get<PatientProfile>(`${API_BASE_URL}/patients/me`);
  }

  updateProfile(request: UpdatePatientProfileRequest): Observable<PatientProfile> {
    return this.http.put<PatientProfile>(`${API_BASE_URL}/patients/me`, request);
  }
}