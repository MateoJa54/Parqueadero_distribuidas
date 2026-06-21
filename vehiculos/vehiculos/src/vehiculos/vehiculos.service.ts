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

@Injectable()
export class VehiculosService {
  constructor(
    @InjectRepository(Vehiculo)
    private readonly repositoryVehiculo: Repository<Vehiculo>,
  ) {}

  async create(createVehiculoDto: CreateVehiculoDto): Promise<Vehiculo> {
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
      throw new ConflictException('Vehiculo already exists');
    }

    const vehiculo = FactoryVehiculos.crear(createVehiculoDto);
    return this.repositoryVehiculo.save(vehiculo);
  }

  async findAll(): Promise<Vehiculo[]> {
    return this.repositoryVehiculo.find();
  }

  async findOne(id: string): Promise<Vehiculo> {
    const existe = await this.repositoryVehiculo.findOne({
      where: {
        id,
      },
    });
    if (!existe) {
      throw new NotFoundException('Vehiculo not found');
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
      throw new NotFoundException('Vehiculo not found');
    }
    return existe;
  }

  async update(id: string, updateVehiculoDto: UpdateVehiculoDto): Promise<Vehiculo> {
    const existe = await this.repositoryVehiculo.findOne({ where: { id } });
    if (!existe) {
      throw new NotFoundException('Vehiculo not found');
    }

    const partialDatos =
      ((updateVehiculoDto as { datos?: DeepPartial<Vehiculo> }).datos as
        | DeepPartial<Vehiculo>
        | undefined) ?? {};

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
    return this.repositoryVehiculo.save(updated);
  }

  async remove(id: string): Promise<void> {
    const existe = await this.repositoryVehiculo.findOne({ where: { id } });
    if (!existe) {
      throw new NotFoundException('Vehiculo not found');
    }
    await this.repositoryVehiculo.remove(existe);
  }
}
