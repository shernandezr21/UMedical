export interface AuditEntry {
  id: number;
  occurredAt: string;
  actorUsername: string;
  actorRole: string;
  action: string;
  entityType: string | null;
  entityId: number | null;
  patientId: number | null;
  details: string | null;
  ipAddress: string | null;
}

export interface AuditFilters {
  action?: string;
  actor?: string;
  startDate?: string;
  endDate?: string;
}
