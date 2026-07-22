import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { VehiculosPage } from './VehiculosPage';
import { ToastProvider } from '@/ui/ToastProvider';

vi.mock('@/api/vehiculos', () => ({
  vehiculosApi: {
    list: vi.fn(), create: vi.fn(), update: vi.fn(),
    activar: vi.fn(), desactivar: vi.fn(),
  },
}));
import { vehiculosApi } from '@/api/vehiculos';

const mock = (fn: unknown) => fn as ReturnType<typeof vi.fn>;
const vehiculos = [
  { id: 'v1', placa: 'ABC-1234', marca: 'Toyota', modelo: 'Corolla', color: 'Rojo', anio: 2020, clasificacion: 'Gasolina', tipo: 'Auto', activo: true, numeroPuertas: 4, capacidadMaletero: 400 },
  { id: 'v2', placa: 'XYZ-9876', marca: 'Chevrolet', modelo: 'Sail', color: null, anio: null, clasificacion: null, tipo: 'Auto', activo: false },
];

const renderPage = () => render(<ToastProvider><VehiculosPage /></ToastProvider>);

describe('VehiculosPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('lista vehículos', async () => {
    mock(vehiculosApi.list).mockResolvedValue(vehiculos);
    renderPage();
    expect(await screen.findByText('ABC-1234')).toBeInTheDocument();
    expect(screen.getByText('XYZ-9876')).toBeInTheDocument();
  });

  it('estado vacío', async () => {
    mock(vehiculosApi.list).mockResolvedValue([]);
    renderPage();
    expect(await screen.findByText('Sin vehículos')).toBeInTheDocument();
  });

  it('error con reintento', async () => {
    mock(vehiculosApi.list).mockRejectedValueOnce(new Error('x')).mockResolvedValueOnce(vehiculos);
    renderPage();
    await screen.findByText('Ocurrió un error');
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByText('ABC-1234')).toBeInTheDocument();
  });

  it('filtra por término', async () => {
    mock(vehiculosApi.list).mockResolvedValue(vehiculos);
    renderPage();
    await screen.findByText('ABC-1234');
    await userEvent.type(screen.getByLabelText('Buscar vehículos'), 'chevrolet');
    expect(screen.queryByText('ABC-1234')).not.toBeInTheDocument();
    expect(screen.getByText('XYZ-9876')).toBeInTheDocument();
  });

  it('recarga al incluir inactivos', async () => {
    mock(vehiculosApi.list).mockResolvedValue(vehiculos);
    renderPage();
    await screen.findByText('ABC-1234');
    await userEvent.click(screen.getByLabelText(/Incluir inactivos/));
    await waitFor(() => expect(vehiculosApi.list).toHaveBeenCalledWith(true));
  });

  it('valida placa inválida al crear', async () => {
    mock(vehiculosApi.list).mockResolvedValue(vehiculos);
    renderPage();
    await screen.findByText('ABC-1234');
    await userEvent.click(screen.getByRole('button', { name: /Nuevo vehículo/ }));
    fireEvent.change(screen.getByLabelText(/Marca/), { target: { value: 'Kia' } });
    fireEvent.change(screen.getByLabelText(/Modelo/), { target: { value: 'Rio' } });
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(vehiculosApi.create).not.toHaveBeenCalled();
    expect(await screen.findByText('Formato ABC-1234.')).toBeInTheDocument();
  });

  it('crea un vehículo válido', async () => {
    mock(vehiculosApi.list).mockResolvedValue(vehiculos);
    mock(vehiculosApi.create).mockResolvedValue({});
    renderPage();
    await screen.findByText('ABC-1234');
    await userEvent.click(screen.getByRole('button', { name: /Nuevo vehículo/ }));
    fireEvent.change(screen.getByLabelText(/Placa/), { target: { value: 'KIA-0001' } });
    fireEvent.change(screen.getByLabelText(/Marca/), { target: { value: 'Kia' } });
    fireEvent.change(screen.getByLabelText(/Modelo/), { target: { value: 'Rio' } });
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await waitFor(() => expect(vehiculosApi.create).toHaveBeenCalled());
    const arg = mock(vehiculosApi.create).mock.calls[0][0];
    expect(arg.tipo).toBe('Auto');
    expect(arg.datos).toMatchObject({ placa: 'KIA-0001', marca: 'Kia', modelo: 'Rio' });
  });

  it('edita un vehículo existente', async () => {
    mock(vehiculosApi.list).mockResolvedValue(vehiculos);
    mock(vehiculosApi.update).mockResolvedValue({});
    renderPage();
    const row = (await screen.findByText('ABC-1234')).closest('tr')!;
    await userEvent.click(within(row).getByRole('button', { name: 'Editar' }));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await waitFor(() => expect(vehiculosApi.update).toHaveBeenCalledWith('v1', expect.objectContaining({ marca: 'Toyota' })));
  });

  it('activa y desactiva', async () => {
    mock(vehiculosApi.list).mockResolvedValue(vehiculos);
    mock(vehiculosApi.desactivar).mockResolvedValue({});
    mock(vehiculosApi.activar).mockResolvedValue({});
    renderPage();
    const rowA = (await screen.findByText('ABC-1234')).closest('tr')!;
    await userEvent.click(within(rowA).getByRole('button', { name: 'Desactivar' }));
    await waitFor(() => expect(vehiculosApi.desactivar).toHaveBeenCalledWith('v1'));
    const rowX = screen.getByText('XYZ-9876').closest('tr')!;
    await userEvent.click(within(rowX).getByRole('button', { name: 'Activar' }));
    await waitFor(() => expect(vehiculosApi.activar).toHaveBeenCalledWith('v2'));
  });

  it('cambia el tipo a Motocicleta mostrando cilindraje', async () => {
    mock(vehiculosApi.list).mockResolvedValue(vehiculos);
    renderPage();
    await screen.findByText('ABC-1234');
    await userEvent.click(screen.getByRole('button', { name: /Nuevo vehículo/ }));
    await userEvent.selectOptions(screen.getByLabelText(/Tipo de vehículo/), 'Motocicleta');
    expect(screen.getByLabelText(/Cilindraje/)).toBeInTheDocument();
  });
});
