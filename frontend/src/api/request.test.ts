import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { request, http, tokenStore, setUnauthorizedHandler, ApiError } from './client';

function makeJwt(claims: Record<string, unknown>): string {
  const b64url = (obj: unknown) => {
    const bytes = new TextEncoder().encode(JSON.stringify(obj));
    return btoa(String.fromCharCode(...bytes)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  };
  return `${b64url({ alg: 'HS256' })}.${b64url(claims)}.sig`;
}

const validToken = () => makeJwt({ sub: 'x', exp: Math.floor(Date.now() / 1000) + 10000 });

function mockFetch(resp: { status?: number; body?: unknown; ok?: boolean }) {
  const status = resp.status ?? 200;
  const text = resp.body === undefined ? '' : typeof resp.body === 'string' ? resp.body : JSON.stringify(resp.body);
  return vi.fn((_url?: string, _init?: RequestInit) =>
    Promise.resolve({
      status,
      ok: resp.ok ?? (status >= 200 && status < 300),
      text: () => Promise.resolve(text),
      json: () => Promise.resolve(resp.body),
    } as Response),
  );
}

describe('request', () => {
  beforeEach(() => {
    localStorage.clear();
    setUnauthorizedHandler(() => {});
  });
  afterEach(() => vi.restoreAllMocks());

  it('parsea JSON en respuesta OK', async () => {
    vi.stubGlobal('fetch', mockFetch({ body: { a: 1 } }));
    const res = await request<{ a: number }>('http://x', { auth: false });
    expect(res).toEqual({ a: 1 });
  });

  it('devuelve undefined en 204', async () => {
    vi.stubGlobal('fetch', mockFetch({ status: 204 }));
    const res = await request('http://x', { auth: false });
    expect(res).toBeUndefined();
  });

  it('adjunta Authorization Bearer con token válido', async () => {
    tokenStore.set(validToken(), 'r');
    const f = mockFetch({ body: {} });
    vi.stubGlobal('fetch', f);
    await request('http://x');
    const opts = f.mock.calls[0][1] as RequestInit;
    expect((opts.headers as Record<string, string>).Authorization).toContain('Bearer ');
  });

  it('lanza ApiError con mensaje del payload', async () => {
    vi.stubGlobal('fetch', mockFetch({ status: 400, body: { mensaje: 'malo' } }));
    await expect(request('http://x', { auth: false })).rejects.toMatchObject({ status: 400, message: 'malo' });
  });

  it('usa mensaje por defecto por status', async () => {
    vi.stubGlobal('fetch', mockFetch({ status: 404, body: '' }));
    await expect(request('http://x', { auth: false })).rejects.toThrow('Recurso no encontrado.');
  });

  it('extrae errores de campo', async () => {
    vi.stubGlobal('fetch', mockFetch({ status: 400, body: { errores: { a: 'x', b: 'y' } } }));
    await expect(request('http://x', { auth: false })).rejects.toThrow('x · y');
  });

  it('401 sin refresh dispara handler y lanza', async () => {
    const handler = vi.fn();
    setUnauthorizedHandler(handler);
    vi.stubGlobal('fetch', mockFetch({ status: 401 }));
    await expect(request('http://x')).rejects.toBeInstanceOf(ApiError);
    expect(handler).toHaveBeenCalled();
  });

  it('http.post envía body serializado', async () => {
    const f = mockFetch({ body: {} });
    vi.stubGlobal('fetch', f);
    await http.post('http://x', { n: 1 }, { auth: false });
    const opts = f.mock.calls[0][1] as RequestInit;
    expect(opts.method).toBe('POST');
    expect(opts.body).toBe(JSON.stringify({ n: 1 }));
  });
});
