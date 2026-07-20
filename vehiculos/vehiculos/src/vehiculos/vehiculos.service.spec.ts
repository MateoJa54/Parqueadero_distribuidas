import {
  BadRequestException,
  ConflictException,
  NotFoundException,
} from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { AutoDto, CreateVehiculoDto } from './dto/create-vehiculo.dto';
import { Auto } from './entities/auto.entity';
import { Clasificacion, Vehiculo } from './entities/vehiculo.entity';
import { EventPublisher } from './event-publisher.service';
import { AuditContext, VehiculosService } from './vehiculos.service';

describe('VehiculosService', () => {
  let service: VehiculosService;
  let repository: jest.Mocked<Pick<Repository<Vehiculo>, 'findOne' | 'find' | 'save' | 'merge'>>;
  let eventPublisher: { publish: jest.Mock };

  const auditContext: AuditContext = {
    usuario: 'qa.admin',
    rol: 'ADMIN',
    ip: '127.0.0.1',
    mac: 'AA:BB:CC:DD:EE:FF',
  };

  beforeEach(async () => {
    repository = {
      findOne: jest.fn(),
      find: jest.fn(),
      save: jest.fn(),
      merge: jest.fn(),
    };
    eventPublisher = { publish: jest.fn().mockResolvedValue(undefined) };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        VehiculosService,
        { provide: getRepositoryToken(Vehiculo), useValue: repository },
        { provide: EventPublisher, useValue: eventPublisher },
      ],
    }).compile();

    service = module.get(VehiculosService);
  });

  function autoDto(placa = ' abc-1234 '): CreateVehiculoDto {
    const datos = Object.assign(new AutoDto(), {
      placa,
      marca: 'Toyota',
      modelo: 'Corolla',
      color: 'Azul',
      anio: 2024,
      clasificacion: Clasificacion.GASOLINA,
      numeroPuertas: 4,
      capacidadMaletero: 470,
    });
    return { tipo: 'Auto', datos };
  }

  it('normaliza la placa, guarda el vehiculo y publica la auditoria', async () => {
    repository.findOne.mockResolvedValue(null);
    repository.save.mockImplementation(async (vehiculo) =>
      Object.assign(vehiculo, { id: 'vehiculo-1', activo: true }),
    );

    const result = await service.create(autoDto(), auditContext);

    expect(result).toBeInstanceOf(Auto);
    expect(result.placa).toBe('ABC-1234');
    expect(repository.findOne).toHaveBeenCalledWith({ where: { placa: 'ABC-1234' } });
    expect(eventPublisher.publish).toHaveBeenCalledWith(
      expect.objectContaining({
        servicio: 'ms-vehiculos',
        accion: 'CREATE',
        entidad: 'VEHICULO',
        usuario: 'qa.admin',
        ip: '127.0.0.1',
      }),
    );
  });

  it('rechaza una placa vacia antes de consultar el repositorio', async () => {
    const dto = autoDto('   ');

    await expect(service.create(dto, auditContext)).rejects.toBeInstanceOf(BadRequestException);
    expect(repository.findOne).not.toHaveBeenCalled();
    expect(eventPublisher.publish).not.toHaveBeenCalled();
  });

  it('rechaza una placa duplicada sin guardar ni auditar', async () => {
    repository.findOne.mockResolvedValue(Object.assign(new Auto(), { id: 'existente' }));

    await expect(service.create(autoDto(), auditContext)).rejects.toBeInstanceOf(ConflictException);
    expect(repository.save).not.toHaveBeenCalled();
    expect(eventPublisher.publish).not.toHaveBeenCalled();
  });

  it('lista solo activos por defecto y todos cuando se solicita', async () => {
    repository.find.mockResolvedValue([]);

    await service.findAll();
    expect(repository.find).toHaveBeenLastCalledWith({ where: { activo: true } });

    await service.findAll(true);
    expect(repository.find).toHaveBeenLastCalledWith();
  });

  it('normaliza la busqueda por placa y reporta cuando no existe', async () => {
    repository.findOne.mockResolvedValue(null);

    await expect(service.findByPlaca(' abc-9999 ')).rejects.toBeInstanceOf(NotFoundException);
    expect(repository.findOne).toHaveBeenCalledWith({ where: { placa: 'ABC-9999' } });
  });
});
