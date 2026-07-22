import { Type } from 'class-transformer';
import {
  IsEnum,
  IsIn,
  IsInt,
  IsNotEmpty,
  IsNumber,
  IsString,
  Matches,
  Max,
  MaxLength,
  Min,
  MinLength,
  ValidateNested,
} from 'class-validator';
import { Clasificacion } from '../entities/vehiculo.entity';
import { TipoMoto } from '../entities/motocicleta.entity';

// Constantes parametrizables para el rango de años válidos
const ANIO_MIN = 1886; // Benz Patent-Motorwagen, primer automóvil de la historia
const ANIO_MAX = new Date().getFullYear() + 1; // permite pre-registro del modelo del año siguiente

export class BaseVehiculoDto {
  @IsString()
  @IsNotEmpty({ message: 'La placa no puede estar vacía' })
  @Matches(/^[A-Z]{3}-\d{4}$/, {
    message: 'La placa debe tener el formato ABC-1234',
  })
  placa!: string;

  @IsString()
  @IsNotEmpty({ message: 'La marca no puede estar vacía' })
  @MinLength(2, { message: 'La marca debe tener al menos 2 caracteres' })
  @MaxLength(50, { message: 'La marca no puede tener más de 50 caracteres' })
  @Matches(/^[a-zA-ZáéíóúÁÉÍÓÚ\s]+$/, {
    message: 'La marca solo puede contener letras y espacios',
  })
  marca!: string;

  // Los modelos pueden incluir números y guiones: RAV4, Civic 2.0, Serie 5, X-Trail
  @IsString()
  @IsNotEmpty({ message: 'El modelo no puede estar vacío' })
  @MinLength(1, { message: 'El modelo debe tener al menos 1 carácter' })
  @MaxLength(100, { message: 'El modelo no puede tener más de 100 caracteres' })
  @Matches(/^[a-zA-Z0-9áéíóúÁÉÍÓÚ\s.\\-]+$/, {
    message:
      'El modelo solo puede contener letras, números, espacios, puntos y guiones',
  })
  modelo!: string;

  @IsString()
  @IsNotEmpty({ message: 'El color no puede estar vacío' })
  @MinLength(3, { message: 'El color debe tener al menos 3 caracteres' })
  @MaxLength(50, { message: 'El color no puede tener más de 50 caracteres' })
  @Matches(/^[a-zA-ZáéíóúÁÉÍÓÚ\s]+$/, {
    message: 'El color solo puede contener letras y espacios',
  })
  color!: string;

  @IsInt({ message: 'El año debe ser un número entero' })
  @Min(ANIO_MIN, { message: `El año debe ser mayor o igual a ${ANIO_MIN}` })
  @Max(ANIO_MAX, { message: `El año no puede ser mayor a ${ANIO_MAX}` })
  anio!: number;

  @IsEnum(Clasificacion, {
    message: `La clasificación debe ser: ${Object.values(Clasificacion).join(', ')}`,
  })
  clasificacion!: Clasificacion;
}

export class AutoDto extends BaseVehiculoDto {
  @IsInt({ message: 'El número de puertas debe ser un entero' })
  @Min(2, { message: 'Un auto debe tener al menos 2 puertas' })
  @Max(5, { message: 'Un auto no puede tener más de 5 puertas' })
  numeroPuertas!: number;

  @IsInt({ message: 'La capacidad de maletero debe ser un entero en litros' })
  @Min(50, { message: 'La capacidad de maletero mínima es 50 litros' })
  @Max(1500, { message: 'La capacidad de maletero máxima es 1500 litros' })
  capacidadMaletero!: number;
}

export class MotocicletaDto extends BaseVehiculoDto {
  // Las motos en Ecuador usan el formato AB-123A
  @IsString()
  @IsNotEmpty({ message: 'La placa no puede estar vacía' })
  @Matches(/^[A-Z]{2}-\d{3}[A-Z]$/, {
    message: 'La placa de motocicleta debe tener el formato AB-123A',
  })
  declare placa: string;

  @IsNumber()
  @Min(50, { message: 'El cilindraje mínimo es 50 cc' })
  @Max(2500, { message: 'El cilindraje máximo es 2500 cc' })
  cilindraje!: number;

  @IsEnum(TipoMoto, {
    message: `El tipo de moto debe ser: ${Object.values(TipoMoto).join(', ')}`,
  })
  tipoMoto!: TipoMoto;
}

export class CamionetaDto extends BaseVehiculoDto {
  @IsIn([2, 4], { message: 'La cabina debe ser simple (2) o doble (4)' })
  cabina!: number;

  @IsString()
  @IsNotEmpty({ message: 'La capacidad de carga no puede estar vacía' })
  @Matches(/^\d+(\.\d+)?\s?(kg|KG|t|T)$/,
    {
      message: 'La capacidad de carga debe tener formato como 750kg o 3.5t',
    })
  capacidadCarga!: string;
}

export class CreateVehiculoDto {
  @IsIn(['Auto', 'Motocicleta', 'Camioneta'], {
    message: "El tipo debe ser 'Auto', 'Motocicleta' o 'Camioneta'",
  })
  tipo!: string;

  @ValidateNested()
  @Type((opts) => {
    const obj = opts?.object as CreateVehiculoDto;
    switch (obj?.tipo) {
      case 'Auto':
        return AutoDto;
      case 'Motocicleta':
        return MotocicletaDto;
      case 'Camioneta':
        return CamionetaDto;
      default:
        return BaseVehiculoDto;
    }
  })
  datos!: AutoDto | MotocicletaDto | CamionetaDto;
}
