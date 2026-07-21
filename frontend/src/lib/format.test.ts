import { describe, it, expect } from 'vitest';
import { fmtFecha, fmtDinero, iniciales, esCedulaEc, rgx } from './format';

describe('fmtFecha', () => {
  it('devuelve — para valores nulos/vacíos', () => {
    expect(fmtFecha()).toBe('—');
    expect(fmtFecha(null)).toBe('—');
    expect(fmtFecha('')).toBe('—');
  });

  it('devuelve la cadena original si la fecha es inválida', () => {
    expect(fmtFecha('no-es-fecha')).toBe('no-es-fecha');
  });

  it('formatea una fecha ISO válida', () => {
    const out = fmtFecha('2026-01-15T10:30:00Z');
    expect(out).not.toBe('—');
    expect(typeof out).toBe('string');
  });
});

describe('fmtDinero', () => {
  it('devuelve — para null/undefined', () => {
    expect(fmtDinero()).toBe('—');
    expect(fmtDinero(null)).toBe('—');
  });

  it('formatea 0 como moneda (no como —)', () => {
    expect(fmtDinero(0)).not.toBe('—');
  });

  it('incluye el monto en el resultado', () => {
    expect(fmtDinero(12.5)).toMatch(/12/);
  });
});

describe('iniciales', () => {
  it('devuelve ?? sin nombre', () => {
    expect(iniciales()).toBe('??');
    expect(iniciales('')).toBe('??');
  });

  it('toma la primera letra de las dos primeras palabras', () => {
    expect(iniciales('Mateo Jaramillo')).toBe('MJ');
  });

  it('con una sola palabra toma solo la primera letra', () => {
    expect(iniciales('Mateo')).toBe('M');
  });

  it('ignora espacios extra', () => {
    expect(iniciales('  Ana   Perez ')).toBe('AP');
  });
});

describe('esCedulaEc', () => {
  it('rechaza longitud incorrecta o no numérica', () => {
    expect(esCedulaEc('123')).toBe(false);
    expect(esCedulaEc('abcdefghij')).toBe(false);
    expect(esCedulaEc('12345678901')).toBe(false);
  });

  it('rechaza provincia inválida', () => {
    expect(esCedulaEc('9912345678')).toBe(false);
    expect(esCedulaEc('0012345678')).toBe(false);
  });

  it('rechaza tercer dígito > 5', () => {
    expect(esCedulaEc('0161234567')).toBe(false);
  });

  it('acepta una cédula válida y rechaza una con verificador incorrecto', () => {
    expect(esCedulaEc('1710034065')).toBe(true);
    expect(esCedulaEc('1710034066')).toBe(false);
  });
});

describe('rgx', () => {
  it('username permite alfanumérico y ._-', () => {
    expect(rgx.username.test('user.name_1-2')).toBe(true);
    expect(rgx.username.test('mal usuario')).toBe(false);
  });

  it('password requiere minúscula, mayúscula y dígito', () => {
    expect(rgx.password.test('Abc12345')).toBe(true);
    expect(rgx.password.test('todominuscula1')).toBe(false);
    expect(rgx.password.test('SINDIGITOS')).toBe(false);
  });

  it('email valida formato básico', () => {
    expect(rgx.email.test('a@b.co')).toBe(true);
    expect(rgx.email.test('sin-arroba.com')).toBe(false);
  });

  it('placaAuto y placaMoto', () => {
    expect(rgx.placaAuto.test('ABC-1234')).toBe(true);
    expect(rgx.placaAuto.test('AB-1234')).toBe(false);
    expect(rgx.placaMoto.test('AB-123C')).toBe(true);
  });

  it('telefono acepta 7 a 10 dígitos', () => {
    expect(rgx.telefono.test('0999999')).toBe(true);
    expect(rgx.telefono.test('123')).toBe(false);
  });
});
