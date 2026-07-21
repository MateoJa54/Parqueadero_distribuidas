import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('./client', () => ({
  API: { zonas: 'http://zn' },
  http: {
    get: vi.fn(() => Promise.resolve('ok')),
    post: vi.fn(() => Promise.resolve('ok')),
    put: vi.fn(() => Promise.resolve('ok')),
    patch: vi.fn(() => Promise.resolve('ok')),
  },
}));

import { http } from './client';
import { zonasApi, espaciosApi } from './zonas';

const h = http as unknown as Record<string, ReturnType<typeof vi.fn>>;

describe('zonasApi', () => {
  beforeEach(() => vi.clearAllMocks());
  it('list', async () => { await zonasApi.list(); expect(h.get).toHaveBeenCalledWith('http://zn/zonas'); });
  it('get', async () => { await zonasApi.get('1'); expect(h.get).toHaveBeenCalledWith('http://zn/zonas/1'); });
  it('create', async () => { const b = {} as never; await zonasApi.create(b); expect(h.post).toHaveBeenCalledWith('http://zn/zonas', b); });
  it('update', async () => { const b = {} as never; await zonasApi.update('1', b); expect(h.put).toHaveBeenCalledWith('http://zn/zonas/1', b); });
  it('activar', async () => { await zonasApi.activar('1'); expect(h.patch).toHaveBeenCalledWith('http://zn/zonas/1/activar'); });
  it('desactivar', async () => { await zonasApi.desactivar('1'); expect(h.patch).toHaveBeenCalledWith('http://zn/zonas/1/desactivar'); });
});

describe('espaciosApi', () => {
  beforeEach(() => vi.clearAllMocks());
  it('list', async () => { await espaciosApi.list(); expect(h.get).toHaveBeenCalledWith('http://zn/espacios'); });
  it('get', async () => { await espaciosApi.get('1'); expect(h.get).toHaveBeenCalledWith('http://zn/espacios/1'); });
  it('create', async () => { const b = {} as never; await espaciosApi.create(b); expect(h.post).toHaveBeenCalledWith('http://zn/espacios', b); });
  it('update', async () => { const b = {} as never; await espaciosApi.update('1', b); expect(h.put).toHaveBeenCalledWith('http://zn/espacios/1', b); });
  it('cambiarEstado usa query', async () => {
    await espaciosApi.cambiarEstado('1', 'LIBRE' as never);
    expect(h.patch).toHaveBeenCalledWith('http://zn/espacios/1/estado?estado=LIBRE');
  });
  it('porEstado', async () => {
    await espaciosApi.porEstado('LIBRE' as never);
    expect(h.get).toHaveBeenCalledWith('http://zn/espacios/estado/LIBRE');
  });
  it('disponibles sin params', async () => {
    await espaciosApi.disponibles();
    expect(h.get).toHaveBeenCalledWith('http://zn/espacios/disponibles');
  });
  it('disponibles con params', async () => {
    await espaciosApi.disponibles({ idZona: 'z1', tipo: 'AUTO' as never });
    expect(h.get).toHaveBeenCalledWith('http://zn/espacios/disponibles?idZona=z1&tipo=AUTO');
  });
  it('disponibilidad', async () => {
    await espaciosApi.disponibilidad('1');
    expect(h.get).toHaveBeenCalledWith('http://zn/espacios/1/disponibilidad');
  });
  it('activar', async () => { await espaciosApi.activar('1'); expect(h.patch).toHaveBeenCalledWith('http://zn/espacios/1/activar'); });
  it('desactivar', async () => { await espaciosApi.desactivar('1'); expect(h.patch).toHaveBeenCalledWith('http://zn/espacios/1/desactivar'); });
});
