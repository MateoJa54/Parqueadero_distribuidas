import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AsyncView, Loading, TableSkeleton, EmptyState, ErrorState } from './States';

describe('Loading', () => {
  it('usa el label por defecto', () => {
    render(<Loading />);
    expect(screen.getByText('Cargando…')).toBeInTheDocument();
  });
  it('acepta un label personalizado', () => {
    render(<Loading label="Espere" />);
    expect(screen.getByText('Espere')).toBeInTheDocument();
  });
});

describe('TableSkeleton', () => {
  it('renderiza rows*cols skeletons', () => {
    const { container } = render(<TableSkeleton cols={3} rows={2} />);
    expect(container.querySelectorAll('.skeleton')).toHaveLength(6);
  });
});

describe('EmptyState', () => {
  it('muestra título, mensaje y acción', () => {
    render(<EmptyState title="Vacío" message="nada" action={<button>Crear</button>} />);
    expect(screen.getByText('Vacío')).toBeInTheDocument();
    expect(screen.getByText('nada')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Crear' })).toBeInTheDocument();
  });
});

describe('ErrorState', () => {
  it('muestra el mensaje sin botón si no hay onRetry', () => {
    render(<ErrorState message="boom" />);
    expect(screen.getByText('boom')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /reintentar/i })).not.toBeInTheDocument();
  });
  it('invoca onRetry al hacer clic', async () => {
    const onRetry = vi.fn();
    render(<ErrorState message="boom" onRetry={onRetry} />);
    await userEvent.click(screen.getByRole('button', { name: /reintentar/i }));
    expect(onRetry).toHaveBeenCalledOnce();
  });
});

describe('AsyncView defaults', () => {
  it('usa Loading por defecto cuando no hay loadingNode', () => {
    render(<AsyncView loading><div>x</div></AsyncView>);
    expect(screen.getByText('Cargando…')).toBeInTheDocument();
  });
  it('usa EmptyState por defecto cuando no hay emptyNode', () => {
    render(<AsyncView loading={false} isEmpty><div>x</div></AsyncView>);
    expect(screen.getByText('Sin registros')).toBeInTheDocument();
  });
});

describe('AsyncView', () => {
  it('muestra el loadingNode cuando loading=true', () => {
    render(
      <AsyncView loading loadingNode={<div>cargando-custom</div>}>
        <div>contenido</div>
      </AsyncView>,
    );
    expect(screen.getByText('cargando-custom')).toBeInTheDocument();
    expect(screen.queryByText('contenido')).not.toBeInTheDocument();
  });

  it('muestra estado de error y permite reintentar', async () => {
    const onRetry = vi.fn();
    render(
      <AsyncView loading={false} error="algo falló" onRetry={onRetry}>
        <div>contenido</div>
      </AsyncView>,
    );
    expect(screen.getByText('algo falló')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /reintentar/i }));
    expect(onRetry).toHaveBeenCalledOnce();
  });

  it('muestra emptyNode cuando isEmpty=true', () => {
    render(
      <AsyncView loading={false} isEmpty emptyNode={<div>vacio-custom</div>}>
        <div>contenido</div>
      </AsyncView>,
    );
    expect(screen.getByText('vacio-custom')).toBeInTheDocument();
  });

  it('renderiza children cuando no hay loading/error/empty', () => {
    render(
      <AsyncView loading={false} error={null} isEmpty={false}>
        <div>contenido</div>
      </AsyncView>,
    );
    expect(screen.getByText('contenido')).toBeInTheDocument();
  });

  it('prioriza loading sobre error y empty', () => {
    render(
      <AsyncView loading error="err" isEmpty>
        <div>contenido</div>
      </AsyncView>,
    );
    expect(screen.queryByText('err')).not.toBeInTheDocument();
  });
});
