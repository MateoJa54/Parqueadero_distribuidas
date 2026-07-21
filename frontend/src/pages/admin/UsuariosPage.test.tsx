import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UsuariosPage } from './UsuariosPage';
import { ToastProvider } from '@/ui/ToastProvider';

vi.mock('@/api/usuarios', () => ({
  usuariosApi: {
    list: vi.fn(), create: vi.fn(), update: vi.fn(),
    activar: vi.fn(), desactivar: vi.fn(),
  },
  personasApi: { list: vi.fn() },
}));
import { usuariosApi, personasApi } from '@/api/usuarios';

const mock = (fn: unknown) => fn as ReturnType<typeof vi.fn>;
const usuarios = [
  { id: 'u1', idPersona: 'p1', username: 'mateo', nombreCompleto: 'Mateo Iza', lastLogin: '2026-07-20T10:00:00Z', active: true },
  { id: 'u2', idPersona: 'p2', username: 'ana', nombreCompleto: 'Ana P', lastLogin: null, active: false },
];
const personas = [
  { id: 'p1', firstName: 'Mateo', lastName: 'Iza', dni: '111', active: true },
  { id: 'p2', firstName: 'Ana', lastName: 'P', dni: '222', active: true },
  { id: 'p3', firstName: 'Luis', lastName: 'G', dni: '333', active: true },
];

function setAll() {
  mock(usuariosApi.list).mockResolvedValue(usuarios);
  mock(personasApi.list).mockResolvedValue(personas);
}

const renderPage = () => render(<ToastProvider><UsuariosPage /></ToastProvider>);

describe('UsuariosPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('lista usuarios', async () => {
    setAll();
    renderPage();
    expect(await screen.findByText('mateo')).toBeInTheDocument();
    expect(screen.getByText('ana')).toBeInTheDocument();
  });

  it('estado vacío', async () => {
    mock(usuariosApi.list).mockResolvedValue([]);
    mock(personasApi.list).mockResolvedValue([]);
    renderPage();
    expect(await screen.findByText('Sin usuarios')).toBeInTheDocument();
  });

  it('filtra por término de búsqueda', async () => {
    setAll();
    renderPage();
    await screen.findByText('mateo');
    await userEvent.type(screen.getByLabelText('Buscar usuarios'), 'ana');
    expect(screen.queryByText('mateo')).not.toBeInTheDocument();
    expect(screen.getByText('ana')).toBeInTheDocument();
  });

  it('valida datos inválidos al crear', async () => {
    setAll();
    renderPage();
    await screen.findByText('mateo');
    await userEvent.click(screen.getByRole('button', { name: /Nuevo usuario/ }));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(usuariosApi.create).not.toHaveBeenCalled();
  });

  it('crea un usuario válido', async () => {
    setAll();
    mock(usuariosApi.create).mockResolvedValue({});
    renderPage();
    await screen.findByText('mateo');
    await userEvent.click(screen.getByRole('button', { name: /Nuevo usuario/ }));
    await userEvent.selectOptions(screen.getByLabelText(/Persona/), 'p3');
    fireEvent.change(screen.getByLabelText(/^Usuario/), { target: { value: 'luisg' } });
    fireEvent.change(screen.getByLabelText(/Contraseña/), { target: { value: 'Abc123' } });
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await waitFor(() => expect(usuariosApi.create).toHaveBeenCalledWith({
      idPersona: 'p3', username: 'luisg', password: 'Abc123',
    }));
  });

  it('edita un usuario existente', async () => {
    setAll();
    mock(usuariosApi.update).mockResolvedValue({});
    renderPage();
    const row = (await screen.findByText('mateo')).closest('tr')!;
    await userEvent.click(within(row).getByRole('button', { name: 'Editar' }));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await waitFor(() => expect(usuariosApi.update).toHaveBeenCalledWith('u1', {
      idPersona: 'p1', username: 'mateo', password: undefined,
    }));
  });

  it('activa y desactiva', async () => {
    setAll();
    mock(usuariosApi.desactivar).mockResolvedValue({});
    mock(usuariosApi.activar).mockResolvedValue({});
    renderPage();
    const rowM = (await screen.findByText('mateo')).closest('tr')!;
    await userEvent.click(within(rowM).getByRole('button', { name: 'Desactivar' }));
    await waitFor(() => expect(usuariosApi.desactivar).toHaveBeenCalledWith('u1'));
    const rowA = screen.getByText('ana').closest('tr')!;
    await userEvent.click(within(rowA).getByRole('button', { name: 'Activar' }));
    await waitFor(() => expect(usuariosApi.activar).toHaveBeenCalledWith('u2'));
  });
});
