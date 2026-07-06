import { Module } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { VehiculosModule } from './vehiculos/vehiculos.module';
import { Vehiculo } from './vehiculos/entities/vehiculo.entity';
import { Auto } from './vehiculos/entities/auto.entity';
import { Motocicleta } from './vehiculos/entities/motocicleta.entity';
import { Camioneta } from './vehiculos/entities/camioneta.entity';
import { JwtAuthGuard } from './auth/jwt-auth.guard';
import { RolesGuard } from './auth/roles.guard';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: '.env',
    }),
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      useFactory: (configService: ConfigService) => ({
        type: 'postgres',
        host: configService.get<string>('DB_HOST') ?? 'localhost',
        port: parseInt(configService.get<string>('DB_PORT') ?? '5433', 10),
        username: configService.get<string>('DB_USUARIO') ?? 'postgres',
        password: configService.get<string>('DB_CONTRASENA') ?? 'postgres',
        database: configService.get<string>('DB_NOMBRE') ?? 'vehiculos_db',
        entities: [Vehiculo, Auto, Motocicleta, Camioneta],
        synchronize: true,
        logging: true,
      }),
      inject: [ConfigService],
    }),
    VehiculosModule,
  ],
  providers: [
    // Toda ruta exige JWT valido; @Roles() en el controller decide el resto.
    { provide: APP_GUARD, useClass: JwtAuthGuard },
    { provide: APP_GUARD, useClass: RolesGuard },
  ],
})
export class AppModule {}
