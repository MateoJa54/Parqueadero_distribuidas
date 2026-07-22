import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { AuditService } from './audit.service';
import { CreateAuditEventDto } from './dto/create-audit.dto';
import { EventoAuditoria } from './entities/evento-auditoria.entity';

describe('AuditService', () => {
  let service: AuditService;
  let repository: jest.Mocked<Pick<Repository<EventoAuditoria>, 'create' | 'save' | 'find' | 'findOne'>>;

  beforeEach(async () => {
    repository = {
      create: jest.fn(),
      save: jest.fn(),
      find: jest.fn(),
      findOne: jest.fn(),
    };
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        AuditService,
        { provide: getRepositoryToken(EventoAuditoria), useValue: repository },
      ],
    }).compile();
    service = module.get(AuditService);
  });

  it('agrega el timestamp del servidor y persiste el evento', async () => {
    const dto: CreateAuditEventDto = {
      servicio: 'ms-tickets',
      accion: 'CREATE',
      entidad: 'TICKET',
      ip: '127.0.0.1',
      mac: 'AA:BB:CC:DD:EE:FF',
    };
    const entity = Object.assign(new EventoAuditoria(), dto, { id: 'evento-1' });
    repository.create.mockReturnValue(entity);
    repository.save.mockResolvedValue(entity);

    const result = await service.create(dto);

    expect(repository.create).toHaveBeenCalledWith(
      expect.objectContaining({ ...dto, timestamp: expect.any(Date) }),
    );
    expect(repository.save).toHaveBeenCalledWith(entity);
    expect(result).toBe(entity);
  });

  it('lista los eventos del mas reciente al mas antiguo', async () => {
    repository.find.mockResolvedValue([]);
    await service.findAll();
    expect(repository.find).toHaveBeenCalledWith({ order: { timestamp: 'DESC' } });
  });

  it('busca un evento por id', async () => {
    repository.findOne.mockResolvedValue(null);
    await expect(service.findOne('evento-1')).resolves.toBeNull();
    expect(repository.findOne).toHaveBeenCalledWith({ where: { id: 'evento-1' } });
  });
});
