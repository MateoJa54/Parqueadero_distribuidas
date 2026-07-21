import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('./client', () => ({
  API: { vehiculos: 'http://veh' },
  http: {
    get: vi.fn(() => Promise.resolve('ok')),
    post: vi.fn(() => Promise.resolve('ok')),
    patch: vi.fn(() => Promise.resolve('ok')),
  },
}));

import { http } from './client';
import { vehiculosApi } from './vehiculos';

const h = http as unknown as Record<string, ReturnType<typeof vi.fn>>;

describe('vehiculosApi', () => {
  beforeEach(() => vi.clearAllMocks());

  it('list por defecto sin inactivos', async () => {
    await vehiculosApi.list();
    expect(h.get).toHaveBeenCalledWith('http://veh?incluirInactivos=false');
  });

  it('list incluyendo inactivos', async () => {
    await vehiculosApi.list(true);
    expect(h.get).toHaveBeenCalledWith('http://veh?incluirInactivos=true');
  });

  it('get', async () => {
    await vehiculosApi.get('v1');
    expect(h.get).toHaveBeenCalledWith('http://veh/v1');
  });

  it('porPlaca codifica', async () => {
    await vehiculosApi.porPlaca('ABC 123');
    expect(h.get).toHaveBeenCalledWith('http://veh/placa/ABC%20123');
  });

  it('create', async () => {
    const b = { tipo: 'Auto' } as never;
    await vehiculosApi.create(b);
    expect(h.post).toHaveBeenCalledWith('http://veh', b);
  });

  it('update usa patch', async () => {
    await vehiculosApi.update('v1', { color: 'rojo' });
    expect(h.patch).toHaveBeenCalledWith('http://veh/v1', { color: 'rojo' });
  });

  it('activar', async () => {
    await vehiculosApi.activar('v1');
    expect(h.patch).toHaveBeenCalledWith('http://veh/v1/activar');
  });

  it('desactivar', async () => {
    await vehiculosApi.desactivar('v1');
    expect(h.patch).toHaveBeenCalledWith('http://veh/v1/desactivar');
  });
});
