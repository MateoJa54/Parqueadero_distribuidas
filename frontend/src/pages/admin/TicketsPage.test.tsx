import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TicketsPage } from './TicketsPage';
import { ToastProvider } from '@/ui/ToastProvider';
import { ApiError } from '@/api/client';
import type { Ticket, Page } from '@/types';

vi.mock('@/api/tickets', () => ({
  ticketsApi: {
    ingreso: vi.fn(),
    pagar: vi.fn(),
    anular: vi.fn(),
    listar: vi.fn(),
    porCodigo: vi.fn(),
  },
}));
vi.mock('@/api/zonas', () => ({ espaciosApi: { disponibles: vi.fn() } }));

import { ticketsApi } from '@/api/tickets';
import { espaciosApi } from '@/api/zonas';

const mock = (fn: unknown) => fn as ReturnType<typeof vi.fn>;

const ticketActivo: Ticket = {
  id: 't1', codigo: 'TK-001', idEspacio: 'e1', codigoEspacio: 'A-01',
  placa: 'ABC-1234', fechaHoraIngreso: '2026-07-21T10:00:00Z',
  estadoTicket: 'ACTIVO', valorRecaudado: null,
};
const ticketPagado: Ticket = {
  id: 't2', codigo: 'TK-002', idEspacio: 'e2', codigoEspacio: 'A-02',
  placa: 'XYZ-9876', fechaHoraIngreso: '2026-07-21T09:00:00Z',
  fechaHoraSalida: '2026-07-21T11:00:00Z', estadoTicket: 'PAGADO', valorRecaudado: 2.5,
  motivoAnulacion: null,
};
const pageData: Page<Ticket> = {
  content: [ticketActivo, ticketPagado], totalElements: 2, totalPages: 1,
  number: 0, size: 20, first: true, last: true,
};
const espacios = [
  { id: 'e1', codigo: 'A-01', tipo: 'AUTO', nombreZona: 'Zona Norte' },
  { id: 'e2', codigo: 'A-02', tipo: 'MOTO' },
];

const renderPage = () => render(<ToastProvider><TicketsPage /></ToastProvider>);

