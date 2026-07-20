import { describe, expect, it } from 'vitest';
import { esCedulaEc, fmtDinero, fmtFecha, iniciales, rgx } from './format';

describe('fmtFecha', () => {
  it('devuelve — para valores vacíos o nulos', () => {
    expect(fmtFecha(undefined)).toBe('—');
    expect(fmtFecha(null)).toBe('—');
    expect(fmtFecha('')).toBe('—');
  });

  it('devuelve el string original si la fecha es inválida', () => {
    expect(fmtFecha('no-es-una-fecha')).toBe('no-es-una-fecha');
  });

  it('formatea una fecha ISO válida sin lanzar error', () => {
    const out = fmtFecha('2026-07-20T12:00:00.000Z');
    expect(out).not.toBe('—');
    expect(out.length).toBeGreaterThan(0);
  });
});

describe('fmtDinero', () => {
  it('devuelve — para null/undefined', () => {
    expect(fmtDinero(null)).toBe('—');
    expect(fmtDinero(undefined)).toBe('—');
  });

  it('formatea 0 como moneda válida, no como vacío (bug común: 0 es falsy)', () => {
    expect(fmtDinero(0)).not.toBe('—');
  });

  it('formatea un valor positivo con símbolo de moneda ($ y separador decimal es-EC)', () => {
    expect(fmtDinero(0.9)).toContain('0,90');
    expect(fmtDinero(0.9)).toContain('$');
  });
});

describe('iniciales', () => {
  it('devuelve ?? si no hay nombre', () => {
    expect(iniciales(undefined)).toBe('??');
    expect(iniciales('')).toBe('??');
  });

  it('toma la primera letra de nombre y apellido', () => {
    expect(iniciales('Mateo Criollo')).toBe('MC');
  });

  it('no revienta con un solo nombre (sin apellido)', () => {
    expect(iniciales('Mateo')).toBe('M');
  });
});

describe('esCedulaEc', () => {
  it('rechaza cédulas que no tienen 10 dígitos', () => {
    expect(esCedulaEc('123')).toBe(false);
    expect(esCedulaEc('12345678901')).toBe(false);
  });

  it('rechaza código de provincia fuera de 01-24', () => {
    expect(esCedulaEc('9900012345')).toBe(false);
    expect(esCedulaEc('0000012345')).toBe(false);
  });

  it('rechaza tercer dígito > 5 (tipo de persona inválido)', () => {
    expect(esCedulaEc('0160012345')).toBe(false);
  });

  it('acepta una cédula ecuatoriana con dígito verificador válido', () => {
    // 0100012244 ya se usa como dato de prueba real en scripts/seed_datos.py
    expect(esCedulaEc('0100012244')).toBe(true);
  });

  it('rechaza si el dígito verificador no coincide', () => {
    expect(esCedulaEc('0100012243')).toBe(false);
  });
});

describe('rgx', () => {
  it('placaAuto exige el formato AAA-1234', () => {
    expect(rgx.placaAuto.test('ABC-1234')).toBe(true);
    expect(rgx.placaAuto.test('ABC1234')).toBe(false);
    expect(rgx.placaAuto.test('AB-1234')).toBe(false);
  });

  it('placaMoto exige el formato AA-123A', () => {
    expect(rgx.placaMoto.test('AB-123C')).toBe(true);
    expect(rgx.placaMoto.test('ABC-1234')).toBe(false);
  });

  it('password exige minúscula, mayúscula y dígito', () => {
    expect(rgx.password.test('Abcdef1')).toBe(true);
    expect(rgx.password.test('abcdefg')).toBe(false);
    expect(rgx.password.test('ABCDEFG1')).toBe(false);
  });

  it('email valida formato básico usuario@dominio', () => {
    expect(rgx.email.test('qa.admin@parqueadero.test')).toBe(true);
    expect(rgx.email.test('no-es-email')).toBe(false);
  });
});
