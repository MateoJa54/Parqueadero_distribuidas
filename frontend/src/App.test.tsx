import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { App } from './App';
import { AuthContext, type AuthContextValue } from '@/auth/context';
import { ThemeProvider } from '@/ui/ThemeProvider';

function renderApp(initial: string, user: AuthContextValue['user'] = null) {
  const value = {
    user, loading: false,
    login: vi.fn(), registrarCliente: vi.fn(), registrarCompleto: vi.fn(),
    logout: vi.fn(), hasRole: vi.fn(),
  } as AuthContextValue;
  return render(
    <AuthContext.Provider value={value}>
      <ThemeProvider>
        <MemoryRouter initialEntries={[initial]}>
          <App />
        </MemoryRouter>
      </ThemeProvider>
    </AuthContext.Provider>,
  );
}

describe('App', () => {
  it('renderiza la página de login en /login', () => {
    renderApp('/login');
    expect(screen.getByRole('button', { name: 'Iniciar sesión' })).toBeInTheDocument();
  });

  it('renderiza la página de registro en /registro', () => {
    renderApp('/registro');
    expect(screen.getByText(/Paso 1 de 2/)).toBeInTheDocument();
  });

  it('renderiza la página 403 en /403', () => {
    renderApp('/403');
    expect(document.body.textContent).toBeTruthy();
  });

  it('la raíz sin sesión redirige a login', () => {
    renderApp('/');
    expect(screen.getByRole('button', { name: 'Iniciar sesión' })).toBeInTheDocument();
  });

  it('ruta desconocida muestra NotFound', () => {
    renderApp('/no-existe-esta-ruta');
    expect(document.body.textContent).toBeTruthy();
  });
});
