import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

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

  // CORS habilitado para poder consumir desde Postman / frontend
  app.enableCors();

  const port = process.env.PORT ?? 3000;
  await app.listen(port);
  console.log(`Aplicación corriendo en: http://localhost:${port}/api`);
}
bootstrap();
