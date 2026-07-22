import { ExecutionContext, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as jwt from 'jsonwebtoken';
import { JwtAuthGuard } from './jwt-auth.guard';

jest.mock('jsonwebtoken');

const mockedVerify = jwt.verify as jest.MockedFunction<typeof jwt.verify>;

describe('JwtAuthGuard', () => {
  let guard: JwtAuthGuard;
  let config: jest.Mocked<Pick<ConfigService, 'get'>>;

  const buildContext = (authorization?: string): ExecutionContext => {
    const request: { headers: Record<string, string>; user?: unknown } = {
      headers: authorization ? { authorization } : {},
    };
    return {
      switchToHttp: () => ({ getRequest: () => request }),
    } as unknown as ExecutionContext;
  };

  beforeEach(() => {
    config = { get: jest.fn() };
    guard = new JwtAuthGuard(config as unknown as ConfigService);
    mockedVerify.mockReset();
  });

  it('rechaza cuando no hay header Authorization', () => {
    expect(() => guard.canActivate(buildContext())).toThrow(UnauthorizedException);
  });

  it('rechaza cuando el header no empieza con Bearer', () => {
    expect(() => guard.canActivate(buildContext('Basic abc'))).toThrow(
      UnauthorizedException,
    );
  });

  it('rechaza cuando falta JWT_PUBLIC_KEY', () => {
    config.get.mockReturnValue(undefined);
    expect(() => guard.canActivate(buildContext('Bearer tok'))).toThrow(
      'Configuracion invalida: falta JWT_PUBLIC_KEY',
    );
  });

  it('rechaza cuando el token es invalido', () => {
    config.get.mockImplementation((key: string) =>
      key === 'JWT_PUBLIC_KEY' ? '-----BEGIN PUBLIC KEY-----' : undefined,
    );
    mockedVerify.mockImplementation(() => {
      throw new Error('bad signature');
    });
    expect(() => guard.canActivate(buildContext('Bearer tok'))).toThrow(
      UnauthorizedException,
    );
  });

  it('rechaza cuando el token no es de tipo access', () => {
    config.get.mockImplementation((key: string) =>
      key === 'JWT_PUBLIC_KEY' ? '-----BEGIN PUBLIC KEY-----' : undefined,
    );
    mockedVerify.mockReturnValue({ sub: '1', type: 'refresh' } as never);
    expect(() => guard.canActivate(buildContext('Bearer tok'))).toThrow(
      UnauthorizedException,
    );
  });

  it('acepta un access token valido y adjunta el payload', () => {
    const payload = { sub: '1', type: 'access', roles: ['ADMIN'] };
    config.get.mockImplementation((key: string) =>
      key === 'JWT_PUBLIC_KEY' ? '-----BEGIN PUBLIC KEY-----' : undefined,
    );
    mockedVerify.mockReturnValue(payload as never);
    const context = buildContext('Bearer tok');
    const request = context.switchToHttp().getRequest();
    expect(guard.canActivate(context)).toBe(true);
    expect(request.user).toEqual(payload);
  });

  it('decodifica una clave publica en base64', () => {
    const pem = Buffer.from('-----BEGIN PUBLIC KEY-----').toString('base64');
    config.get.mockImplementation((key: string) =>
      key === 'JWT_PUBLIC_KEY' ? pem : undefined,
    );
    mockedVerify.mockReturnValue({ sub: '1', type: 'access' } as never);
    expect(guard.canActivate(buildContext('Bearer tok'))).toBe(true);
  });
});
