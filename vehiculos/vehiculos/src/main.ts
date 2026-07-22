import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import type { Request, Response, NextFunction } from 'express';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  // Cabeceras de seguridad (sin dependencias externas tipo helmet).
  app.use((_req: Request, res: Response, next: NextFunction) => {
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
    res.setHeader(
      'Strict-Transport-Security',
      'max-age=31536000; includeSubDomains',
    );
    res.removeHeader('X-Powered-By');
    next();
  });

  // Prefijo global para todos los endpoints: /api/vehiculos
  app.setGlobalPrefix('api');

  // Validación global. transform:true es OBLIGATORIO para que la
  // validación polimórfica anidada (@ValidateNested + @Type) funcione.
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true, // elimina propiedades no declaradas en el DTO
      forbidNonWhitelisted: true, // lanza error si llegan propiedades extra
      transform: true, // convierte el payload a la instancia del DTO
      transformOptions: {
        enableImplicitConversion: true, // convierte "2024" -> 2024 en query/params
      },
    }),
  );

  // CORS restringido: por defecto lista blanca de orígenes de desarrollo,
  // configurable en producción vía CORS_ORIGINS (separado por comas).
  const corsOrigins = (
    process.env.CORS_ORIGINS ??
    'http://localhost:5500,http://127.0.0.1:5500,http://localhost:3000,http://localhost:4200'
  )
    .split(',')
    .map((o) => o.trim())
    .filter(Boolean);
  app.enableCors({ origin: corsOrigins, credentials: true });

  const port = process.env.PORT ?? 3000;
  await app.listen(port);
  console.log(`Aplicación corriendo en: http://localhost:${port}/api`);
}
bootstrap();
// Trigger SonarCloud analysis
