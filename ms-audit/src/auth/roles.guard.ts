import {
  CanActivate,
  ExecutionContext,
  ForbiddenException,
  Injectable,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { Request } from 'express';
import { ROLES_KEY } from './roles.decorator';
import { JwtPayload } from './jwt-auth.guard';

/**
 * Autoriza por rol. La auditoria contiene datos sensibles (ip, mac, usuario):
 * un token valido NO basta, se exige ademas un rol permitido (p.ej. ADMIN/ROOT).
 * Corrige la exposicion en la que un token con rol CLIENTE podia leer toda la
 * auditoria. Depende de que JwtAuthGuard (global) ya haya poblado request.user.
 */
@Injectable()
export class RolesGuard implements CanActivate {
  constructor(private readonly reflector: Reflector) {}

  canActivate(context: ExecutionContext): boolean {
    const rolesRequeridos = this.reflector.getAllAndOverride<string[]>(
      ROLES_KEY,
      [context.getHandler(), context.getClass()],
    );

    if (!rolesRequeridos || rolesRequeridos.length === 0) {
      return true;
    }

    const request = context.switchToHttp().getRequest<Request>();
    const user = request.user as JwtPayload | undefined;
    const rolesUsuario = user?.roles ?? [];

    const permitido = rolesUsuario.some((rol) => rolesRequeridos.includes(rol));
    if (!permitido) {
      throw new ForbiddenException(
        'No tiene permisos para acceder a la auditoria',
      );
    }
    return true;
  }
}
