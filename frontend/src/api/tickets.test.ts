import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('./client', () => ({
  API: { tickets: 'http://tk', audit: 'http://au' },
  http: {
    get: vi.fn(() => Promise.resolve('ok')),
    post: vi.fn(() => Promise.resolve('ok')),
    patch: vi.fn(() => Promise.resolve('ok')),
  },
}));

import { http } from './client';
import { ticketsApi, auditApi } from './tickets';

const h = http as unknown as Record<string, ReturnType<typeof vi.fn>>;

describe('ticketsApi', () => {
  beforeEach(() => vi.clearAllMocks());

  it('ingreso', async () => {
    const body = { placa: 'ABC-1234', idEspacio: 'e1' } as never;
    await ticketsApi.ingreso(body);
    expect(h.post).toHaveBeenCalledWith('http://tk/tickets', body);
  });

  it('pagar', async () => {
    await ticketsApi.pagar('t1');
    expect(h.patch).toHaveBeenCalledWith('http://tk/tickets/t1/pagar');
  });

  it('anular envía motivo', async () => {
    await ticketsApi.anular('t1', 'perdido');
    expect(h.patch).toHaveBeenCalledWith('http://tk/tickets/t1/anular', { motivo: 'perdido' });
  });

  it('get', async () => {
    await ticketsApi.get('t1');
    expect(h.get).toHaveBeenCalledWith('http://tk/tickets/t1');
  });

  it('porCodigo codifica el código', async () => {
    await ticketsApi.porCodigo('AB C/1');
    expect(h.get).toHaveBeenCalledWith('http://tk/tickets/codigo/AB%20C%2F1');
  });

  it('listar por defecto page 0 size 20', async () => {
    await ticketsApi.listar();
    expect(h.get).toHaveBeenCalledWith('http://tk/tickets?page=0&size=20');
  });

  it('listar con estado, page y size', async () => {
    await ticketsApi.listar({ estado: 'ACTIVO', page: 2, size: 5 });
    expect(h.get).toHaveBeenCalledWith('http://tk/tickets?estado=ACTIVO&page=2&size=5');
  });

  it('activoPorEspacio', async () => {
    await ticketsApi.activoPorEspacio('e1');
    expect(h.get).toHaveBeenCalledWith('http://tk/tickets/activo/espacio/e1');
  });
});

describe('auditApi', () => {
  beforeEach(() => vi.clearAllMocks());

  it('list', async () => {
    await auditApi.list();
    expect(h.get).toHaveBeenCalledWith('http://au');
  });

  it('get', async () => {
    await auditApi.get('a1');
    expect(h.get).toHaveBeenCalledWith('http://au/a1');
  });
});
