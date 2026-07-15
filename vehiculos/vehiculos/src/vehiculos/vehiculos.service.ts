import {
  BadRequestException,
  ConflictException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { CreateVehiculoDto } from './dto/create-vehiculo.dto';
import { UpdateVehiculoDto } from './dto/update-vehiculo.dto';
import { InjectRepository } from '@nestjs/typeorm';
import { DeepPartial, Not, Repository } from 'typeorm';
import { Vehiculo } from './entities/vehiculo.entity';
import { FactoryVehiculos } from './factory/factory.vehiculo';
import { AuditEvent, EventPublisher } from './event-publisher.service';

// Datos de la petición HTTP que viajan en cada evento de auditoría
// (quién hizo la acción y desde dónde). El controller los arma a partir
// del JWT y de los headers de la petición.
export interface AuditContext {
  usuario?: string;
  rol?: string;
  ip: string;
  mac: string;
}

@Injectable()
export class VehiculosService {
  constructor(
    @InjectRepository(Vehiculo)
    private readonly repositoryVehiculo: Repository<Vehiculo>,
    private eventPublisher: EventPublisher,
  ) {}

  // Método auxiliar para publicar eventos
  private async emitEvent(
    accion: string,
    vehiculo: Vehiculo,
    auditContext: AuditContext,
    datosExtra?: any,
  ) {
    const event: AuditEvent = {
      servicio: 'ms-vehiculos',
      accion,
      entidad: 'VEHICULO',
      datos: { ...vehiculo, ...datosExtra },
      usuario: auditContext.usuario,
      rol: auditContext.rol,
      ip: auditContext.ip,
      mac: auditContext.mac,
    };
    await this.eventPublisher.publish(event);
  }

  async create(
    createVehiculoDto: CreateVehiculoDto,
    auditContext: AuditContext,
  ): Promise<Vehiculo> {
    const placa = createVehiculoDto.datos?.placa?.trim().toUpperCase();
    if (!placa) {
      throw new BadRequestException('La placa es requerida en datos');
    }
    // Se guarda siempre normalizada (sin espacios y en mayusculas) para que
    // la unicidad sea insensible a mayusculas/minusculas.
    createVehiculoDto.datos.placa = placa;

    const existe = await this.repositoryVehiculo.findOne({
      where: { placa },
    });
    if (existe) {
      throw new ConflictException('Ya existe un vehículo con la placa: ' + placa);
    }

    const vehiculo = FactoryVehiculos.crear(createVehiculoDto);
    const saved = await this.repositoryVehiculo.save(vehiculo);
    await this.emitEvent('CREATE', saved, auditContext);

    return saved;
  }

  // Por defecto solo lista vehiculos ACTIVOS (soft-delete). Con
  // incluirInactivos=true se devuelven tambien los desactivados.
  async findAll(incluirInactivos = false): Promise<Vehiculo[]> {
    if (incluirInactivos) {
      return this.repositoryVehiculo.find();
    }
    return this.repositoryVehiculo.find({ where: { activo: true } });
  }

  async findOne(id: string): Promise<Vehiculo> {
    const existe = await this.repositoryVehiculo.findOne({
      where: {
        id,
      },
    });
    if (!existe) {
      throw new NotFoundException('Vehículo no encontrado con ID: ' + id);
    }
    return existe;
  }

  // Busqueda por placa (clave de negocio). Se normaliza igual que al crear
  // para que la coincidencia sea insensible a mayusculas/espacios.
  async findByPlaca(placa: string): Promise<Vehiculo> {
    const placaNormalizada = placa?.trim().toUpperCase();
    if (!placaNormalizada) {
      throw new BadRequestException('La placa es requerida');
    }
    const existe = await this.repositoryVehiculo.findOne({
      where: { placa: placaNormalizada },
    });
    if (!existe) {
      throw new NotFoundException('Vehículo no encontrado con placa: ' + placaNormalizada);
    }
    return existe;
  }

  async update(
    id: string,
    updateVehiculoDto: UpdateVehiculoDto,
    auditContext: AuditContext,
  ): Promise<Vehiculo> {
    const existe = await this.repositoryVehiculo.findOne({ where: { id } });
    if (!existe) {
      throw new NotFoundException('Vehículo no encontrado con ID: ' + id);
    }

    const partialDatos =
      ((updateVehiculoDto as { datos?: DeepPartial<Vehiculo> }).datos as
        | DeepPartial<Vehiculo>
        | undefined) ?? {};

    // 'activo' se gestiona SOLO con los endpoints activar/desactivar,
    // nunca por una edición de atributos.
    delete (partialDatos as { activo?: boolean }).activo;

    // Si se actualiza la placa, se normaliza (trim + mayusculas) y se valida
    // que no exista en OTRO vehiculo (excluyendo el actual por su id).
    if (typeof partialDatos.placa === 'string') {
      const placa = partialDatos.placa.trim().toUpperCase();
      if (!placa) {
        throw new BadRequestException('La placa no puede estar vacia');
      }
      partialDatos.placa = placa;

      const duplicada = await this.repositoryVehiculo.findOne({
        where: { placa, id: Not(id) },
      });
      if (duplicada) {
        throw new ConflictException('Ya existe otro vehiculo con esa placa');
      }
    }

    const updated = this.repositoryVehiculo.merge(existe, partialDatos);
    const saved = await this.repositoryVehiculo.save(updated);
    await this.emitEvent('UPDATE', saved, auditContext);
    return saved;
  }

  async desactivar(id: string, auditContext: AuditContext): Promise<Vehiculo> {
    const existe = await this.repositoryVehiculo.findOne({ where: { id } });
    if (!existe) {
      throw new NotFoundException('Vehículo no encontrado con ID: ' + id);
    }
    if (!existe.activo) {
      throw new ConflictException('El vehículo ya está inactivo');
    }
    existe.activo = false;
    const saved = await this.repositoryVehiculo.save(existe);
    await this.emitEvent('UPDATE', saved, auditContext, {
      accionDetalle: 'DESACTIVAR',
    });
    return saved;
  }

  async activar(id: string, auditContext: AuditContext): Promise<Vehiculo> {
    const existe = await this.repositoryVehiculo.findOne({ where: { id } });
    if (!existe) {
      throw new NotFoundException('Vehículo no encontrado con ID: ' + id);
    }
    if (existe.activo) {
      throw new ConflictException('El vehículo ya está activo');
    }
    existe.activo = true;
    const saved = await this.repositoryVehiculo.save(existe);
    await this.emitEvent('UPDATE', saved, auditContext, {
      accionDetalle: 'ACTIVAR',
    });
    return saved;
  }
}
