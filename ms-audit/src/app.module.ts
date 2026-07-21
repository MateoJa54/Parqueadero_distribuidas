import { Module } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ThrottlerModule } from '@nestjs/throttler';
import { AuditModule } from './audit/audit.module';
import { EventoAuditoria } from './audit/entities/evento-auditoria.entity';
import { JwtAuthGuard } from './auth/jwt-auth.guard';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: '.env',
    }),
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      useFactory: (config: ConfigService) => ({
        type: 'postgres',
        host: config.get<string>('DB_HOST') ?? 'localhost',
        port: Number.parseInt(config.get<string>('DB_PORT') ?? '5433', 10),
        username: config.get<string>('DB_USER') ?? 'audit_user',
        password: config.get<string>('DB_PASSWORD') ?? 'audit_pass',
        database: config.get<string>('DB_NAME') ?? 'audit_db',
        entities: [EventoAuditoria],
        synchronize: true,
        logging: false,
      }),
      inject: [ConfigService],
    }),
    ThrottlerModule.forRootAsync({
      imports: [ConfigModule],
      useFactory: (config: ConfigService) => ({
        throttlers: [
          {
            ttl: +(config.get('THROTTLE_TTL') ?? 60),
            limit: +(config.get('THROTTLE_LIMIT') ?? 10),
          },
        ],
      }),
      inject: [ConfigService],
    }),
    AuditModule,
  ],
  providers: [
    // Toda ruta de ms-audit exige JWT valido: contiene datos sensibles
    // (ip, mac, usuario) y debe protegerse aunque se acceda sin pasar por Kong.
    { provide: APP_GUARD, useClass: JwtAuthGuard },
  ],
})
export class AppModule {}
