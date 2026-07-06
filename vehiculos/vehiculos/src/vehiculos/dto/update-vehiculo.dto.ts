import { PartialType } from '@nestjs/mapped-types';
import { Type } from 'class-transformer';
import { IsIn, IsOptional, ValidateNested } from 'class-validator';
import {
  AutoDto,
  BaseVehiculoDto,
  CamionetaDto,
  MotocicletaDto,
} from './create-vehiculo.dto';

// En una edicion PATCH todos los atributos son opcionales: solo se validan
// (con las mismas reglas de formato) los campos que realmente se envian.
// Por eso cada variante se transforma en su version parcial.
class UpdateBaseVehiculoDto extends PartialType(BaseVehiculoDto) {}
class UpdateAutoDto extends PartialType(AutoDto) {}
class UpdateMotocicletaDto extends PartialType(MotocicletaDto) {}
class UpdateCamionetaDto extends PartialType(CamionetaDto) {}

export class UpdateVehiculoDto {
  // 'tipo' es opcional. Si se omite, se validan solo los atributos base
  // comunes; para editar atributos propios de un subtipo (p. ej.
  // numeroPuertas de un Auto) se debe incluir el 'tipo' correspondiente.
  @IsOptional()
  @IsIn(['Auto', 'Motocicleta', 'Camioneta'], {
    message: "El tipo debe ser 'Auto', 'Motocicleta' o 'Camioneta'",
  })
  tipo?: string;

  @IsOptional()
  @ValidateNested()
  @Type((opts) => {
    const obj = opts?.object as UpdateVehiculoDto;
    switch (obj?.tipo) {
      case 'Auto':
        return UpdateAutoDto;
      case 'Motocicleta':
        return UpdateMotocicletaDto;
      case 'Camioneta':
        return UpdateCamionetaDto;
      default:
        return UpdateBaseVehiculoDto;
    }
  })
  datos?:
    | UpdateAutoDto
    | UpdateMotocicletaDto
    | UpdateCamionetaDto
    | UpdateBaseVehiculoDto;
}
