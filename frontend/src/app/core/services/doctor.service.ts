import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { DoctorDetail, UpdateDoctorRequest } from '../models/admin.model';
import { CreateDoctorRequest, DoctorResponse } from '../models/doctor.model';

@Injectable({ providedIn: 'root' })
export class DoctorService {
  private http = inject(HttpClient);

  /** Listado publico: solo medicos activos. */
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

  // ---- Administracion ----
  getManagementList(): Observable<DoctorDetail[]> {
    return this.http.get<DoctorDetail[]>(`${API_BASE_URL}/doctors/management`);
  }

  updateDoctor(id: number, request: UpdateDoctorRequest): Observable<DoctorDetail> {
    return this.http.put<DoctorDetail>(`${API_BASE_URL}/doctors/${id}`, request);
  }

  setDoctorActive(id: number, active: boolean): Observable<DoctorDetail> {
    return this.http.patch<DoctorDetail>(`${API_BASE_URL}/doctors/${id}/active`, { active });
  }
}