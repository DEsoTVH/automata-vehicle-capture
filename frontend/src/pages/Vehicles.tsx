import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import StatusBadge from '../components/StatusBadge';
import { STATUS_LABEL, fmtDate, type RecordStatus, type VehicleSummary } from '../types';

export default function Vehicles() {
  const [q, setQ] = useState('');
  const [status, setStatus] = useState<RecordStatus | ''>('');
  const [rows, setRows] = useState<VehicleSummary[] | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    const t = setTimeout(() => {
      api.vehicles({ q, status }).then(setRows).catch((e: Error) => setError(e.message));
    }, 250); // espera a que termine de escribir
    return () => clearTimeout(t);
  }, [q, status]);

  return (
    <>
      <h1>Vehículos</h1>
      <div className="row">
        <input className="grow" type="search" placeholder="Buscar VIN, marca o modelo" value={q}
               onChange={(e) => setQ(e.target.value)} />
        <select value={status} onChange={(e) => setStatus(e.target.value as RecordStatus | '')}>
          <option value="">Todos</option>
          {Object.entries(STATUS_LABEL).map(([k, v]) => (
            <option key={k} value={k}>{v}</option>
          ))}
        </select>
      </div>
      {error && <p className="error">{error}</p>}
      {rows && rows.length === 0 && <p className="hint">No hay registros que coincidan.</p>}
      <ul className="cards">
        {rows?.map((v) => (
          <li key={v.id}>
            <Link to={`/vehicles/${v.id}`} className="card" data-status={v.status}>
              <div className="card-main">
                <strong>{[v.brand, v.model].filter(Boolean).join(' ') || 'Sin identificar'}</strong>
                <div className="mono small">{v.vin ?? 'VIN pendiente'}</div>
                <div className="muted small">{fmtDate(v.capturedAt)}</div>
              </div>
              <StatusBadge status={v.status} />
            </Link>
          </li>
        ))}
      </ul>
    </>
  );
}
