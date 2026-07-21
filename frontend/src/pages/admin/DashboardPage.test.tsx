import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { DashboardPage } from './DashboardPage';
import { AuthContext, type AuthContextValue } from '@/auth/context';

vi.mock('@/api/zonas', () => ({
  espaciosApi: { list: vi.fn() },
  zonasApi: { list: vi.fn() },
}));
vi.mock('@/api/tickets', () => ({ ticketsApi: { listar: vi.fn() } }));
vi.mock('@/api/vehiculos', () => ({ vehiculosApi: { list: vi.fn() } }));
import { espaciosApi, zonasApi } from '@/api/zonas';
import { ticketsApi } from '@/api/tickets';
import { vehiculosApi } from '@/api/vehiculos';

const mock = (fn: unknown) => fn as ReturnType<typeof vi.fn>;

function renderPage(roles: string[]) {
  const value = {
    user: { idUsuario: 'u1', username: 'admin', roles: roles as never },
    loading: false,
    login: vi.fn(), registrarCliente: vi.fn(), registrarCompleto: vi.fn(),
    logout: vi.fn(), hasRole: vi.fn(),
  } as AuthContextValue;
  return render(
    <AuthContext.Provider value={value}><DashboardPage /></AuthContext.Provider>,
  );
}

describe('DashboardPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('muestra estadísticas y ocupación para admin', async () => {
    mock(espaciosApi.list).mockResolvedValue([
      { id: 'e1', codigo: 'A-01', estado: 'DISPONIBLE' },
      { id: 'e2', codigo: 'A-02', estado: 'OCUPADO' },
      { id: 'e3', codigo: 'A-03', estado: 'RESERVADO' },
    ]);
    mock(zonasApi.list).mockResolvedValue([{ id: 'z1' }]);
    mock(ticketsApi.listar).mockImplementation((p: { estado: string }) =>
      p.estado === 'ACTIVO'
        ? Promise.resolve({ totalElements: 4, content: [] })
        : Promise.resolve({ content: [{ valorRecaudado: 10 }, { valorRecaudado: 5 }] }),
    );
    mock(vehiculosApi.list).mockResolvedValue([{ id: 'v1' }, { id: 'v2' }]);

    renderPage(['ADMIN']);
    expect(await screen.findByText('A-01')).toBeInTheDocument();
    expect(screen.getByText('Tickets activos')).toBeInTheDocument();
    expect(screen.getByText('Zonas')).toBeInTheDocument();
    expect(screen.getByText('Vehículos')).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText('4')).toBeInTheDocument());
  });

  it('muestra estado vacío sin espacios y oculta catálogo sin permiso', async () => {
    mock(espaciosApi.list).mockResolvedValue([]);
    mock(zonasApi.list).mockResolvedValue([]);
    mock(ticketsApi.listar).mockResolvedValue({ totalElements: 0, content: [] });
    mock(vehiculosApi.list).mockResolvedValue([]);

    renderPage(['CLIENTE']);
    await waitFor(() =>
      expect(screen.getByText('No hay espacios registrados.')).toBeInTheDocument(),
    );
    expect(screen.queryByText('Zonas')).not.toBeInTheDocument();
    expect(screen.queryByText('Vehículos')).not.toBeInTheDocument();
  });
});
