import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { controlErrorText } from '../../../core/form-errors';
import { CreateUserRequest, PageResponse, UserSummary } from '../../../core/models/admin.model';
import { AuthService } from '../../../core/services/auth.service';
import { UserAdminService } from '../../../core/services/user-admin.service';

const ROLE_LABELS: Record<string, string> = {
  ROLE_ADMIN: 'Administrador',
  ROLE_DOCTOR: 'Medico',
  ROLE_SECRETARY: 'Secretaria',
  ROLE_PATIENT: 'Paciente'
};

@Component({
  selector: 'app-users-admin',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './users.html'
})
export class UsersAdmin {
  private fb = inject(FormBuilder);
  private userService = inject(UserAdminService);
  private authService = inject(AuthService);
  private search$ = new Subject<string>();
  private searchText = '';

  readonly pageSize = 15;
  readonly currentUserId = this.authService.getUserId();
  readonly filterRoles = Object.entries(ROLE_LABELS).map(([value, label]) => ({ value, label }));
  readonly creatableRoles = this.filterRoles.filter((r) => r.value !== 'ROLE_DOCTOR');

  result = signal<PageResponse<UserSummary> | null>(null);
  page = signal(0);
  roleFilter = signal('');
  activeFilter = signal('');
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  showForm = signal(false);
  isSaving = signal(false);
  formError = signal<string | null>(null);
  credentials = signal<{ title: string; username: string; password: string } | null>(null);
  copied = signal(false);

  editingId = signal<number | null>(null);
  editError = signal<string | null>(null);

  createForm = this.fb.group({
    role: ['ROLE_SECRETARY', Validators.required],
    firstName: ['', [Validators.required, Validators.maxLength(80)]],
    lastName: ['', [Validators.required, Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    phone: ['', Validators.maxLength(20)],
    username: ['', Validators.pattern(/^$|^[A-Za-z0-9._-]{4,50}$/)],
    password: ['', Validators.pattern(/^$|^.{8,72}$/)]
  });

  editForm = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(80)]],
    lastName: ['', [Validators.required, Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    phone: ['', Validators.maxLength(20)]
  });

  constructor() {
    this.search$.pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed()).subscribe((text) => {
      this.searchText = text;
      this.page.set(0);
      this.load();
    });
    this.load();
  }

  roleLabel(role: string): string {
    return ROLE_LABELS[role] ?? role;
  }

  createError(name: string): string | null {
    return controlErrorText(this.createForm, name);
  }

  editErrorText(name: string): string | null {
    return controlErrorText(this.editForm, name);
  }

  // ---------------------------------------------------------------- listado

  load(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const active = this.activeFilter();
    this.userService.search({
      search: this.searchText,
      role: this.roleFilter() || undefined,
      active: active === '' ? undefined : active === 'true',
      page: this.page(),
      size: this.pageSize
    }).subscribe({
      next: (result) => {
        this.result.set(result);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('No se pudo cargar la lista de usuarios.');
      }
    });
  }

  onSearchInput(event: Event): void {
    this.search$.next((event.target as HTMLInputElement).value);
  }

  onRoleFilter(event: Event): void {
    this.roleFilter.set((event.target as HTMLSelectElement).value);
    this.page.set(0);
    this.load();
  }

  onActiveFilter(event: Event): void {
    this.activeFilter.set((event.target as HTMLSelectElement).value);
    this.page.set(0);
    this.load();
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.page.update((p) => p - 1);
      this.load();
    }
  }

  nextPage(): void {
    const totalPages = this.result()?.totalPages ?? 0;
    if (this.page() + 1 < totalPages) {
      this.page.update((p) => p + 1);
      this.load();
    }
  }

  // ---------------------------------------------------------------- crear

  toggleForm(): void {
    this.showForm.update((open) => !open);
    this.formError.set(null);
  }

  createUser(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    const raw = this.createForm.getRawValue();
    const request: CreateUserRequest = {
      role: raw.role!,
      firstName: raw.firstName!.trim(),
      lastName: raw.lastName!.trim(),
      email: raw.email!.trim(),
      phone: raw.phone?.trim() || undefined,
      username: raw.username?.trim() || undefined,
      password: raw.password || undefined
    };

    this.formError.set(null);
    this.successMessage.set(null);
    this.credentials.set(null);
    this.isSaving.set(true);

    this.userService.create(request).subscribe({
      next: (created) => {
        this.isSaving.set(false);
        this.showForm.set(false);
        this.createForm.reset({ role: 'ROLE_SECRETARY' });
        if (created.temporaryPassword) {
          this.copied.set(false);
          this.credentials.set({
            title: `Usuario creado: ${created.user.firstName} ${created.user.lastName}`,
            username: created.username,
            password: created.temporaryPassword
          });
        } else {
          this.successMessage.set(`Usuario creado: ${created.username}.`);
        }
        this.load();
      },
      error: (err) => {
        this.isSaving.set(false);
        const details: string[] | undefined = err?.error?.details;
        this.formError.set(
          details?.length
            ? `${err.error.message}: ${details.join(' | ')}`
            : err?.error?.message ?? 'No se pudo crear el usuario.'
        );
      }
    });
  }

  // ---------------------------------------------------------------- editar

  startEdit(user: UserSummary): void {
    this.editingId.set(user.id);
    this.editError.set(null);
    this.editForm.reset({
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      phone: user.phone ?? ''
    });
  }

  cancelEdit(): void {
    this.editingId.set(null);
  }

  saveEdit(user: UserSummary): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    const raw = this.editForm.getRawValue();
    this.editError.set(null);

    this.userService.update(user.id, {
      firstName: raw.firstName!.trim(),
      lastName: raw.lastName!.trim(),
      email: raw.email!.trim(),
      phone: raw.phone?.trim() || null
    }).subscribe({
      next: () => {
        this.editingId.set(null);
        this.successMessage.set('Usuario actualizado.');
        this.load();
      },
      error: (err) => this.editError.set(err?.error?.message ?? 'No se pudo actualizar el usuario.')
    });
  }

  // ---------------------------------------------------------------- acciones

  toggleActive(user: UserSummary): void {
    const action = user.active ? 'desactivar' : 'activar';
    if (!window.confirm(`¿Seguro que quieres ${action} a ${user.firstName} ${user.lastName}?`)) {
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.userService.setActive(user.id, !user.active).subscribe({
      next: () => {
        this.successMessage.set(user.active ? 'La cuenta fue desactivada.' : 'La cuenta fue activada.');
        this.load();
      },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'No se pudo cambiar el estado de la cuenta.')
    });
  }

  resetPassword(user: UserSummary): void {
    if (!window.confirm(`Se generara una contrasena temporal nueva para ${user.firstName} ${user.lastName}. ¿Continuar?`)) {
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.userService.resetPassword(user.id).subscribe({
      next: (result) => {
        this.copied.set(false);
        this.credentials.set({
          title: `Contrasena restablecida: ${user.firstName} ${user.lastName}`,
          username: user.username,
          password: result.temporaryPassword
        });
      },
      error: (err) => this.errorMessage.set(err?.error?.message ?? 'No se pudo restablecer la contrasena.')
    });
  }

  copyCredentials(): void {
    const c = this.credentials();
    if (!c) {
      return;
    }
    navigator.clipboard
      .writeText(`Usuario: ${c.username}\nContrasena temporal: ${c.password}`)
      .then(() => this.copied.set(true));
  }

  dismissCredentials(): void {
    this.credentials.set(null);
  }
}