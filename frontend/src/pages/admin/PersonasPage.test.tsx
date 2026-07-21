import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PersonasPage } from './PersonasPage';
import { ToastProvider } from '@/ui/ToastProvider';

vi.mock('@/api/usuarios', () => ({
  personasApi: {
    list: vi.fn(), create: vi.fn(), update: vi.fn(),
    activar: vi.fn(), desactivar: vi.fn(),
  },
}));
import { personasApi } from '@/api/usuarios';

const mock = (fn: unknown) => fn as ReturnType<typeof vi.fn>;
const personas = [
  { id: 'p1', firstName: 'Mateo', middleName: null, lastName: 'Iza', dni: '1710034065', email: 'm@x.com', phone: '0999999999', address: null, nationality: 'Ecuatoriana', active: true },
  { id: 'p2', firstName: 'Ana', middleName: null, lastName: 'Perez', dni: '0102030405', email: 'a@x.com', phone: null, address: null, nationality: 'Ecuatoriana', active: false },
];

const renderPage = () => render(<ToastProvider><PersonasPage /></ToastProvider>);

describe('PersonasPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('lista personas', async () => {
    mock(personasApi.list).mockResolvedValue(personas);
    renderPage();
    expect(await screen.findByText('m@x.com')).toBeInTheDocument();
    expect(screen.getByText('a@x.com')).toBeInTheDocument();
  });

  it('estado vacío', async () => {
    mock(personasApi.list).mockResolvedValue([]);
    renderPage();
    expect(await screen.findByText('Sin personas')).toBeInTheDocument();
  });

  it('error con reintento', async () => {
    mock(personasApi.list).mockRejectedValueOnce(new Error('x')).mockResolvedValueOnce(personas);
    renderPage();
    await screen.findByText('Ocurrió un error');
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByText('m@x.com')).toBeInTheDocument();
  });

  it('filtra por término', async () => {
    mock(personasApi.list).mockResolvedValue(personas);
    renderPage();
    await screen.findByText('m@x.com');
    await userEvent.type(screen.getByLabelText('Buscar personas'), 'ana');
    expect(screen.queryByText('m@x.com')).not.toBeInTheDocument();
    expect(screen.getByText('a@x.com')).toBeInTheDocument();
  });

  it('valida campos inválidos al crear', async () => {
    mock(personasApi.list).mockResolvedValue(personas);
    renderPage();
    await screen.findByText('m@x.com');
    await userEvent.click(screen.getByRole('button', { name: /Nueva persona/ }));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(personasApi.create).not.toHaveBeenCalled();
    expect(await screen.findByText('Cédula ecuatoriana inválida.')).toBeInTheDocument();
  });

  it('crea una persona válida', async () => {
    mock(personasApi.list).mockResolvedValue(personas);
    mock(personasApi.create).mockResolvedValue({});
    renderPage();
    await screen.findByText('m@x.com');
    await userEvent.click(screen.getByRole('button', { name: /Nueva persona/ }));
    fireEvent.change(screen.getByLabelText(/Primer nombre/), { target: { value: 'Luis' } });
    fireEvent.change(screen.getByLabelText(/Apellidos/), { target: { value: 'Gomez' } });
    fireEvent.change(screen.getByLabelText(/Cédula/), { target: { value: '1710034065' } });
    fireEvent.change(screen.getByLabelText(/Teléfono/), { target: { value: '0999999999' } });
    fireEvent.change(screen.getByLabelText(/Correo/), { target: { value: 'luis@x.com' } });
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await waitFor(() => expect(personasApi.create).toHaveBeenCalled());
    const payload = mock(personasApi.create).mock.calls[0][0];
    expect(payload).toMatchObject({ firstName: 'Luis', lastName: 'Gomez', dni: '1710034065', email: 'luis@x.com' });
  });

  it('edita una persona existente', async () => {
    mock(personasApi.list).mockResolvedValue(personas);
    mock(personasApi.update).mockResolvedValue({});
    renderPage();
    const row = (await screen.findByText('m@x.com')).closest('tr')!;
    await userEvent.click(within(row).getByRole('button', { name: 'Editar' }));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await waitFor(() => expect(personasApi.update).toHaveBeenCalledWith('p1', expect.objectContaining({ dni: '1710034065' })));
  });

  it('activa y desactiva', async () => {
    mock(personasApi.list).mockResolvedValue(personas);
    mock(personasApi.desactivar).mockResolvedValue({});
    mock(personasApi.activar).mockResolvedValue({});
    renderPage();
    const rowM = (await screen.findByText('m@x.com')).closest('tr')!;
    await userEvent.click(within(rowM).getByRole('button', { name: 'Desactivar' }));
    await waitFor(() => expect(personasApi.desactivar).toHaveBeenCalledWith('p1'));
    const rowA = screen.getByText('a@x.com').closest('tr')!;
    await userEvent.click(within(rowA).getByRole('button', { name: 'Activar' }));
    await waitFor(() => expect(personasApi.activar).toHaveBeenCalledWith('p2'));
  });
});
