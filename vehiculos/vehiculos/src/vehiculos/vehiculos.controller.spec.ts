import { Test, TestingModule } from '@nestjs/testing';
import type { Request } from 'express';
import { CreateVehiculoDto } from './dto/create-vehiculo.dto';
import { VehiculosController } from './vehiculos.controller';
import { VehiculosService } from './vehiculos.service';

describe('VehiculosController', () => {
  let controller: VehiculosController;
  let service: { create: jest.Mock; findAll: jest.Mock };

  beforeEach(async () => {
    service = {
      create: jest.fn(),
      findAll: jest.fn(),
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
});
