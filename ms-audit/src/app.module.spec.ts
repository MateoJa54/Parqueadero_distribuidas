import { AppModule } from './app.module';

type DynamicLike = {
  imports?: DynamicLike[];
  providers?: Array<{ useFactory?: (...args: unknown[]) => unknown }>;
};

function collectFactories(
  nodes: DynamicLike[],
): Array<(...args: unknown[]) => unknown> {
  const factories: Array<(...args: unknown[]) => unknown> = [];
  for (const node of nodes) {
    if (!node) continue;
    for (const provider of node.providers ?? []) {
      if (typeof provider.useFactory === 'function') {
        factories.push(provider.useFactory);
      }
    }
    if (node.imports?.length) {
      factories.push(...collectFactories(node.imports));
    }
  }
  return factories;
}

function getFactories(): Array<(...args: unknown[]) => unknown> {
  const imports = (Reflect.getMetadata('imports', AppModule) ??
    []) as DynamicLike[];
  return collectFactories(imports);
}

describe('AppModule', () => {
  it('esta definido', () => {
    expect(AppModule).toBeDefined();
  });

  it('ejecuta las factories async con valores por defecto', () => {
    const config = { get: jest.fn(() => undefined) } as unknown;
    const factories = getFactories();
    expect(factories.length).toBeGreaterThan(0);
    for (const factory of factories) {
      factory(config);
    }
  });

  it('ejecuta las factories async con valores configurados', () => {
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
    const factories = getFactories();
    for (const factory of factories) {
      factory(config);
    }
  });
});
