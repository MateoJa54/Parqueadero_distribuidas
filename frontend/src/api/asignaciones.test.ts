import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('./client', () => ({
  API: { asignaciones: 'http://asig' },
  http: {
    get: vi.fn(() => Promise.resolve('ok')),
    post: vi.fn(() => Promise.resolve('ok')),
    put: vi.fn(() => Promise.resolve('ok')),
    patch: vi.fn(() => Promise.resolve('ok')),
    del: vi.fn(() => Promise.resolve('ok')),
  },
}));

import { http } from './client';
import { asignVehiculoApi, propietariosApi } from './asignaciones';

const h = http as unknown as Record<string, ReturnType<typeof vi.fn>>;

describe('asignVehiculoApi', () => {
  beforeEach(() => vi.clearAllMocks());

  it('listAll sin filtro', async () => {
    await asignVehiculoApi.listAll();
    expect(h.get).toHaveBeenCalledWith('http://asig/asignaciones-vehiculos');
  });

  it('listAll solo activas', async () => {
    await asignVehiculoApi.listAll(true);
    expect(h.get).toHaveBeenCalledWith('http://asig/asignaciones-vehiculos?activas=true');
  });

  it('create envía body', async () => {
    const body = { userId: 'u', vehicleId: 'v' } as never;
    await asignVehiculoApi.create(body);
    expect(h.post).toHaveBeenCalledWith('http://asig/asignaciones-vehiculos', body);
  });

  it('update usa patch con ids', async () => {
    const body = { rol: 'DUENIO' } as never;
    await asignVehiculoApi.update('u1', 'v1', body);
    expect(h.patch).toHaveBeenCalledWith('http://asig/asignaciones-vehiculos/u1/v1', body);
  });

  it('desactivar', async () => {
    await asignVehiculoApi.desactivar('u1', 'v1');
    expect(h.patch).toHaveBeenCalledWith('http://asig/asignaciones-vehiculos/u1/v1/desactivar');
  });

  it('activar', async () => {
    await asignVehiculoApi.activar('u1', 'v1');
    expect(h.patch).toHaveBeenCalledWith('http://asig/asignaciones-vehiculos/u1/v1/activar');
  });

  it('porVehiculo', async () => {
    await asignVehiculoApi.porVehiculo('v1');
    expect(h.get).toHaveBeenCalledWith('http://asig/asignaciones-vehiculos/vehiculo/v1');
  });

  it('trazabilidad', async () => {
    await asignVehiculoApi.trazabilidad('u1', 'v1');
    expect(h.get).toHaveBeenCalledWith('http://asig/asignaciones-vehiculos/u1/v1/trazabilidad');
  });
});

describe('propietariosApi', () => {
  beforeEach(() => vi.clearAllMocks());
  it('vehiculos del propietario', async () => {
    await propietariosApi.vehiculos('u1');
    expect(h.get).toHaveBeenCalledWith('http://asig/propietarios/u1/vehiculos');
  });
});
