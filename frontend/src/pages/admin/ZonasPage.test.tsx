import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ZonasPage } from './ZonasPage';
import { ToastProvider } from '@/ui/ToastProvider';

vi.mock('@/api/zonas', () => ({
  zonasApi: {
    list: vi.fn(), create: vi.fn(), update: vi.fn(),
    activar: vi.fn(), desactivar: vi.fn(),
  },
}));
import { zonasApi } from '@/api/zonas';

const mock = (fn: unknown) => fn as ReturnType<typeof vi.fn>;
const zonas = [
  { idZona: 'z1', nombre: 'Zona VIP', descripcion: 'desc', tipoZona: 'VIP', capacidad: 20, activo: true, codigo: 'Z1' },
  { idZona: 'z2', nombre: 'Zona Reg', descripcion: null, tipoZona: 'REGULAR', capacidad: 5, activo: false },
];

const renderPage = () => render(<ToastProvider><ZonasPage /></ToastProvider>);

describe('ZonasPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('lista zonas', async () => {
    mock(zonasApi.list).mockResolvedValue(zonas);
    renderPage();
    expect(await screen.findByText('Zona VIP')).toBeInTheDocument();
    expect(screen.getByText('Zona Reg')).toBeInTheDocument();
  });

  it('estado vacío', async () => {
    mock(zonasApi.list).mockResolvedValue([]);
    renderPage();
    expect(await screen.findByText('Sin zonas')).toBeInTheDocument();
  });

  it('error con reintento', async () => {
    mock(zonasApi.list).mockRejectedValueOnce(new Error('x')).mockResolvedValueOnce(zonas);
    renderPage();
    await screen.findByText('Ocurrió un error');
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByText('Zona VIP')).toBeInTheDocument();
  });

  it('valida nombre corto al crear', async () => {
    mock(zonasApi.list).mockResolvedValue(zonas);
    renderPage();
    await screen.findByText('Zona VIP');
    await userEvent.click(screen.getByRole('button', { name: /Nueva zona/ }));
    fireEvent.change(screen.getByLabelText(/Nombre/), { target: { value: 'ab' } });
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(zonasApi.create).not.toHaveBeenCalled();
    expect(screen.getByText('Mínimo 3 caracteres.')).toBeInTheDocument();
  });

  it('crea una zona válida', async () => {
    mock(zonasApi.list).mockResolvedValue(zonas);
    mock(zonasApi.create).mockResolvedValue({});
    renderPage();
    await screen.findByText('Zona VIP');
    await userEvent.click(screen.getByRole('button', { name: /Nueva zona/ }));
    fireEvent.change(screen.getByLabelText(/Nombre/), { target: { value: 'Nueva' } });
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await waitFor(() => expect(zonasApi.create).toHaveBeenCalled());
  });

  it('edita una zona', async () => {
    mock(zonasApi.list).mockResolvedValue(zonas);
    mock(zonasApi.update).mockResolvedValue({});
    renderPage();
    const card = (await screen.findByText('Zona VIP')).closest('.card')!;
    await userEvent.click(within(card as HTMLElement).getByRole('button', { name: 'Editar' }));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await waitFor(() => expect(zonasApi.update).toHaveBeenCalledWith('z1', expect.objectContaining({ nombre: 'Zona VIP' })));
  });

  it('activa y desactiva', async () => {
    mock(zonasApi.list).mockResolvedValue(zonas);
    mock(zonasApi.desactivar).mockResolvedValue({});
    mock(zonasApi.activar).mockResolvedValue({});
    renderPage();
    const cardV = (await screen.findByText('Zona VIP')).closest('.card')!;
    await userEvent.click(within(cardV as HTMLElement).getByRole('button', { name: 'Desactivar' }));
    await waitFor(() => expect(zonasApi.desactivar).toHaveBeenCalledWith('z1'));
    const cardR = screen.getByText('Zona Reg').closest('.card')!;
    await userEvent.click(within(cardR as HTMLElement).getByRole('button', { name: 'Activar' }));
    await waitFor(() => expect(zonasApi.activar).toHaveBeenCalledWith('z2'));
  });
});
