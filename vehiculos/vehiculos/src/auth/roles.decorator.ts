import { SetMetadata } from '@nestjs/common';

export const ROLES_KEY = 'roles';

/** Roles permitidos para el endpoint. Se combina con RolesGuard. */
export const Roles = (...roles: string[]) => SetMetadata(ROLES_KEY, roles);
