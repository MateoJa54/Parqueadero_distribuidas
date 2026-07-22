import { plainToInstance } from 'class-transformer';
import { validate } from 'class-validator';
import { CreateAuditEventDto } from './create-audit.dto';

describe('CreateAuditEventDto', () => {
  const valido = {
    servicio: 'ms-tickets',
    accion: 'CREATE',
    entidad: 'TICKET',
    usuario: 'qa.admin',
    rol: 'ADMIN',
    ip: '127.0.0.1',
    mac: 'AA:BB:CC:DD:EE:FF',
  };

  it('acepta un evento completo valido', async () => {
    const errors = await validate(plainToInstance(CreateAuditEventDto, valido));
    expect(errors).toHaveLength(0);
  });

  it.each([
    ['servicio', { servicio: 'tickets' }],
    ['accion', { accion: 'UPSERT' }],
    ['entidad', { entidad: 'Ticket' }],
    ['ip', { ip: '::1' }],
    ['mac', { mac: 'sin-mac' }],
  ])('rechaza un %s invalido', async (_campo, cambio) => {
    const dto = plainToInstance(CreateAuditEventDto, { ...valido, ...cambio });
    const errors = await validate(dto);
    expect(errors).not.toHaveLength(0);
  });
});
