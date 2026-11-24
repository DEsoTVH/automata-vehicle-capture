import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { prepareImage } from '../image';
import { IconArrow, IconCamera, IconClose, IconImage } from '../components/Icons';
import StatusBadge from '../components/StatusBadge';
import { isProcessing, type RecordStatus } from '../types';

type Phase = 'ready' | 'uploading' | 'processing' | 'done' | 'error';

interface Item {
  key: string;
  file: File;
  preview: string;
  phase: Phase;
  recordId?: string;
  status?: RecordStatus;
  duplicate?: boolean;
  error?: string;
}

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

export default function Capture() {
  const [items, setItems] = useState<Item[]>([]);
  const [busy, setBusy] = useState(false);
  const itemsRef = useRef<Item[]>([]);
  itemsRef.current = items;

  // Libera las URLs de previsualización al salir de la pantalla.
  useEffect(() => () => itemsRef.current.forEach((i) => URL.revokeObjectURL(i.preview)), []);

  const patch = (key: string, p: Partial<Item>) =>
    setItems((prev) => prev.map((i) => (i.key === key ? { ...i, ...p } : i)));

  function addFiles(list: FileList | null) {
    if (!list || list.length === 0) return;
    const added: Item[] = Array.from(list).map((file) => ({
      key: crypto.randomUUID(),
      file,
      preview: URL.createObjectURL(file),
      phase: 'ready',
    }));
    setItems((prev) => [...prev, ...added]);
  }

  function remove(key: string) {
    const it = items.find((i) => i.key === key);
    if (it) URL.revokeObjectURL(it.preview);
    setItems((prev) => prev.filter((i) => i.key !== key));
  }

  async function watch(key: string, recordId: string) {
    // Consulta el estado hasta que termine el procesamiento.
    for (let n = 0; n < 120; n++) {
      try {
        const d = await api.vehicle(recordId);
        patch(key, { status: d.status });
        if (!isProcessing(d.status)) {
          patch(key, { phase: 'done' });
          return;
        }
      } catch {
        /* reintenta */
      }
      await sleep(1000);
    }
    patch(key, { phase: 'error', error: 'El procesamiento tarda más de lo esperado. Revísalo en Vehículos.' });
  }

  async function processAll() {
    setBusy(true);
    for (const it of itemsRef.current.filter((i) => i.phase === 'ready' || i.phase === 'error')) {
      patch(it.key, { phase: 'uploading', error: undefined });
      try {
        const { blob, name } = await prepareImage(it.file);
        const res = await api.capture(blob, name);
        patch(it.key, { phase: 'processing', recordId: res.recordId, duplicate: res.duplicate });
        void watch(it.key, res.recordId); // el procesamiento sigue en segundo plano mientras se suben las demás
      } catch (e) {
        patch(it.key, { phase: 'error', error: (e as Error).message });
      }
    }
    setBusy(false);
  }

  const pending = items.filter((i) => i.phase === 'ready' || i.phase === 'error').length;

  return (
    <>
      <h1>Capturar fichas</h1>
      <div className="row">
        <label className="btn primary big grow">
          <IconCamera /> Tomar foto
          <input type="file" accept="image/*" capture="environment" hidden
                 onChange={(e) => { addFiles(e.target.files); e.target.value = ''; }} />
        </label>
        <label className="btn big grow">
          <IconImage /> Galería
          <input type="file" accept="image/*" multiple hidden
                 onChange={(e) => { addFiles(e.target.files); e.target.value = ''; }} />
        </label>
      </div>

      {items.length === 0 && <p className="hint">Toma una foto de la ficha o elige imágenes de la galería. Puedes agregar varias.</p>}

      <ul className="capture-list">
        {items.map((it) => (
          <li key={it.key} className="capture-item">
            <img src={it.preview} alt="Vista previa de la ficha" />
            <div className="capture-info">
              <div className="small">{it.file.name}</div>
              {it.phase === 'ready' && <span className="muted">Lista para procesar</span>}
              {it.phase === 'uploading' && <span className="muted"><span className="spinner" /> Subiendo…</span>}
              {(it.phase === 'processing' || it.phase === 'done') && it.status && <StatusBadge status={it.status} />}
              {it.phase === 'processing' && !it.status && <span className="muted"><span className="spinner" /> Procesando…</span>}
              {it.duplicate && <div className="warn-text">Esta foto ya estaba cargada.</div>}
              {it.phase === 'error' && <div className="error">{it.error}</div>}
              {it.recordId && it.phase === 'done' && (
                <Link to={`/vehicles/${it.recordId}`} className="btn small-btn">Revisar ficha <IconArrow size={16} /></Link>
              )}
            </div>
            {(it.phase === 'ready' || it.phase === 'error') && (
              <button className="icon-btn" aria-label="Quitar" onClick={() => remove(it.key)}><IconClose size={20} /></button>
            )}
          </li>
        ))}
      </ul>

      {pending > 0 && (
        <button className="btn primary big sticky" disabled={busy} onClick={processAll}>
          {busy ? 'Procesando…' : `Procesar ${pending} ficha${pending > 1 ? 's' : ''}`}
        </button>
      )}
    </>
  );
}
