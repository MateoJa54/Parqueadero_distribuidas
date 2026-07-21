import 'reflect-metadata';
import { ConfigService } from '@nestjs/config';
import { AppModule } from './app.module';

type Factory = (...args: unknown[]) => unknown;

// forRootAsync anida el modulo dinamico varios niveles; recorremos el arbol de
// 'imports'/'providers' para localizar todas las useFactory registradas.
function recolectarFactories(node: unknown, acc: Factory[] = []): Factory[] {
  if (!node || typeof node !== 'object') return acc;
  const obj = node as Record<string, unknown>;
  if (typeof obj.useFactory === 'function') acc.push(obj.useFactory as Factory);
  for (const key of ['imports', 'providers']) {
    const value = obj[key];
    if (Array.isArray(value)) value.forEach((child) => recolectarFactories(child, acc));
  }
  return acc;
}

describe('AppModule', () => {
  it('se define correctamente', () => {
    expect(new AppModule()).toBeInstanceOf(AppModule);
  });

  it('la fabrica de TypeOrm construye la config desde variables de entorno', () => {
    const imports = Reflect.getMetadata('imports', AppModule) as unknown[];
    const factories = imports.flatMap((imp) => recolectarFactories(imp));

    expect(factories.length).toBeGreaterThan(0);
    const factory = factories[0];

    const configConValores = {
      get: (key: string) =>
        ({
          DB_HOST: 'db',
          DB_PORT: '5555',
          DB_USUARIO: 'user',
          DB_CONTRASENA: 'pass',
          DB_NOMBRE: 'mydb',
        })[key],
    } as unknown as ConfigService;
    const opciones = factory(configConValores) as Record<string, unknown>;
    expect(opciones).toMatchObject({
      type: 'postgres',
      host: 'db',
      port: 5555,
      username: 'user',
      database: 'mydb',
      synchronize: true,
    });

    const configVacia = {
      get: () => undefined,
    } as unknown as ConfigService;
    const opcionesDefault = factory(configVacia) as Record<string, unknown>;
    expect(opcionesDefault).toMatchObject({
      host: 'localhost',
      port: 5433,
      username: 'postgres',
      database: 'vehiculos_db',
    });
  });
});
