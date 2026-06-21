import { ChildEntity, Column } from 'typeorm';
import { Vehiculo } from './vehiculo.entity';

@ChildEntity('Camioneta')
export class Camioneta extends Vehiculo {
  @Column()
  cabina!: number;

  @Column()
  capacidadCarga!: string;

  obtenerTipo(): string {
    return 'Camioneta';
  }
}
