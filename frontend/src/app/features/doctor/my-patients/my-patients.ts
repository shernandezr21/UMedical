import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { PageResponse } from '../../../core/models/admin.model';
import { DoctorPatient } from '../../../core/models/doctor-portal.model';
import { DoctorPortalService } from '../../../core/services/doctor-portal.service';

@Component({
  selector: 'app-my-patients',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './my-patients.html'
})
export class MyPatients {
  private portal = inject(DoctorPortalService);
  private search$ = new Subject<string>();
  private searchText = '';

  readonly pageSize = 12;

  result = signal<PageResponse<DoctorPatient> | null>(null);
  page = signal(0);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  constructor() {
    this.search$.pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed()).subscribe((text) => {
      this.searchText = text;
      this.page.set(0);
      this.load();
    });
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.portal.getMyPatients(this.searchText, this.page(), this.pageSize).subscribe({
      next: (result) => {
        this.result.set(result);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('No se pudo cargar la lista de pacientes.');
      }
    });
  }

  onSearchInput(event: Event): void {
    this.search$.next((event.target as HTMLInputElement).value);
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.page.update((p) => p - 1);
      this.load();
    }
  }

  nextPage(): void {
    if (this.page() + 1 < (this.result()?.totalPages ?? 0)) {
      this.page.update((p) => p + 1);
      this.load();
    }
  }

  initials(name: string): string {
    return name.split(' ').filter(Boolean).slice(0, 2).map((part) => part[0].toUpperCase()).join('');
  }
}