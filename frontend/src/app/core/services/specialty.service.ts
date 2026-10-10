import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateSpecialtyRequest, Specialty } from '../models/specialty.model';

const API_BASE_URL = 'http://localhost:8080/api/v1';

@Injectable({ providedIn: 'root' })
export class SpecialtyService {
  private http = inject(HttpClient);

  getSpecialties(): Observable<Specialty[]> {
    return this.http.get<Specialty[]>(`${API_BASE_URL}/specialties`);
  }

  createSpecialty(request: CreateSpecialtyRequest): Observable<Specialty> {
    return this.http.post<Specialty>(`${API_BASE_URL}/specialties`, request);
  }

  updateSpecialty(id: number, request: CreateSpecialtyRequest): Observable<Specialty> {
    return this.http.put<Specialty>(`${API_BASE_URL}/specialties/${id}`, request);
  }
}