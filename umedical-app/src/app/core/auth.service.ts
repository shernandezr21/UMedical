import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Preferences } from '@capacitor/preferences';
import { from, map, switchMap, throwError, Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoginResponse } from './models';

const TOKEN_KEY = 'jwt_token';
const USER_KEY = 'user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/auth/login`, { username, password })
      .pipe(
        switchMap((res) => {
          if (!res.role.includes('PATIENT')) {
            return throwError(() => new Error('Esta app es solo para pacientes'));
          }
          return from(this.saveSession(res)).pipe(map(() => res));
        })
      );
  }

  private async saveSession(res: LoginResponse) {
    await Preferences.set({ key: TOKEN_KEY, value: res.token });
    await Preferences.set({ key: USER_KEY, value: JSON.stringify(res) });
  }

  async getToken() { return (await Preferences.get({ key: TOKEN_KEY })).value; }
  async getUser(): Promise<LoginResponse | null> {
    const { value } = await Preferences.get({ key: USER_KEY });
    return value ? JSON.parse(value) : null;
  }
  async logout() {
    await Preferences.remove({ key: TOKEN_KEY });
    await Preferences.remove({ key: USER_KEY });
    this.router.navigateByUrl('/login', { replaceUrl: true });
  }
}