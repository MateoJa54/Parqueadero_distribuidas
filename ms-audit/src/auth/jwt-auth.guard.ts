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
 * Valida el JWT emitido por el microservicio usuarios (RS256). Solo verifica la
 * firma con la clave PUBLICA (JWT_PUBLIC_KEY); no puede emitir ni re-firmar
 * tokens. Se aplica globalmente: ms-audit contiene datos sensibles (ip, mac,
 * usuario) y debe exigir un token valido aunque se acceda directo al puerto
 * 3002, sin pasar por Kong.
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
    const publicKey = this.resolvePublicKey();
    const issuer = this.configService.get<string>('JWT_ISSUER') ?? 'parqueadero';

    try {
      const payload = jwt.verify(token, publicKey, {
        issuer,
        algorithms: ['RS256'],
      }) as JwtPayload;
      if (payload.type !== 'access') {
        throw new UnauthorizedException('Se esperaba un access token');
      }
      request.user = payload;
      return true;
    } catch {
      throw new UnauthorizedException('Token ausente o invalido: inicie sesion');
    }
  }

  /**
   * Carga la clave PUBLICA RSA usada solo para VERIFICAR la firma. Acepta el PEM
   * directo o el PEM codificado en base64 (variable JWT_PUBLIC_KEY). No existe
   * valor por defecto: sin clave publica no se verifica, evitando secretos
   * hardcodeados. Al ser asimetrica, este servicio NO puede firmar/re-firmar
   * tokens: solo usuarios (dueno de la clave privada) puede emitirlos.
   */
  private resolvePublicKey(): string {
    const material = this.configService.get<string>('JWT_PUBLIC_KEY');
    if (!material) {
      throw new UnauthorizedException(
        'Configuracion invalida: falta JWT_PUBLIC_KEY',
      );
    }
    const value = material.trim();
    return value.includes('BEGIN')
      ? value
      : Buffer.from(value, 'base64').toString('utf8');
  }
}
