import { createContext, useContext } from 'react';
import type { RegistroCompletoRequest, Rol } from '@/types';

export interface AuthUser {
  idUsuario: string;
  username: string;
  roles: Rol[];
}

export interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<AuthUser>;
  registrarCliente: (body: {
    dni: string;
    email: string;
    username: string;
    password: string;
  }) => Promise<AuthUser>;
  registrarCompleto: (body: RegistroCompletoRequest) => Promise<AuthUser>;
  logout: () => void;
  hasRole: (...roles: Rol[]) => boolean;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>');
  return ctx;
}
