import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { LoginRequest, LoginResponse } from '../models/auth.model';

const API_BASE_URL = 'http://localhost:8080/api/v1';
const TOKEN_KEY = 'umedical_token';
const ROLE_KEY = 'umedical_role';
const FULLNAME_KEY = 'umedical_fullname';

/**
 * Maneja el login, el logout, y el estado de sesion en memoria (via signal)
 * mas la persistencia en localStorage para que la sesion sobreviva un
 * refresh de pagina. El interceptor (auth.interceptor.ts) y los guards
 * (auth.guard.ts, role.guard.ts) dependen de este servicio.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  // Signal reactivo: cualquier componente puede leer isAuthenticated()
  // y Angular actualiza la vista solo si cambia.
  isAuthenticated = signal<boolean>(!!localStorage.getItem(TOKEN_KEY));

  constructor(private http: HttpClient) {}

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${API_BASE_URL}/auth/login`, request).pipe(
      tap((response) => {
        localStorage.setItem(TOKEN_KEY, response.token);
        localStorage.setItem(ROLE_KEY, response.role);
        localStorage.setItem(FULLNAME_KEY, response.fullName);
        this.isAuthenticated.set(true);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
    localStorage.removeItem(FULLNAME_KEY);
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
}