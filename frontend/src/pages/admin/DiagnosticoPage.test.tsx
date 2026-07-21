import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DiagnosticoPage } from './DiagnosticoPage';
import { AuthContext, type AuthContextValue } from '@/auth/context';
import { ApiError } from '@/api/client';

vi.mock('@/api/auth', () => ({ authApi: { me: vi.fn() } }));
vi.mock('@/api/usuarios', () => ({ rolesApi: { list: vi.fn() } }));
vi.mock('@/api/zonas', () => ({ zonasApi: { list: vi.fn() }, espaciosApi: { disponibles: vi.fn() } }));
vi.mock('@/api/asignaciones', () => ({ propietariosApi: { vehiculos: vi.fn() } }));
vi.mock('@/api/vehiculos', () => ({ vehiculosApi: { list: vi.fn() } }));
vi.mock('@/api/tickets', () => ({ ticketsApi: { listar: vi.fn() }, auditApi: { list: vi.fn() } }));

import { authApi } from '@/api/auth';
import { rolesApi } from '@/api/usuarios';
import { zonasApi, espaciosApi } from '@/api/zonas';
import { propietariosApi } from '@/api/asignaciones';
import { vehiculosApi } from '@/api/vehiculos';
import { ticketsApi, auditApi } from '@/api/tickets';

const mock = (fn: unknown) => fn as ReturnType<typeof vi.fn>;

function setAllOk() {
  mock(authApi.me).mockResolvedValue({ firstName: 'Mateo', lastName: 'Iza' });
  mock(rolesApi.list).mockResolvedValue([{ id: 'r1' }]);
  mock(zonasApi.list).mockResolvedValue([{ id: 'z1' }]);
  mock(espaciosApi.disponibles).mockResolvedValue([{ id: 'e1' }]);
  mock(propietariosApi.vehiculos).mockResolvedValue([{ id: 'v1' }]);
  mock(vehiculosApi.list).mockResolvedValue([{ id: 'v1' }]);
  mock(ticketsApi.listar).mockResolvedValue({ totalElements: 3 });
  mock(auditApi.list).mockResolvedValue([{ id: 'a1' }]);
}

function renderPage() {
  const value = {
    user: { idUsuario: 'u1', username: 'admin', roles: ['ADMIN'] as never },
    loading: false,
    login: vi.fn(), registrarCliente: vi.fn(), registrarCompleto: vi.fn(),
    logout: vi.fn(), hasRole: vi.fn(),
  } as AuthContextValue;
  return render(
    <AuthContext.Provider value={value}>
      <DiagnosticoPage />
    </AuthContext.Provider>,
  );
}

describe('DiagnosticoPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('ejecuta todas las pruebas y muestra OK al montar', async () => {
    setAllOk();
    renderPage();
    await waitFor(() => expect(screen.getAllByText('OK')).toHaveLength(8));
    expect(screen.getByText('Identidad: Mateo Iza')).toBeInTheDocument();
    expect(screen.getByText('3 tickets')).toBeInTheDocument();
  });

  it('muestra el fallo HTTP con su código y pista', async () => {
    setAllOk();
    mock(rolesApi.list).mockRejectedValue(new ApiError(403, 'No autorizado'));
    renderPage();
    expect(await screen.findByText('Fallo 403')).toBeInTheDocument();
    expect(screen.getByText(/Falta permiso/)).toBeInTheDocument();
  });

  it('muestra "Sin conexión" ante error de red', async () => {
    setAllOk();
    mock(zonasApi.list).mockRejectedValue(new TypeError('Failed to fetch'));
    renderPage();
    expect(await screen.findByText('Sin conexión')).toBeInTheDocument();
    expect(screen.getByText(/Servicio caído/)).toBeInTheDocument();
  });

  it('actualiza el conteo de correctas y con fallos', async () => {
    setAllOk();
    mock(auditApi.list).mockRejectedValue(new ApiError(500, 'boom'));
    renderPage();
    expect(await screen.findByText('Fallo 500')).toBeInTheDocument();
    const correctas = screen.getByText('Correctas').nextElementSibling!;
    const conFallos = screen.getByText('Con fallos').nextElementSibling!;
    expect(correctas).toHaveTextContent('7');
    expect(conFallos).toHaveTextContent('1');
  });

  it('reintenta una prueba individual', async () => {
    setAllOk();
    renderPage();
    await waitFor(() => expect(screen.getAllByText('OK')).toHaveLength(8));
    const row = screen.getByText('Roles (GET /roles)').closest('tr')!;
    await userEvent.click(within(row).getByRole('button', { name: 'Reintentar' }));
    await waitFor(() => expect(rolesApi.list).toHaveBeenCalledTimes(2));
  });

  it('re-ejecuta todo con el botón principal', async () => {
    setAllOk();
    renderPage();
    await waitFor(() => expect(screen.getAllByText('OK')).toHaveLength(8));
    await userEvent.click(screen.getByRole('button', { name: /Ejecutar todo/ }));
    await waitFor(() => expect(authApi.me).toHaveBeenCalledTimes(2));
  });
});
