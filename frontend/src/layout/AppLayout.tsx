import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/context';
import { puede, type Permiso } from '@/auth/rbac';
import { useTheme } from '@/ui/ThemeProvider';
import { iniciales } from '@/lib/format';

interface NavItem {
  to: string;
  label: string;
  icon: string;
  permiso: Permiso;
}

interface NavGroup {
  title: string;
  items: NavItem[];
}

const GROUPS: NavGroup[] = [
  {
    title: 'General',
    items: [{ to: '/app', label: 'Dashboard', icon: '▚', permiso: 'dashboard' }],
  },
  {
    title: 'Operación',
    items: [
      { to: '/app/tickets', label: 'Tickets', icon: '🎟', permiso: 'tickets:ver' },
      { to: '/app/espacios', label: 'Espacios', icon: '▦', permiso: 'espacios' },
      { to: '/app/zonas', label: 'Zonas', icon: '◫', permiso: 'zonas' },
    ],
  },
  {
    title: 'Catálogo',
    items: [
      { to: '/app/vehiculos', label: 'Vehículos', icon: '🚗', permiso: 'vehiculos' },
      {
        to: '/app/asignaciones-vehiculos',
        label: 'Asignaciones',
        icon: '🔗',
        permiso: 'asignaciones-vehiculos',
      },
    ],
  },
  {
    title: 'Administración',
    items: [
      { to: '/app/personas', label: 'Personas', icon: '👤', permiso: 'personas' },
      { to: '/app/usuarios', label: 'Usuarios', icon: '🪪', permiso: 'usuarios' },
      { to: '/app/roles', label: 'Roles', icon: '🛡', permiso: 'roles' },
      {
        to: '/app/asignacion-roles',
        label: 'Asignar roles',
        icon: '➕',
        permiso: 'asignacion-roles',
      },
      { to: '/app/auditoria', label: 'Auditoría', icon: '📜', permiso: 'auditoria' },
    ],
  },
  {
    title: 'Sistema',
    items: [
      { to: '/app/configuracion', label: 'Configuración', icon: '⚙', permiso: 'configuracion' },
      { to: '/app/diagnostico', label: 'Diagnóstico', icon: '🩺', permiso: 'diagnostico' },
    ],
  },
];

export function AppLayout() {
  const { user, logout } = useAuth();
  const { theme, toggle } = useTheme();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  const roles = user?.roles ?? [];

  const visibleGroups = GROUPS.map((g) => ({
    ...g,
    items: g.items.filter((i) => puede(roles, i.permiso)),
  })).filter((g) => g.items.length > 0);

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
          <span>Parqueadero</span>
        </div>
        <nav aria-label="Navegación principal">
          {visibleGroups.map((g) => (
            <div className="nav-section" key={g.title}>
              <div className="nav-section-title">{g.title}</div>
              {g.items.map((i) => (
                <NavLink
                  key={i.to}
                  to={i.to}
                  end={i.to === '/app'}
                  className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                  onClick={() => setOpen(false)}
                >
                  <span className="nav-ico" aria-hidden>
                    {i.icon}
                  </span>
                  {i.label}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>
        <div className="sidebar-footer">
          <NavLink to="/portal" className="nav-link">
            <span className="nav-ico" aria-hidden>
              🏠
            </span>
            {' '}Portal cliente
          </NavLink>
        </div>
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
            <div>
              <div className="crumb">Panel de gestión</div>
            </div>
          </div>
          <div className="row">
            <button
              type="button"
              className="icon-btn"
              onClick={toggle}
              aria-label={`Cambiar a tema ${theme === 'light' ? 'oscuro' : 'claro'}`}
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
