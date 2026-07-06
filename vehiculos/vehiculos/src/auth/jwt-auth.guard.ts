import {
  CanActivate,
  ExecutionContext,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as jwt from 'jsonwebtoken';
import { Request } from 'express';

export interface UsuarioAutenticado {
  id: string;
  roles: string[];
}

// Node augmenta Request en tiempo de ejecucion; TypeScript necesita la
// declaracion explicita para poder leer/escribir request.usuario despues.
declare module 'express' {
  interface Request {
    usuario?: UsuarioAutenticado;
  }
}

/**
 * Valida el JWT emitido por el microservicio usuarios (mismo secreto/issuer,
 * verificacion stateless sin llamar a usuarios en cada peticion). Si no hay
 * token o es invalido, responde 401 antes de llegar al controlador.
 */
@Injectable()
export class JwtAuthGuard implements CanActivate {
  constructor(private readonly configService: ConfigService) {}

  canActivate(context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest<Request>();
    const header = request.headers['authorization'];

    if (!header || Array.isArray(header) || !header.startsWith('Bearer ')) {
      throw new UnauthorizedException('Token ausente o invalido: inicie sesion');
    }

    const token = header.substring('Bearer '.length).trim();
    const secret =
      this.configService.get<string>('JWT_SECRET') ??
      'parqueadero-espe-clave-secreta-jwt-cambia-esto-en-produccion-2026';
    const issuer = this.configService.get<string>('JWT_ISSUER') ?? 'parqueadero';

    try {
      const payload = jwt.verify(token, secret, { issuer }) as jwt.JwtPayload;
      if (payload.type !== 'access') {
        throw new UnauthorizedException('Se esperaba un access token');
      }
      request.usuario = {
        id: String(payload.sub),
        roles: Array.isArray(payload.roles) ? (payload.roles as string[]) : [],
      };
      return true;
    } catch {
      throw new UnauthorizedException('Token ausente o invalido: inicie sesion');
    }
  }
}
