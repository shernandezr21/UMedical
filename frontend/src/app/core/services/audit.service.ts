import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { PageResponse } from '../models/admin.model';
import { AuditEntry, AuditFilters } from '../models/audit.model';

@Injectable({ providedIn: 'root' })
export class AuditService {
  private http = inject(HttpClient);

  search(filters: AuditFilters, page: number, size: number): Observable<PageResponse<AuditEntry>> {
    const params = this.toParams(filters).set('page', page).set('size', size);
    return this.http.get<PageResponse<AuditEntry>>(`${API_BASE_URL}/audit`, { params });
  }

  downloadCsv(filters: AuditFilters): Observable<Blob> {
    return this.http.get(`${API_BASE_URL}/audit/export.csv`, { params: this.toParams(filters), responseType: 'blob' });
  }

  private toParams(filters: AuditFilters): HttpParams {
    let params = new HttpParams();
    if (filters.action) {
      params = params.set('action', filters.action);
    }
    if (filters.actor) {
      params = params.set('actor', filters.actor);
    }
    if (filters.startDate) {
      params = params.set('startDate', filters.startDate);
    }
    if (filters.endDate) {
      params = params.set('endDate', filters.endDate);
    }
    return params;
  }
}
