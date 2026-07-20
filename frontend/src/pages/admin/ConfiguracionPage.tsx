import { useState } from 'react';
import { useAuth } from '@/auth/context';
import { ETIQUETA_PERMISO, MATRIZ, type Permiso } from '@/auth/rbac';
import type { Rol } from '@/types';
import { API, decodeJwt, tokenStore } from '@/api/client';
import { useTheme } from '@/ui/ThemeProvider';
import { PageHead } from '@/ui/PageHead';
import { Button } from '@/ui/Button';
import { Select } from '@/ui/Input';
import { Badge } from '@/ui/Badge';
import { useToast } from '@/ui/ToastProvider';
import { getPrefs, setPrefs } from '@/lib/prefs';
import { fmtFecha } from '@/lib/format';

const ROLES: Rol[] = ['ROOT', 'ADMIN', 'RECAUDADOR', 'CLIENTE', 'INVITADO'];
const PERMISOS = Object.keys(ETIQUETA_PERMISO) as Permiso[];

const SERVICIOS: { nombre: string; url: string }[] = [
  { nombre: 'Usuarios / Auth', url: API.usuarios },
  { nombre: 'Zonas / Espacios', url: API.zonas },
  { nombre: 'Asignaciones de vehículos', url: API.asignaciones },
  { nombre: 'Tickets', url: API.tickets },
  { nombre: 'Vehículos', url: API.vehiculos },
  { nombre: 'Auditoría', url: API.audit },
];

export function ConfiguracionPage() {
  const { user } = useAuth();
  const { theme, toggle } = useTheme();
  const toast = useToast();
  const [pageSize, setPageSize] = useState(getPrefs().pageSize);

  const token = tokenStore.get();
  const claims = token ? decodeJwt(token) : null;
  const expira = claims?.exp ? fmtFecha(new Date(claims.exp * 1000).toISOString()) : '—';

  const cambiarTema = (destino: 'light' | 'dark') => {
    if (theme !== destino) toggle();
  };

  const guardarPageSize = (v: number) => {
    setPageSize(v);
    setPrefs({ pageSize: v });
    toast.success('Preferencia guardada', `Filas por página: ${v}`);
  };

  return (
    <>
      <PageHead
        title="Configuración del sistema"
        subtitle="Preferencias de la interfaz, matriz de permisos y estado de la plataforma."
      />

      <div className="grid grid-2">
        {/* Apariencia */}
        <div className="card card-pad">
          <h3 style={{ marginTop: 0 }}>Apariencia</h3>
          <p className="muted" style={{ marginTop: 0 }}>
            Tema de la interfaz (se guarda en este dispositivo).
          </p>
          <div className="row" style={{ gap: 8 }}>
            <Button
              variant={theme === 'light' ? 'primary' : 'secondary'}
              onClick={() => cambiarTema('light')}
              icon="☀"
            >
              Claro
            </Button>
            <Button
              variant={theme === 'dark' ? 'primary' : 'secondary'}
              onClick={() => cambiarTema('dark')}
              icon="🌙"
            >
              Oscuro
            </Button>
          </div>
        </div>

        {/* Preferencias de tablas */}
        <div className="card card-pad">
          <h3 style={{ marginTop: 0 }}>Preferencias de listados</h3>
          <p className="muted" style={{ marginTop: 0 }}>
            Filas por página en tablas paginadas (tickets).
          </p>
          <Select
            label="Filas por página"
            value={String(pageSize)}
            onChange={(e) => guardarPageSize(Number(e.target.value))}
            options={[10, 15, 20, 30, 50].map((n) => ({ value: String(n), label: String(n) }))}
          />
        </div>
      </div>

      {/* Sesión actual */}
      <div className="card card-pad" style={{ marginTop: 24 }}>
        <h3 style={{ marginTop: 0 }}>Sesión actual</h3>
        <dl className="dl">
          <dt>Usuario</dt>
          <dd>{user?.username}</dd>
          <dt>ID</dt>
          <dd className="tnum">{user?.idUsuario}</dd>
          <dt>Roles</dt>
          <dd>
            <div className="row-wrap" style={{ gap: 6 }}>
              {(user?.roles ?? []).map((r) => (
                <Badge key={r} tone="info">
                  {r}
                </Badge>
              ))}
            </div>
          </dd>
          <dt>Token expira</dt>
          <dd>{expira}</dd>
        </dl>
      </div>

      {/* Conexión con servicios (configuración de despliegue) */}
      <div className="card card-pad" style={{ marginTop: 24 }}>
        <h3 style={{ marginTop: 0 }}>Conexión con microservicios</h3>
        <p className="muted" style={{ marginTop: 0 }}>
          URLs base configuradas (variables de entorno). Para verificar conectividad usa{' '}
          <strong>Diagnóstico</strong>.
        </p>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Servicio</th>
                <th>URL base</th>
              </tr>
            </thead>
            <tbody>
              {SERVICIOS.map((s) => (
                <tr key={s.nombre}>
                  <td>{s.nombre}</td>
                  <td className="tnum">{s.url}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Matriz de roles y permisos (referencia de solo lectura) */}
      <div className="card card-pad" style={{ marginTop: 24 }}>
        <h3 style={{ marginTop: 0 }}>Matriz de roles y permisos</h3>
        <p className="muted" style={{ marginTop: 0 }}>
          Qué puede ver y hacer cada rol. Fuente de verdad de la protección de rutas.
        </p>
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Permiso</th>
                {ROLES.map((r) => (
                  <th key={r} style={{ textAlign: 'center' }}>
                    {r}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {PERMISOS.map((p) => (
                <tr key={p}>
                  <td>{ETIQUETA_PERMISO[p]}</td>
                  {ROLES.map((r) => (
                    <td key={r} style={{ textAlign: 'center' }}>
                      {MATRIZ[r].includes(p) ? (
                        <span style={{ color: 'var(--success)' }} aria-label="permitido">
                          ✓
                        </span>
                      ) : (
                        <span className="subtle" aria-label="denegado">
                          —
                        </span>
                      )}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}
