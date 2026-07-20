import { ForbiddenException } from '@nestjs/common';
import type { ExecutionContext } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { RolesGuard } from './roles.guard';

describe('RolesGuard', () => {
  function context(roles: string[]): ExecutionContext {
    return {
      getHandler: jest.fn(),
      getClass: jest.fn(),
      switchToHttp: () => ({
        getRequest: () => ({ user: { roles } }),
      }),
    } as unknown as ExecutionContext;
  }

  it('permite ROOT cuando el endpoint exige ADMIN o ROOT', () => {
    const reflector = {
      getAllAndOverride: jest.fn().mockReturnValue(['ADMIN', 'ROOT']),
    } as unknown as Reflector;
    expect(new RolesGuard(reflector).canActivate(context(['ROOT']))).toBe(true);
  });

  it('rechaza CLIENTE cuando intenta acceder a auditoria', () => {
    const reflector = {
      getAllAndOverride: jest.fn().mockReturnValue(['ADMIN', 'ROOT']),
    } as unknown as Reflector;
    expect(() => new RolesGuard(reflector).canActivate(context(['CLIENTE']))).toThrow(
      ForbiddenException,
    );
  });

  it('permite rutas sin metadatos de rol', () => {
    const reflector = {
      getAllAndOverride: jest.fn().mockReturnValue(undefined),
    } as unknown as Reflector;
    expect(new RolesGuard(reflector).canActivate(context([]))).toBe(true);
  });
});
