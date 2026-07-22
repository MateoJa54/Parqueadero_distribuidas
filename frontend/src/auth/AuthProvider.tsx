import { useCallback, useEffect, useMemo, useState } from 'react';
import { authApi } from '@/api/auth';
import {
  decodeJwt,
  isTokenExpired,
  setUnauthorizedHandler,
  tokenStore,
} from '@/api/client';
import type { RegistroCompletoRequest, Rol } from '@/types';
import { AuthContext, type AuthUser } from './context';

function userFromToken(token: string | null): AuthUser | null {
  if (!token) return null;
  const claims = decodeJwt(token);
  if (!claims || isTokenExpired(token, 0)) return null;
  return {
    idUsuario: claims.sub,
    username: claims.username,
    roles: (claims.roles ?? []) as Rol[],
  };
}

export function AuthProvider({ children }: { readonly children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() =>
    userFromToken(tokenStore.get()),
  );
  const [loading, setLoading] = useState(true);

  // Restaura sesión al montar: si hay token válido (o refrescable) reconstruye el usuario.
  useEffect(() => {
    const token = tokenStore.get();
    if (!token) {
      setLoading(false);
      return;
    }
    const restored = userFromToken(token);
    if (restored) {
      setUser(restored);
      setLoading(false);
    } else {
      // Token expirado: intenta refrescar.
      const refresh = tokenStore.getRefresh();
      if (!refresh) {
        tokenStore.clear();
        setLoading(false);
        return;
      }
      authApi
        .refresh(refresh)
        .then((res) => {
          tokenStore.set(res.token, res.refreshToken);
          setUser({
            idUsuario: res.idUsuario,
            username: res.username,
            roles: res.roles,
          });
        })
        .catch(() => {
          tokenStore.clear();
          setUser(null);
        })
        .finally(() => setLoading(false));
    }
  }, []);

  const logout = useCallback(() => {
    authApi.logout();
    setUser(null);
  }, []);

  // 401 irrecuperable desde la capa HTTP → cierra sesión.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      tokenStore.clear();
      setUser(null);
    });
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const res = await authApi.login({ username, password });
    tokenStore.set(res.token, res.refreshToken);
    const u: AuthUser = {
      idUsuario: res.idUsuario,
      username: res.username,
      roles: res.roles,
    };
    setUser(u);
    return u;
  }, []);

  const registrarCliente = useCallback(
    async (body: { dni: string; email: string; username: string; password: string }) => {
      const res = await authApi.registrarCliente(body);
      tokenStore.set(res.token, res.refreshToken);
      const u: AuthUser = {
        idUsuario: res.idUsuario,
        username: res.username,
        roles: res.roles,
      };
      setUser(u);
      return u;
    },
    [],
  );

  const registrarCompleto = useCallback(
    async (body: RegistroCompletoRequest) => {
      const res = await authApi.registrarCompleto(body);
      tokenStore.set(res.token, res.refreshToken);
      const u: AuthUser = {
        idUsuario: res.idUsuario,
        username: res.username,
        roles: res.roles,
      };
      setUser(u);
      return u;
    },
    [],
  );

  const hasRole = useCallback(
    (...roles: Rol[]) => !!user && roles.some((r) => user.roles.includes(r)),
    [user],
  );

  const value = useMemo(
    () => ({ user, loading, login, registrarCliente, registrarCompleto, logout, hasRole }),
    [user, loading, login, registrarCliente, registrarCompleto, logout, hasRole],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
