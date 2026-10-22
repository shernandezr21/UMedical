import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import {
  CreatedPatient, CreatePatientRequest, PatientProfile, PatientSummary, UpdatePatientProfileRequest
} from '../models/patient.model';
import { PageResponse } from '../models/admin.model';

@Injectable({ providedIn: 'root' })
export class PatientService {
  private http = inject(HttpClient);

  // ---- Paciente: su propio perfil ----
  getProfile(): Observable<PatientProfile> {
    return this.http.get<PatientProfile>(`${API_BASE_URL}/patients/me`);
  }

  updateProfile(request: UpdatePatientProfileRequest): Observable<PatientProfile> {
    return this.http.put<PatientProfile>(`${API_BASE_URL}/patients/me`, request);
  }

  // ---- Secretaria y admin ----
  search(query: string, page: number, size: number): Observable<PageResponse<PatientSummary>> {
    const params = new HttpParams().set('search', query).set('page', page).set('size', size);
    return this.http.get<PageResponse<PatientSummary>>(`${API_BASE_URL}/patients`, { params });
  }

  getById(id: number): Observable<PatientSummary> {
    return this.http.get<PatientSummary>(`${API_BASE_URL}/patients/${id}`);
  }

  create(request: CreatePatientRequest): Observable<CreatedPatient> {
    return this.http.post<CreatedPatient>(`${API_BASE_URL}/patients`, request);
  }
}