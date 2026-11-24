export type RecordStatus =
  | 'PENDING_PROCESSING'
  | 'PROCESSING'
  | 'PROCESSING_FAILED'
  | 'NEEDS_REVIEW'
  | 'PENDING_VALIDATION'
  | 'VALIDATED'
  | 'EXPORTED';

export const STATUS_LABEL: Record<RecordStatus, string> = {
  PENDING_PROCESSING: 'En cola',
  PROCESSING: 'Procesando',
  PROCESSING_FAILED: 'Falló el procesamiento',
  NEEDS_REVIEW: 'Requiere revisión',
  PENDING_VALIDATION: 'Pendiente de validación',
  VALIDATED: 'Validado',
  EXPORTED: 'Exportado',
};

export const isProcessing = (s: RecordStatus) => s === 'PENDING_PROCESSING' || s === 'PROCESSING';

export interface FieldDef {
  key: string;
  label: string;
  required: boolean;
  type: 'text' | 'number';
}

export interface Schema {
  fields: FieldDef[];
  lowConfidenceThreshold: number;
}

export interface VehicleSummary {
  id: string;
  status: RecordStatus;
  vin?: string;
  brand?: string;
  model?: string;
  capturedAt: string;
}

export interface AuditEntry {
  action: string;
  user: string;
  at: string;
  details?: Record<string, string>;
}

export interface VehicleDetail {
  id: string;
  status: RecordStatus;
  data: Record<string, string>;
  confidences: Record<string, number>;
  lowConfidenceFields: string[];
  missingRequiredFields: string[];
  capturedAt: string;
  processedAt?: string;
  validatedBy?: string;
  validatedAt?: string;
  imageUrl: string;
  possibleDuplicates: string[];
  history: AuditEntry[];
  editable: boolean;
}

export interface Stats {
  total: number;
  pendingReview: number;
  validated: number;
  exported: number;
  capturedToday: number;
}

export interface ExportEntry {
  id: string;
  createdAt: string;
  createdBy: string;
  recordCount: number;
  fileName: string;
  downloadUrl: string;
}

export const fmtDate = (iso?: string) => (iso ? new Date(iso).toLocaleString('es-CL') : '—');
