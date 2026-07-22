import { SetMetadata } from '@nestjs/common';

export const ROLES_KEY = 'roles';

/**
 * Marca los roles autorizados para una ruta. Se evalua con RolesGuard tras el
 * JwtAuthGuard global (que ya deja request.user con los roles del token).
 */
export const Roles = (...roles: string[]) => SetMetadata(ROLES_KEY, roles);
