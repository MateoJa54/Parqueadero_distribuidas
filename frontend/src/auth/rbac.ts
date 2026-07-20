import type { Rol } from '@/types';

// ============================================================================
// Modelo de permisos por rol. Fuente única de verdad para navegación y guards.
// Las rutas se protegen DE VERDAD en el router (no solo ocultando botones).
// ============================================================================

export type Permiso =
  | 'dashboard'
  | 'personas'
  | 'usuarios'
  | 'roles'
  | 'asignacion-roles'
  | 'zonas'
  | 'espacios'
  | 'vehiculos'
  | 'asignaciones-vehiculos'
  | 'tickets:operar'
  | 'tickets:ver'
  | 'auditoria'
  | 'configuracion'
  | 'diagnostico'
  | 'portal:perfil'
  | 'portal:mis-vehiculos'
  | 'portal:disponibilidad';

// Etiquetas legibles para mostrar la matriz de permisos en Configuración.
export const ETIQUETA_PERMISO: Record<Permiso, string> = {
  dashboard: 'Dashboard',
  personas: 'Personas',
  usuarios: 'Usuarios',
  roles: 'Roles',
  'asignacion-roles': 'Asignación de roles',
  zonas: 'Zonas',
  espacios: 'Espacios',
  vehiculos: 'Vehículos',
  'asignaciones-vehiculos': 'Asignaciones de vehículos',
  'tickets:operar': 'Operar tickets',
  'tickets:ver': 'Ver tickets',
  auditoria: 'Auditoría',
  configuracion: 'Configuración',
  diagnostico: 'Diagnóstico',
  'portal:perfil': 'Portal · Perfil',
  'portal:mis-vehiculos': 'Portal · Mis vehículos',
  'portal:disponibilidad': 'Portal · Disponibilidad',
};

export const MATRIZ: Record<Rol, Permiso[]> = {
  ROOT: [
    'dashboard',
    'personas',
    'usuarios',
    'roles',
    'asignacion-roles',
    'zonas',
    'espacios',
    'vehiculos',
    'asignaciones-vehiculos',
    'tickets:operar',
    'tickets:ver',
    'auditoria',
    'configuracion',
    'diagnostico',
    'portal:perfil',
    'portal:mis-vehiculos',
    'portal:disponibilidad',
  ],
  ADMIN: [
    'dashboard',
    'personas',
    'usuarios',
    'roles',
    'asignacion-roles',
    'zonas',
    'espacios',
    'vehiculos',
    'asignaciones-vehiculos',
    'tickets:operar',
    'tickets:ver',
    'auditoria',
    'configuracion',
    'diagnostico',
    'portal:perfil',
    'portal:mis-vehiculos',
    'portal:disponibilidad',
  ],
  RECAUDADOR: [
    // Operador de ventanilla: SOLO tickets (ingreso / cobro / anulación / consulta).
    // NO gestiona personas, usuarios, roles, zonas, espacios ni vehículos.
    'tickets:operar',
    'tickets:ver',
    // Autoservicio mínimo: su propio perfil y consulta de disponibilidad.
    'portal:perfil',
    'portal:disponibilidad',
  ],
  CLIENTE: ['portal:perfil', 'portal:mis-vehiculos', 'portal:disponibilidad'],
  INVITADO: ['portal:disponibilidad'],
};

export function permisosDe(roles: Rol[]): Set<Permiso> {
  const set = new Set<Permiso>();
  for (const r of roles) {
    for (const p of MATRIZ[r] ?? []) set.add(p);
  }
  return set;
}

export function puede(roles: Rol[], permiso: Permiso): boolean {
  return roles.some((r) => (MATRIZ[r] ?? []).includes(permiso));
}

/** ¿El usuario es "staff" (panel admin) o solo cliente/invitado (portal)? */
export function esStaff(roles: Rol[]): boolean {
  return roles.some((r) => r === 'ROOT' || r === 'ADMIN' || r === 'RECAUDADOR');
}

/** Ruta de aterrizaje según rol tras login. */
export function rutaInicial(roles: Rol[]): string {
  // ADMIN/ROOT aterrizan en el dashboard; el RECAUDADOR (sin dashboard) va directo a Tickets.
  if (puede(roles, 'dashboard')) return '/app';
  if (esStaff(roles)) return '/app/tickets';
  return '/portal';
}
