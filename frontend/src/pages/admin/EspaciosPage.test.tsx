import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EspaciosPage } from './EspaciosPage';
import { ToastProvider } from '@/ui/ToastProvider';

vi.mock('@/api/zonas', () => ({
  espaciosApi: {
    list: vi.fn(), create: vi.fn(), update: vi.fn(),
    cambiarEstado: vi.fn(), activar: vi.fn(), desactivar: vi.fn(),
  },
  zonasApi: { list: vi.fn() },
}));
import { espaciosApi, zonasApi } from '@/api/zonas';

const mock = (fn: unknown) => fn as ReturnType<typeof vi.fn>;
const zonas = [
  { idZona: 'z1', nombre: 'Zona A', activo: true },
  { idZona: 'z2', nombre: 'Zona B', activo: true },
];
const espacios = [
  { id: 'e1', codigo: 'A-01', idZona: 'z1', nombreZona: 'Zona A', descripcion: 'Cerca puerta', tipo: 'AUTO', estado: 'DISPONIBLE', activo: true },
  { id: 'e2', codigo: 'B-05', idZona: 'z2', nombreZona: 'Zona B', descripcion: null, tipo: 'MOTO', estado: 'OCUPADO', activo: false },
];

function setAll() {
  mock(espaciosApi.list).mockResolvedValue(espacios);
  mock(zonasApi.list).mockResolvedValue(zonas);
}

const renderPage = () => render(<ToastProvider><EspaciosPage /></ToastProvider>);

describe('EspaciosPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('lista espacios', async () => {
    setAll();
    renderPage();
    expect(await screen.findByText('A-01')).toBeInTheDocument();
    expect(screen.getByText('B-05')).toBeInTheDocument();
  });

  it('estado vacío', async () => {
    mock(espaciosApi.list).mockResolvedValue([]);
    mock(zonasApi.list).mockResolvedValue(zonas);
    renderPage();
    expect(await screen.findByText('Sin espacios')).toBeInTheDocument();
  });

  it('error con reintento', async () => {
    mock(zonasApi.list).mockResolvedValue(zonas);
    mock(espaciosApi.list).mockRejectedValueOnce(new Error('x')).mockResolvedValueOnce(espacios);
    renderPage();
    await screen.findByText('Ocurrió un error');
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByText('A-01')).toBeInTheDocument();
  });

  it('filtra por estado', async () => {
    setAll();
    renderPage();
    await screen.findByText('A-01');
    await userEvent.selectOptions(screen.getByLabelText('Filtrar por estado'), 'OCUPADO');
    expect(screen.queryByText('A-01')).not.toBeInTheDocument();
    expect(screen.getByText('B-05')).toBeInTheDocument();
  });

  it('filtra por zona', async () => {
    setAll();
    renderPage();
    await screen.findByText('A-01');
    await userEvent.selectOptions(screen.getByLabelText('Filtrar por zona'), 'z2');
    expect(screen.queryByText('A-01')).not.toBeInTheDocument();
    expect(screen.getByText('B-05')).toBeInTheDocument();
  });

  it('valida zona requerida al crear', async () => {
    setAll();
    renderPage();
    await screen.findByText('A-01');
    await userEvent.click(screen.getByRole('button', { name: /Nuevo espacio/ }));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(espaciosApi.create).not.toHaveBeenCalled();
    expect(await screen.findByText('Selecciona una zona.')).toBeInTheDocument();
  });

  it('crea un espacio válido', async () => {
    setAll();
    mock(espaciosApi.create).mockResolvedValue({});
    renderPage();
    await screen.findByText('A-01');
    await userEvent.click(screen.getByRole('button', { name: /Nuevo espacio/ }));
    await userEvent.selectOptions(screen.getByLabelText(/Zona/), 'z1');
    fireEvent.change(screen.getByLabelText(/Descripción/), { target: { value: 'Nueva plaza' } });
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await waitFor(() => expect(espaciosApi.create).toHaveBeenCalled());
    const arg = mock(espaciosApi.create).mock.calls[0][0];
    expect(arg).toMatchObject({ idZona: 'z1', tipo: 'AUTO', descripcion: 'Nueva plaza', estado: 'DISPONIBLE' });
  });

  it('edita un espacio existente', async () => {
    setAll();
    mock(espaciosApi.update).mockResolvedValue({});
    renderPage();
    const row = (await screen.findByText('A-01')).closest('tr')!;
    await userEvent.click(within(row).getByRole('button', { name: 'Editar' }));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await waitFor(() => expect(espaciosApi.update).toHaveBeenCalledWith('e1', expect.objectContaining({ idZona: 'z1' })));
  });

  it('cambia el estado de un espacio', async () => {
    setAll();
    mock(espaciosApi.cambiarEstado).mockResolvedValue({});
    renderPage();
    const row = (await screen.findByText('A-01')).closest('tr')!;
    await userEvent.click(within(row).getByRole('button', { name: 'Estado' }));
    await userEvent.selectOptions(screen.getByLabelText('Nuevo estado'), 'MANTENIMIENTO');
    await userEvent.click(screen.getByRole('button', { name: 'Aplicar' }));
    await waitFor(() => expect(espaciosApi.cambiarEstado).toHaveBeenCalledWith('e1', 'MANTENIMIENTO'));
  });

  it('activa y desactiva', async () => {
    setAll();
    mock(espaciosApi.desactivar).mockResolvedValue({});
    mock(espaciosApi.activar).mockResolvedValue({});
    renderPage();
    const rowA = (await screen.findByText('A-01')).closest('tr')!;
    await userEvent.click(within(rowA).getByRole('button', { name: 'Desactivar' }));
    await waitFor(() => expect(espaciosApi.desactivar).toHaveBeenCalledWith('e1'));
    const rowB = screen.getByText('B-05').closest('tr')!;
    await userEvent.click(within(rowB).getByRole('button', { name: 'Activar' }));
    await waitFor(() => expect(espaciosApi.activar).toHaveBeenCalledWith('e2'));
  });
});
