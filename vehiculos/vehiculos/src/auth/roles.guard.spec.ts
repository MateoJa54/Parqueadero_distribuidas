import { ExecutionContext, ForbiddenException } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { RolesGuard } from './roles.guard';

describe('RolesGuard', () => {
  let guard: RolesGuard;
  let reflector: jest.Mocked<Pick<Reflector, 'getAllAndOverride'>>;

  const buildContext = (user?: unknown): ExecutionContext => {
    const request = { user };
    return {
      getHandler: () => undefined,
      getClass: () => undefined,
      switchToHttp: () => ({ getRequest: () => request }),
    } as unknown as ExecutionContext;
  };

  beforeEach(() => {
    reflector = { getAllAndOverride: jest.fn() };
    guard = new RolesGuard(reflector as unknown as Reflector);
  });

  it('permite cuando el endpoint no exige roles', () => {
    reflector.getAllAndOverride.mockReturnValue(undefined);
    expect(guard.canActivate(buildContext())).toBe(true);
  });

  it('permite cuando la lista de roles esta vacia', () => {
    reflector.getAllAndOverride.mockReturnValue([]);
    expect(guard.canActivate(buildContext())).toBe(true);
  });

  it('permite cuando el usuario tiene uno de los roles requeridos', () => {
    reflector.getAllAndOverride.mockReturnValue(['ADMIN']);
    expect(guard.canActivate(buildContext({ roles: ['USER', 'ADMIN'] }))).toBe(true);
  });

  it('rechaza cuando el usuario no tiene ningun rol requerido', () => {
    reflector.getAllAndOverride.mockReturnValue(['ADMIN']);
    expect(() => guard.canActivate(buildContext({ roles: ['USER'] }))).toThrow(
      ForbiddenException,
    );
  });

  it('rechaza cuando no hay usuario en el request', () => {
    reflector.getAllAndOverride.mockReturnValue(['ADMIN']);
    expect(() => guard.canActivate(buildContext())).toThrow(ForbiddenException);
  });
});
