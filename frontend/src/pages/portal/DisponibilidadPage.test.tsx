import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DisponibilidadPage } from './DisponibilidadPage';

vi.mock('@/api/zonas', () => ({
  espaciosApi: { disponibles: vi.fn() },
}));
import { espaciosApi } from '@/api/zonas';

const espacios = [
  { id: 'e1', codigo: 'A-01', tipo: 'AUTO', nombreZona: 'Zona A' },
  { id: 'e2', codigo: 'M-01', tipo: 'MOTO', nombreZona: 'Zona B' },
  { id: 'e3', codigo: 'A-02', tipo: 'AUTO' },
];

describe('DisponibilidadPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('muestra espacios y conteos por tipo', async () => {
    (espaciosApi.disponibles as ReturnType<typeof vi.fn>).mockResolvedValue(espacios);
    render(<DisponibilidadPage />);
    expect(await screen.findByText('A-01')).toBeInTheDocument();
    expect(screen.getByText('M-01')).toBeInTheDocument();
  });

  it('filtra por tipo', async () => {
    (espaciosApi.disponibles as ReturnType<typeof vi.fn>).mockResolvedValue(espacios);
    render(<DisponibilidadPage />);
    await screen.findByText('A-01');
    await userEvent.selectOptions(screen.getByLabelText('Filtrar por tipo'), 'MOTO');
    expect(screen.getByText('M-01')).toBeInTheDocument();
    expect(screen.queryByText('A-01')).not.toBeInTheDocument();
  });

  it('muestra estado vacío al filtrar sin coincidencias', async () => {
    (espaciosApi.disponibles as ReturnType<typeof vi.fn>).mockResolvedValue(espacios);
    render(<DisponibilidadPage />);
    await screen.findByText('A-01');
    await userEvent.selectOptions(screen.getByLabelText('Filtrar por tipo'), 'BUSETA');
    expect(screen.getByText('Sin espacios disponibles')).toBeInTheDocument();
  });

  it('refresca al hacer clic', async () => {
    (espaciosApi.disponibles as ReturnType<typeof vi.fn>).mockResolvedValue(espacios);
    render(<DisponibilidadPage />);
    await screen.findByText('A-01');
    await userEvent.click(screen.getByRole('button', { name: 'Refrescar' }));
    await waitFor(() => expect(espaciosApi.disponibles).toHaveBeenCalledTimes(2));
  });

  it('muestra error al fallar', async () => {
    (espaciosApi.disponibles as ReturnType<typeof vi.fn>).mockRejectedValue(new Error('x'));
    render(<DisponibilidadPage />);
    await waitFor(() => expect(screen.getByText('Ocurrió un error')).toBeInTheDocument());
  });
});
