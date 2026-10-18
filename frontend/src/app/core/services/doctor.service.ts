import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { CreateDoctorRequest, DoctorResponse } from '../models/doctor.model';

@Injectable({ providedIn: 'root' })
export class DoctorService {
  private http = inject(HttpClient);

  getDoctors(specialtyId?: number): Observable<DoctorResponse[]> {
    let params = new HttpParams();
    if (specialtyId != null) {
      params = params.set('specialtyId', specialtyId);
    }
    return this.http.get<DoctorResponse[]>(`${API_BASE_URL}/doctors`, { params });
  }

  createDoctor(request: CreateDoctorRequest): Observable<DoctorResponse> {
    return this.http.post<DoctorResponse>(`${API_BASE_URL}/doctors`, request);
  }
}