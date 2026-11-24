import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { fmtDate, type ExportEntry, type VehicleSummary } from '../types';

function download(url: string) {
  const a = document.createElement('a');
  a.href = url;
  a.click();
}

export default function Exports() {
  const [available, setAvailable] = useState<VehicleSummary[]>([]);
  const [history, setHistory] = useState<ExportEntry[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const refresh = useCallback(() => {
    Promise.all([api.vehicles({ status: 'VALIDATED' }), api.exports()])
      .then(([v, h]) => {
        setAvailable(v);
        setHistory(h);
        setSelected(new Set());
      })
      .catch((e: Error) => setError(e.message));
  }, []);

  useEffect(refresh, [refresh]);

  const toggle = (id: string) =>
    setSelected((s) => {
      const n = new Set(s);
      if (n.has(id)) n.delete(id);
      else n.add(id);
      return n;
    });

  async function generate(ids?: string[]) {
    setBusy(true);
    setError('');
    try {
      const exp = await api.createExport(ids);
      download(exp.downloadUrl);
      refresh();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <h1>Exportar</h1>
      <h2>Disponibles para exportar ({available.length})</h2>
      <p className="hint small">Solo aparecen los registros validados que aún no se exportaron.</p>

      {available.length > 0 && (
        <>
          <label className="check">
            <input type="checkbox" checked={selected.size === available.length}
                   onChange={(e) => setSelected(e.target.checked ? new Set(available.map((v) => v.id)) : new Set())} />
            Seleccionar todos
          </label>
          <ul className="cards">
            {available.map((v) => (
              <li key={v.id} className="card">
                <label className="check grow">
                  <input type="checkbox" checked={selected.has(v.id)} onChange={() => toggle(v.id)} />
                  <span>
                    <strong>{[v.brand, v.model].filter(Boolean).join(' ')}</strong>
                    <span className="mono small"> {v.vin}</span>
                  </span>
                </label>
                <Link to={`/vehicles/${v.id}`} className="small">ver</Link>
              </li>
            ))}
          </ul>
          <div className="actions">
            <button className="btn" disabled={busy || selected.size === 0} onClick={() => generate([...selected])}>
              Exportar seleccionados ({selected.size})
            </button>
            <button className="btn primary" disabled={busy} onClick={() => generate()}>
              Exportar todos los validados
            </button>
          </div>
        </>
      )}
      {error && <p className="error">{error}</p>}

      <h2>Historial de exportaciones</h2>
      {history.length === 0 && <p className="hint">Aún no hay exportaciones.</p>}
      <ul className="cards">
        {history.map((h) => (
          <li key={h.id} className="card">
            <div className="card-main">
              <strong>{h.fileName}</strong>
              <div className="muted small">{fmtDate(h.createdAt)} · {h.recordCount} registro(s) · {h.createdBy}</div>
            </div>
            <a className="btn small-btn" href={h.downloadUrl}>Descargar</a>
          </li>
        ))}
      </ul>
    </>
  );
}
