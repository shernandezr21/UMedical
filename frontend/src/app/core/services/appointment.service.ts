import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { AppointmentRequest, AppointmentResponse, RescheduleRequest } from '../models/appointment.model';
import { DocumentRequest, DocumentResponse } from '../models/document.model';

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  private http = inject(HttpClient);

  // ---- Paciente ----
  getMyHistory(): Observable<AppointmentResponse[]> {
    return this.http.get<AppointmentResponse[]>(`${API_BASE_URL}/appointments/my-history`);
  }

  createAppointment(request: AppointmentRequest): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>(`${API_BASE_URL}/appointments`, request);
  }

  rescheduleAppointment(id: number, request: RescheduleRequest): Observable<AppointmentResponse> {
    return this.http.put<AppointmentResponse>(`${API_BASE_URL}/appointments/${id}/reschedule`, request);
  }

  cancelAppointment(id: number, reason?: string): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>(`${API_BASE_URL}/appointments/${id}/cancel`, { reason: reason || null });
  }

  // ---- Doctor ----
  getMyAgendaRange(startDate: string, endDate: string): Observable<AppointmentResponse[]> {
    return this.http.get<AppointmentResponse[]>(`${API_BASE_URL}/appointments/my-agenda-range`, {
      params: { startDate, endDate }
    });
  }

  addDiagnosis(appointmentId: number, request: DocumentRequest): Observable<DocumentResponse> {
    return this.http.put<DocumentResponse>(`${API_BASE_URL}/appointments/${appointmentId}/diagnosis`, request);
  }

  uploadDocumentFile(documentId: number, file: File): Observable<void> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<void>(`${API_BASE_URL}/documents/${documentId}/file`, formData);
  }

  // ---- Doctor y paciente ----
  getDocuments(appointmentId: number): Observable<DocumentResponse[]> {
    return this.http.get<DocumentResponse[]>(`${API_BASE_URL}/appointments/${appointmentId}/documents`);
  }

  downloadDocumentFile(documentId: number): Observable<Blob> {
    return this.http.get(`${API_BASE_URL}/documents/${documentId}/file`, { responseType: 'blob' });
  }

  // ---- Secretaria y admin ----
  getClinicAgenda(startDate: string, endDate: string, doctorId?: number): Observable<AppointmentResponse[]> {
    let params = new HttpParams().set('startDate', startDate).set('endDate', endDate);
    if (doctorId != null) {
      params = params.set('doctorId', doctorId);
    }
    return this.http.get<AppointmentResponse[]>(`${API_BASE_URL}/appointments`, { params });
  }

}