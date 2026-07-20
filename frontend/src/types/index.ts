// ============================================================================
// Tipos de dominio — nombres de campos EXACTOS según
// docs/contrato-api-microservicios.md (§2.7, §3.5, §4.5, §5.3, §6.2, §7.2)
// ============================================================================

export type Rol = 'ROOT' | 'ADMIN' | 'RECAUDADOR' | 'CLIENTE' | 'INVITADO';

// ---- Auth ----
export interface AuthResponse {
  token: string;
  refreshToken: string;
  tokenType: string; // "Bearer"
  expiresIn: number;
  refreshExpiresIn: number;
  idUsuario: string;
  username: string;
  roles: Rol[];
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegistroClienteRequest {
  dni: string;
  email: string;
  username: string;
  password: string;
}

export interface RegistroCompletoRequest {
  firstName: string;
  middleName?: string;
  lastName: string;
  dni: string;
  email: string;
  phone: string;
  address?: string;
  nationality: string;
  username: string;
  password: string;
}

export interface JwtClaims {
  sub: string;
  username: string;
  roles: Rol[];
  type: string;
  iss: string;
  iat: number;
  exp: number;
}

export interface PerfilResponse {
  idUsuario: string;
  username: string;
  nombreCompleto: string;
  active: boolean;
  roles: string[];
}

// ---- Personas (§2.7 PersonaResponseDto) ----
export interface Persona {
  id: string;
  firstName: string;
  middleName?: string | null;
  lastName: string;
  dni: string;
  email: string;
  phone?: string | null;
  address?: string | null;
  nationality: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface PersonaRequest {
  firstName: string;
  middleName?: string;
  lastName: string;
  dni: string;
  email: string;
  phone: string;
  address?: string;
  nationality: string;
}

// ---- Usuarios (§2.7 UsuarioResponseDto) ----
export interface Usuario {
  id: string;
  idPersona: string;
  username: string;
  nombreCompleto: string;
  active: boolean;
  lastLogin?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface UsuarioRequest {
  idPersona: string;
  username: string;
  password: string;
}

export interface UsuarioUpdate {
  idPersona: string;
  username: string;
  password?: string; // "" conserva la actual
}

// ---- Roles (§2.7 RolResponseDto) ----
export interface RolEntity {
  id: string;
  name: string;
  description?: string | null;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface RolRequest {
  name: string;
  description?: string;
}

// ---- Asignaciones de rol (§2.7 AsignacionResponseDto) ----
export interface AsignacionRol {
  idUser: string;
  username: string;
  idRole: string;
  rol: string;
  active: boolean;
  assignedAt?: string;
  updatedAt?: string;
}

export interface AsignarRolRequest {
  idUser: string;
  idRole: string;
}

// ---- Zonas (§3.5 ZonaRespondeDto) ----
export type TipoZona = 'VIP' | 'REGULAR' | 'INTERNA' | 'EXTERNA' | 'PREFERENCIAL';
export type TipoEspacio = 'MOTO' | 'AUTO' | 'BUSETA';
export type EstadoEspacio =
  | 'DISPONIBLE'
  | 'OCUPADO'
  | 'RESERVADO'
  | 'MANTENIMIENTO';

export interface Zona {
  idZona: string;
  nombre: string;
  codigo?: string;
  descripcion?: string | null;
  activo: boolean;
  tipoZona: TipoZona;
  capacidad: number;
  espacios?: Espacio[];
  fechaCreacion?: string;
  fechaActualizacion?: string;
}

export interface ZonaRequest {
  nombre: string;
  descripcion?: string;
  tipo: TipoZona;
  capacidad: number;
}

// ---- Espacios (§3.5 EspacioRespondeDto) ----
export interface Espacio {
  id: string;
  codigo: string;
  descripcion?: string | null;
  tipo: TipoEspacio;
  activo: boolean;
  idZona: string;
  nombreZona?: string;
  estado: EstadoEspacio;
}

export interface EspacioRequest {
  idZona: string;
  descripcion?: string;
  tipo: TipoEspacio;
  estado?: EstadoEspacio;
}

export interface Disponibilidad {
  idEspacio: string;
  codigo: string;
  disponible: boolean;
  activo: boolean;
  estado: EstadoEspacio;
}

// ---- Vehículos (NestJS §5) ----
export type TipoVehiculoApi = 'Auto' | 'Motocicleta' | 'Camioneta';
export type Clasificacion = 'Eléctrico' | 'Híbrido' | 'Gasolina' | 'Diésel';
export type TipoMoto = 'Deportiva' | 'Scooter' | 'Motocross';

export interface Vehiculo {
  id: string;
  placa: string;
  marca: string;
  modelo: string;
  color?: string;
  anio?: number;
  clasificacion?: Clasificacion;
  tipo: string; // "Auto" | "Motocicleta" | "Camioneta"
  activo: boolean;
  numeroPuertas?: number;
  capacidadMaletero?: number;
  cilindraje?: number;
  tipoMoto?: TipoMoto;
  cabina?: number;
  capacidadCarga?: string;
  [k: string]: unknown;
}

export interface VehiculoRequest {
  tipo: TipoVehiculoApi;
  datos: Record<string, unknown>;
}

// ---- Asignaciones vehículo-propietario (§4.5 AssignmentResponse) ----
export type AssignmentType = 'PROPIETARIO' | 'AUTORIZADO' | 'TEMPORAL';
export type AssignmentStatus = 'ACTIVA' | 'SUSPENDIDA' | 'FINALIZADA';

export interface Assignment {
  userId: string;
  vehicleId: string;
  active: boolean;
  status: AssignmentStatus;
  assignmentType: AssignmentType;
  authorizationRoleName?: string | null;
  validFrom?: string;
  validUntil?: string | null;
  vehicleAlias?: string | null;
  entryAuthorized?: boolean;
  observation?: string | null;
  changeReason?: string | null;
  assignedAt?: string;
  updatedAt?: string;
}

export interface CreateAssignmentRequest {
  userId: string;
  vehicleId: string;
  assignmentType?: AssignmentType;
  vehicleAlias?: string;
  observation?: string;
}

export interface UpdateAssignmentRequest {
  status?: AssignmentStatus;
  assignmentType?: AssignmentType;
  validUntil?: string;
  vehicleAlias?: string;
  entryAuthorized?: boolean;
  observation?: string;
  changeReason?: string;
}

// ---- Tickets (§6.2 TicketResponse) ----
export type EstadoTicket = 'ACTIVO' | 'PAGADO' | 'ANULADO';

export interface Ticket {
  id: string;
  codigo: string;
  idEspacio: string;
  codigoEspacio?: string;
  tipoEspacio?: string;
  idUsuario?: string;
  idVehiculo?: string;
  placa: string;
  tipoVehiculo?: string;
  categoriaTarifa?: string;
  fechaHoraIngreso: string;
  fechaHoraSalida?: string | null;
  estadoTicket: EstadoTicket;
  idEmpleado?: string;
  valorRecaudado?: number | null;
  motivoAnulacion?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface RegistrarIngresoRequest {
  placa: string;
  idEspacio: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first?: boolean;
  last?: boolean;
}

// ---- Auditoría (§7.2) ----
export type AuditAccion =
  | 'CREATE'
  | 'UPDATE'
  | 'DELETE'
  | 'LOGIN'
  | 'LOGOUT'
  | 'SELECT';

export interface AuditLog {
  id: string;
  servicio: string;
  accion: AuditAccion;
  entidad?: string;
  datos?: Record<string, unknown>;
  usuario?: string;
  rol?: string;
  ip?: string;
  mac?: string;
  timestamp: string;
}
