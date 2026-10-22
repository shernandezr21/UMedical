import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AppointmentService } from '../../../core/services/appointment.service';
import { AppointmentResponse } from '../../../core/models/appointment.model';
import { DocumentResponse } from '../../../core/models/document.model';

const MAX_PDF_BYTES = 10 * 1024 * 1024;

interface WeekDay {
  iso: string;
  label: string;
  isToday: boolean;
}

@Component({
  selector: 'app-my-agenda',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './my-agenda.html',
  styleUrl: './my-agenda.scss'
})
export class MyAgenda {
  private fb = inject(FormBuilder);
  private appointmentService = inject(AppointmentService);

  private readonly DIAS = ['Dom', 'Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab'];

  weekStart = signal<Date>(this.mondayOf(new Date()));
  appointments = signal<AppointmentResponse[]>([]);
  selectedDate = signal<string>(this.toIso(new Date()));

  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  openDiagnosisId = signal<number | null>(null);
  existingDocuments = signal<DocumentResponse[]>([]);
  selectedFile = signal<File | null>(null);
  fileError = signal<string | null>(null);
  isSaving = signal(false);

  pdfInput = viewChild<ElementRef<HTMLInputElement>>('pdfInput');

  diagnosisForm = this.fb.group({
    type: ['DIAGNOSTICO', Validators.required],
    content: ['', Validators.required]
  });

  weekDays = computed<WeekDay[]>(() => {
    const start = this.weekStart();
    const todayIso = this.toIso(new Date());
    return Array.from({ length: 7 }, (_, i) => {
      const d = new Date(start);
      d.setDate(d.getDate() + i);
      const iso = this.toIso(d);
      return {
        iso,
        label: `${this.DIAS[d.getDay()]} ${d.getDate()}`,
        isToday: iso === todayIso
      };
    });
  });

  weekRangeLabel = computed<string>(() => {
    const days = this.weekDays();
    return `${days[0].label} - ${days[6].label}`;
  });

  appointmentsForSelectedDate = computed<AppointmentResponse[]>(() =>
    this.appointments()
      .filter((a) => a.appointmentDate === this.selectedDate())
      .sort((a, b) => a.appointmentTime.localeCompare(b.appointmentTime))
  );

  constructor() {
    this.loadWeek();
  }

  private mondayOf(date: Date): Date {
    const d = new Date(date);
    const day = d.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    d.setDate(d.getDate() + diff);
    d.setHours(0, 0, 0, 0);
    return d;
  }

