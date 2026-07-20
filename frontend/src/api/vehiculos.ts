import { API, http } from './client';
import type { Vehiculo, VehiculoRequest } from '@/types';

const V = API.vehiculos;

export const vehiculosApi = {
  list: (incluirInactivos = false) =>
    http.get<Vehiculo[]>(`${V}?incluirInactivos=${incluirInactivos}`),
  get: (id: string) => http.get<Vehiculo>(`${V}/${id}`),
  porPlaca: (placa: string) => http.get<Vehiculo>(`${V}/placa/${encodeURIComponent(placa)}`),
  create: (body: VehiculoRequest) => http.post<Vehiculo>(`${V}`, body),
  update: (id: string, datos: Record<string, unknown>) =>
    http.patch<Vehiculo>(`${V}/${id}`, datos),
  activar: (id: string) => http.patch<Vehiculo>(`${V}/${id}/activar`),
  desactivar: (id: string) => http.patch<Vehiculo>(`${V}/${id}/desactivar`),
};
