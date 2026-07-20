import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '@/auth/context';
import { API, ApiError } from '@/api/client';
import { authApi } from '@/api/auth';
import { rolesApi } from '@/api/usuarios';
import { zonasApi, espaciosApi } from '@/api/zonas';
import { propietariosApi } from '@/api/asignaciones';
import { vehiculosApi } from '@/api/vehiculos';
import { ticketsApi, auditApi } from '@/api/tickets';
import { PageHead } from '@/ui/PageHead';
import { Button } from '@/ui/Button';
import { Badge } from '@/ui/Badge';

type Estado = 'idle' | 'running' | 'ok' | 'fail';

interface Check {
  id: string;
  servicio: string;
  nombre: string;
  base: string;
  run: () => Promise<string>;
}

interface Resultado {
  estado: Estado;
  ms?: number;
  httpStatus?: number;
  detalle?: string;
  hint?: string;
}

function pista(status?: number, esRed?: boolean): string | undefined {
  if (esRed) return 'Servicio caído o CORS no habilitado para el origen del frontend.';
  if (status === undefined) return undefined;
  const m: Record<number, string> = {
    401: 'Token inválido/expirado o refresh fallando.',
    403: 'Falta permiso (el rol no autoriza este endpoint).',
    404: 'Ruta o URL base incorrecta (revisa .env / contrato).',
    409: 'Regla de negocio (esperable en algunos casos).',
    500: 'Error interno del microservicio (revisa sus logs).',
  };
  return m[status];
}

