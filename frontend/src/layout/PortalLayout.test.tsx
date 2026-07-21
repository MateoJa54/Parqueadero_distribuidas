import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { PortalLayout } from './PortalLayout';
import { AuthContext, type AuthContextValue } from '@/auth/context';
import { ThemeProvider } from '@/ui/ThemeProvider';

function renderLayout(roles: string[], logout = vi.fn()) {
  const value = {
    user: { idUsuario: 'u', username: 'cliente', roles: roles as never },
    loading: false,
    login: vi.fn(), registrarCliente: vi.fn(), registrarCompleto: vi.fn(),
    logout, hasRole: vi.fn(),
  } as AuthContextValue;
  return render(
    <AuthContext.Provider value={value}>
      <ThemeProvider>
        <MemoryRouter initialEntries={['/portal']}>
          <Routes>
            <Route element={<PortalLayout />}>
              <Route path="/portal" element={<div>PORTAL_CONTENIDO</div>} />
            </Route>
            <Route path="/login" element={<div>LOGIN</div>} />
          </Routes>
        </MemoryRouter>
      </ThemeProvider>
    </AuthContext.Provider>,
  );
}

describe('PortalLayout', () => {
  it('muestra enlaces del portal y el Outlet', () => {
    renderLayout(['CLIENTE']);
    expect(screen.getByText('PORTAL_CONTENIDO')).toBeInTheDocument();
    expect(screen.getByText('Mi perfil')).toBeInTheDocument();
    expect(screen.getByText('Mis vehículos')).toBeInTheDocument();
  });

  it('muestra enlace a panel de gestión solo para staff', () => {
    renderLayout(['ADMIN']);
    expect(screen.getByText('Panel de gestión')).toBeInTheDocument();
  });

  it('no muestra panel de gestión para cliente', () => {
    renderLayout(['CLIENTE']);
    expect(screen.queryByText('Panel de gestión')).not.toBeInTheDocument();
  });

  it('abre y cierra el menú', async () => {
    const { container } = renderLayout(['CLIENTE']);
    await userEvent.click(screen.getByLabelText('Menú'));
    expect(container.querySelector('.sidebar.open')).toBeInTheDocument();
    await userEvent.click(screen.getByLabelText('Cerrar menú'));
    expect(container.querySelector('.sidebar.open')).not.toBeInTheDocument();
  });

  it('cierra sesión y navega a login', async () => {
    const logout = vi.fn();
    renderLayout(['CLIENTE'], logout);
    await userEvent.click(screen.getByRole('button', { name: 'Salir' }));
    expect(logout).toHaveBeenCalled();
    expect(screen.getByText('LOGIN')).toBeInTheDocument();
  });

  it('alterna el tema', async () => {
    renderLayout(['CLIENTE']);
    await userEvent.click(screen.getByLabelText('Cambiar tema'));
    expect(screen.getByLabelText('Cambiar tema')).toBeInTheDocument();
  });
});
