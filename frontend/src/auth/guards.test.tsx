import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { RequireAuth, RequirePermiso, HomeRedirect } from './guards';
import { AuthContext, type AuthContextValue } from './context';

function wrap(ctx: Partial<AuthContextValue>, initial: string, element: React.ReactNode, extraRoutes?: React.ReactNode) {
  const value = {
    user: null, loading: false,
    login: vi.fn(), registrarCliente: vi.fn(), registrarCompleto: vi.fn(),
    logout: vi.fn(), hasRole: vi.fn(),
    ...ctx,
  } as AuthContextValue;
  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter initialEntries={[initial]}>
        <Routes>
          <Route path={initial} element={element} />
          <Route path="/login" element={<div>LOGIN</div>} />
          <Route path="/403" element={<div>FORBIDDEN</div>} />
          <Route path="/app" element={<div>APP</div>} />
          <Route path="/portal" element={<div>PORTAL</div>} />
          {extraRoutes}
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

const admin = { idUsuario: 'u', username: 'a', roles: ['ADMIN' as const] };
const cliente = { idUsuario: 'u', username: 'c', roles: ['CLIENTE' as const] };

describe('RequireAuth', () => {
  it('muestra spinner en loading', () => {
    wrap({ loading: true }, '/x', <RequireAuth />);
    expect(screen.getByText('Cargando sesión…')).toBeInTheDocument();
  });
  it('redirige a login sin usuario', () => {
    wrap({ user: null }, '/x', <RequireAuth />);
    expect(screen.getByText('LOGIN')).toBeInTheDocument();
  });
  it('renderiza Outlet con usuario', () => {
    const value = {
      user: admin, loading: false,
      login: vi.fn(), registrarCliente: vi.fn(), registrarCompleto: vi.fn(),
      logout: vi.fn(), hasRole: vi.fn(),
    } as AuthContextValue;
    render(
      <AuthContext.Provider value={value}>
        <MemoryRouter initialEntries={['/x']}>
          <Routes>
            <Route element={<RequireAuth />}>
              <Route path="/x" element={<div>OK</div>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>,
    );
    expect(screen.getByText('OK')).toBeInTheDocument();
  });
});

describe('RequirePermiso', () => {
  it('sin usuario va a login', () => {
    wrap({ user: null }, '/x', <RequirePermiso permiso="usuarios" />);
    expect(screen.getByText('LOGIN')).toBeInTheDocument();
  });
  it('sin permiso va a 403', () => {
    wrap({ user: cliente }, '/x', <RequirePermiso permiso="usuarios" />);
    expect(screen.getByText('FORBIDDEN')).toBeInTheDocument();
  });
  it('con permiso no redirige', () => {
    wrap({ user: admin }, '/x', <RequirePermiso permiso="usuarios" />);
    expect(screen.queryByText('FORBIDDEN')).not.toBeInTheDocument();
    expect(screen.queryByText('LOGIN')).not.toBeInTheDocument();
  });
});

describe('HomeRedirect', () => {
  it('null en loading', () => {
    const { container } = wrap({ loading: true }, '/', <HomeRedirect />);
    expect(container.textContent).toBe('');
  });
  it('a login sin usuario', () => {
    wrap({ user: null }, '/', <HomeRedirect />);
    expect(screen.getByText('LOGIN')).toBeInTheDocument();
  });
  it('a app para staff', () => {
    wrap({ user: admin }, '/', <HomeRedirect />);
    expect(screen.getByText('APP')).toBeInTheDocument();
  });
  it('a portal para cliente', () => {
    wrap({ user: cliente }, '/', <HomeRedirect />);
    expect(screen.getByText('PORTAL')).toBeInTheDocument();
  });
});
