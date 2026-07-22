import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('./client', () => ({
  API: { usuarios: 'http://usr' },
  http: {
    get: vi.fn(() => Promise.resolve('ok')),
    post: vi.fn(() => Promise.resolve('ok')),
    put: vi.fn(() => Promise.resolve('ok')),
    patch: vi.fn(() => Promise.resolve('ok')),
  },
}));

import { http } from './client';
import { personasApi, usuariosApi, rolesApi, asignacionesRolApi } from './usuarios';

const h = http as unknown as Record<string, ReturnType<typeof vi.fn>>;

describe('personasApi', () => {
  beforeEach(() => vi.clearAllMocks());
  it('list', async () => { await personasApi.list(); expect(h.get).toHaveBeenCalledWith('http://usr/personas'); });
  it('get', async () => { await personasApi.get('1'); expect(h.get).toHaveBeenCalledWith('http://usr/personas/1'); });
  it('buscar arma query', async () => {
    await personasApi.buscar({ dni: '9', nombre: 'a', apellido: 'b' });
    expect(h.get).toHaveBeenCalledWith('http://usr/personas/buscar?dni=9&nombre=a&apellido=b');
  });
  it('buscar sin params', async () => {
    await personasApi.buscar({});
    expect(h.get).toHaveBeenCalledWith('http://usr/personas/buscar?');
  });
  it('create', async () => { const b = { dni: '1' } as never; await personasApi.create(b); expect(h.post).toHaveBeenCalledWith('http://usr/personas', b); });
  it('update', async () => { const b = { dni: '1' } as never; await personasApi.update('1', b); expect(h.put).toHaveBeenCalledWith('http://usr/personas/1', b); });
  it('activar', async () => { await personasApi.activar('1'); expect(h.patch).toHaveBeenCalledWith('http://usr/personas/1/activar'); });
  it('desactivar', async () => { await personasApi.desactivar('1'); expect(h.patch).toHaveBeenCalledWith('http://usr/personas/1/desactivar'); });
});

describe('usuariosApi', () => {
  beforeEach(() => vi.clearAllMocks());
  it('list', async () => { await usuariosApi.list(); expect(h.get).toHaveBeenCalledWith('http://usr/usuarios'); });
  it('get', async () => { await usuariosApi.get('1'); expect(h.get).toHaveBeenCalledWith('http://usr/usuarios/1'); });
  it('buscar codifica username', async () => { await usuariosApi.buscar('a b'); expect(h.get).toHaveBeenCalledWith('http://usr/usuarios/buscar?username=a%20b'); });
  it('create', async () => { const b = {} as never; await usuariosApi.create(b); expect(h.post).toHaveBeenCalledWith('http://usr/usuarios', b); });
  it('update', async () => { const b = {} as never; await usuariosApi.update('1', b); expect(h.put).toHaveBeenCalledWith('http://usr/usuarios/1', b); });
  it('activar', async () => { await usuariosApi.activar('1'); expect(h.patch).toHaveBeenCalledWith('http://usr/usuarios/1/activar'); });
  it('desactivar', async () => { await usuariosApi.desactivar('1'); expect(h.patch).toHaveBeenCalledWith('http://usr/usuarios/1/desactivar'); });
});

describe('rolesApi', () => {
  beforeEach(() => vi.clearAllMocks());
  it('list', async () => { await rolesApi.list(); expect(h.get).toHaveBeenCalledWith('http://usr/roles'); });
  it('get', async () => { await rolesApi.get('1'); expect(h.get).toHaveBeenCalledWith('http://usr/roles/1'); });
  it('create', async () => { const b = {} as never; await rolesApi.create(b); expect(h.post).toHaveBeenCalledWith('http://usr/roles', b); });
  it('update', async () => { const b = {} as never; await rolesApi.update('1', b); expect(h.put).toHaveBeenCalledWith('http://usr/roles/1', b); });
  it('activar', async () => { await rolesApi.activar('1'); expect(h.patch).toHaveBeenCalledWith('http://usr/roles/1/activar'); });
  it('desactivar', async () => { await rolesApi.desactivar('1'); expect(h.patch).toHaveBeenCalledWith('http://usr/roles/1/desactivar'); });
});

describe('asignacionesRolApi', () => {
  beforeEach(() => vi.clearAllMocks());
  it('list', async () => { await asignacionesRolApi.list(); expect(h.get).toHaveBeenCalledWith('http://usr/asignaciones'); });
  it('porUsuario', async () => { await asignacionesRolApi.porUsuario('u1'); expect(h.get).toHaveBeenCalledWith('http://usr/asignaciones/usuario/u1'); });
  it('asignar', async () => { const b = {} as never; await asignacionesRolApi.asignar(b); expect(h.post).toHaveBeenCalledWith('http://usr/asignaciones', b); });
  it('desactivar', async () => { await asignacionesRolApi.desactivar('u1', 'r1'); expect(h.patch).toHaveBeenCalledWith('http://usr/asignaciones/usuario/u1/rol/r1/desactivar'); });
  it('activar', async () => { await asignacionesRolApi.activar('u1', 'r1'); expect(h.patch).toHaveBeenCalledWith('http://usr/asignaciones/usuario/u1/rol/r1/activar'); });
});
