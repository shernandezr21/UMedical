import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Preferences } from '@capacitor/preferences';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoginResponse } from './models';

const TOKEN_KEY = 'jwt_token';
const USER_KEY = 'user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private token: string | null = null;
  readonly currentUser = signal<LoginResponse | null>(null);

  private initialized = false;
  private initPromise: Promise<void> | null = null;

  async init(): Promise<void> {
    if (this.initialized) return;
    if (!this.initPromise) {
      this.initPromise = (async () => {
        try {
          const [tokenRes, userRes] = await Promise.all([
            Preferences.get({ key: TOKEN_KEY }),
            Preferences.get({ key: USER_KEY }),
          ]);
          this.token = tokenRes.value ?? null;
          if (userRes.value) {
            try {
              this.currentUser.set(JSON.parse(userRes.value));
            } catch {
              this.currentUser.set(null);
            }
          }
        } catch {
          // Ignore storage read errors on initial load
        } finally {
          this.initialized = true;
        }
      })();
    }
    return this.initPromise;
  }

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/auth/login`, { username, password })
      .pipe(
        tap((res) => {
          if (!res.role.includes('PATIENT')) {
            throw new Error('Esta app es solo para pacientes');
          }
          // Immediately cache in memory so the app can access it synchronously
          this.token = res.token;
          this.currentUser.set(res);
          this.initialized = true;

          // Persist to storage in background
          this.saveSession(res);
        })
      );
  }

  private async saveSession(res: LoginResponse) {
    await Preferences.set({ key: TOKEN_KEY, value: res.token });
    await Preferences.set({ key: USER_KEY, value: JSON.stringify(res) });
  }

  getToken(): string | null {
    return this.token;
  }

  getUser(): LoginResponse | null {
    return this.currentUser();
  }

  isLoggedIn(): boolean {
    return Boolean(this.token);
  }

  async logout() {
    this.token = null;
    this.currentUser.set(null);
    await Preferences.remove({ key: TOKEN_KEY });
    await Preferences.remove({ key: USER_KEY });
    this.router.navigateByUrl('/login', { replaceUrl: true });
  }
}