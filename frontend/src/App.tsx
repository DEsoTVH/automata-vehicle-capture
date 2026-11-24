import { NavLink, Route, Routes } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import Capture from './pages/Capture';
import Vehicles from './pages/Vehicles';
import VehicleDetailPage from './pages/VehicleDetail';
import Exports from './pages/Exports';
import { IconCamera, IconCar, IconExport, IconHome } from './components/Icons';

const NAV = [
  { to: '/', label: 'Inicio', icon: <IconHome />, end: true },
  { to: '/capture', label: 'Capturar', icon: <IconCamera /> },
  { to: '/vehicles', label: 'Vehículos', icon: <IconCar /> },
  { to: '/exports', label: 'Exportar', icon: <IconExport /> },
];

export default function App() {
  return (
    <div className="app">
      <header className="topbar">
        <div className="logo" aria-hidden>A</div>
        <div className="brand">
          <strong>Automata</strong>
          <span>Vehicle Capture</span>
        </div>
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
            {n.icon}
            <span>{n.label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
