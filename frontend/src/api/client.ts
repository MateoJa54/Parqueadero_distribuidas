// ============================================================================
// Cliente HTTP base — inyección de Authorization, manejo de errores y refresh
// ============================================================================
import type { JwtClaims } from '@/types';

export const API = {
  usuarios: import.meta.env.VITE_API_USUARIOS,
  zonas: import.meta.env.VITE_API_ZONAS,
  asignaciones: import.meta.env.VITE_API_ASIGNACIONES,
  tickets: import.meta.env.VITE_API_TICKETS,
  vehiculos: import.meta.env.VITE_API_VEHICULOS,
  audit: import.meta.env.VITE_API_AUDIT,
} as const;

const TOKEN_KEY = 'pq_token';
const REFRESH_KEY = 'pq_refresh';

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  getRefresh: () => localStorage.getItem(REFRESH_KEY),
  set: (token: string, refresh: string) => {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(REFRESH_KEY, refresh);
  },
  clear: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};

/** Decodifica el payload de un JWT sin validar la firma (solo lectura de claims). */
export function decodeJwt(token: string): JwtClaims | null {
  try {
    const [, payload] = token.split('.');
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(decodeURIComponent(escape(json))) as JwtClaims;
  } catch {
    return null;
  }
}

export function isTokenExpired(token: string, skewSeconds = 30): boolean {
  const claims = decodeJwt(token);
  if (!claims?.exp) return true;
  return Date.now() / 1000 >= claims.exp - skewSeconds;
}

export class ApiError extends Error {
  status: number;
  body: unknown;
  constructor(status: number, message: string, body?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}

// Callback que la capa de auth registra para forzar logout ante 401 irrecuperable.
let onUnauthorized: (() => void) | null = null;
export function setUnauthorizedHandler(fn: () => void) {
  onUnauthorized = fn;
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  auth?: boolean; // adjuntar Bearer (default true)
  signal?: AbortSignal;
  raw?: boolean; // no parsear JSON de respuesta
}

let refreshing: Promise<string | null> | null = null;

/** Intenta refrescar el access token usando el refresh token. Devuelve el nuevo token o null. */
async function tryRefresh(): Promise<string | null> {
  const refresh = tokenStore.getRefresh();
  if (!refresh) return null;
  // Evita múltiples refresh en paralelo.
  if (!refreshing) {
    refreshing = (async () => {
      try {
        const res = await fetch(`${API.usuarios}/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken: refresh }),
        });
        if (!res.ok) return null;
        const data = (await res.json()) as { token: string; refreshToken: string };
        tokenStore.set(data.token, data.refreshToken);
        return data.token;
      } catch {
        return null;
      } finally {
        // liberado abajo
      }
    })();
  }
  const result = await refreshing;
  refreshing = null;
  return result;
}

async function doFetch(url: string, options: RequestOptions, token: string | null) {
  const headers: Record<string, string> = {};
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';
  if (options.auth !== false && token) headers['Authorization'] = `Bearer ${token}`;

  return fetch(url, {
    method: options.method ?? 'GET',
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    signal: options.signal,
  });
}

export async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  let token = tokenStore.get();

  // Refresco proactivo si el access está por expirar.
  if (options.auth !== false && token && isTokenExpired(token)) {
    const refreshed = await tryRefresh();
    token = refreshed ?? token;
  }

  let res = await doFetch(url, options, token);

  // 401: intento un refresh reactivo y reintento UNA vez.
  if (res.status === 401 && options.auth !== false) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      res = await doFetch(url, options, refreshed);
    } else {
      onUnauthorized?.();
      throw new ApiError(401, 'Sesión expirada. Inicia sesión nuevamente.');
    }
  }

  if (res.status === 204) return undefined as T;

  const text = await res.text();
  let payload: unknown = undefined;
  if (text) {
    try {
      payload = JSON.parse(text);
    } catch {
      payload = text;
    }
  }

  if (!res.ok) {
    if (res.status === 401) onUnauthorized?.();
    throw new ApiError(res.status, extractMessage(payload, res.status), payload);
  }

  return payload as T;
}

function extractMessage(payload: unknown, status: number): string {
  if (payload && typeof payload === 'object') {
    const p = payload as Record<string, unknown>;
    if (typeof p.mensaje === 'string') return p.mensaje;
    if (typeof p.message === 'string') return p.message;
    if (typeof p.error === 'string') return p.error;
    if (p.errores && typeof p.errores === 'object') {
      return Object.values(p.errores as Record<string, string>).join(' · ');
    }
  }
  if (typeof payload === 'string' && payload.trim()) return payload;
  const map: Record<number, string> = {
    400: 'Solicitud inválida.',
    403: 'No tienes permiso para esta acción.',
    404: 'Recurso no encontrado.',
    409: 'Conflicto con el estado actual.',
    500: 'Error interno del servidor.',
  };
  return map[status] ?? `Error ${status}`;
}

// Helpers por verbo
export const http = {
  get: <T>(url: string, opts?: RequestOptions) => request<T>(url, { ...opts, method: 'GET' }),
  post: <T>(url: string, body?: unknown, opts?: RequestOptions) =>
    request<T>(url, { ...opts, method: 'POST', body }),
  put: <T>(url: string, body?: unknown, opts?: RequestOptions) =>
    request<T>(url, { ...opts, method: 'PUT', body }),
  patch: <T>(url: string, body?: unknown, opts?: RequestOptions) =>
    request<T>(url, { ...opts, method: 'PATCH', body }),
  del: <T>(url: string, opts?: RequestOptions) => request<T>(url, { ...opts, method: 'DELETE' }),
};
