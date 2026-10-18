import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { ChangePasswordRequest, LoginRequest, LoginResponse, RegisterRequest } from '../models/auth.model';

const TOKEN_KEY = 'umedical_token';
const ROLE_KEY = 'umedical_role';
const FULLNAME_KEY = 'umedical_fullname';
const USERID_KEY = 'umedical_userid';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  isAuthenticated = signal<boolean>(!!localStorage.getItem(TOKEN_KEY));

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${API_BASE_URL}/auth/login`, request)
      .pipe(tap((response) => this.saveSession(response)));
  }

  register(request: RegisterRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${API_BASE_URL}/auth/register`, request)
      .pipe(tap((response) => this.saveSession(response)));
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http.put<void>(`${API_BASE_URL}/users/me/password`, request);
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
    localStorage.removeItem(FULLNAME_KEY);
    localStorage.removeItem(USERID_KEY);
    this.isAuthenticated.set(false);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  getRole(): string | null {
    return localStorage.getItem(ROLE_KEY);
  }

  getFullName(): string | null {
    return localStorage.getItem(FULLNAME_KEY);
  }

  getUserId(): number | null {
    const value = localStorage.getItem(USERID_KEY);
    return value ? Number(value) : null;
  }

  setFullName(fullName: string): void {
    localStorage.setItem(FULLNAME_KEY, fullName);
  }

  /** Pantalla a la que se entra despues de iniciar sesion, segun el rol. */
  landingRoute(): string {
    switch (this.getRole()) {
      case 'ROLE_PATIENT': return '/dashboard/mis-citas';
      case 'ROLE_SECRETARY': return '/dashboard/calendario';
      case 'ROLE_ADMIN': return '/dashboard/panel';
      default: return '/dashboard';
    }
  }

  private saveSession(response: LoginResponse): void {
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(ROLE_KEY, response.role);
    localStorage.setItem(FULLNAME_KEY, response.fullName);
    localStorage.setItem(USERID_KEY, String(response.userId));
    this.isAuthenticated.set(true);
  }
}