import { Test, TestingModule } from '@nestjs/testing';
import type { Request } from 'express';
import { CreateVehiculoDto } from './dto/create-vehiculo.dto';
import { UpdateVehiculoDto } from './dto/update-vehiculo.dto';
import { VehiculosController } from './vehiculos.controller';
import { VehiculosService } from './vehiculos.service';

describe('VehiculosController', () => {
  let controller: VehiculosController;
  let service: {
    create: jest.Mock;
    findAll: jest.Mock;
    findByPlaca: jest.Mock;
    findOne: jest.Mock;
    update: jest.Mock;
    activar: jest.Mock;
    desactivar: jest.Mock;
  };

  const reqConUsuario = (): Request =>
    ({
      user: { username: 'qa.admin', roles: ['ADMIN'] },
      headers: {},
      ip: '10.0.0.1',
      socket: {},
    }) as unknown as Request;

  beforeEach(async () => {
    service = {
      create: jest.fn(),
      findAll: jest.fn(),
      findByPlaca: jest.fn(),
      findOne: jest.fn(),
      update: jest.fn(),
      activar: jest.fn(),
      desactivar: jest.fn(),
    };
    const module: TestingModule = await Test.createTestingModule({
      controllers: [VehiculosController],
      providers: [{ provide: VehiculosService, useValue: service }],
    }).compile();

    controller = module.get(VehiculosController);
  });

  it('convierte el query incluirInactivos a booleano estricto', () => {
    controller.findAll('true');
    expect(service.findAll).toHaveBeenCalledWith(true);

    controller.findAll('TRUE');
    expect(service.findAll).toHaveBeenLastCalledWith(false);
  });

  it('prioriza X-Forwarded-For y normaliza la IP del contexto de auditoria', () => {
    const dto = {} as CreateVehiculoDto;
    const req = {
      user: { username: 'qa.admin', roles: ['ADMIN'] },
      headers: {
        'x-forwarded-for': '::ffff:192.168.1.15, 10.0.0.1',
        'x-device-mac': 'AA:BB:CC:DD:EE:FF',
      },
      ip: '::1',
      socket: {},
    } as unknown as Request;

    controller.create(dto, req);

    expect(service.create).toHaveBeenCalledWith(dto, {
      usuario: 'qa.admin',
      rol: 'ADMIN',
      ip: '192.168.1.15',
      mac: 'AA:BB:CC:DD:EE:FF',
    });
  });

  it('usa valores locales seguros cuando no llegan cabeceras opcionales', () => {
    const dto = {} as CreateVehiculoDto;
    const req = {
      headers: {},
      ip: '::1',
      socket: {},
    } as unknown as Request;

    controller.create(dto, req);

    expect(service.create).toHaveBeenCalledWith(dto, {
      usuario: undefined,
      rol: undefined,
      ip: '127.0.0.1',
      mac: '00:00:00:00:00:00',
    });
  });

  it('findByPlaca y findOne delegan con el parametro', () => {
    controller.findByPlaca('ABC-1234');
    expect(service.findByPlaca).toHaveBeenCalledWith('ABC-1234');
    controller.findOne('id-1');
    expect(service.findOne).toHaveBeenCalledWith('id-1');
  });

  it('update, activar y desactivar delegan con id y contexto', () => {
    const dto = {} as UpdateVehiculoDto;
    controller.update('id-1', dto, reqConUsuario());
    expect(service.update).toHaveBeenCalledWith(
      'id-1',
      dto,
      expect.objectContaining({ usuario: 'qa.admin' }),
    );

    controller.activar('id-1', reqConUsuario());
    expect(service.activar).toHaveBeenCalledWith(
      'id-1',
      expect.objectContaining({ usuario: 'qa.admin' }),
    );

    controller.desactivar('id-1', reqConUsuario());
    expect(service.desactivar).toHaveBeenCalledWith(
      'id-1',
      expect.objectContaining({ usuario: 'qa.admin' }),
    );
  });
});
