import { API, http, tokenStore } from './client';
import type {
  AuthResponse,
  LoginRequest,
  PerfilResponse,
  RegistroClienteRequest,
  RegistroCompletoRequest,
} from '@/types';

export const authApi = {
  login: (body: LoginRequest) =>
    http.post<AuthResponse>(`${API.usuarios}/auth/login`, body, { auth: false }),

  registrarCliente: (body: RegistroClienteRequest) =>
    http.post<AuthResponse>(`${API.usuarios}/auth/registro-cliente`, body, {
      auth: false,
    }),

  registrarCompleto: (body: RegistroCompletoRequest) =>
    http.post<AuthResponse>(`${API.usuarios}/auth/registro-completo`, body, {
      auth: false,
    }),

  refresh: (refreshToken: string) =>
    http.post<AuthResponse>(`${API.usuarios}/auth/refresh`, { refreshToken }, { auth: false }),

  me: () => http.get<PerfilResponse>(`${API.usuarios}/auth/me`),

  logout: () => tokenStore.clear(),
};
