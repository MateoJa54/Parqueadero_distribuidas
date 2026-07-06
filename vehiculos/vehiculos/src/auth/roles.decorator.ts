import { SetMetadata } from '@nestjs/common';

export const ROLES_KEY = 'roles';

/**
 * Restringe un endpoint a los roles dados (ej. @Roles('ADMIN', 'ROOT')).
 * Sin este decorador, el endpoint solo exige estar autenticado (cualquier rol).
 */
export const Roles = (...roles: string[]) => SetMetadata(ROLES_KEY, roles);
