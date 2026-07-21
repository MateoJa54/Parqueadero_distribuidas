import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PerfilPage } from './PerfilPage';
import { AuthContext, type AuthContextValue } from '@/auth/context';
import { ToastProvider } from '@/ui/ToastProvider';

vi.mock('@/api/auth', () => ({ authApi: { me: vi.fn() } }));
vi.mock('@/api/usuarios', () => ({ usuariosApi: { update: vi.fn() } }));
import { authApi } from '@/api/auth';
import { usuariosApi } from '@/api/usuarios';

const persona = {
  id: 'p1', firstName: 'Mateo', middleName: 'J', lastName: 'Iza',
  dni: '0102030405', email: 'm@x.com', phone: '099', nationality: 'EC',
};

function renderPage() {
  const value = {
    user: { idUsuario: 'u1', username: 'mateo', roles: ['CLIENTE'] as never },
    loading: false,
    login: vi.fn(), registrarCliente: vi.fn(), registrarCompleto: vi.fn(),
    logout: vi.fn(), hasRole: vi.fn(),
  } as AuthContextValue;
  return render(
    <AuthContext.Provider value={value}>
      <ToastProvider><PerfilPage /></ToastProvider>
    </AuthContext.Provider>,
  );
}

describe('PerfilPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('carga y muestra los datos personales', async () => {
    (authApi.me as ReturnType<typeof vi.fn>).mockResolvedValue(persona);
    renderPage();
    expect(await screen.findByText('0102030405')).toBeInTheDocument();
    expect(screen.getByText('m@x.com')).toBeInTheDocument();
  });

  it('muestra error si falla la carga', async () => {
    (authApi.me as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('x'));
    renderPage();
    await waitFor(() => expect(screen.getByText('Ocurrió un error')).toBeInTheDocument());
  });

  it('valida username inválido y no llama update', async () => {
    (authApi.me as ReturnType<typeof vi.fn>).mockResolvedValue(persona);
    renderPage();
    await screen.findByText('0102030405');
    const user = screen.getByLabelText(/Usuario/);
    await userEvent.clear(user);
    await userEvent.type(user, 'ab');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));
    expect(usuariosApi.update).not.toHaveBeenCalled();
  });

  it('guarda cambios válidos', async () => {
    (authApi.me as ReturnType<typeof vi.fn>).mockResolvedValue(persona);
    (usuariosApi.update as ReturnType<typeof vi.fn>).mockResolvedValue({});
    renderPage();
    await screen.findByText('0102030405');
    await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));
    await waitFor(() => expect(usuariosApi.update).toHaveBeenCalledWith('u1', {
      idPersona: 'p1', username: 'mateo', password: undefined,
    }));
    expect(await screen.findByText('Perfil actualizado')).toBeInTheDocument();
  });
});