  // Fecha LOCAL en formato yyyy-MM-dd (toISOString() usa UTC y se desfasa de noche)
  private toIso(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  loadWeek(): void {
    this.isLoading.set(true);
    const days = this.weekDays();
    this.appointmentService.getMyAgendaRange(days[0].iso, days[6].iso).subscribe({
      next: (data) => {
        this.appointments.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('No se pudo cargar la agenda.');
      }
    });
  }

  appointmentsForDay(iso: string): AppointmentResponse[] {
    return this.appointments()
      .filter((a) => a.appointmentDate === iso)
      .sort((a, b) => a.appointmentTime.localeCompare(b.appointmentTime));
  }

  selectDay(iso: string): void {
    this.selectedDate.set(iso);
    this.openDiagnosisId.set(null);
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  previousWeek(): void {
    const d = new Date(this.weekStart());
    d.setDate(d.getDate() - 7);
    this.weekStart.set(d);
    this.loadWeek();
  }

  nextWeek(): void {
    const d = new Date(this.weekStart());
    d.setDate(d.getDate() + 7);
    this.weekStart.set(d);
    this.loadWeek();
  }

  toggleDiagnosis(appointmentId: number): void {
    const willOpen = this.openDiagnosisId() !== appointmentId;
    this.openDiagnosisId.set(willOpen ? appointmentId : null);
    this.resetEntryForm();
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.existingDocuments.set([]);

    if (willOpen) {
      this.reloadDocuments(appointmentId);
    }
  }

  private reloadDocuments(appointmentId: number): void {
    this.appointmentService.getDocuments(appointmentId)
      .subscribe((docs) => this.existingDocuments.set(docs));
  }

  private resetEntryForm(): void {
    this.diagnosisForm.reset({ type: 'DIAGNOSTICO', content: '' });
    this.selectedFile.set(null);
    this.fileError.set(null);
    const input = this.pdfInput();
    if (input) {
      input.nativeElement.value = '';
    }
  }

  private validatePdf(file: File): string | null {
    if (file.type !== 'application/pdf') {
      return 'El archivo debe ser un PDF.';
    }
    if (file.size > MAX_PDF_BYTES) {
      return 'El PDF no puede superar los 10 MB.';
    }
    return null;
  }

  // PDF del formulario de nueva entrada
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.fileError.set(null);

    if (file) {
      const problem = this.validatePdf(file);
      if (problem) {
        this.fileError.set(problem);
        input.value = '';
        this.selectedFile.set(null);
        return;
      }
    }
    this.selectedFile.set(file);
  }

  submitDiagnosis(appointmentId: number): void {
    if (this.diagnosisForm.invalid) {
      this.diagnosisForm.markAllAsTouched();
      return;
    }

    const content = this.diagnosisForm.value.content!.trim();
    if (!content) {
      this.diagnosisForm.controls.content.setErrors({ required: true });
      this.diagnosisForm.controls.content.markAsTouched();
      return;
    }

    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.isSaving.set(true);

    const file = this.selectedFile();

    this.appointmentService.addDiagnosis(appointmentId, {
      type: this.diagnosisForm.value.type as 'DIAGNOSTICO' | 'RECETA',
      content
    }).subscribe({
      next: (created) => {
        if (!file) {
          this.finishSave(appointmentId, 'Documento guardado.');
          return;
        }
        this.appointmentService.uploadDocumentFile(created.id, file).subscribe({
          next: () => this.finishSave(appointmentId, 'Documento y PDF guardados.'),
          error: () => {
            this.finishSave(appointmentId, null);
            this.errorMessage.set(
              'El documento se guardo, pero el PDF no se pudo subir. Puedes adjuntarlo desde el historial.'
            );
          }
        });
      },
      error: (err) => {
        this.isSaving.set(false);
        this.errorMessage.set(err?.error?.message ?? 'No se pudo guardar el documento.');
      }
    });
  }

  private finishSave(appointmentId: number, message: string | null): void {
    this.isSaving.set(false);
    this.successMessage.set(message);
    this.resetEntryForm();
    this.reloadDocuments(appointmentId);
    this.loadWeek();
  }

  // Adjuntar PDF a un documento que todavia no tiene uno
  attachFile(doc: DocumentResponse, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    const problem = this.validatePdf(file);
    if (problem) {
      this.errorMessage.set(problem);
      input.value = '';
      return;
    }

    this.errorMessage.set(null);
    this.appointmentService.uploadDocumentFile(doc.id, file).subscribe({
      next: () => {
        this.successMessage.set('PDF adjuntado.');
        this.reloadDocuments(doc.appointmentId);
      },
      error: (err) => {
        input.value = '';
        this.errorMessage.set(err?.error?.message ?? 'No se pudo subir el PDF.');
      }
    });
  }

  viewFile(documentId: number): void {
    this.appointmentService.downloadDocumentFile(documentId).subscribe((blob) => {
      const url = window.URL.createObjectURL(blob);
      window.open(url, '_blank');
    });
  }

  statusClass(status: string): string {
    return {
      PENDIENTE: 'badge badge-pendiente',
      COMPLETADA: 'badge badge-completada',
      CANCELADA: 'badge badge-cancelada',
      NO_ASISTIO: 'badge badge-noasistio'
    }[status] ?? 'badge';
  }

  isFuture(appt: AppointmentResponse): boolean {
    return appt.appointmentDate > this.toIso(new Date());
  }

  goToToday(): void {
    const today = new Date();
    this.weekStart.set(this.mondayOf(today));
    this.selectedDate.set(this.toIso(today));
    this.openDiagnosisId.set(null);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.loadWeek();
  }

      hasStarted(appt: AppointmentResponse): boolean {
      return new Date(`${appt.appointmentDate}T${appt.appointmentTime}`).getTime() <= Date.now();
    }

    statusText(status: string): string {
      return status === 'NO_ASISTIO' ? 'NO ASISTIO' : status;
    }

    markNoShow(appt: AppointmentResponse): void {
      if (!window.confirm(`¿Marcar que ${appt.patientName} no asistio a su cita?`)) {
        return;
      }
      this.errorMessage.set(null);
      this.successMessage.set(null);
      this.appointmentService.markNoShow(appt.id).subscribe({
        next: () => {
          this.successMessage.set('La cita quedo marcada como no asistio.');
          this.loadWeek();
        },
        error: (err) => this.errorMessage.set(err?.error?.message ?? 'No se pudo marcar la inasistencia.')
      });
    }
  
}

// import { Component, computed, inject, signal } from '@angular/core';
// import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
// import { AppointmentService } from '../../../core/services/appointment.service';
// import { AppointmentResponse } from '../../../core/models/appointment.model';
// import { DocumentResponse } from '../../../core/models/document.model';

// interface WeekDay {
//   iso: string;
//   label: string;
//   isToday: boolean;
// }

// @Component({
//   selector: 'app-my-agenda',
//   standalone: true,
//   imports: [ReactiveFormsModule],
//   templateUrl: './my-agenda.html',
//   styleUrl: './my-agenda.scss'
// })
// export class MyAgenda {
//   private fb = inject(FormBuilder);
//   private appointmentService = inject(AppointmentService);

//   private readonly DIAS = ['Dom', 'Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab'];

//   weekStart = signal<Date>(this.mondayOf(new Date()));
//   appointments = signal<AppointmentResponse[]>([]);
//   selectedDate = signal<string>(this.toIso(new Date()));

//   isLoading = signal(false);
//   errorMessage = signal<string | null>(null);
//   successMessage = signal<string | null>(null);

//   openDiagnosisId = signal<number | null>(null);
//   existingDocuments = signal<DocumentResponse[]>([]);
//   lastCreatedDocumentId = signal<number | null>(null);
//   selectedFile = signal<File | null>(null);
//   isUploading = signal(false);

//   diagnosisForm = this.fb.group({
//     type: ['DIAGNOSTICO', Validators.required],
//     content: ['', Validators.required]
//   });

//   weekDays = computed<WeekDay[]>(() => {
//     const start = this.weekStart();
//     const todayIso = this.toIso(new Date());
//     return Array.from({ length: 7 }, (_, i) => {
//       const d = new Date(start);
//       d.setDate(d.getDate() + i);
//       const iso = this.toIso(d);
//       return {
//         iso,
//         label: `${this.DIAS[d.getDay()]} ${d.getDate()}`,
//         isToday: iso === todayIso
//       };
//     });
//   });

//   weekRangeLabel = computed<string>(() => {
//     const days = this.weekDays();
//     return `${days[0].label} - ${days[6].label}`;
//   });

//   appointmentsForSelectedDate = computed<AppointmentResponse[]>(() =>
//     this.appointments()
//       .filter((a) => a.appointmentDate === this.selectedDate())
//       .sort((a, b) => a.appointmentTime.localeCompare(b.appointmentTime))
//   );

//   constructor() {
//     this.loadWeek();
//   }

//   private mondayOf(date: Date): Date {
//     const d = new Date(date);
//     const day = d.getDay();
//     const diff = day === 0 ? -6 : 1 - day;
//     d.setDate(d.getDate() + diff);
//     d.setHours(0, 0, 0, 0);
//     return d;
//   }

//   private toIso(date: Date): string {
//     return date.toISOString().slice(0, 10);
//   }

//   loadWeek(): void {
//     this.isLoading.set(true);
//     const days = this.weekDays();
//     this.appointmentService.getMyAgendaRange(days[0].iso, days[6].iso).subscribe({
//       next: (data) => {
//         this.appointments.set(data);
//         this.isLoading.set(false);
//       },
//       error: () => {
//         this.isLoading.set(false);
//         this.errorMessage.set('No se pudo cargar la agenda.');
//       }
//     });
//   }

//   appointmentsForDay(iso: string): AppointmentResponse[] {
//     return this.appointments()
//       .filter((a) => a.appointmentDate === iso)
//       .sort((a, b) => a.appointmentTime.localeCompare(b.appointmentTime));
//   }

//   selectDay(iso: string): void {
//     this.selectedDate.set(iso);
//     this.openDiagnosisId.set(null);
//   }

//   previousWeek(): void {
//     const d = new Date(this.weekStart());
//     d.setDate(d.getDate() - 7);
//     this.weekStart.set(d);
//     this.loadWeek();
//   }

//   nextWeek(): void {
//     const d = new Date(this.weekStart());
//     d.setDate(d.getDate() + 7);
//     this.weekStart.set(d);
//     this.loadWeek();
//   }

//   toggleDiagnosis(appointmentId: number): void {
//     const willOpen = this.openDiagnosisId() !== appointmentId;
//     this.openDiagnosisId.set(willOpen ? appointmentId : null);
//     this.diagnosisForm.reset({ type: 'DIAGNOSTICO', content: '' });
//     this.lastCreatedDocumentId.set(null);
//     this.selectedFile.set(null);
//     this.successMessage.set(null);
//     this.existingDocuments.set([]);

//     if (willOpen) {
//       this.appointmentService.getDocuments(appointmentId).subscribe((docs) => this.existingDocuments.set(docs));
//     }
//   }

//   submitDiagnosis(appointmentId: number): void {
//     if (this.diagnosisForm.invalid) {
//       this.diagnosisForm.markAllAsTouched();
//       return;
//     }

//     this.appointmentService.addDiagnosis(appointmentId, {
//       type: this.diagnosisForm.value.type as 'DIAGNOSTICO' | 'RECETA',
//       content: this.diagnosisForm.value.content!
//     }).subscribe({
//       next: (created) => {
//         this.successMessage.set('Documento registrado. Puedes adjuntar un PDF (opcional).');
//         this.lastCreatedDocumentId.set(created.id);
//         this.loadWeek();
//       },
//       error: (err) => {
//         this.errorMessage.set(err?.error?.message ?? 'No se pudo registrar el documento.');
//       }
//     });
//   }

//   onFileSelected(event: Event): void {
//     const input = event.target as HTMLInputElement;
//     this.selectedFile.set(input.files?.[0] ?? null);
//   }

//   uploadFile(): void {
//     const documentId = this.lastCreatedDocumentId();
//     const file = this.selectedFile();
//     if (!documentId || !file) {
//       return;
//     }

//     this.isUploading.set(true);
//     this.appointmentService.uploadDocumentFile(documentId, file).subscribe({
//       next: () => {
//         this.isUploading.set(false);
//         this.successMessage.set('PDF adjuntado correctamente.');
//         this.closeDiagnosisPanel();
//       },
//       error: () => {
//         this.isUploading.set(false);
//         this.errorMessage.set('No se pudo subir el PDF. Verifica que sea un archivo .pdf valido.');
//       }
//     });
//   }

//   closeDiagnosisPanel(): void {
//     this.openDiagnosisId.set(null);
//     this.lastCreatedDocumentId.set(null);
//     this.selectedFile.set(null);
//   }

//   viewFile(documentId: number): void {
//     this.appointmentService.downloadDocumentFile(documentId).subscribe((blob) => {
//       const url = window.URL.createObjectURL(blob);
//       window.open(url, '_blank');
//     });
//   }

//   statusClass(status: string): string {
//     return {
//       PENDIENTE: 'badge badge-pendiente',
//       COMPLETADA: 'badge badge-completada',
//       CANCELADA: 'badge badge-cancelada'
//     }[status] ?? 'badge';
//   }
// }