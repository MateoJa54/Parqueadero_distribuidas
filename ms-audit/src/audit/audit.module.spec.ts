import { AuditModule } from './audit.module';

describe('AuditModule', () => {
  it('esta definido y registra controladores y proveedores', () => {
    expect(AuditModule).toBeDefined();

    const controllers = Reflect.getMetadata('controllers', AuditModule) ?? [];
    const providers = Reflect.getMetadata('providers', AuditModule) ?? [];
    const imports = Reflect.getMetadata('imports', AuditModule) ?? [];

    expect(controllers.length).toBeGreaterThan(0);
    expect(providers.length).toBeGreaterThan(0);
    expect(imports.length).toBeGreaterThan(0);
  });
});
