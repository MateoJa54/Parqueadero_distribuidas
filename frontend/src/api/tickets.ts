import { API, http } from './client';
import type {
  AuditLog,
  EstadoTicket,
  Page,
  RegistrarIngresoRequest,
  Ticket,
} from '@/types';

const T = API.tickets;

export const ticketsApi = {
  ingreso: (body: RegistrarIngresoRequest) => http.post<Ticket>(`${T}/tickets`, body),
  pagar: (id: string) => http.patch<Ticket>(`${T}/tickets/${id}/pagar`),
  anular: (id: string, motivo: string) =>
    http.patch<Ticket>(`${T}/tickets/${id}/anular`, { motivo }),
  get: (id: string) => http.get<Ticket>(`${T}/tickets/${id}`),
  porCodigo: (codigo: string) =>
    http.get<Ticket>(`${T}/tickets/codigo/${encodeURIComponent(codigo)}`),
  listar: (params?: { estado?: EstadoTicket; page?: number; size?: number }) => {
    const q = new URLSearchParams();
    if (params?.estado) q.set('estado', params.estado);
    q.set('page', String(params?.page ?? 0));
    q.set('size', String(params?.size ?? 20));
    return http.get<Page<Ticket>>(`${T}/tickets?${q.toString()}`);
  },
  activoPorEspacio: (idEspacio: string) =>
    http.get<Ticket>(`${T}/tickets/activo/espacio/${idEspacio}`),
};

export const auditApi = {
  list: () => http.get<AuditLog[]>(`${API.audit}`),
  get: (id: string) => http.get<AuditLog>(`${API.audit}/${id}`),
};
