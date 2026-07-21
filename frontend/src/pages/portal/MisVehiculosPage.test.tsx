import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MisVehiculosPage } from './MisVehiculosPage';
import { AuthContext, type AuthContextValue } from '@/auth/context';

vi.mock('@/api/asignaciones', () => ({
  propietariosApi: { vehiculos: vi.fn() },
}));
import { propietariosApi } from '@/api/asignaciones';

function renderPage() {
  const value = {
    user: { idUsuario: 'u1', username: 'c', roles: ['CLIENTE'] as never },
    loading: false,
    login: vi.fn(), registrarCliente: vi.fn(), registrarCompleto: vi.fn(),
    logout: vi.fn(), hasRole: vi.fn(),
  } as AuthContextValue;
  return render(
    <AuthContext.Provider value={value}><MisVehiculosPage /></AuthContext.Provider>,
  );
}

describe('MisVehiculosPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('muestra vehículos del propietario', async () => {
    (propietariosApi.vehiculos as ReturnType<typeof vi.fn>).mockResolvedValue([
      { id: 'v1', placa: 'ABC123', marca: 'Toyota', modelo: 'Corolla', tipo: 'AUTO', activo: true, color: 'Rojo', anio: 2020, clasificacion: 'PARTICULAR' },
    ]);
    renderPage();
    expect(await screen.findByText('ABC123')).toBeInTheDocument();
    expect(screen.getByText('Toyota Corolla')).toBeInTheDocument();
  });

  it('muestra estado vacío sin vehículos', async () => {
    (propietariosApi.vehiculos as ReturnType<typeof vi.fn>).mockResolvedValue([]);
    renderPage();
    await waitFor(() => expect(screen.getByText('Aún no tienes vehículos')).toBeInTheDocument());
  });

  it('muestra error al fallar la carga', async () => {
    (propietariosApi.vehiculos as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('x'));
    renderPage();
    await waitFor(() => expect(screen.getByText('Ocurrió un error')).toBeInTheDocument());
  });
});
