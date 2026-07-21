import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConfiguracionPage } from './ConfiguracionPage';
import { AuthContext, type AuthContextValue } from '@/auth/context';
import { ThemeProvider } from '@/ui/ThemeProvider';
import { ToastProvider } from '@/ui/ToastProvider';

function renderPage() {
  const value = {
    user: { idUsuario: 'u1', username: 'admin', roles: ['ADMIN'] as never },
    loading: false,
    login: vi.fn(), registrarCliente: vi.fn(), registrarCompleto: vi.fn(),
    logout: vi.fn(), hasRole: vi.fn(),
  } as AuthContextValue;
  return render(
    <AuthContext.Provider value={value}>
      <ThemeProvider>
        <ToastProvider><ConfiguracionPage /></ToastProvider>
      </ThemeProvider>
    </AuthContext.Provider>,
  );
}

describe('ConfiguracionPage', () => {
  beforeEach(() => localStorage.clear());

  it('muestra la sesión actual y la matriz de permisos', () => {
    renderPage();
    expect(screen.getByText('Configuración del sistema')).toBeInTheDocument();
    expect(screen.getByText('admin')).toBeInTheDocument();
    expect(screen.getByText('Matriz de roles y permisos')).toBeInTheDocument();
    expect(screen.getByText('Conexión con microservicios')).toBeInTheDocument();
  });

  it('cambia el tema a oscuro y claro', async () => {
    renderPage();
    await userEvent.click(screen.getByRole('button', { name: /Oscuro/ }));
    expect(document.documentElement.dataset.theme).toBe('dark');
    await userEvent.click(screen.getByRole('button', { name: /Claro/ }));
    expect(document.documentElement.dataset.theme).toBe('light');
  });

  it('guarda la preferencia de filas por página', async () => {
    renderPage();
    await userEvent.selectOptions(screen.getByLabelText('Filas por página'), '30');
    expect(screen.getByText(/Preferencia guardada/)).toBeInTheDocument();
  });
});
