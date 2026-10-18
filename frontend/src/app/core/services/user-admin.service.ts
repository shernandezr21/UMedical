import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import {
  CreatedUser, CreateUserRequest, PageResponse, UpdateUserRequest, UserSearchParams, UserSummary
} from '../models/admin.model';

@Injectable({ providedIn: 'root' })
export class UserAdminService {
  private http = inject(HttpClient);

  search(params: UserSearchParams): Observable<PageResponse<UserSummary>> {
    let httpParams = new HttpParams().set('page', params.page ?? 0).set('size', params.size ?? 15);
    if (params.search) {
      httpParams = httpParams.set('search', params.search);
    }
    if (params.role) {
      httpParams = httpParams.set('role', params.role);
    }
    if (params.active !== undefined) {
      httpParams = httpParams.set('active', params.active);
    }
    return this.http.get<PageResponse<UserSummary>>(`${API_BASE_URL}/users`, { params: httpParams });
  }

  create(request: CreateUserRequest): Observable<CreatedUser> {
    return this.http.post<CreatedUser>(`${API_BASE_URL}/users`, request);
  }

  update(id: number, request: UpdateUserRequest): Observable<UserSummary> {
    return this.http.put<UserSummary>(`${API_BASE_URL}/users/${id}`, request);
  }

  setActive(id: number, active: boolean): Observable<UserSummary> {
    return this.http.patch<UserSummary>(`${API_BASE_URL}/users/${id}/active`, { active });
  }

  resetPassword(id: number): Observable<{ temporaryPassword: string }> {
    return this.http.post<{ temporaryPassword: string }>(`${API_BASE_URL}/users/${id}/reset-password`, {});
  }
}