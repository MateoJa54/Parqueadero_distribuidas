import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AppLayout } from './AppLayout';
import { AuthContext, type AuthContextValue } from '@/auth/context';
import { ThemeProvider } from '@/ui/ThemeProvider';

function renderLayout(roles: string[], logout = vi.fn()) {
  const value = {
    user: { idUsuario: 'u', username: 'mateo', roles: roles as never },
    loading: false,
    login: vi.fn(), registrarCliente: vi.fn(), registrarCompleto: vi.fn(),
    logout, hasRole: vi.fn(),
  } as AuthContextValue;
  return render(
    <AuthContext.Provider value={value}>
      <ThemeProvider>
        <MemoryRouter initialEntries={['/app']}>
          <Routes>
            <Route element={<AppLayout />}>
              <Route path="/app" element={<div>CONTENIDO</div>} />
            </Route>
            <Route path="/login" element={<div>LOGIN</div>} />
          </Routes>
        </MemoryRouter>
      </ThemeProvider>
    </AuthContext.Provider>,
  );
}

describe('AppLayout', () => {
  it('muestra grupos de navegación para ADMIN y el Outlet', () => {
    renderLayout(['ADMIN']);
    expect(screen.getByText('CONTENIDO')).toBeInTheDocument();
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Usuarios')).toBeInTheDocument();
    expect(screen.getByText('mateo')).toBeInTheDocument();
  });

  it('filtra grupos sin permisos para rol limitado', () => {
    renderLayout(['CLIENTE']);
    expect(screen.queryByText('Usuarios')).not.toBeInTheDocument();
  });

  it('abre y cierra el menú lateral', async () => {
    const { container } = renderLayout(['ADMIN']);
    const toggle = screen.getByLabelText('Menú');
    await userEvent.click(toggle);
    expect(container.querySelector('.sidebar.open')).toBeInTheDocument();
    await userEvent.click(screen.getByLabelText('Cerrar menú'));
    expect(container.querySelector('.sidebar.open')).not.toBeInTheDocument();
  });

  it('alterna el tema', async () => {
    renderLayout(['ADMIN']);
    const btn = screen.getByLabelText(/Cambiar a tema/);
    await userEvent.click(btn);
    expect(btn).toBeInTheDocument();
  });

  it('cierra sesión y navega a login', async () => {
    const logout = vi.fn();
    renderLayout(['ADMIN'], logout);
    await userEvent.click(screen.getByRole('button', { name: 'Salir' }));
    expect(logout).toHaveBeenCalled();
    expect(screen.getByText('LOGIN')).toBeInTheDocument();
  });
});
