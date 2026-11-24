import type { ExportEntry, RecordStatus, Schema, Stats, VehicleDetail, VehicleSummary } from './types';

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, init);
  if (!res.ok) {
    let message = `Error ${res.status}`;
    try {
      const problem = await res.json();
      if (problem?.detail) message = problem.detail;
    } catch {
      /* respuesta sin cuerpo JSON */
    }
    throw new Error(message);
  }
  return res.json() as Promise<T>;
}

const json = (method: string, body?: unknown): RequestInit => ({
  method,
  headers: { 'Content-Type': 'application/json' },
  body: body === undefined ? undefined : JSON.stringify(body),
});

export const api = {
  schema: () => request<Schema>('/api/schema'),
  dashboard: () => request<Stats>('/api/dashboard'),

  vehicles: (params: { status?: RecordStatus | ''; q?: string } = {}) => {
    const qs = new URLSearchParams();
    if (params.status) qs.set('status', params.status);
    if (params.q) qs.set('q', params.q);
    return request<VehicleSummary[]>(`/api/vehicles?${qs}`);
  },
  vehicle: (id: string) => request<VehicleDetail>(`/api/vehicles/${id}`),
  updateVehicle: (id: string, data: Record<string, string>) =>
    request<VehicleDetail>(`/api/vehicles/${id}`, json('PUT', { data })),
  validate: (id: string) => request<VehicleDetail>(`/api/vehicles/${id}/validate`, json('POST')),
  retry: (id: string) => request<VehicleDetail>(`/api/vehicles/${id}/retry`, json('POST')),

  capture: (file: Blob, filename: string) => {
    const form = new FormData();
    form.append('file', file, filename);
    return request<{ recordId: string; duplicate: boolean }>('/api/captures', { method: 'POST', body: form });
  },

  exports: () => request<ExportEntry[]>('/api/exports'),
  createExport: (recordIds?: string[]) => request<ExportEntry>('/api/exports', json('POST', { recordIds })),
};
