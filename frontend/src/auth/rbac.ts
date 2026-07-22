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
  | 'portal:mis-tickets'
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
  'portal:mis-tickets': 'Portal · Mis tickets',
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
    'portal:mis-tickets',
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
    'portal:mis-tickets',
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
  CLIENTE: ['portal:perfil', 'portal:mis-vehiculos', 'portal:mis-tickets', 'portal:disponibilidad'],
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

// Mapeo ruta protegida -> permiso requerido. Refleja los guards del router (App.tsx).
// Las entradas más específicas van primero para que gane la coincidencia correcta.
const RUTA_PERMISO: ReadonlyArray<readonly [string, Permiso]> = [
  ['/app/personas', 'personas'],
  ['/app/usuarios', 'usuarios'],
  ['/app/roles', 'roles'],
  ['/app/asignacion-roles', 'asignacion-roles'],
  ['/app/zonas', 'zonas'],
  ['/app/espacios', 'espacios'],
  ['/app/asignaciones-vehiculos', 'asignaciones-vehiculos'],
  ['/app/vehiculos', 'vehiculos'],
  ['/app/tickets', 'tickets:ver'],
  ['/app/auditoria', 'auditoria'],
  ['/app/configuracion', 'configuracion'],
  ['/app/diagnostico', 'diagnostico'],
  ['/portal/vehiculos', 'portal:mis-vehiculos'],
  ['/portal/tickets', 'portal:mis-tickets'],
  ['/portal/disponibilidad', 'portal:disponibilidad'],
  ['/app', 'dashboard'],
  ['/portal', 'portal:perfil'],
];

/**
 * ¿El usuario con esos roles puede entrar a una ruta concreta?
 * Se usa para validar el destino guardado (`from`) tras el login y no redirigir
 * a una ruta protegida que terminaría en "Acceso denegado" (DEF-02).
 * Las rutas públicas o desconocidas no se bloquean aquí.
 */
export function rutaPermitida(roles: Rol[], path: string): boolean {
  const limpio = (path.split('?')[0] ?? '').replace(/\/+$/, '') || '/';
  const match = RUTA_PERMISO.find(
    ([prefijo]) => limpio === prefijo || limpio.startsWith(`${prefijo}/`),
  );
  if (!match) return true;
  return puede(roles, match[1]);
}
