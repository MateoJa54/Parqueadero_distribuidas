import { AppModule } from './app.module';

type DynamicLike = {
  providers?: Array<{ useFactory?: (...args: unknown[]) => unknown }>;
};

describe('AppModule', () => {
  it('esta definido', () => {
    expect(AppModule).toBeDefined();
  });

  it('ejecuta las factories async con valores por defecto', () => {
    const imports = (Reflect.getMetadata('imports', AppModule) ??
      []) as DynamicLike[];

    const config = { get: jest.fn(() => undefined) } as unknown;

    const factories = imports
      .flatMap((m) => m?.providers ?? [])
      .map((p) => p.useFactory)
      .filter((f): f is (...args: unknown[]) => unknown => typeof f === 'function');

    expect(factories.length).toBeGreaterThan(0);
    for (const factory of factories) {
      const result = factory(config);
      expect(result).toBeDefined();
    }
  });

  it('ejecuta las factories async con valores configurados', () => {
    const imports = (Reflect.getMetadata('imports', AppModule) ??
      []) as DynamicLike[];

    const config = {
      get: jest.fn((key: string) => {
        const map: Record<string, string> = {
          DB_HOST: 'db',
          DB_PORT: '5432',
          DB_USER: 'user',
          DB_PASSWORD: 'pass',
          DB_NAME: 'name',
          THROTTLE_TTL: '30',
          THROTTLE_LIMIT: '5',
        };
        return map[key];
      }),
    } as unknown;

    const factories = imports
      .flatMap((m) => m?.providers ?? [])
      .map((p) => p.useFactory)
      .filter((f): f is (...args: unknown[]) => unknown => typeof f === 'function');

    for (const factory of factories) {
      expect(factory(config)).toBeDefined();
    }
  });
});
