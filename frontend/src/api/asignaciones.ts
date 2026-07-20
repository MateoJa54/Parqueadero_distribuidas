import { API, http } from './client';
import type {
  Assignment,
  CreateAssignmentRequest,
  UpdateAssignmentRequest,
  Vehiculo,
} from '@/types';

const A = API.asignaciones;

// Asignaciones vehículo-propietario (/api/v1/asignaciones-vehiculos)
export const asignVehiculoApi = {
  listAll: (soloActivas = false) =>
    http.get<Assignment[]>(`${A}/asignaciones-vehiculos${soloActivas ? '?activas=true' : ''}`),
  create: (body: CreateAssignmentRequest) =>
    http.post<Assignment>(`${A}/asignaciones-vehiculos`, body),
  update: (userId: string, vehicleId: string, body: UpdateAssignmentRequest) =>
    http.patch<Assignment>(`${A}/asignaciones-vehiculos/${userId}/${vehicleId}`, body),
  desactivar: (userId: string, vehicleId: string) =>
    http.patch<Assignment>(`${A}/asignaciones-vehiculos/${userId}/${vehicleId}/desactivar`),
  activar: (userId: string, vehicleId: string) =>
    http.patch<Assignment>(`${A}/asignaciones-vehiculos/${userId}/${vehicleId}/activar`),
  // Devuelve la ÚNICA asignación activa del vehículo (o 404 si no hay).
  porVehiculo: (vehicleId: string) =>
    http.get<Assignment>(`${A}/asignaciones-vehiculos/vehiculo/${vehicleId}`),
  trazabilidad: (userId: string, vehicleId: string) =>
    http.get<Assignment[]>(`${A}/asignaciones-vehiculos/${userId}/${vehicleId}/trazabilidad`),
};

// Flota del propietario (/api/v1/propietarios/{userId}/vehiculos)
export const propietariosApi = {
  vehiculos: (userId: string) =>
    http.get<Vehiculo[]>(`${A}/propietarios/${userId}/vehiculos`),
};
