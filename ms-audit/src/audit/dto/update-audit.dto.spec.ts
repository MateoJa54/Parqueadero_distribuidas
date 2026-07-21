import { UpdateAuditDto } from './update-audit.dto';

describe('UpdateAuditDto', () => {
  it('se puede instanciar y permite campos parciales', () => {
    const dto = new UpdateAuditDto();
    dto.accion = 'CREATE';
    expect(dto).toBeInstanceOf(UpdateAuditDto);
    expect(dto.accion).toBe('CREATE');
  });
});
