import { Test, TestingModule } from '@nestjs/testing';
import { AuditController } from './audit.controller';
import { AuditService } from './audit.service';
import { CreateAuditEventDto } from './dto/create-audit.dto';

describe('AuditController', () => {
  let controller: AuditController;
  const service = {
    create: jest.fn(),
    findAll: jest.fn(),
    findOne: jest.fn(),
  };

  beforeEach(async () => {
    jest.clearAllMocks();
    const module: TestingModule = await Test.createTestingModule({
      controllers: [AuditController],
      providers: [{ provide: AuditService, useValue: service }],
    }).compile();
    controller = module.get(AuditController);
  });

  it('create delega en el servicio', () => {
    const dto = {} as CreateAuditEventDto;
    service.create.mockReturnValue('creado');
    expect(controller.create(dto)).toBe('creado');
    expect(service.create).toHaveBeenCalledWith(dto);
  });

  it('findAll delega en el servicio', () => {
    service.findAll.mockReturnValue(['a']);
    expect(controller.findAll()).toEqual(['a']);
    expect(service.findAll).toHaveBeenCalled();
  });

  it('findOne delega en el servicio con el id', () => {
    service.findOne.mockReturnValue('uno');
    expect(controller.findOne('id-1')).toBe('uno');
    expect(service.findOne).toHaveBeenCalledWith('id-1');
  });
});
