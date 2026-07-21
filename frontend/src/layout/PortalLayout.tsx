import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/context';
import { esStaff, puede } from '@/auth/rbac';
import { useTheme } from '@/ui/ThemeProvider';
import { iniciales } from '@/lib/format';

export function PortalLayout() {
  const { user, logout } = useAuth();
  const { theme, toggle } = useTheme();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const roles = user?.roles ?? [];

  const links = [
    { to: '/portal', label: 'Mi perfil', icon: '👤', permiso: 'portal:perfil' as const, end: true },
    {
      to: '/portal/vehiculos',
      label: 'Mis vehículos',
      icon: '🚗',
      permiso: 'portal:mis-vehiculos' as const,
      end: false,
    },
    {
      to: '/portal/disponibilidad',
      label: 'Disponibilidad',
      icon: '▦',
      permiso: 'portal:disponibilidad' as const,
      end: false,
    },
  ].filter((l) => puede(roles, l.permiso));

  return (
    <div className="app-shell">
      <button
        type="button"
        className={`scrim ${open ? 'show' : ''}`}
        aria-label="Cerrar menú"
        tabIndex={open ? 0 : -1}
        onClick={() => setOpen(false)}
      />
      <aside className={`sidebar ${open ? 'open' : ''}`}>
        <div className="sidebar-brand">
          <span className="logo" aria-hidden>
            P
          </span>
          <span>Mi Parqueadero</span>
        </div>
        <nav aria-label="Navegación del portal">
          <div className="nav-section">
            {links.map((l) => (
              <NavLink
                key={l.to}
                to={l.to}
                end={l.end}
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                onClick={() => setOpen(false)}
              >
                <span className="nav-ico" aria-hidden>
                  {l.icon}
                </span>
                {l.label}
              </NavLink>
            ))}
          </div>
        </nav>
        {esStaff(roles) && (
          <div className="sidebar-footer">
            <NavLink to="/app" className="nav-link">
              <span className="nav-ico" aria-hidden>
                ⚙
              </span>
              {' '}Panel de gestión
            </NavLink>
          </div>
        )}
      </aside>

      <div className="main">
        <header className="topbar">
          <div className="row">
            <button
              type="button"
              className="icon-btn sidebar-toggle"
              onClick={() => setOpen((o) => !o)}
              aria-label="Menú"
            >
              ☰
            </button>
            <div className="crumb">Portal del cliente</div>
          </div>
          <div className="row">
            <button
              type="button"
              className="icon-btn"
              onClick={toggle}
              aria-label="Cambiar tema"
              title="Cambiar tema"
            >
              {theme === 'light' ? '🌙' : '☀'}
            </button>
            <div className="user-chip">
              <span className="avatar" aria-hidden>
                {iniciales(user?.username).toUpperCase()}
              </span>
              <div>
                <div className="u-name">{user?.username}</div>
                <div className="u-role">{roles.join(' · ')}</div>
              </div>
            </div>
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              onClick={() => {
                logout();
                navigate('/login', { replace: true });
              }}
            >
              Salir
            </button>
          </div>
        </header>
        <main className="content">
          <div className="content-narrow">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