export function DiagnosticoPage() {
  const { user } = useAuth();

  const checks: Check[] = [
    {
      id: 'auth',
      servicio: 'usuarios',
      nombre: 'Autenticación (GET /auth/me)',
      base: API.usuarios,
      run: async () => {
        const p = await authApi.me();
        return `Identidad: ${p.firstName} ${p.lastName}`;
      },
    },
    {
      id: 'roles',
      servicio: 'usuarios',
      nombre: 'Roles (GET /roles)',
      base: API.usuarios,
      run: async () => `${(await rolesApi.list()).length} roles`,
    },
    {
      id: 'zonas',
      servicio: 'zonas',
      nombre: 'Zonas (GET /zonas)',
      base: API.zonas,
      run: async () => `${(await zonasApi.list()).length} zonas`,
    },
    {
      id: 'espacios',
      servicio: 'zonas',
      nombre: 'Espacios disponibles (GET /espacios/disponibles)',
      base: API.zonas,
      run: async () => `${(await espaciosApi.disponibles()).length} disponibles`,
    },
    {
      id: 'asignaciones',
      servicio: 'asignaciones',
      nombre: 'Flota propietario (GET /propietarios/{id}/vehiculos)',
      base: API.asignaciones,
      run: async () => {
        if (!user) throw new ApiError(401, 'Sin sesión');
        return `${(await propietariosApi.vehiculos(user.idUsuario)).length} vehículos`;
      },
    },
    {
      id: 'vehiculos',
      servicio: 'vehiculos',
      nombre: 'Vehículos (GET /vehiculos)',
      base: API.vehiculos,
      run: async () => `${(await vehiculosApi.list(false)).length} vehículos`,
    },
    {
      id: 'tickets',
      servicio: 'tickets',
      nombre: 'Tickets (GET /tickets?size=1)',
      base: API.tickets,
      run: async () => `${(await ticketsApi.listar({ size: 1 })).totalElements} tickets`,
    },
    {
      id: 'audit',
      servicio: 'auditoría',
      nombre: 'Auditoría (GET /audit)',
      base: API.audit,
      run: async () => `${(await auditApi.list()).length} eventos`,
    },
  ];

  const [res, setRes] = useState<Record<string, Resultado>>({});
  const [corriendo, setCorriendo] = useState(false);

  const ejecutar = useCallback(async (check: Check) => {
    setRes((r) => ({ ...r, [check.id]: { estado: 'running' } }));
    const t0 = performance.now();
    try {
      const detalle = await check.run();
      const ms = Math.round(performance.now() - t0);
      setRes((r) => ({ ...r, [check.id]: { estado: 'ok', ms, detalle } }));
    } catch (err) {
      const ms = Math.round(performance.now() - t0);
      if (err instanceof ApiError) {
        setRes((r) => ({
          ...r,
          [check.id]: {
            estado: 'fail',
            ms,
            httpStatus: err.status,
            detalle: err.message,
            hint: pista(err.status),
          },
        }));
      } else {
        // Error de red / CORS / DNS: fetch lanza TypeError.
        setRes((r) => ({
          ...r,
          [check.id]: {
            estado: 'fail',
            ms,
            detalle: err instanceof Error ? err.message : 'Error desconocido',
            hint: pista(undefined, true),
          },
        }));
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.idUsuario]);

  const ejecutarTodo = useCallback(async () => {
    setCorriendo(true);
    await Promise.allSettled(checks.map((c) => ejecutar(c)));
    setCorriendo(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ejecutar]);

  useEffect(() => {
    ejecutarTodo();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const total = checks.length;
  const oks = Object.values(res).filter((r) => r.estado === 'ok').length;
  const fails = Object.values(res).filter((r) => r.estado === 'fail').length;

  return (
    <>
      <PageHead
        title="Diagnóstico"
        subtitle="Verifica en vivo la conexión con cada microservicio y el flujo de autenticación."
        actions={
          <Button onClick={ejecutarTodo} loading={corriendo} icon="⟳">
            Ejecutar todo
          </Button>
        }
      />

      <div className="row-wrap" style={{ gap: 12, marginBottom: 20 }}>
        <div className="card card-pad" style={{ minWidth: 120 }}>
          <div className="stat-label">Pruebas</div>
          <div className="stat-value tnum">{total}</div>
        </div>
        <div className="card card-pad" style={{ minWidth: 120 }}>
          <div className="stat-label">Correctas</div>
          <div className="stat-value tnum" style={{ color: 'var(--success)' }}>
            {oks}
          </div>
        </div>
        <div className="card card-pad" style={{ minWidth: 120 }}>
          <div className="stat-label">Con fallos</div>
          <div className="stat-value tnum" style={{ color: fails ? 'var(--danger)' : undefined }}>
            {fails}
          </div>
        </div>
      </div>

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Servicio</th>
              <th>Prueba</th>
              <th>Estado</th>
              <th className="num">Latencia</th>
              <th>Detalle</th>
              <th style={{ textAlign: 'right' }}>Acción</th>
            </tr>
          </thead>
          <tbody>
            {checks.map((c) => {
              const r = res[c.id] ?? { estado: 'idle' as Estado };
              return (
                <tr key={c.id}>
                  <td>
                    <Badge tone="neutral">{c.servicio}</Badge>
                    <div className="subtle tnum" style={{ fontSize: '0.7rem', marginTop: 2 }}>
                      {c.base}
                    </div>
                  </td>
                  <td>{c.nombre}</td>
                  <td>
                    {r.estado === 'running' ? (
                      <span className="row" style={{ gap: 6 }}>
                        <span className="spinner" style={{ width: 14, height: 14 }} /> Probando…
                      </span>
                    ) : r.estado === 'ok' ? (
                      <Badge tone="success">OK</Badge>
                    ) : r.estado === 'fail' ? (
                      <Badge tone="danger">
                        {r.httpStatus ? `Fallo ${r.httpStatus}` : 'Sin conexión'}
                      </Badge>
                    ) : (
                      <Badge tone="neutral">Pendiente</Badge>
                    )}
                  </td>
                  <td className="num tnum">{r.ms !== undefined ? `${r.ms} ms` : '—'}</td>
                  <td>
                    {r.detalle && <div>{r.detalle}</div>}
                    {r.hint && (
                      <div className="subtle" style={{ fontSize: '0.75rem', color: 'var(--warning)' }}>
                        ⚠ {r.hint}
                      </div>
                    )}
                  </td>
                  <td>
                    <div className="actions">
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => ejecutar(c)}
                        loading={r.estado === 'running'}
                      >
                        Reintentar
                      </Button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <p className="subtle" style={{ fontSize: '0.8125rem', marginTop: 16 }}>
        Cada prueba ejecuta una llamada real al backend (como una colección de Postman). Un fallo de red
        sin código HTTP suele indicar servicio caído o CORS no habilitado. Un 401 apunta a problemas de
        token; un 404, a URL base equivocada; un 403, a permisos.
      </p>
    </>
  );
}
