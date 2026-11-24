import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import StatusBadge from '../components/StatusBadge';
import { fmtDate, isProcessing, type Schema, type VehicleDetail } from '../types';

const ACTION_LABEL: Record<string, string> = {
  CAPTURED: 'Foto capturada',
  PROCESSED: 'Procesada por OCR',
  PROCESSING_FAILED: 'Falló el procesamiento',
  RETRY_REQUESTED: 'Reintento solicitado',
  EDITED: 'Datos editados',
  VALIDATED: 'Validada',
  EXPORTED: 'Exportada',
};

export default function VehicleDetailPage() {
  const { id = '' } = useParams();
  const [schema, setSchema] = useState<Schema | null>(null);
  const [detail, setDetail] = useState<VehicleDetail | null>(null);
  const [values, setValues] = useState<Record<string, string>>({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    const d = await api.vehicle(id);
    setDetail(d);
    return d;
  }, [id]);

  // Esquema + registro inicial
  useEffect(() => {
    api.schema().then(setSchema).catch((e: Error) => setError(e.message));
    load().then((d) => setValues(d.data)).catch((e: Error) => setError(e.message));
  }, [id, load]);

  // Mientras se procesa, consulta cada 1,5 s y carga los datos al terminar.
  useEffect(() => {
    if (!detail || !isProcessing(detail.status)) return;
    const t = setInterval(() => {
      load().then((d) => { if (!isProcessing(d.status)) setValues(d.data); }).catch(() => {});
    }, 1500);
    return () => clearInterval(t);
  }, [detail, load]);

  if (error && !detail) return <p className="error">{error}</p>;
  if (!detail || !schema) return <p className="hint">Cargando…</p>;

  const dirty = schema.fields.some((f) => (values[f.key] ?? '') !== (detail.data[f.key] ?? ''));
  const editable = detail.editable;

  async function run(action: () => Promise<VehicleDetail>) {
    setBusy(true);
    setError('');
    try {
      const d = await action();
      setDetail(d);
      setValues(d.data);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }

  const save = () => run(() => api.updateVehicle(id, values));
  const validate = () =>
    run(async () => {
      if (dirty) await api.updateVehicle(id, values);
      return api.validate(id);
    });

  return (
    <>
      <p><Link to="/vehicles">← Vehículos</Link></p>
      <div className="row between">
        <h1>Ficha del vehículo</h1>
        <StatusBadge status={detail.status} />
      </div>

      <a href={detail.imageUrl} target="_blank" rel="noreferrer">
        <img className="photo" src={detail.imageUrl} alt="Fotografía original de la ficha" />
      </a>
      <p className="hint small">Foto original (toca para ampliar). Capturada: {fmtDate(detail.capturedAt)}</p>

      {isProcessing(detail.status) && <p className="info"><span className="spinner" /> Leyendo la ficha…</p>}
      {detail.status === 'PROCESSING_FAILED' && (
        <div className="error-box">
          No se pudo leer la ficha.
          <button className="btn small-btn" disabled={busy} onClick={() => run(() => api.retry(id))}>Reintentar</button>
        </div>
      )}
      {detail.possibleDuplicates.length > 0 && (
        <div className="warn-box">
          ⚠️ Posible duplicado: ya existe otro registro con este VIN.{' '}
          {detail.possibleDuplicates.map((d) => (
            <Link key={d} to={`/vehicles/${d}`}>ver</Link>
          ))}
        </div>
      )}
      {(detail.status === 'NEEDS_REVIEW' || detail.status === 'PENDING_VALIDATION') && (
        <p className="hint small">
          Los datos los leyó la IA y aún no son definitivos. Revísalos contra la foto y confirma.
        </p>
      )}

      <form className="form" onSubmit={(e) => e.preventDefault()}>
        {schema.fields.map((f) => {
          const conf = detail.confidences[f.key];
          const low = detail.lowConfidenceFields.includes(f.key);
          const missing = detail.missingRequiredFields.includes(f.key) && (values[f.key] ?? '') === '';
          return (
            <label key={f.key} className={`field ${low ? 'low' : ''} ${missing ? 'missing' : ''}`}>
              <span className="field-label">
                {f.label}{f.required && ' *'}
                {low && <em> · revisar ({Math.round(conf * 100)}%)</em>}
                {missing && <em> · falta</em>}
              </span>
              <input
                value={values[f.key] ?? ''}
                disabled={!editable || busy}
                inputMode={f.type === 'number' ? 'numeric' : 'text'}
                autoCapitalize={f.key === 'vin' ? 'characters' : 'sentences'}
                onChange={(e) => setValues((v) => ({ ...v, [f.key]: e.target.value }))}
              />
            </label>
          );
        })}
      </form>

      {error && <p className="error">{error}</p>}

      {editable && (
        <div className="actions">
          <button className="btn" disabled={!dirty || busy} onClick={save}>Guardar cambios</button>
          {detail.status !== 'VALIDATED' && (
            <button className="btn primary" disabled={busy} onClick={validate}>
              {dirty ? 'Guardar y validar' : 'Confirmar y validar'}
            </button>
          )}
        </div>
      )}
      {detail.status === 'VALIDATED' && (
        <p className="hint small">Validado por {detail.validatedBy} · {fmtDate(detail.validatedAt)}. Editar lo devuelve a validación.</p>
      )}
      {detail.status === 'EXPORTED' && <p className="hint small">Registro exportado: ya no se puede modificar.</p>}

      <h2>Historial</h2>
      <ul className="history">
        {detail.history.map((h, i) => (
          <li key={i}>
            <div><strong>{ACTION_LABEL[h.action] ?? h.action}</strong> <span className="muted small">· {h.user} · {fmtDate(h.at)}</span></div>
            {h.action === 'EDITED' && h.details && (
              <ul className="changes small">
                {Object.entries(h.details).map(([k, v]) => (
                  <li key={k}>{schema.fields.find((f) => f.key === k)?.label ?? k}: {v}</li>
                ))}
              </ul>
            )}
          </li>
        ))}
      </ul>
    </>
  );
}
