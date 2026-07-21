import { Injectable, Logger, OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { AuditService } from './audit.service';
import * as amqp from 'amqplib';
import { plainToInstance } from 'class-transformer';
import { validate, ValidationError } from 'class-validator';
import { CreateAuditEventDto } from './dto/create-audit.dto';

@Injectable()
export class AuditConsumer implements OnModuleInit {
  private readonly logger = new Logger(AuditConsumer.name);
  private connection: any;
  private channel: any;

  constructor(
    private readonly configService: ConfigService,
    private readonly auditService: AuditService,
  ) {}

  async onModuleInit() {
    await this.connect();
  }

  private async connect() {
    const host = this.configService.get('RABBITMQ_HOST');
    const port = this.configService.get('RABBITMQ_PORT');
    const user = this.configService.get('RABBITMQ_USER');
    const pass = this.configService.get('RABBITMQ_PASSWORD');
    const url = `amqp://${user}:${pass}@${host}:${port}`;

    try {
      this.connection = await amqp.connect(url);
      this.channel = await this.connection.createChannel();
      this.logger.log(`Conectado a RabbitMQ en ${host}:${port}`);

      this.connection.on('close', () => {
        this.logger.warn('Conexión a RabbitMQ cerrada, reintentando...');
        setTimeout(() => this.connect(), 5000);
      });
      this.connection.on('error', (err: any) => {
        this.logger.error(`Error en conexión RabbitMQ: ${err.message}`);
      });

      await this.consume();
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : 'Error desconocido';
      this.logger.error(`Fallo al conectar a RabbitMQ: ${errorMessage}`);
      setTimeout(() => this.connect(), 5000);
    }
  }

  private async consume() {
    const queue = this.configService.get('RABBITMQ_QUEUE');
    const exchange = this.configService.get('RABBITMQ_EXCHANGE');
    const routingKey = this.configService.get('RABBITMQ_ROUTING_KEY');

    try {
      await this.channel.assertExchange(exchange, 'topic', { durable: true });
      await this.channel.assertQueue(queue, { durable: true });
      await this.channel.bindQueue(queue, exchange, routingKey);

      this.channel.consume(
        queue,
        async (msg) => {
          if (!msg) return;

          const content = msg.content.toString();
          this.logger.debug(`Mensaje recibido: ${content}`);
          try {
            const raw = JSON.parse(content);
            const dto = plainToInstance(CreateAuditEventDto, raw);
            const errors = await validate(dto);

            if (errors.length > 0) {
              const errorMessages = errors.map((e: ValidationError) =>
                Object.values(e.constraints || {}).join(', '),
              );
              this.logger.warn(`Evento de auditoría inválido, se descarta: ${errorMessages.join('; ')}`);
              this.channel.nack(msg, false, false);
              return;
            }

            await this.auditService.create(dto);
            this.logger.debug('Evento de auditoría guardado exitosamente');
            this.channel.ack(msg);
          } catch (err) {
            const errorMessage =
              err instanceof Error ? err.message : 'Error desconocido';
            this.logger.error(`Error procesando mensaje: ${errorMessage}`);
            this.channel.nack(msg, false, false);
          }
        },
        { noAck: false },
      );
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : 'Error desconocido';
      this.logger.error(`Error configurando consumidor: ${errorMessage}`);
    }
  }
}
