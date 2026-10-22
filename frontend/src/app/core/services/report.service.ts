import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { ReportSummary } from '../models/admin.model';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private http = inject(HttpClient);

  getSummary(startDate: string, endDate: string): Observable<ReportSummary> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate);
    return this.http.get<ReportSummary>(`${API_BASE_URL}/reports/summary`, { params });
  }

  /** Todas las citas del rango en un CSV (se pide como archivo porque la API exige el token en la cabecera). */
  downloadAppointmentsCsv(startDate: string, endDate: string): Observable<Blob> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate);
    return this.http.get(`${API_BASE_URL}/reports/appointments.csv`, { params, responseType: 'blob' });
  }
}
