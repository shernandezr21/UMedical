import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppointmentResponse } from '../models/appointment.model';
import { DocumentRequest, DocumentResponse } from '../models/document.model';

const API_BASE_URL = 'http://localhost:8080/api/v1';

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  private http = inject(HttpClient);

  getMyAgendaRange(startDate: string, endDate: string): Observable<AppointmentResponse[]> {
    return this.http.get<AppointmentResponse[]>(`${API_BASE_URL}/appointments/my-agenda-range`, {
      params: { startDate, endDate }
    });
  }

  getDocuments(appointmentId: number): Observable<DocumentResponse[]> {
    return this.http.get<DocumentResponse[]>(`${API_BASE_URL}/appointments/${appointmentId}/documents`);
  }

  addDiagnosis(appointmentId: number, request: DocumentRequest): Observable<DocumentResponse> {
    return this.http.put<DocumentResponse>(`${API_BASE_URL}/appointments/${appointmentId}/diagnosis`, request);
  }

  uploadDocumentFile(documentId: number, file: File): Observable<void> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<void>(`${API_BASE_URL}/documents/${documentId}/file`, formData);
  }

  downloadDocumentFile(documentId: number): Observable<Blob> {
    return this.http.get(`${API_BASE_URL}/documents/${documentId}/file`, { responseType: 'blob' });
  }
}