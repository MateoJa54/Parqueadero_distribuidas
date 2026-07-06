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

/** Exige que request.user (puesto por JwtAuthGuard) tenga alguno de los @Roles del endpoint. */
@Injectable()
export class RolesGuard implements CanActivate {
  constructor(private readonly reflector: Reflector) {}

  canActivate(context: ExecutionContext): boolean {
    const rolesPermitidos = this.reflector.getAllAndOverride<string[]>(ROLES_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);

    if (!rolesPermitidos || rolesPermitidos.length === 0) {
      return true;
    }

    const request = context.switchToHttp().getRequest<Request>();
    const user = request.user as JwtPayload | undefined;
    const rolesUsuario = user?.roles ?? [];

    const autorizado = rolesPermitidos.some((rol) => rolesUsuario.includes(rol));
    if (!autorizado) {
      throw new ForbiddenException('No tiene permisos para esta operacion');
    }
    return true;
  }
}
