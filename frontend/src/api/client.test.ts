import { describe, it, expect, beforeEach } from 'vitest';
import { decodeJwt, isTokenExpired, ApiError, tokenStore } from './client';

// Construye un JWT de prueba (header.payload.signature) sin firma real.
function makeJwt(claims: Record<string, unknown>): string {
  const b64url = (obj: unknown) => {
    const bytes = new TextEncoder().encode(JSON.stringify(obj));
    const binary = String.fromCharCode(...bytes);
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  };
  return `${b64url({ alg: 'HS256', typ: 'JWT' })}.${b64url(claims)}.sig`;
}

describe('decodeJwt', () => {
  it('decodifica claims válidos', () => {
    const token = makeJwt({ sub: 'mateo', roles: ['ADMIN'], exp: 1234 });
    const claims = decodeJwt(token);
    expect(claims?.sub).toBe('mateo');
    expect(claims?.exp).toBe(1234);
  });

  it('decodifica correctamente caracteres UTF-8', () => {
    const token = makeJwt({ sub: 'José Ñandú', exp: 1 });
    expect(decodeJwt(token)?.sub).toBe('José Ñandú');
  });

  it('devuelve null ante token malformado', () => {
    expect(decodeJwt('esto-no-es-un-jwt')).toBeNull();
    expect(decodeJwt('a.b.c')).toBeNull();
  });
});

describe('isTokenExpired', () => {
  it('true si no hay exp', () => {
    expect(isTokenExpired(makeJwt({ sub: 'x' }))).toBe(true);
  });

  it('true si ya expiró', () => {
    const past = Math.floor(Date.now() / 1000) - 100;
    expect(isTokenExpired(makeJwt({ exp: past }))).toBe(true);
  });

  it('false si expira lejos en el futuro', () => {
    const future = Math.floor(Date.now() / 1000) + 10000;
    expect(isTokenExpired(makeJwt({ exp: future }))).toBe(false);
  });

  it('considera el skew: expira dentro del margen → true', () => {
    const soon = Math.floor(Date.now() / 1000) + 10;
    expect(isTokenExpired(makeJwt({ exp: soon }), 30)).toBe(true);
  });
});

describe('ApiError', () => {
  it('guarda status, mensaje y body', () => {
    const err = new ApiError(404, 'no encontrado', { detalle: 'x' });
    expect(err).toBeInstanceOf(Error);
    expect(err.name).toBe('ApiError');
    expect(err.status).toBe(404);
    expect(err.message).toBe('no encontrado');
    expect(err.body).toEqual({ detalle: 'x' });
  });
});

describe('tokenStore', () => {
  beforeEach(() => localStorage.clear());

  it('set guarda token y refresh; get los recupera', () => {
    tokenStore.set('tok', 'ref');
    expect(tokenStore.get()).toBe('tok');
    expect(tokenStore.getRefresh()).toBe('ref');
  });

  it('clear elimina ambos', () => {
    tokenStore.set('tok', 'ref');
    tokenStore.clear();
    expect(tokenStore.get()).toBeNull();
    expect(tokenStore.getRefresh()).toBeNull();
  });
});
