import { Entity, Column, PrimaryGeneratedColumn } from 'typeorm';

@Entity({ name: 'evento_auditoria' })
export class EventoAuditoria {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ type: 'varchar', length: 20 })
  servicio!: string;

  @Column({ type: 'varchar', length: 15 }) // CRUD
  accion!: string;

  @Column({ type: 'varchar', length: 30 })
  entidad!: string;

  @Column({ type: 'jsonb', nullable: true })
  datos?: any;

  @Column({ type: 'varchar', length: 50, nullable: true })
  usuario?: string;

  @Column({ type: 'varchar', length: 15, nullable: true })
  rol?: string;

  @Column({ type: 'varchar', length: 15, nullable: true })
  ip?: string;

  @Column({ type: 'varchar', length: 17, nullable: true })
  mac?: string;

  @Column()
  timestamp!: Date;
}
