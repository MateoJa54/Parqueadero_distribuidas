import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { RegistroPage } from './RegistroPage';
import { AuthContext, type AuthContextValue } from '@/auth/context';
import { ThemeProvider } from '@/ui/ThemeProvider';
import { ApiError } from '@/api/client';

function renderRegistro(registrarCompleto: AuthContextValue['registrarCompleto']) {
  const value = {
    user: null, loading: false,
    login: vi.fn(), registrarCliente: vi.fn(), registrarCompleto,
    logout: vi.fn(), hasRole: vi.fn(),
  } as AuthContextValue;
  return render(
    <AuthContext.Provider value={value}>
      <ThemeProvider>
        <MemoryRouter initialEntries={['/registro']}>
          <Routes>
            <Route path="/registro" element={<RegistroPage />} />
            <Route path="/app" element={<div>APP</div>} />
            <Route path="/portal" element={<div>PORTAL</div>} />
          </Routes>
        </MemoryRouter>
      </ThemeProvider>
    </AuthContext.Provider>,
  );
}

const set = (label: RegExp, value: string) =>
  fireEvent.change(screen.getByLabelText(label), { target: { value } });

async function llenarPaso1() {
  set(/Primer nombre/, 'Juan');
  set(/Segundo nombre/, 'Carlos');
  set(/Apellidos/, 'Perez Lopez');
  set(/Cédula/, '1710034065');
  set(/Teléfono/, '0991234567');
  set(/Correo electrónico/, 'juan@correo.com');
  set(/Dirección/, 'Av. Principal 123');
  set(/Nacionalidad/, 'Ecuatoriana');
  await userEvent.click(screen.getByRole('button', { name: 'Continuar' }));
}

describe('RegistroPage', () => {
  it('muestra el paso 1 inicialmente', () => {
    renderRegistro(vi.fn());
    expect(screen.getByText(/Paso 1 de 2/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeInTheDocument();
  });

  it('valida datos personales inválidos y no avanza', async () => {
    renderRegistro(vi.fn());
    await userEvent.click(screen.getByRole('button', { name: 'Continuar' }));
    expect(await screen.findByText('Cédula ecuatoriana inválida (10 dígitos).')).toBeInTheDocument();
    expect(screen.getByText(/Paso 1 de 2/)).toBeInTheDocument();
  });

  it('avanza al paso 2 con datos válidos', async () => {
    renderRegistro(vi.fn());
    await llenarPaso1();
    expect(await screen.findByText(/Paso 2 de 2/)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Usuario/)).toBeInTheDocument();
  });

  it('permite volver al paso 1', async () => {
    renderRegistro(vi.fn());
    await llenarPaso1();
    await screen.findByText(/Paso 2 de 2/);
    await userEvent.click(screen.getByRole('button', { name: 'Atrás' }));
    expect(await screen.findByText(/Paso 1 de 2/)).toBeInTheDocument();
  });

  it('valida credenciales inválidas en el paso 2', async () => {
    renderRegistro(vi.fn());
    await llenarPaso1();
    await screen.findByText(/Paso 2 de 2/);
    await userEvent.click(screen.getByRole('button', { name: 'Crear mi cuenta' }));
    expect(await screen.findByText(/3–15 caracteres/)).toBeInTheDocument();
  });

  it('detecta contraseñas que no coinciden', async () => {
    renderRegistro(vi.fn());
    await llenarPaso1();
    await screen.findByText(/Paso 2 de 2/);
    set(/^Usuario/, 'juanp');
    set(/^Contraseña/, 'Abc123');
    set(/Confirmar contraseña/, 'Abc124');
    await userEvent.click(screen.getByRole('button', { name: 'Crear mi cuenta' }));
    expect(await screen.findByText('Las contraseñas no coinciden.')).toBeInTheDocument();
  });

  it('registra con éxito y navega según el rol', async () => {
    const registrarCompleto = vi.fn().mockResolvedValue({ roles: ['CLIENTE'] });
    renderRegistro(registrarCompleto);
    await llenarPaso1();
    await screen.findByText(/Paso 2 de 2/);
    set(/^Usuario/, 'juanp');
    set(/^Contraseña/, 'Abc123');
    set(/Confirmar contraseña/, 'Abc123');
    await userEvent.click(screen.getByRole('button', { name: 'Crear mi cuenta' }));
    expect(await screen.findByText('PORTAL')).toBeInTheDocument();
    expect(registrarCompleto).toHaveBeenCalledWith(
      expect.objectContaining({
        firstName: 'Juan', middleName: 'Carlos', lastName: 'Perez Lopez',
        dni: '1710034065', email: 'juan@correo.com', phone: '0991234567',
        username: 'juanp', password: 'Abc123',
      }),
    );
  });

  it('muestra mensaje de ApiError al fallar el registro', async () => {
    const registrarCompleto = vi.fn().mockRejectedValue(new ApiError(409, 'Usuario ya existe'));
    renderRegistro(registrarCompleto);
    await llenarPaso1();
    await screen.findByText(/Paso 2 de 2/);
    set(/^Usuario/, 'juanp');
    set(/^Contraseña/, 'Abc123');
    set(/Confirmar contraseña/, 'Abc123');
    await userEvent.click(screen.getByRole('button', { name: 'Crear mi cuenta' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Usuario ya existe');
  });

  it('muestra mensaje genérico ante error no-Api', async () => {
    const registrarCompleto = vi.fn().mockRejectedValue(new Error('boom'));
    renderRegistro(registrarCompleto);
    await llenarPaso1();
    await screen.findByText(/Paso 2 de 2/);
    set(/^Usuario/, 'juanp');
    set(/^Contraseña/, 'Abc123');
    set(/Confirmar contraseña/, 'Abc123');
    await userEvent.click(screen.getByRole('button', { name: 'Crear mi cuenta' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('No pudimos completar el registro');
  });

  it('alterna el tema', async () => {
    renderRegistro(vi.fn());
    await userEvent.click(screen.getByRole('button', { name: 'Cambiar tema' }));
    expect(screen.getByRole('button', { name: 'Cambiar tema' })).toBeInTheDocument();
  });

  it('enlace a login presente', () => {
    renderRegistro(vi.fn());
    expect(screen.getByRole('link', { name: 'Inicia sesión' })).toHaveAttribute('href', '/login');
  });
});
