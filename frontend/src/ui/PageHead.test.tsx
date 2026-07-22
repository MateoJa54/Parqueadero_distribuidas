import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { PageHead } from './PageHead';

describe('PageHead', () => {
  it('renderiza solo el título', () => {
    render(<PageHead title="Titulo" />);
    expect(screen.getByRole('heading', { name: 'Titulo' })).toBeInTheDocument();
    expect(screen.queryByText('sub')).not.toBeInTheDocument();
  });

  it('renderiza subtítulo y acciones', () => {
    render(<PageHead title="T" subtitle="sub" actions={<button type="button">Accion</button>} />);
    expect(screen.getByText('sub')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Accion' })).toBeInTheDocument();
  });
});
