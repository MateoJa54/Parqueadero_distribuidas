const app = {
  use: jest.fn(),
  setGlobalPrefix: jest.fn(),
  useGlobalPipes: jest.fn(),
  enableCors: jest.fn(),
  listen: jest.fn().mockResolvedValue(undefined),
};

const createMock = jest.fn().mockResolvedValue(app);

// AppModule arrastra TypeORM/entidades; lo aislamos para que importar main.ts
// no intente conectar a una base de datos real.
jest.mock('./app.module', () => ({ AppModule: class {} }));

async function cargarBootstrap(): Promise<void> {
  jest.resetModules();
  jest.doMock('@nestjs/core', () => ({
    NestFactory: { create: createMock },
  }));
  jest.doMock('./app.module', () => ({ AppModule: class {} }));
  require('./main');
  await new Promise((r) => setImmediate(r));
}

describe('main bootstrap', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    createMock.mockResolvedValue(app);
    jest.spyOn(console, 'log').mockImplementation(() => undefined);
  });

  afterEach(() => {
    delete process.env.CORS_ORIGINS;
    delete process.env.PORT;
  });

  it('configura seguridad, prefijo, validacion, cors y escucha el puerto', async () => {
    process.env.CORS_ORIGINS = 'https://a.com, https://b.com';
    process.env.PORT = '4100';

    await cargarBootstrap();

    expect(createMock).toHaveBeenCalled();
    expect(app.setGlobalPrefix).toHaveBeenCalledWith('api');
    expect(app.useGlobalPipes).toHaveBeenCalled();
    expect(app.enableCors).toHaveBeenCalledWith({
      origin: ['https://a.com', 'https://b.com'],
      credentials: true,
    });
    expect(app.listen).toHaveBeenCalledWith('4100');

    // Ejercita el middleware de cabeceras de seguridad.
    const middleware = app.use.mock.calls[0][0] as (
      req: unknown,
      res: { setHeader: jest.Mock; removeHeader: jest.Mock },
      next: jest.Mock,
    ) => void;
    const res = { setHeader: jest.fn(), removeHeader: jest.fn() };
    const next = jest.fn();
    middleware({}, res, next);
    expect(res.setHeader).toHaveBeenCalledWith('X-Content-Type-Options', 'nosniff');
    expect(res.removeHeader).toHaveBeenCalledWith('X-Powered-By');
    expect(next).toHaveBeenCalled();
  });

  it('usa origenes y puerto por defecto cuando no hay variables', async () => {
    await cargarBootstrap();

    expect(app.enableCors).toHaveBeenCalledWith(
      expect.objectContaining({
        origin: expect.arrayContaining(['http://localhost:3000']),
        credentials: true,
      }),
    );
    expect(app.listen).toHaveBeenCalledWith(3000);
  });
});
