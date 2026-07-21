import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AsignacionesVehiculosPage } from './AsignacionesVehiculosPage';
import { ToastProvider } from '@/ui/ToastProvider';

vi.mock('@/api/asignaciones', () => ({
  asignVehiculoApi: {
    listAll: vi.fn(), create: vi.fn(), activar: vi.fn(), desactivar: vi.fn(),
  },
}));
vi.mock('@/api/vehiculos', () => ({ vehiculosApi: { list: vi.fn() } }));
vi.mock('@/api/usuarios', () => ({ usuariosApi: { list: vi.fn() } }));

import { asignVehiculoApi } from '@/api/asignaciones';
import { vehiculosApi } from '@/api/vehiculos';
import { usuariosApi } from '@/api/usuarios';

const mock = (fn: unknown) => fn as ReturnType<typeof vi.fn>;

const vehiculos = [
  { id: 'v1', placa: 'ABC-1234', marca: 'Toyota', modelo: 'Corolla' },
  { id: 'v2', placa: 'XYZ-9876', marca: 'Chevrolet', modelo: 'Sail' },
];
const usuarios = [
  { id: 'u1', username: 'mateo', nombreCompleto: 'Mateo Iza', active: true },
  { id: 'u2', username: 'ana', nombreCompleto: 'Ana Perez', active: true },
];
const asignaciones = [
  { userId: 'u1', vehicleId: 'v1', assignmentType: 'PROPIETARIO', vehicleAlias: 'Mi auto', status: 'ACTIVA', active: true },
  { userId: 'u2', vehicleId: 'v2', assignmentType: 'AUTORIZADO', vehicleAlias: null, status: 'INACTIVA', active: false },
];

function setAll() {
  mock(asignVehiculoApi.listAll).mockResolvedValue(asignaciones);
  mock(vehiculosApi.list).mockResolvedValue(vehiculos);
  mock(usuariosApi.list).mockResolvedValue(usuarios);
}

const renderPage = () => render(<ToastProvider><AsignacionesVehiculosPage /></ToastProvider>);

describe('AsignacionesVehiculosPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('lista asignaciones', async () => {
    setAll();
    renderPage();
    expect(await screen.findByText('mateo')).toBeInTheDocument();
    expect(screen.getByText('ana')).toBeInTheDocument();
    expect(screen.getByText('Mi auto')).toBeInTheDocument();
  });

  it('estado vacío sin asignaciones', async () => {
    mock(asignVehiculoApi.listAll).mockResolvedValue([]);
    mock(vehiculosApi.list).mockResolvedValue(vehiculos);
    mock(usuariosApi.list).mockResolvedValue(usuarios);
    renderPage();
    expect(await screen.findByText('Sin asignaciones')).toBeInTheDocument();
  });

  it('error con reintento', async () => {
    mock(vehiculosApi.list).mockResolvedValue(vehiculos);
    mock(usuariosApi.list).mockResolvedValue(usuarios);
    mock(asignVehiculoApi.listAll).mockRejectedValueOnce(new Error('x')).mockResolvedValueOnce(asignaciones);
    renderPage();
    await screen.findByText('Ocurrió un error');
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByText('mateo')).toBeInTheDocument();
  });

  it('filtra por término de búsqueda', async () => {
    setAll();
    renderPage();
    await screen.findByText('mateo');
    fireEvent.change(screen.getByLabelText(/Buscar/), { target: { value: 'chevrolet' } });
    expect(screen.queryByText('mateo')).not.toBeInTheDocument();
    expect(screen.getByText('ana')).toBeInTheDocument();
  });

  it('filtra por estado (solo activas)', async () => {
    setAll();
    renderPage();
    await screen.findByText('mateo');
    await userEvent.selectOptions(screen.getByLabelText('Estado'), 'ACTIVAS');
    expect(screen.getByText('mateo')).toBeInTheDocument();
    expect(screen.queryByText('ana')).not.toBeInTheDocument();
  });

  it('sin resultados cuando el filtro no coincide', async () => {
    setAll();
    renderPage();
    await screen.findByText('mateo');
    fireEvent.change(screen.getByLabelText(/Buscar/), { target: { value: 'zzzz' } });
    expect(await screen.findByText('Sin resultados')).toBeInTheDocument();
  });

  it('valida vehículo y usuario requeridos al crear', async () => {
    setAll();
    renderPage();
    await screen.findByText('mateo');
    await userEvent.click(screen.getByRole('button', { name: /Nueva asignación/ }));
    await userEvent.click(screen.getByRole('button', { name: 'Asignar' }));
    expect(asignVehiculoApi.create).not.toHaveBeenCalled();
  });

  it('crea una asignación válida', async () => {
    setAll();
    mock(asignVehiculoApi.create).mockResolvedValue({});
    renderPage();
    await screen.findByText('mateo');
    await userEvent.click(screen.getByRole('button', { name: /Nueva asignación/ }));
    // Seleccionar vehículo vía combobox (filtrar por query y elegir con Enter)
    const vehCombo = screen.getByRole('combobox', { name: /Vehículo/ });
    fireEvent.focus(vehCombo);
    fireEvent.change(vehCombo, { target: { value: 'ABC-1234' } });
    fireEvent.keyDown(vehCombo, { key: 'Enter' });
    // Seleccionar usuario vía combobox
    const userCombo = screen.getByRole('combobox', { name: /Usuario/ });
    fireEvent.focus(userCombo);
    fireEvent.change(userCombo, { target: { value: 'ana' } });
    fireEvent.keyDown(userCombo, { key: 'Enter' });
    await userEvent.click(screen.getByRole('button', { name: 'Asignar' }));
    await waitFor(() => expect(asignVehiculoApi.create).toHaveBeenCalled());
    const arg = mock(asignVehiculoApi.create).mock.calls[0][0];
    expect(arg).toMatchObject({ vehicleId: 'v1', userId: 'u2', assignmentType: 'PROPIETARIO' });
  });

  it('activa y desactiva', async () => {
    setAll();
    mock(asignVehiculoApi.desactivar).mockResolvedValue({});
    mock(asignVehiculoApi.activar).mockResolvedValue({});
    renderPage();
    const rowM = (await screen.findByText('mateo')).closest('tr')!;
    await userEvent.click(within(rowM).getByRole('button', { name: 'Desactivar' }));
    await waitFor(() => expect(asignVehiculoApi.desactivar).toHaveBeenCalledWith('u1', 'v1'));
    const rowA = screen.getByText('ana').closest('tr')!;
    await userEvent.click(within(rowA).getByRole('button', { name: 'Activar' }));
    await waitFor(() => expect(asignVehiculoApi.activar).toHaveBeenCalledWith('u2', 'v2'));
  });
});
