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

  it('findByPlaca rechaza placa vacia', async () => {
    await expect(service.findByPlaca('   ')).rejects.toBeInstanceOf(BadRequestException);
    expect(repository.findOne).not.toHaveBeenCalled();
  });

  it('findOne devuelve el vehiculo o falla si no existe', async () => {
    const auto = Object.assign(new Auto(), { id: 'v1' });
    repository.findOne.mockResolvedValueOnce(auto);
    await expect(service.findOne('v1')).resolves.toBe(auto);

    repository.findOne.mockResolvedValueOnce(null);
    await expect(service.findOne('nope')).rejects.toBeInstanceOf(NotFoundException);
  });

  it('update normaliza placa, valida duplicados y persiste', async () => {
    const existente = Object.assign(new Auto(), { id: 'v1', activo: true });
    repository.findOne.mockResolvedValueOnce(existente); // busca por id
    repository.findOne.mockResolvedValueOnce(null); // no hay duplicado
    repository.merge.mockImplementation((target, patch) => Object.assign(target, patch));
    repository.save.mockImplementation(async (v) => v);

    const result = await service.update(
      'v1',
      { datos: { placa: ' xyz-1111 ', activo: false } } as never,
      auditContext,
    );

    expect(result.placa).toBe('XYZ-1111');
    expect(eventPublisher.publish).toHaveBeenCalledWith(
      expect.objectContaining({ accion: 'UPDATE' }),
    );
  });

  it('update rechaza cuando el id no existe', async () => {
    repository.findOne.mockResolvedValueOnce(null);
    await expect(
      service.update('nope', { datos: {} } as never, auditContext),
    ).rejects.toBeInstanceOf(NotFoundException);
  });

  it('update rechaza placa vacia', async () => {
    repository.findOne.mockResolvedValueOnce(Object.assign(new Auto(), { id: 'v1' }));
    await expect(
      service.update('v1', { datos: { placa: '   ' } } as never, auditContext),
    ).rejects.toBeInstanceOf(BadRequestException);
  });

  it('update rechaza placa duplicada en otro vehiculo', async () => {
    repository.findOne.mockResolvedValueOnce(Object.assign(new Auto(), { id: 'v1' }));
    repository.findOne.mockResolvedValueOnce(Object.assign(new Auto(), { id: 'v2' }));
    await expect(
      service.update('v1', { datos: { placa: 'DUP-0000' } } as never, auditContext),
    ).rejects.toBeInstanceOf(ConflictException);
  });

  it('desactivar cambia activo a false y audita', async () => {
    const auto = Object.assign(new Auto(), { id: 'v1', activo: true });
    repository.findOne.mockResolvedValueOnce(auto);
    repository.save.mockImplementation(async (v) => v);

    const result = await service.desactivar('v1', auditContext);
    expect(result.activo).toBe(false);
    expect(eventPublisher.publish).toHaveBeenCalledWith(
      expect.objectContaining({ datos: expect.objectContaining({ accionDetalle: 'DESACTIVAR' }) }),
    );
  });

  it('desactivar rechaza si ya esta inactivo o no existe', async () => {
    repository.findOne.mockResolvedValueOnce(Object.assign(new Auto(), { activo: false }));
    await expect(service.desactivar('v1', auditContext)).rejects.toBeInstanceOf(ConflictException);

    repository.findOne.mockResolvedValueOnce(null);
    await expect(service.desactivar('nope', auditContext)).rejects.toBeInstanceOf(NotFoundException);
  });

  it('activar cambia activo a true y audita', async () => {
    const auto = Object.assign(new Auto(), { id: 'v1', activo: false });
    repository.findOne.mockResolvedValueOnce(auto);
    repository.save.mockImplementation(async (v) => v);

    const result = await service.activar('v1', auditContext);
    expect(result.activo).toBe(true);
    expect(eventPublisher.publish).toHaveBeenCalledWith(
      expect.objectContaining({ datos: expect.objectContaining({ accionDetalle: 'ACTIVAR' }) }),
    );
  });

  it('activar rechaza si ya esta activo o no existe', async () => {
    repository.findOne.mockResolvedValueOnce(Object.assign(new Auto(), { activo: true }));
    await expect(service.activar('v1', auditContext)).rejects.toBeInstanceOf(ConflictException);

    repository.findOne.mockResolvedValueOnce(null);
    await expect(service.activar('nope', auditContext)).rejects.toBeInstanceOf(NotFoundException);
  });
});
