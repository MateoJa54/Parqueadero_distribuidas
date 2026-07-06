import {
  CanActivate,
  ExecutionContext,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Request } from 'express';
import * as jwt from 'jsonwebtoken';

export interface JwtPayload {
  sub: string;
  username?: string;
  roles?: string[];
  type?: string;
}

/**
 * Valida el JWT emitido por el microservicio usuarios (mismo jwt.secret e
 * issuer). Adjunta el payload decodificado a request.user para que
 * RolesGuard pueda leer los roles.
 */
@Injectable()
export class JwtAuthGuard implements CanActivate {
  constructor(private readonly configService: ConfigService) {}

  canActivate(context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest<Request>();
    const header = request.headers['authorization'];

    if (!header || !header.startsWith('Bearer ')) {
      throw new UnauthorizedException('Token ausente o invalido: inicie sesion');
    }

    const token = header.substring('Bearer '.length).trim();
    const secret =
      this.configService.get<string>('JWT_SECRET') ??
      'parqueadero-espe-clave-secreta-jwt-cambia-esto-en-produccion-2026';
    const issuer = this.configService.get<string>('JWT_ISSUER') ?? 'parqueadero';

    try {
      const payload = jwt.verify(token, secret, { issuer }) as JwtPayload;
      if (payload.type !== 'access') {
        throw new UnauthorizedException('Se esperaba un access token');
      }
      request.user = payload;
      return true;
    } catch {
      throw new UnauthorizedException('Token ausente o invalido: inicie sesion');
    }
  }
}
