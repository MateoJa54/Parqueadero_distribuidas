import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AuthContext, type AuthContextValue, type AuthUser } from './context';
import { HomeRedirect, RequireAuth, RequirePermiso } from './guards';

function noop(): never {
  throw new Error('not implemented in test double');
}

function withAuth(
  overrides: Partial<AuthContextValue>,
  ui: React.ReactNode,
  initialPath: string,
) {
  const value: AuthContextValue = {
    user: null,
    loading: false,
    login: noop,
    registrarCliente: noop,
    registrarCompleto: noop,
    logout: () => undefined,
    hasRole: () => false,
    ...overrides,
  };
  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter initialEntries={[initialPath]}>{ui}</MemoryRouter>
    </AuthContext.Provider>,
  );
}

const admin: AuthUser = { idUsuario: '1', username: 'qa.admin', roles: ['ADMIN'] };
const cliente: AuthUser = { idUsuario: '2', username: 'qa.cliente', roles: ['CLIENTE'] };
const recaudador: AuthUser = { idUsuario: '3', username: 'qa.recauda', roles: ['RECAUDADOR'] };

describe('RequireAuth', () => {
  it('sin sesión activa, redirige a /login', () => {
    withAuth(
      { user: null },
      <Routes>
        <Route element={<RequireAuth />}>
          <Route path="/app" element={<div>Panel admin</div>} />
        </Route>
        <Route path="/login" element={<div>Pantalla de login</div>} />
      </Routes>,
      '/app',
    );
    expect(screen.getByText('Pantalla de login')).toBeInTheDocument();
  });

  it('con sesión activa, renderiza la ruta protegida', () => {
    withAuth(
      { user: admin },
      <Routes>
        <Route element={<RequireAuth />}>
          <Route path="/app" element={<div>Panel admin</div>} />
        </Route>
        <Route path="/login" element={<div>Pantalla de login</div>} />
      </Routes>,
      '/app',
    );
    expect(screen.getByText('Panel admin')).toBeInTheDocument();
  });
});

describe('RequirePermiso', () => {
  it('CLIENTE sin permiso "usuarios" es enviado a /403, no a login (no filtra la existencia de la ruta)', () => {
    withAuth(
      { user: cliente },
      <Routes>
        <Route element={<RequirePermiso permiso="usuarios" />}>
          <Route path="/app/usuarios" element={<div>Gestión de usuarios</div>} />
        </Route>
        <Route path="/403" element={<div>403 - No autorizado</div>} />
      </Routes>,
      '/app/usuarios',
    );
    expect(screen.getByText('403 - No autorizado')).toBeInTheDocument();
  });

  it('RECAUDADOR con permiso "tickets:operar" sí accede a Tickets', () => {
    withAuth(
      { user: recaudador },
      <Routes>
        <Route element={<RequirePermiso permiso="tickets:operar" />}>
          <Route path="/app/tickets" element={<div>Operación de tickets</div>} />
        </Route>
        <Route path="/403" element={<div>403 - No autorizado</div>} />
      </Routes>,
      '/app/tickets',
    );
    expect(screen.getByText('Operación de tickets')).toBeInTheDocument();
  });
});

describe('HomeRedirect', () => {
  it('ADMIN es redirigido a /app (tiene permiso de dashboard)', () => {
    withAuth(
      { user: admin },
      <Routes>
        <Route path="/" element={<HomeRedirect />} />
        <Route path="/app" element={<div>Dashboard admin</div>} />
      </Routes>,
      '/',
    );
    expect(screen.getByText('Dashboard admin')).toBeInTheDocument();
  });

  it('RECAUDADOR (sin dashboard) es redirigido directo a /app/tickets', () => {
    withAuth(
      { user: recaudador },
      <Routes>
        <Route path="/" element={<HomeRedirect />} />
        <Route path="/app/tickets" element={<div>Tickets</div>} />
      </Routes>,
      '/',
    );
    expect(screen.getByText('Tickets')).toBeInTheDocument();
  });

  it('CLIENTE es redirigido al portal', () => {
    withAuth(
      { user: cliente },
      <Routes>
        <Route path="/" element={<HomeRedirect />} />
        <Route path="/portal" element={<div>Portal cliente</div>} />
      </Routes>,
      '/',
    );
    expect(screen.getByText('Portal cliente')).toBeInTheDocument();
  });
});
