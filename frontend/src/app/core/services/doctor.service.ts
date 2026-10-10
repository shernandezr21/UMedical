import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateDoctorRequest, DoctorResponse } from '../models/doctor.model';

const API_BASE_URL = 'http://localhost:8080/api/v1';

@Injectable({ providedIn: 'root' })
export class DoctorService {
  private http = inject(HttpClient);

  createDoctor(request: CreateDoctorRequest): Observable<DoctorResponse> {
    return this.http.post<DoctorResponse>(`${API_BASE_URL}/doctors`, request);
  }
}