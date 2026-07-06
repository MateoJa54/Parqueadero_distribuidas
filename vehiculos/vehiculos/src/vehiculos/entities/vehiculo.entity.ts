import {
  AfterInsert,
  AfterLoad,
  AfterUpdate,
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

  // Discriminador expuesto en las respuestas JSON. NO lleva @Column: TypeORM ya
  // gestiona la columna 'tipo' de la herencia (STI). Se rellena tras cargar,
  // insertar o actualizar para que los microservicios consumidores (p. ej.
  // tickets, que valida la compatibilidad vehiculo/espacio) conozcan el tipo.
  tipo?: string;

  abstract obtenerTipo(): string;

  @AfterLoad()
  @AfterInsert()
  @AfterUpdate()
  protected exponerTipo(): void {
    this.tipo = this.obtenerTipo();
  }
}
