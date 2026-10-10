export type DocumentType = 'DIAGNOSTICO' | 'RECETA';

export interface DocumentRequest {
  type: DocumentType;
  content: string;
}

export interface DocumentResponse {
  id: number;
  type: DocumentType;
  content: string;
  appointmentId: number;
  fileName?: string;
}