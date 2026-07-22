import { API, http } from './client';
import type {
  AsignacionRol,
  AsignarRolRequest,
  Persona,
  PersonaRequest,
  RolEntity,
  RolRequest,
  Usuario,
  UsuarioRequest,
  UsuarioUpdate,
} from '@/types';

const U = API.usuarios;

// ---- Personas ----
export const personasApi = {
  list: () => http.get<Persona[]>(`${U}/personas`),
  get: (id: string) => http.get<Persona>(`${U}/personas/${id}`),
  buscar: (params: { dni?: string; nombre?: string; apellido?: string }) => {
    const q = new URLSearchParams();
    if (params.dni) q.set('dni', params.dni);
    if (params.nombre) q.set('nombre', params.nombre);
    if (params.apellido) q.set('apellido', params.apellido);
    return http.get<Persona[]>(`${U}/personas/buscar?${q.toString()}`);
  },
  create: (body: PersonaRequest) => http.post<Persona>(`${U}/personas`, body),
  update: (id: string, body: PersonaRequest) =>
    http.put<Persona>(`${U}/personas/${id}`, body),
  activar: (id: string) => http.patch<Persona>(`${U}/personas/${id}/activar`),
  desactivar: (id: string) => http.patch<Persona>(`${U}/personas/${id}/desactivar`),
};

// ---- Usuarios ----
export const usuariosApi = {
  list: () => http.get<Usuario[]>(`${U}/usuarios`),
  get: (id: string) => http.get<Usuario>(`${U}/usuarios/${id}`),
  buscar: (username: string) =>
    http.get<Usuario[]>(`${U}/usuarios/buscar?username=${encodeURIComponent(username)}`),
  create: (body: UsuarioRequest) => http.post<Usuario>(`${U}/usuarios`, body),
  update: (id: string, body: UsuarioUpdate) => http.put<Usuario>(`${U}/usuarios/${id}`, body),
  activar: (id: string) => http.patch<Usuario>(`${U}/usuarios/${id}/activar`),
  desactivar: (id: string) => http.patch<Usuario>(`${U}/usuarios/${id}/desactivar`),
};

// ---- Roles ----
export const rolesApi = {
  list: () => http.get<RolEntity[]>(`${U}/roles`),
  get: (id: string) => http.get<RolEntity>(`${U}/roles/${id}`),
  create: (body: RolRequest) => http.post<RolEntity>(`${U}/roles`, body),
  update: (id: string, body: RolRequest) => http.put<RolEntity>(`${U}/roles/${id}`, body),
  activar: (id: string) => http.patch<RolEntity>(`${U}/roles/${id}/activar`),
  desactivar: (id: string) => http.patch<RolEntity>(`${U}/roles/${id}/desactivar`),
};

// ---- Asignaciones de rol ----
export const asignacionesRolApi = {
  list: () => http.get<AsignacionRol[]>(`${U}/asignaciones`),
  porUsuario: (idUsuario: string) =>
    http.get<AsignacionRol[]>(`${U}/asignaciones/usuario/${idUsuario}`),
  asignar: (body: AsignarRolRequest) => http.post<AsignacionRol>(`${U}/asignaciones`, body),
  desactivar: (idUsuario: string, idRol: string) =>
    http.patch<AsignacionRol>(`${U}/asignaciones/usuario/${idUsuario}/rol/${idRol}/desactivar`),
  activar: (idUsuario: string, idRol: string) =>
    http.patch<AsignacionRol>(`${U}/asignaciones/usuario/${idUsuario}/rol/${idRol}/activar`),
};
