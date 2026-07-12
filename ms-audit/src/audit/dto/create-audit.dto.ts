import {
  IsIP,
  IsMACAddress,
  IsNotEmpty,
  IsObject,
  IsOptional,
  IsString,
  Matches,
  MaxLength,
  MinLength,
} from 'class-validator';

export class CreateAuditEventDto {
  @IsString()
  @IsNotEmpty()
  @MinLength(7)
  @MaxLength(50)
  @Matches(/^(ms-[a-zA-Z]+)$/, {
    message: 'El servicio debe comenzar con "ms-" seguido de letras.',
  })
  servicio!: string; // ms-usuarios, ms-zonas, ms-vehiculos, ms-asignaciones, ms-tickets

  @IsString()
  @IsNotEmpty()
  @MinLength(5)
  @MaxLength(10)
  @Matches(/^(CREATE|UPDATE|DELETE|LOGIN|LOGOUT|SELECT)$/, {
    message:
      'La acción debe ser una de las siguientes: CREATE, UPDATE, DELETE, LOGIN, LOGOUT, SELECT.',
  })
  accion!: string;

  @IsString()
  @IsNotEmpty()
  @MinLength(3)
  @MaxLength(20)
  @Matches(/^[A-Z-]+$/, {
    message: 'El campo solo debe contener letras mayúsculas y guiones medios.',
  })
  entidad!: string; // p.ej. VEHICULO, ZONA, USUARIO, ASIGNACION, TICKET

  @IsObject()
  @IsOptional()
  datos?: Record<string, any>;

  @IsString()
  @IsOptional()
  @MinLength(3)
  @MaxLength(40) // suficiente para un UUID (36) o un username corto
  @Matches(/^[a-zA-Z0-9._-]+$/, {
    message:
      'El nombre de usuario solo puede contener letras, números, puntos, guiones bajos y guiones medios.',
  })
  usuario?: string;

  @IsString()
  @IsOptional()
  rol?: string;

  @IsIP('4', { message: 'La dirección IP debe ser una dirección IPv4 válida.' })
  @IsNotEmpty()
  ip!: string;

  @IsMACAddress({
    message: 'La dirección MAC debe ser una dirección MAC válida.',
  })
  @IsNotEmpty()
  mac!: string;
}
