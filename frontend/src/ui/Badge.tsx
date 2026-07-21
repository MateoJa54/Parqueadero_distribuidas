import type { ReactNode } from 'react';

type BadgeTone = 'neutral' | 'primary' | 'success' | 'warning' | 'danger' | 'info';

export function Badge({ tone = 'neutral', children }: { readonly tone?: BadgeTone; readonly children: ReactNode }) {
  return <span className={`badge badge-${tone}`}>{children}</span>;
}

// Mapea estados de dominio a tono + etiqueta (estado NUNCA solo por color: lleva texto).
export function EstadoEspacioBadge({ estado }: { readonly estado: string }) {
  const map: Record<string, BadgeTone> = {
    DISPONIBLE: 'success',
    OCUPADO: 'danger',
    RESERVADO: 'warning',
    MANTENIMIENTO: 'neutral',
  };
  return <Badge tone={map[estado] ?? 'neutral'}>{estado}</Badge>;
}

export function EstadoTicketBadge({ estado }: { readonly estado: string }) {
  const map: Record<string, BadgeTone> = {
    ACTIVO: 'info',
    PAGADO: 'success',
    ANULADO: 'danger',
  };
  return <Badge tone={map[estado] ?? 'neutral'}>{estado}</Badge>;
}

export function ActivoBadge({ activo }: { readonly activo: boolean }) {
  return activo ? <Badge tone="success">Activo</Badge> : <Badge tone="neutral">Inactivo</Badge>;
}

export function EstadoAsignacionBadge({ estado }: { readonly estado: string }) {
  const map: Record<string, BadgeTone> = {
    ACTIVA: 'success',
    SUSPENDIDA: 'warning',
    FINALIZADA: 'neutral',
  };
  return <Badge tone={map[estado] ?? 'neutral'}>{estado}</Badge>;
}
