import { describe, expect, it } from 'vitest';
import { esStaff, permisosDe, puede, rutaInicial } from './rbac';

describe('permisosDe', () => {
  it('une los permisos de todos los roles del usuario sin duplicados', () => {
    const permisos = permisosDe(['RECAUDADOR', 'CLIENTE']);
    expect(permisos.has('tickets:operar')).toBe(true);
    expect(permisos.has('portal:mis-vehiculos')).toBe(true);
    expect(permisos.has('usuarios')).toBe(false);
  });

  it('devuelve un set vacío si el rol no existe en la matriz', () => {
    // @ts-expect-error - rol inválido a propósito para probar el fallback ?? []
    const permisos = permisosDe(['ROL_INEXISTENTE']);
    expect(permisos.size).toBe(0);
  });
});

describe('puede', () => {
  it('RECAUDADOR puede operar tickets pero no gestionar usuarios', () => {
    expect(puede(['RECAUDADOR'], 'tickets:operar')).toBe(true);
    expect(puede(['RECAUDADOR'], 'usuarios')).toBe(false);
  });

  it('CLIENTE no puede acceder a nada del panel admin', () => {
    expect(puede(['CLIENTE'], 'dashboard')).toBe(false);
    expect(puede(['CLIENTE'], 'tickets:operar')).toBe(false);
  });

  it('INVITADO solo puede ver disponibilidad', () => {
    expect(puede(['INVITADO'], 'portal:disponibilidad')).toBe(true);
    expect(puede(['INVITADO'], 'portal:perfil')).toBe(false);
  });

  it('ROOT y ADMIN tienen acceso total al panel admin', () => {
    for (const rol of ['ROOT', 'ADMIN'] as const) {
      expect(puede([rol], 'usuarios')).toBe(true);
      expect(puede([rol], 'auditoria')).toBe(true);
      expect(puede([rol], 'configuracion')).toBe(true);
    }
  });
});

describe('esStaff', () => {
  it('ROOT, ADMIN y RECAUDADOR son staff', () => {
    expect(esStaff(['ROOT'])).toBe(true);
    expect(esStaff(['ADMIN'])).toBe(true);
    expect(esStaff(['RECAUDADOR'])).toBe(true);
  });

  it('CLIENTE e INVITADO no son staff', () => {
    expect(esStaff(['CLIENTE'])).toBe(false);
    expect(esStaff(['INVITADO'])).toBe(false);
  });
});

describe('rutaInicial', () => {
  it('ADMIN/ROOT aterrizan en el dashboard del panel', () => {
    expect(rutaInicial(['ADMIN'])).toBe('/app');
    expect(rutaInicial(['ROOT'])).toBe('/app');
  });

  it('RECAUDADOR (sin permiso de dashboard) aterriza directo en Tickets', () => {
    expect(rutaInicial(['RECAUDADOR'])).toBe('/app/tickets');
  });

  it('CLIENTE e INVITADO aterrizan en el portal', () => {
    expect(rutaInicial(['CLIENTE'])).toBe('/portal');
    expect(rutaInicial(['INVITADO'])).toBe('/portal');
  });
});
