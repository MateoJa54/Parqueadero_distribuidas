import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { RolesPage } from './RolesPage';
import { ToastProvider } from '@/ui/ToastProvider';

vi.mock('@/api/usuarios', () => ({
  rolesApi: {
    list: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    activar: vi.fn(),
    desactivar: vi.fn(),
  },
}));
import { rolesApi } from '@/api/usuarios';

const mock = (fn: unknown) => fn as ReturnType<typeof vi.fn>;
const roles = [
  { id: 'r1', name: 'ADMIN', description: 'Administrador', active: true },
  { id: 'r2', name: 'CLIENTE', description: null, active: false },
];

const renderPage = () => render(<ToastProvider><RolesPage /></ToastProvider>);

describe('RolesPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('lista roles', async () => {
    mock(rolesApi.list).mockResolvedValue(roles);
    renderPage();
    expect(await screen.findByText('ADMIN')).toBeInTheDocument();
    expect(screen.getByText('CLIENTE')).toBeInTheDocument();
  });

  it('estado vacío', async () => {
    mock(rolesApi.list).mockResolvedValue([]);
    renderPage();
    expect(await screen.findByText('Sin roles')).toBeInTheDocument();
  });

  it('estado de error con reintento', async () => {
    mock(rolesApi.list).mockRejectedValueOnce(new Error('x')).mockResolvedValueOnce(roles);
    renderPage();
    await screen.findByText('Ocurrió un error');
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }));
    expect(await screen.findByText('ADMIN')).toBeInTheDocument();
  });

  it('valida nombre inválido al crear', async () => {
    mock(rolesApi.list).mockResolvedValue(roles);
    renderPage();
    await screen.findByText('ADMIN');
    await userEvent.click(screen.getByRole('button', { name: /Nuevo rol/ }));
    fireEvent.change(screen.getByLabelText(/Nombre del rol/), { target: { value: 'AB' } });
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    expect(rolesApi.create).not.toHaveBeenCalled();
  });

  it('crea un rol válido', async () => {
    mock(rolesApi.list).mockResolvedValue(roles);
    mock(rolesApi.create).mockResolvedValue({});
    renderPage();
    await screen.findByText('ADMIN');
    await userEvent.click(screen.getByRole('button', { name: /Nuevo rol/ }));
    fireEvent.change(screen.getByLabelText(/Nombre del rol/), { target: { value: 'RECAUDADOR' } });
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await waitFor(() =>
      expect(rolesApi.create).toHaveBeenCalledWith({ name: 'RECAUDADOR', description: '' }),
    );
  });

  it('edita un rol existente', async () => {
    mock(rolesApi.list).mockResolvedValue(roles);
    mock(rolesApi.update).mockResolvedValue({});
    renderPage();
    const row = (await screen.findByText('ADMIN')).closest('tr')!;
    await userEvent.click(within(row).getByRole('button', { name: 'Editar' }));
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await waitFor(() => expect(rolesApi.update).toHaveBeenCalledWith('r1', {
      name: 'ADMIN', description: 'Administrador',
    }));
  });

  it('activa y desactiva', async () => {
    mock(rolesApi.list).mockResolvedValue(roles);
    mock(rolesApi.desactivar).mockResolvedValue({});
    mock(rolesApi.activar).mockResolvedValue({});
    renderPage();
    const rowA = (await screen.findByText('ADMIN')).closest('tr')!;
    await userEvent.click(within(rowA).getByRole('button', { name: 'Desactivar' }));
    await waitFor(() => expect(rolesApi.desactivar).toHaveBeenCalledWith('r1'));
    const rowC = screen.getByText('CLIENTE').closest('tr')!;
    await userEvent.click(within(rowC).getByRole('button', { name: 'Activar' }));
    await waitFor(() => expect(rolesApi.activar).toHaveBeenCalledWith('r2'));
  });
});
