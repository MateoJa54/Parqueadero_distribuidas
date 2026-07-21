import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import {
  Badge,
  EstadoEspacioBadge,
  EstadoTicketBadge,
  ActivoBadge,
  EstadoAsignacionBadge,
} from './Badge';

describe('Badge', () => {
  it('usa tono neutral por defecto', () => {
    render(<Badge>hola</Badge>);
    expect(screen.getByText('hola').className).toContain('badge-neutral');
  });

  it('aplica el tono indicado', () => {
    render(<Badge tone="success">ok</Badge>);
    expect(screen.getByText('ok').className).toContain('badge-success');
  });
});

describe('EstadoEspacioBadge', () => {
  it('mapea DISPONIBLE a success', () => {
    render(<EstadoEspacioBadge estado="DISPONIBLE" />);
    expect(screen.getByText('DISPONIBLE').className).toContain('badge-success');
  });

  it('estado desconocido cae en neutral pero muestra el texto', () => {
    render(<EstadoEspacioBadge estado="RARO" />);
    expect(screen.getByText('RARO').className).toContain('badge-neutral');
  });
});

describe('EstadoTicketBadge', () => {
  it('PAGADO → success', () => {
    render(<EstadoTicketBadge estado="PAGADO" />);
    expect(screen.getByText('PAGADO').className).toContain('badge-success');
  });
});

describe('ActivoBadge', () => {
  it('activo muestra Activo', () => {
    render(<ActivoBadge activo />);
    expect(screen.getByText('Activo').className).toContain('badge-success');
  });

  it('inactivo muestra Inactivo', () => {
    render(<ActivoBadge activo={false} />);
    expect(screen.getByText('Inactivo')).toBeInTheDocument();
  });
});

describe('EstadoAsignacionBadge', () => {
  it('ACTIVA → success', () => {
    render(<EstadoAsignacionBadge estado="ACTIVA" />);
    expect(screen.getByText('ACTIVA').className).toContain('badge-success');
  });
});
