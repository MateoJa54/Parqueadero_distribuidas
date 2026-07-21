import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('./client', () => ({
  API: { usuarios: 'http://usr' },
  http: {
    get: vi.fn(() => Promise.resolve('ok')),
    post: vi.fn(() => Promise.resolve('ok')),
  },
  tokenStore: { clear: vi.fn() },
}));

import { http, tokenStore } from './client';
import { authApi } from './auth';

const h = http as unknown as Record<string, ReturnType<typeof vi.fn>>;
const clear = tokenStore.clear as unknown as ReturnType<typeof vi.fn>;

describe('authApi', () => {
  beforeEach(() => vi.clearAllMocks());

  it('login sin auth', async () => {
    const body = { username: 'a', password: 'b' } as never;
    await authApi.login(body);
    expect(h.post).toHaveBeenCalledWith('http://usr/auth/login', body, { auth: false });
  });

  it('registrarCliente sin auth', async () => {
    const body = { username: 'a' } as never;
    await authApi.registrarCliente(body);
    expect(h.post).toHaveBeenCalledWith('http://usr/auth/registro-cliente', body, { auth: false });
  });

  it('registrarCompleto sin auth', async () => {
    const body = { username: 'a' } as never;
    await authApi.registrarCompleto(body);
    expect(h.post).toHaveBeenCalledWith('http://usr/auth/registro-completo', body, { auth: false });
  });

  it('refresh envía refreshToken', async () => {
    await authApi.refresh('rt');
    expect(h.post).toHaveBeenCalledWith('http://usr/auth/refresh', { refreshToken: 'rt' }, { auth: false });
  });

  it('me obtiene persona', async () => {
    await authApi.me();
    expect(h.get).toHaveBeenCalledWith('http://usr/auth/me');
  });

  it('logout limpia tokenStore', () => {
    authApi.logout();
    expect(clear).toHaveBeenCalled();
  });
});
