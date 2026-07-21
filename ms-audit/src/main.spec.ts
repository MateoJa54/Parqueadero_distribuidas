const listen = jest.fn().mockResolvedValue(undefined);
const use = jest.fn();
const useGlobalPipes = jest.fn();
const setGlobalPrefix = jest.fn();

const app = { listen, use, useGlobalPipes, setGlobalPrefix };
const create = jest.fn().mockResolvedValue(app);

jest.mock('@nestjs/core', () => ({
  NestFactory: { create: (...args: unknown[]) => create(...args) },
}));

describe('main bootstrap', () => {
  const flush = () => new Promise((r) => setImmediate(r));

  afterEach(() => {
    jest.resetModules();
    jest.clearAllMocks();
  });

  it('arranca la app, aplica pipes, prefijo y escucha', async () => {
    jest.isolateModules(() => {
      require('./main');
    });
    await flush();

    expect(create).toHaveBeenCalled();
    expect(use).toHaveBeenCalled();
    expect(useGlobalPipes).toHaveBeenCalled();
    expect(setGlobalPrefix).toHaveBeenCalledWith('api/v1');
    expect(listen).toHaveBeenCalled();
  });

  it('el middleware fija cabeceras de seguridad y llama next', async () => {
    jest.isolateModules(() => {
      require('./main');
    });
    await flush();

    const middleware = use.mock.calls[0][0] as (
      req: unknown,
      res: unknown,
      next: () => void,
    ) => void;

    const res = { setHeader: jest.fn(), removeHeader: jest.fn() };
    const next = jest.fn();
    middleware({}, res, next);

    expect(res.setHeader).toHaveBeenCalledWith(
      'X-Content-Type-Options',
      'nosniff',
    );
    expect(res.removeHeader).toHaveBeenCalledWith('X-Powered-By');
    expect(next).toHaveBeenCalled();
  });
});