describe('TicketsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mock(ticketsApi.listar).mockResolvedValue(pageData);
    mock(espaciosApi.disponibles).mockResolvedValue(espacios);
  });

  it('lista tickets por defecto', async () => {
    renderPage();
    expect(await screen.findByText('TK-001')).toBeInTheDocument();
    expect(screen.getByText('TK-002')).toBeInTheDocument();
  });

  it('filtra por estado', async () => {
    renderPage();
    await screen.findByText('TK-001');
    await userEvent.selectOptions(screen.getByLabelText('Filtrar por estado'), 'PAGADO');
    await waitFor(() =>
      expect(mock(ticketsApi.listar)).toHaveBeenLastCalledWith(
        expect.objectContaining({ estado: 'PAGADO', page: 0 }),
      ),
    );
  });

  it('estado vacío sin tickets', async () => {
    mock(ticketsApi.listar).mockResolvedValue({ ...pageData, content: [], totalElements: 0 });
    renderPage();
    expect(await screen.findByText('Sin tickets')).toBeInTheDocument();
  });

  it('error al cargar con reintento', async () => {
    mock(ticketsApi.listar)
      .mockRejectedValueOnce(new ApiError(500, 'boom'))
      .mockResolvedValueOnce(pageData);
    renderPage();
    await screen.findByText('boom');
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByText('TK-001')).toBeInTheDocument();
  });

  it('cobra un ticket activo', async () => {
    mock(ticketsApi.pagar).mockResolvedValue({ ...ticketActivo, valorRecaudado: 3 });
    renderPage();
    await screen.findByText('TK-001');
    const row = screen.getByText('TK-001').closest('tr')!;
    await userEvent.click(within(row).getByRole('button', { name: 'Cobrar' }));
    await waitFor(() => expect(ticketsApi.pagar).toHaveBeenCalledWith('t1'));
  });

  it('cobro falla muestra toast', async () => {
    mock(ticketsApi.pagar).mockRejectedValue(new ApiError(400, 'no cobra'));
    renderPage();
    await screen.findByText('TK-001');
    const row = screen.getByText('TK-001').closest('tr')!;
    await userEvent.click(within(row).getByRole('button', { name: 'Cobrar' }));
    expect(await screen.findByText('no cobra')).toBeInTheDocument();
  });

  it('anula un ticket con motivo válido', async () => {
    mock(ticketsApi.anular).mockResolvedValue({ ...ticketActivo, estadoTicket: 'ANULADO' });
    renderPage();
    await screen.findByText('TK-001');
    const row = screen.getByText('TK-001').closest('tr')!;
    await userEvent.click(within(row).getByRole('button', { name: 'Anular' }));
    const motivo = await screen.findByLabelText(/Motivo de anulación/);
    fireEvent.change(motivo, { target: { value: 'placa incorrecta' } });
    await userEvent.click(screen.getByRole('button', { name: 'Anular ticket' }));
    await waitFor(() => expect(ticketsApi.anular).toHaveBeenCalledWith('t1', 'placa incorrecta'));
  });

  it('no anula con motivo corto (botón deshabilitado)', async () => {
    renderPage();
    await screen.findByText('TK-001');
    const row = screen.getByText('TK-001').closest('tr')!;
    await userEvent.click(within(row).getByRole('button', { name: 'Anular' }));
    await screen.findByLabelText(/Motivo de anulación/);
    expect(screen.getByRole('button', { name: 'Anular ticket' })).toBeDisabled();
  });

  it('cancela el modal de anulación', async () => {
    renderPage();
    await screen.findByText('TK-001');
    const row = screen.getByText('TK-001').closest('tr')!;
    await userEvent.click(within(row).getByRole('button', { name: 'Anular' }));
    await screen.findByLabelText(/Motivo de anulación/);
    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
    await waitFor(() =>
      expect(screen.queryByLabelText(/Motivo de anulación/)).not.toBeInTheDocument(),
    );
  });

  it('registra ingreso en la pestaña ingreso', async () => {
    mock(ticketsApi.ingreso).mockResolvedValue({ ...ticketActivo, codigo: 'TK-NEW' });
    renderPage();
    await screen.findByText('TK-001');
    await userEvent.click(screen.getByRole('tab', { name: 'Registrar ingreso' }));
    fireEvent.change(await screen.findByLabelText(/Placa del vehículo/), {
      target: { value: 'abc-1234' },
    });
    await userEvent.selectOptions(await screen.findByLabelText(/Espacio disponible/), 'e1');
    await userEvent.click(screen.getByRole('button', { name: 'Registrar ingreso' }));
    await waitFor(() =>
      expect(ticketsApi.ingreso).toHaveBeenCalledWith({ placa: 'ABC-1234', idEspacio: 'e1' }),
    );
    expect(await screen.findByText(/creado para/)).toBeInTheDocument();
  });

  it('valida placa inválida en ingreso', async () => {
    renderPage();
    await screen.findByText('TK-001');
    await userEvent.click(screen.getByRole('tab', { name: 'Registrar ingreso' }));
    fireEvent.change(await screen.findByLabelText(/Placa del vehículo/), {
      target: { value: 'ZZZ' },
    });
    await userEvent.selectOptions(await screen.findByLabelText(/Espacio disponible/), 'e1');
    await userEvent.click(screen.getByRole('button', { name: 'Registrar ingreso' }));
    expect(await screen.findByText('Placa inválida.')).toBeInTheDocument();
    expect(ticketsApi.ingreso).not.toHaveBeenCalled();
  });

  it('ingreso sin espacios disponibles muestra aviso', async () => {
    mock(espaciosApi.disponibles).mockResolvedValue([]);
    renderPage();
    await screen.findByText('TK-001');
    await userEvent.click(screen.getByRole('tab', { name: 'Registrar ingreso' }));
    expect(await screen.findByText(/No hay espacios disponibles/)).toBeInTheDocument();
  });

  it('ingreso falla muestra toast', async () => {
    mock(ticketsApi.ingreso).mockRejectedValue(new ApiError(409, 'espacio ocupado'));
    renderPage();
    await screen.findByText('TK-001');
    await userEvent.click(screen.getByRole('tab', { name: 'Registrar ingreso' }));
    fireEvent.change(await screen.findByLabelText(/Placa del vehículo/), {
      target: { value: 'ABC-1234' },
    });
    await userEvent.selectOptions(await screen.findByLabelText(/Espacio disponible/), 'e1');
    await userEvent.click(screen.getByRole('button', { name: 'Registrar ingreso' }));
    expect(await screen.findByText('espacio ocupado')).toBeInTheDocument();
  });

  it('busca ticket por código', async () => {
    mock(ticketsApi.porCodigo).mockResolvedValue(ticketPagado);
    renderPage();
    await screen.findByText('TK-001');
    await userEvent.click(screen.getByRole('tab', { name: 'Buscar por código' }));
    fireEvent.change(await screen.findByLabelText('Código del ticket'), {
      target: { value: 'TK-002' },
    });
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }));
    expect(await screen.findByRole('heading', { name: 'TK-002' })).toBeInTheDocument();
  });

  it('buscar por código con Enter', async () => {
    mock(ticketsApi.porCodigo).mockResolvedValue(ticketPagado);
    renderPage();
    await screen.findByText('TK-001');
    await userEvent.click(screen.getByRole('tab', { name: 'Buscar por código' }));
    const input = await screen.findByLabelText('Código del ticket');
    fireEvent.change(input, { target: { value: 'TK-002' } });
    fireEvent.keyDown(input, { key: 'Enter' });
    expect(await screen.findByRole('heading', { name: 'TK-002' })).toBeInTheDocument();
  });

  it('buscar no encontrado muestra error', async () => {
    mock(ticketsApi.porCodigo).mockRejectedValue(new ApiError(404, 'no existe'));
    renderPage();
    await screen.findByText('TK-001');
    await userEvent.click(screen.getByRole('tab', { name: 'Buscar por código' }));
    fireEvent.change(await screen.findByLabelText('Código del ticket'), {
      target: { value: 'NOPE' },
    });
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }));
    expect(await screen.findByText('no existe')).toBeInTheDocument();
  });
});
