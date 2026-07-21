import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AsignacionRolesPage } from './AsignacionRolesPage';
import { ToastProvider } from '@/ui/ToastProvider';

vi.mock('@/api/usuarios', () => ({
  asignacionesRolApi: { list: vi.fn(), asignar: vi.fn(), activar: vi.fn(), desactivar: vi.fn() },
  usuariosApi: { list: vi.fn() },
  rolesApi: { list: vi.fn() },
}));
import { asignacionesRolApi, usuariosApi, rolesApi } from '@/api/usuarios';

const mock = (fn: unknown) => fn as ReturnType<typeof vi.fn>;
const asigns = [
  { idUser: 'u1', idRole: 'r1', username: 'mateo', rol: 'ADMIN', active: true },
  { idUser: 'u2', idRole: 'r2', username: '', rol: 'CLIENTE', active: false },
];
const usuarios = [
  { id: 'u1', username: 'mateo', nombreCompleto: 'Mateo Iza', active: true },
  { id: 'u2', username: 'ana', nombreCompleto: 'Ana P', active: true },
];
const roles = [{ id: 'r1', name: 'ADMIN', active: true }, { id: 'r2', name: 'CLIENTE', active: true }];

function setAll() {
  mock(asignacionesRolApi.list).mockResolvedValue(asigns);
  mock(usuariosApi.list).mockResolvedValue(usuarios);
  mock(rolesApi.list).mockResolvedValue(roles);
}

const renderPage = () => render(<ToastProvider><AsignacionRolesPage /></ToastProvider>);

describe('AsignacionRolesPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('lista asignaciones', async () => {
    setAll();
    renderPage();
    expect(await screen.findByText('mateo')).toBeInTheDocument();
    expect(screen.getByText('ADMIN')).toBeInTheDocument();
    expect(screen.getByText('ana')).toBeInTheDocument();
  });

  it('estado vacío', async () => {
    mock(asignacionesRolApi.list).mockResolvedValue([]);
    mock(usuariosApi.list).mockResolvedValue([]);
    mock(rolesApi.list).mockResolvedValue([]);
    renderPage();
    expect(await screen.findByText('Sin asignaciones')).toBeInTheDocument();
  });

  it('no asigna si faltan datos', async () => {
    setAll();
    renderPage();
    await screen.findByText('mateo');
    await userEvent.click(screen.getByRole('button', { name: /Asignar rol/ }));
    await userEvent.click(screen.getByRole('button', { name: 'Asignar' }));
    expect(asignacionesRolApi.asignar).not.toHaveBeenCalled();
  });

  it('asigna rol seleccionado', async () => {
    setAll();
    mock(asignacionesRolApi.asignar).mockResolvedValue({});
    renderPage();
    await screen.findByText('mateo');
    await userEvent.click(screen.getByRole('button', { name: /Asignar rol/ }));
    await userEvent.selectOptions(screen.getByLabelText(/Usuario/), 'u1');
    await userEvent.selectOptions(screen.getByLabelText(/Rol/), 'r1');
    await userEvent.click(screen.getByRole('button', { name: 'Asignar' }));
    await waitFor(() =>
      expect(asignacionesRolApi.asignar).toHaveBeenCalledWith({ idUser: 'u1', idRole: 'r1' }),
    );
  });

  it('activa y desactiva', async () => {
    setAll();
    mock(asignacionesRolApi.desactivar).mockResolvedValue({});
    mock(asignacionesRolApi.activar).mockResolvedValue({});
    renderPage();
    const rowA = (await screen.findByText('mateo')).closest('tr')!;
    await userEvent.click(within(rowA).getByRole('button', { name: 'Desactivar' }));
    await waitFor(() => expect(asignacionesRolApi.desactivar).toHaveBeenCalledWith('u1', 'r1'));
    const rowB = screen.getByText('ana').closest('tr')!;
    await userEvent.click(within(rowB).getByRole('button', { name: 'Activar' }));
    await waitFor(() => expect(asignacionesRolApi.activar).toHaveBeenCalledWith('u2', 'r2'));
  });
});
