import {
  Column,
  Entity,
  PrimaryGeneratedColumn,
  TableInheritance,
} from 'typeorm';

export enum Clasificacion {
  ELECTRICO = 'Eléctrico',
  HIBRIDO = 'Híbrido',
  GASOLINA = 'Gasolina',
  DIESEL = 'Diésel',
}

@Entity()
@TableInheritance({ column: { type: 'varchar', name: 'tipo' } })
export abstract class Vehiculo {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ unique: true })
  placa!: string;

  @Column()
  marca!: string;

  @Column()
  modelo!: string;

  @Column()
  color!: string;

  @Column()
  anio!: number;

  @Column({ type: 'enum', enum: Clasificacion })
  clasificacion!: Clasificacion;

  // Soft-delete: un vehiculo nunca se borra fisicamente, se desactiva.
  @Column({ default: true })
  activo!: boolean;

  abstract obtenerTipo(): string;

  // 'tipo' es la columna discriminadora de @TableInheritance: TypeORM la usa
  // para saber que subclase hidratar, pero no la expone como propiedad propia,
  // asi que JSON.stringify() nunca la incluye por si sola. Sin este toJSON,
  // todo consumidor externo (ej. tickets validando compatibilidad vehiculo/
  // espacio) recibe 'tipo: null'.
  toJSON() {
    return { ...this, tipo: this.obtenerTipo() };
  }
}
