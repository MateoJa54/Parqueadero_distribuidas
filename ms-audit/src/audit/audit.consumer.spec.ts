import { ConfigService } from '@nestjs/config';
import * as amqp from 'amqplib';
import { AuditConsumer } from './audit.consumer';
import { AuditService } from './audit.service';

jest.mock('amqplib');

const mockedConnect = amqp.connect as jest.MockedFunction<typeof amqp.connect>;

describe('AuditConsumer', () => {
  let consumer: AuditConsumer;
  let auditService: { create: jest.Mock };
  let channel: {
    assertExchange: jest.Mock;
    assertQueue: jest.Mock;
    bindQueue: jest.Mock;
    consume: jest.Mock;
    ack: jest.Mock;
    nack: jest.Mock;
  };
  let connection: { createChannel: jest.Mock; on: jest.Mock };

  const config = {
    get: jest.fn((key: string) => `valor-${key}`),
  };

  const buildMessage = (payload: unknown) => ({
    content: Buffer.from(JSON.stringify(payload)),
  });

  const validPayload = {
    servicio: 'ms-vehiculos',
    accion: 'CREATE',
    entidad: 'VEHICULO',
    usuario: 'qa.admin',
    rol: 'ADMIN',
    ip: '127.0.0.1',
    mac: 'AA:BB:CC:DD:EE:FF',
    datos: {},
  };

  beforeEach(() => {
    jest.clearAllMocks();
    auditService = { create: jest.fn().mockResolvedValue(undefined) };
    channel = {
      assertExchange: jest.fn().mockResolvedValue(undefined),
      assertQueue: jest.fn().mockResolvedValue(undefined),
      bindQueue: jest.fn().mockResolvedValue(undefined),
      consume: jest.fn(),
      ack: jest.fn(),
      nack: jest.fn(),
    };
    connection = {
      createChannel: jest.fn().mockResolvedValue(channel),
      on: jest.fn(),
    };
    mockedConnect.mockResolvedValue(connection as never);
    consumer = new AuditConsumer(
      config as unknown as ConfigService,
      auditService as unknown as AuditService,
    );
  });

  async function getMessageHandler() {
    await consumer.onModuleInit();
    const [, handler] = channel.consume.mock.calls[0];
    return handler as (msg: unknown) => Promise<void>;
  }

  it('se conecta, crea canal y registra el consumidor', async () => {
    await consumer.onModuleInit();
    expect(mockedConnect).toHaveBeenCalled();
    expect(channel.assertExchange).toHaveBeenCalled();
    expect(channel.assertQueue).toHaveBeenCalled();
    expect(channel.bindQueue).toHaveBeenCalled();
    expect(channel.consume).toHaveBeenCalled();
  });

  it('procesa un evento valido y hace ack', async () => {
    const handler = await getMessageHandler();
    await handler(buildMessage(validPayload));
    expect(auditService.create).toHaveBeenCalled();
    expect(channel.ack).toHaveBeenCalled();
  });

  it('ignora mensajes nulos', async () => {
    const handler = await getMessageHandler();
    await handler(null);
    expect(auditService.create).not.toHaveBeenCalled();
  });

  it('descarta con nack un evento invalido', async () => {
    const handler = await getMessageHandler();
    await handler(buildMessage({ accion: 'CREATE' }));
    expect(auditService.create).not.toHaveBeenCalled();
    expect(channel.nack).toHaveBeenCalled();
  });

  it('hace nack cuando el guardado lanza un error', async () => {
    auditService.create.mockRejectedValueOnce(new Error('db caida'));
    const handler = await getMessageHandler();
    await handler(buildMessage(validPayload));
    expect(channel.nack).toHaveBeenCalled();
  });

  it('hace nack cuando el contenido no es JSON valido', async () => {
    const handler = await getMessageHandler();
    await handler({ content: Buffer.from('no-json{') });
    expect(auditService.create).not.toHaveBeenCalled();
    expect(channel.nack).toHaveBeenCalled();
  });

  it('registra los handlers close y error de la conexion', async () => {
    jest.useFakeTimers();
    await consumer.onModuleInit();

    const closeHandler = connection.on.mock.calls.find(
      ([evt]) => evt === 'close',
    )?.[1];
    const errorHandler = connection.on.mock.calls.find(
      ([evt]) => evt === 'error',
    )?.[1];

    expect(closeHandler).toBeDefined();
    expect(errorHandler).toBeDefined();

    closeHandler();
    expect(jest.getTimerCount()).toBeGreaterThan(0);

    expect(() => errorHandler({ message: 'boom' })).not.toThrow();

    jest.clearAllTimers();
    jest.useRealTimers();
  });

  it('reintenta cuando falla la conexion inicial', async () => {
    jest.useFakeTimers();
    mockedConnect.mockRejectedValueOnce(new Error('sin broker'));
    await consumer.onModuleInit();
    expect(jest.getTimerCount()).toBeGreaterThan(0);
    jest.clearAllTimers();
    jest.useRealTimers();
  });

  it('reintenta cuando la conexion inicial falla con valor no-Error', async () => {
    jest.useFakeTimers();
    mockedConnect.mockRejectedValueOnce('cadena de error');
    await consumer.onModuleInit();
    expect(jest.getTimerCount()).toBeGreaterThan(0);
    jest.clearAllTimers();
    jest.useRealTimers();
  });

  it('maneja errores al configurar el consumidor', async () => {
    channel.assertExchange.mockRejectedValueOnce(new Error('exchange fail'));
    await expect(consumer.onModuleInit()).resolves.toBeUndefined();
    expect(channel.consume).not.toHaveBeenCalled();
  });

  it('usa mensaje generico cuando el error de guardado no es Error', async () => {
    auditService.create.mockRejectedValueOnce('fallo raro');
    const handler = await getMessageHandler();
    await handler(buildMessage(validPayload));
    expect(channel.nack).toHaveBeenCalled();
  });
});
