import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { LoginPage } from './LoginPage';
import { AuthContext, type AuthContextValue } from '@/auth/context';
import { ThemeProvider } from '@/ui/ThemeProvider';
import { ApiError } from '@/api/client';

function renderLogin(login: AuthContextValue['login']) {
  const value = {
    user: null, loading: false,
    login, registrarCliente: vi.fn(), registrarCompleto: vi.fn(),
    logout: vi.fn(), hasRole: vi.fn(),
  } as AuthContextValue;
  return render(
    <AuthContext.Provider value={value}>
      <ThemeProvider>
        <MemoryRouter initialEntries={['/login']}>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/app" element={<div>APP</div>} />
            <Route path="/portal" element={<div>PORTAL</div>} />
            <Route path="/registro" element={<div>REGISTRO</div>} />
          </Routes>
        </MemoryRouter>
      </ThemeProvider>
    </AuthContext.Provider>,
  );
}

describe('LoginPage', () => {
  it('login exitoso de staff navega a /app', async () => {
    const login = vi.fn().mockResolvedValue({ idUsuario: 'u', username: 'a', roles: ['ADMIN'] });
    renderLogin(login);
    await userEvent.type(screen.getByLabelText(/Usuario/), 'admin');
    await userEvent.type(screen.getByLabelText(/Contraseña/), 'secret');
    await userEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    await waitFor(() => expect(screen.getByText('APP')).toBeInTheDocument());
    expect(login).toHaveBeenCalledWith('admin', 'secret');
  });

  it('login de cliente navega a /portal', async () => {
    const login = vi.fn().mockResolvedValue({ idUsuario: 'u', username: 'c', roles: ['CLIENTE'] });
    renderLogin(login);
    await userEvent.type(screen.getByLabelText(/Usuario/), 'cli');
    await userEvent.type(screen.getByLabelText(/Contraseña/), 'secret');
    await userEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    await waitFor(() => expect(screen.getByText('PORTAL')).toBeInTheDocument());
  });

  it('muestra mensaje de ApiError al fallar', async () => {
    const login = vi.fn().mockRejectedValue(new ApiError(401, 'Credenciales inválidas'));
    renderLogin(login);
    await userEvent.type(screen.getByLabelText(/Usuario/), 'x');
    await userEvent.type(screen.getByLabelText(/Contraseña/), 'y');
    await userEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Credenciales inválidas');
  });

  it('muestra mensaje genérico ante error no-Api', async () => {
    const login = vi.fn().mockRejectedValue(new Error('boom'));
    renderLogin(login);
    await userEvent.type(screen.getByLabelText(/Usuario/), 'x');
    await userEvent.type(screen.getByLabelText(/Contraseña/), 'y');
    await userEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo iniciar sesión');
  });

  it('botón deshabilitado sin credenciales', () => {
    renderLogin(vi.fn());
    expect(screen.getByRole('button', { name: 'Iniciar sesión' })).toBeDisabled();
  });

  it('enlace a registro presente', () => {
    renderLogin(vi.fn());
    expect(screen.getByRole('link', { name: 'Crea tu cuenta' })).toHaveAttribute('href', '/registro');
  });
});
