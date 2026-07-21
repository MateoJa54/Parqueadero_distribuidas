import { ConfigService } from '@nestjs/config';
import * as amqp from 'amqplib';
import { AuditEvent, EventPublisher } from './event-publisher.service';

jest.mock('amqplib');

const mockedConnect = amqp.connect as jest.MockedFunction<typeof amqp.connect>;

describe('EventPublisher', () => {
  let config: { get: jest.Mock };
  let channel: {
    assertExchange: jest.Mock;
    publish: jest.Mock;
    close: jest.Mock;
  };
  let connection: {
    createChannel: jest.Mock;
    on: jest.Mock;
    close: jest.Mock;
    handlers: Record<string, (arg?: unknown) => void>;
  };

  const evento: AuditEvent = {
    servicio: 'ms-vehiculos',
    accion: 'CREATE',
    entidad: 'VEHICULO',
    ip: '127.0.0.1',
    mac: 'AA:BB:CC:DD:EE:FF',
  };

  const buildPublisher = () =>
    new EventPublisher(config as unknown as ConfigService);

  beforeEach(() => {
    jest.useFakeTimers();
    config = { get: jest.fn().mockReturnValue(undefined) };
    channel = {
      assertExchange: jest.fn().mockResolvedValue(undefined),
      publish: jest.fn(),
      close: jest.fn().mockResolvedValue(undefined),
    };
    connection = {
      createChannel: jest.fn().mockResolvedValue(channel),
      on: jest.fn(function (this: unknown, ev: string, cb: (a?: unknown) => void) {
        connection.handlers[ev] = cb;
      }),
      close: jest.fn().mockResolvedValue(undefined),
      handlers: {},
    };
    mockedConnect.mockResolvedValue(connection as never);
  });

  afterEach(() => {
    jest.clearAllTimers();
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  it('usa valores por defecto de exchange y routingKey', () => {
    const publisher = buildPublisher();
    expect(publisher).toBeDefined();
    expect(config.get).toHaveBeenCalledWith('RABBITMQ_EXCHANGE');
    expect(config.get).toHaveBeenCalledWith('RABBITMQ_ROUTING_KEY');
  });

  it('respeta configuracion personalizada de rabbitmq', async () => {
    config.get.mockImplementation((key: string) => {
      const map: Record<string, string> = {
        RABBITMQ_EXCHANGE: 'x',
        RABBITMQ_ROUTING_KEY: 'k',
        RABBITMQ_HOST: 'h',
        RABBITMQ_PORT: '1',
        RABBITMQ_USER: 'u',
        RABBITMQ_PASSWORD: 'p',
      };
      return map[key];
    });
    const publisher = buildPublisher();
    await publisher.onModuleInit();
    expect(mockedConnect).toHaveBeenCalledWith('amqp://u:p@h:1');
    expect(channel.assertExchange).toHaveBeenCalledWith('x', 'topic', {
      durable: true,
    });
  });

  it('onModuleInit conecta y publica un evento', async () => {
    const publisher = buildPublisher();
    await publisher.onModuleInit();
    await publisher.publish(evento);
    expect(channel.publish).toHaveBeenCalledWith(
      'audit_exchange',
      'audit.event',
      expect.any(Buffer),
      { persistent: true },
    );
  });

  it('reutiliza una conexion en curso (connectionPromise)', async () => {
    const publisher = buildPublisher();
    await Promise.all([publisher.onModuleInit(), publisher.onModuleInit()]);
    expect(mockedConnect).toHaveBeenCalledTimes(1);
  });

  it('publish intenta conectar si el canal no existe y aborta si falla', async () => {
    mockedConnect.mockRejectedValue(new Error('down'));
    const publisher = buildPublisher();
    await expect(publisher.publish(evento)).rejects.toThrow('down');
    expect(channel.publish).not.toHaveBeenCalled();
  });

  it('maneja errores al publicar el mensaje', async () => {
    const publisher = buildPublisher();
    await publisher.onModuleInit();
    channel.publish.mockImplementation(() => {
      throw new Error('publish boom');
    });
    await publisher.publish(evento);
    // tras el error queda marcado como desconectado
    expect(channel.publish).toHaveBeenCalled();
  });

  it('programa una reconexion cuando la conexion se cierra', async () => {
    const publisher = buildPublisher();
    await publisher.onModuleInit();
    connection.handlers['close']();
    jest.advanceTimersByTime(5000);
    // se dispara un nuevo intento de conexion
    expect(mockedConnect).toHaveBeenCalledTimes(2);
  });

  it('programa una reconexion cuando hay un error en la conexion', async () => {
    const publisher = buildPublisher();
    await publisher.onModuleInit();
    connection.handlers['error'](new Error('socket'));
    jest.advanceTimersByTime(5000);
    expect(mockedConnect).toHaveBeenCalledTimes(2);
  });

  it('reprograma reconexion si connect falla en el timeout', async () => {
    mockedConnect.mockRejectedValueOnce(new Error('fail1'));
    const publisher = buildPublisher();
    await expect(publisher.onModuleInit()).rejects.toThrow('fail1');
    // el catch programa un reconnect; avanzamos para ejecutarlo
    jest.advanceTimersByTime(5000);
    expect(mockedConnect).toHaveBeenCalledTimes(2);
  });

  it('onModuleDestroy cierra canal y conexion', async () => {
    const publisher = buildPublisher();
    await publisher.onModuleInit();
    await publisher.onModuleDestroy();
    expect(channel.close).toHaveBeenCalled();
    expect(connection.close).toHaveBeenCalled();
  });

  it('onModuleDestroy ignora errores de cierre', async () => {
    const publisher = buildPublisher();
    await publisher.onModuleInit();
    channel.close.mockRejectedValue(new Error('close boom'));
    await expect(publisher.onModuleDestroy()).resolves.toBeUndefined();
  });

  it('onModuleDestroy sin conexion previa no falla', async () => {
    const publisher = buildPublisher();
    await expect(publisher.onModuleDestroy()).resolves.toBeUndefined();
  });
});
