import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AuditoriaPage } from './AuditoriaPage';

vi.mock('@/api/tickets', () => ({ auditApi: { list: vi.fn() } }));
import { auditApi } from '@/api/tickets';

const mock = (fn: unknown) => fn as ReturnType<typeof vi.fn>;
const logs = [
  { id: 'l1', servicio: 'usuarios', accion: 'CREATE', entidad: 'Usuario', usuario: 'admin', rol: 'ADMIN', ip: '1.1.1.1', timestamp: '2026-07-20T10:00:00Z', datos: { a: 1 } },
  { id: 'l2', servicio: 'tickets', accion: 'DELETE', entidad: null, usuario: null, rol: null, ip: null, timestamp: '2026-07-21T10:00:00Z', datos: null },
];

describe('AuditoriaPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('lista eventos ordenados', async () => {
    mock(auditApi.list).mockResolvedValue(logs);
    render(<AuditoriaPage />);
    expect(await screen.findByText('CREATE')).toBeInTheDocument();
    expect(screen.getByText('DELETE')).toBeInTheDocument();
  });

  it('filtra por servicio', async () => {
    mock(auditApi.list).mockResolvedValue(logs);
    render(<AuditoriaPage />);
    await screen.findByText('CREATE');
    await userEvent.selectOptions(screen.getByLabelText('Filtrar por servicio'), 'tickets');
    expect(screen.queryByText('CREATE')).not.toBeInTheDocument();
    expect(screen.getByText('DELETE')).toBeInTheDocument();
  });

  it('estado vacío', async () => {
    mock(auditApi.list).mockResolvedValue([]);
    render(<AuditoriaPage />);
    expect(await screen.findByText('Sin registros')).toBeInTheDocument();
  });

  it('error con reintento', async () => {
    mock(auditApi.list).mockRejectedValueOnce(new Error('x')).mockResolvedValueOnce(logs);
    render(<AuditoriaPage />);
    await screen.findByText('Ocurrió un error');
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByText('CREATE')).toBeInTheDocument();
  });

  it('refresca', async () => {
    mock(auditApi.list).mockResolvedValue(logs);
    render(<AuditoriaPage />);
    await screen.findByText('CREATE');
    await userEvent.click(screen.getByRole('button', { name: 'Refrescar' }));
    await waitFor(() => expect(auditApi.list).toHaveBeenCalledTimes(2));
  });

  it('abre detalle con datos', async () => {
    mock(auditApi.list).mockResolvedValue(logs);
    render(<AuditoriaPage />);
    const row = (await screen.findByText('CREATE')).closest('tr')!;
    await userEvent.click(within(row).getByRole('button', { name: 'Ver' }));
    expect(await screen.findByText('Detalle del evento')).toBeInTheDocument();
    expect(screen.getByText('1.1.1.1')).toBeInTheDocument();
  });
});
