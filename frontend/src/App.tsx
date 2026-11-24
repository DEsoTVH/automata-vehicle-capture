import { NavLink, Route, Routes } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import Capture from './pages/Capture';
import Vehicles from './pages/Vehicles';
import VehicleDetailPage from './pages/VehicleDetail';
import Exports from './pages/Exports';

const NAV = [
  { to: '/', label: 'Inicio', icon: '🏠', end: true },
  { to: '/capture', label: 'Capturar', icon: '📷' },
  { to: '/vehicles', label: 'Vehículos', icon: '🚗' },
  { to: '/exports', label: 'Exportar', icon: '📄' },
];

export default function App() {
  return (
    <div className="app">
      <header className="topbar">
        <strong>Automata</strong> <span className="muted-on-dark">Captura de vehículos</span>
      </header>
      <main className="content">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/capture" element={<Capture />} />
          <Route path="/vehicles" element={<Vehicles />} />
          <Route path="/vehicles/:id" element={<VehicleDetailPage />} />
          <Route path="/exports" element={<Exports />} />
          <Route path="*" element={<p>Página no encontrada.</p>} />
        </Routes>
      </main>
      <nav className="tabbar">
        {NAV.map((n) => (
          <NavLink key={n.to} to={n.to} end={n.end} className={({ isActive }) => (isActive ? 'tab active' : 'tab')}>
            <span aria-hidden>{n.icon}</span>
            <span>{n.label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
