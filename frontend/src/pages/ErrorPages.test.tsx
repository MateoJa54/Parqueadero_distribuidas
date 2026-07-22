import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ForbiddenPage, NotFoundPage } from './ErrorPages';

describe('ErrorPages', () => {
  it('ForbiddenPage muestra acceso denegado y enlace de inicio', () => {
    render(<MemoryRouter><ForbiddenPage /></MemoryRouter>);
    expect(screen.getByText('Acceso denegado')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Volver al inicio' })).toHaveAttribute('href', '/');
  });

  it('NotFoundPage muestra página no encontrada', () => {
    render(<MemoryRouter><NotFoundPage /></MemoryRouter>);
    expect(screen.getByText('Página no encontrada')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Volver al inicio' })).toBeInTheDocument();
  });
});
