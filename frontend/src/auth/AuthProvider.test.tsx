import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { AuthProvider } from './AuthProvider';
import { useAuth } from './context';
import { tokenStore } from '@/api/client';

vi.mock('@/api/auth', () => ({
  authApi: {
    login: vi.fn(),
    registrarCliente: vi.fn(),
    registrarCompleto: vi.fn(),
    refresh: vi.fn(),
    logout: vi.fn(),
  },
}));
import { authApi } from '@/api/auth';

function makeJwt(claims: Record<string, unknown>): string {
  const b64url = (obj: unknown) => {
    const bytes = new TextEncoder().encode(JSON.stringify(obj));
    return btoa(String.fromCharCode(...bytes)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  };
  return `${b64url({ alg: 'HS256' })}.${b64url(claims)}.sig`;
}
const validJwt = (extra: Record<string, unknown> = {}) =>
  makeJwt({ sub: 'u1', username: 'mateo', roles: ['ADMIN'], exp: Math.floor(Date.now() / 1000) + 10000, ...extra });

function Probe() {
  const { user, loading, login, logout } = useAuth();
  return (
    <div>
      <span data-testid="loading">{String(loading)}</span>
      <span data-testid="user">{user?.username ?? 'none'}</span>
      <button type="button" onClick={() => login('a', 'b')}>login</button>
      <button type="button" onClick={logout}>logout</button>
    </div>
  );
}

describe('AuthProvider', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('sin token: sin usuario y loading false', async () => {
    render(<AuthProvider><Probe /></AuthProvider>);
    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'));
    expect(screen.getByTestId('user')).toHaveTextContent('none');
  });

  it('restaura sesión con token válido', async () => {
    tokenStore.set(validJwt(), 'r');
    render(<AuthProvider><Probe /></AuthProvider>);
    await waitFor(() => expect(screen.getByTestId('user')).toHaveTextContent('mateo'));
  });

  it('token expirado con refresh exitoso reconstruye usuario', async () => {
    tokenStore.set(makeJwt({ sub: 'u1', exp: 1 }), 'r');
    (authApi.refresh as ReturnType<typeof vi.fn>).mockResolvedValue({
      token: validJwt(), refreshToken: 'r2', idUsuario: 'u1', username: 'refreshed', roles: ['CLIENTE'],
    });
    render(<AuthProvider><Probe /></AuthProvider>);
    await waitFor(() => expect(screen.getByTestId('user')).toHaveTextContent('refreshed'));
  });

  it('token expirado sin refresh limpia', async () => {
    localStorage.setItem('pq_token', makeJwt({ sub: 'u1', exp: 1 }));
    render(<AuthProvider><Probe /></AuthProvider>);
    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'));
    expect(screen.getByTestId('user')).toHaveTextContent('none');
  });

  it('login setea usuario', async () => {
    (authApi.login as ReturnType<typeof vi.fn>).mockResolvedValue({
      token: validJwt(), refreshToken: 'r', idUsuario: 'u1', username: 'logueado', roles: ['ADMIN'],
    });
    render(<AuthProvider><Probe /></AuthProvider>);
    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'));
    screen.getByText('login').click();
    await waitFor(() => expect(screen.getByTestId('user')).toHaveTextContent('logueado'));
  });

  it('logout limpia usuario', async () => {
    tokenStore.set(validJwt(), 'r');
    render(<AuthProvider><Probe /></AuthProvider>);
    await waitFor(() => expect(screen.getByTestId('user')).toHaveTextContent('mateo'));
    screen.getByText('logout').click();
    await waitFor(() => expect(screen.getByTestId('user')).toHaveTextContent('none'));
  });
});

describe('useAuth', () => {
  it('lanza fuera del provider', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<Probe />)).toThrow('useAuth debe usarse dentro de <AuthProvider>');
    spy.mockRestore();
  });
});
