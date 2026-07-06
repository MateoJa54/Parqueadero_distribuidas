export class ResponseVehiculoDto {
  id!: string;
  placa!: string;
  marca!: string;
  modelo!: string;
  color!: string;
  anio!: number;
  clasificacion!: string;
  numeroPuertas?: number;
  capacidadMaletero?: number;
  cilindraje?: number;
  cabina?: number;
  tipo!: string;
  tipoMoto?: string;
}
