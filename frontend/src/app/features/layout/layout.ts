import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-layout',
  standalone: true,
    imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './layout.html',
  styleUrl: './layout.scss'
})
export class Layout {
  authService = inject(AuthService);
  private router = inject(Router);

  get role(): string | null {
    return this.authService.getRole();
  }

  get fullName(): string | null {
    return this.authService.getFullName();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}