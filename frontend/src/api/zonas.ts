import { API, http } from './client';
import type {
  Disponibilidad,
  Espacio,
  EspacioRequest,
  EstadoEspacio,
  TipoEspacio,
  Zona,
  ZonaRequest,
} from '@/types';

const Z = API.zonas;

export const zonasApi = {
  list: () => http.get<Zona[]>(`${Z}/zonas`),
  get: (id: string) => http.get<Zona>(`${Z}/zonas/${id}`),
  create: (body: ZonaRequest) => http.post<Zona>(`${Z}/zonas`, body),
  update: (id: string, body: ZonaRequest) => http.put<Zona>(`${Z}/zonas/${id}`, body),
  activar: (id: string) => http.patch<void>(`${Z}/zonas/${id}/activar`),
  desactivar: (id: string) => http.patch<void>(`${Z}/zonas/${id}/desactivar`),
};

export const espaciosApi = {
  list: () => http.get<Espacio[]>(`${Z}/espacios`),
  get: (id: string) => http.get<Espacio>(`${Z}/espacios/${id}`),
  create: (body: EspacioRequest) => http.post<Espacio>(`${Z}/espacios`, body),
  update: (id: string, body: EspacioRequest) => http.put<Espacio>(`${Z}/espacios/${id}`, body),
  cambiarEstado: (id: string, estado: EstadoEspacio) =>
    http.patch<Espacio>(`${Z}/espacios/${id}/estado?estado=${estado}`),
  porEstado: (estado: EstadoEspacio) => http.get<Espacio[]>(`${Z}/espacios/estado/${estado}`),
  disponibles: (params?: { idZona?: string; tipo?: TipoEspacio }) => {
    const q = new URLSearchParams();
    if (params?.idZona) q.set('idZona', params.idZona);
    if (params?.tipo) q.set('tipo', params.tipo);
    const qs = q.toString();
    const suffix = qs ? `?${qs}` : '';
    return http.get<Espacio[]>(`${Z}/espacios/disponibles${suffix}`);
  },
  disponibilidad: (id: string) =>
    http.get<Disponibilidad>(`${Z}/espacios/${id}/disponibilidad`),
  activar: (id: string) => http.patch<void>(`${Z}/espacios/${id}/activar`),
  desactivar: (id: string) => http.patch<void>(`${Z}/espacios/${id}/desactivar`),
};
