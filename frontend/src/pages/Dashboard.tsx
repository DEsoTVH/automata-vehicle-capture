import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import type { Stats } from '../types';

export default function Dashboard() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.dashboard().then(setStats).catch((e: Error) => setError(e.message));
  }, []);

  const tiles: [string, number | undefined, string][] = [
    ['Capturas de hoy', stats?.capturedToday, ''],
    ['Pendientes de revisión', stats?.pendingReview, 'warn'],
    ['Validados', stats?.validated, 'ok'],
    ['Exportados', stats?.exported, ''],
    ['Total registrados', stats?.total, ''],
  ];

  return (
    <>
      <h1>Inicio</h1>
      <Link to="/capture" className="btn primary big">
        📷 Capturar ficha
      </Link>
      {error && <p className="error">{error}</p>}
      <div className="tiles">
        {tiles.map(([label, value, tone]) => (
          <div key={label} className={`tile ${tone}`}>
            <div className="tile-value">{value ?? '…'}</div>
            <div className="tile-label">{label}</div>
          </div>
        ))}
      </div>
      <p className="hint">
        Datos de demostración: el OCR es simulado y los campos son ficticios hasta recibir las fichas reales.
      </p>
    </>
  );
}
