import { describe, it, expect } from 'vitest';
import { permisosDe, puede, esStaff, rutaInicial, MATRIZ } from './rbac';

describe('puede', () => {
  it('ROOT tiene todos los permisos definidos en su matriz', () => {
    for (const p of MATRIZ.ROOT) {
      expect(puede(['ROOT'], p)).toBe(true);
    }
  });

  it('CLIENTE no puede acceder a usuarios', () => {
    expect(puede(['CLIENTE'], 'usuarios')).toBe(false);
  });

  it('combina permisos de múltiples roles', () => {
    expect(puede(['CLIENTE', 'RECAUDADOR'], 'tickets:operar')).toBe(true);
  });

  it('rol desconocido no otorga permisos', () => {
    // @ts-expect-error probando rol inexistente
    expect(puede(['DESCONOCIDO'], 'dashboard')).toBe(false);
  });
});

describe('permisosDe', () => {
  it('unifica sin duplicados', () => {
    const set = permisosDe(['CLIENTE', 'INVITADO']);
    expect(set.has('portal:disponibilidad')).toBe(true);
    expect(set.has('portal:perfil')).toBe(true);
  });

  it('roles vacíos → set vacío', () => {
    expect(permisosDe([]).size).toBe(0);
  });
});

describe('esStaff', () => {
  it('ROOT/ADMIN/RECAUDADOR son staff', () => {
    expect(esStaff(['ROOT'])).toBe(true);
    expect(esStaff(['ADMIN'])).toBe(true);
    expect(esStaff(['RECAUDADOR'])).toBe(true);
  });

  it('CLIENTE/INVITADO no son staff', () => {
    expect(esStaff(['CLIENTE'])).toBe(false);
    expect(esStaff(['INVITADO'])).toBe(false);
  });
});

describe('rutaInicial', () => {
  it('con dashboard → /app', () => {
    expect(rutaInicial(['ADMIN'])).toBe('/app');
  });

  it('staff sin dashboard (RECAUDADOR) → /app/tickets', () => {
    expect(rutaInicial(['RECAUDADOR'])).toBe('/app/tickets');
  });

  it('cliente → /portal', () => {
    expect(rutaInicial(['CLIENTE'])).toBe('/portal');
    expect(rutaInicial(['INVITADO'])).toBe('/portal');
  });
});
