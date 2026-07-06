import {
  CanActivate,
  ExecutionContext,
  ForbiddenException,
  Injectable,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { Request } from 'express';
import { ROLES_KEY } from './roles.decorator';

/**
 * Corre DESPUES de JwtAuthGuard (mismo orden de registro global). Si el
 * endpoint no declara @Roles(...), cualquier usuario autenticado pasa.
 */
@Injectable()
export class RolesGuard implements CanActivate {
  constructor(private readonly reflector: Reflector) {}

  canActivate(context: ExecutionContext): boolean {
    const rolesRequeridos = this.reflector.getAllAndOverride<string[]>(ROLES_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);

    if (!rolesRequeridos || rolesRequeridos.length === 0) {
      return true;
    }

    const request = context.switchToHttp().getRequest<Request>();
    const rolesUsuario = request.usuario?.roles ?? [];
    const autorizado = rolesRequeridos.some((rol) => rolesUsuario.includes(rol));

    if (!autorizado) {
      throw new ForbiddenException('No tiene permisos para esta operacion');
    }
    return true;
  }
}
