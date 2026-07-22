import { useMemo, useState } from 'react';
import { auditApi } from '@/api/tickets';
import type { AuditLog } from '@/types';
import { useAsync } from '@/hooks/useAsync';
import { PageHead } from '@/ui/PageHead';
import { Badge } from '@/ui/Badge';
import { Select } from '@/ui/Input';
import { Modal } from '@/ui/Modal';
import { Button } from '@/ui/Button';
import { EmptyState, AsyncView, TableSkeleton } from '@/ui/States';
import { fmtFecha } from '@/lib/format';

const TONO: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'neutral'> = {
  CREATE: 'success',
  UPDATE: 'warning',
  DELETE: 'danger',
  LOGIN: 'info',
  LOGOUT: 'neutral',
  SELECT: 'neutral',
};

export function AuditoriaPage() {
  const { data, loading, error, reload } = useAsync(() => auditApi.list(), []);
  const [servicio, setServicio] = useState('');
  const [detalle, setDetalle] = useState<AuditLog | null>(null);

  const servicios = useMemo(
    () =>
      Array.from(new Set((data ?? []).map((l) => l.servicio))).sort((a, b) =>
        a.localeCompare(b),
      ),
    [data],
  );

  const lista = useMemo(() => {
    let arr = [...(data ?? [])];
    if (servicio) arr = arr.filter((l) => l.servicio === servicio);
    arr.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
    return arr;
  }, [data, servicio]);

  return (
    <>
      <PageHead
        title="Auditoría"
        subtitle="Registro de eventos de todos los microservicios."
        actions={
          <>
            <Select
              value={servicio}
              onChange={(e) => setServicio(e.target.value)}
              placeholder="Todos los servicios"
              aria-label="Filtrar por servicio"
              options={servicios.map((s) => ({ value: s, label: s }))}
            />
            <Button variant="secondary" size="sm" onClick={reload}>
              Refrescar
            </Button>
          </>
        }
      />

      <AsyncView
        loading={loading}
        error={error}
        isEmpty={lista.length === 0}
        onRetry={reload}
        loadingNode={
          <div className="card card-pad">
            <TableSkeleton cols={6} />
          </div>
        }
        emptyNode={
          <EmptyState title="Sin registros" message="Aún no hay eventos de auditoría." />
        }
      >
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Servicio</th>
                <th>Acción</th>
                <th>Entidad</th>
                <th>Usuario</th>
                <th style={{ textAlign: 'right' }}>Detalle</th>
              </tr>
            </thead>
            <tbody>
              {lista.map((l) => (
                <tr key={l.id}>
                  <td className="tnum">{fmtFecha(l.timestamp)}</td>
                  <td>
                    <Badge tone="neutral">{l.servicio}</Badge>
                  </td>
                  <td>
                    <Badge tone={TONO[l.accion] ?? 'neutral'}>{l.accion}</Badge>
                  </td>
                  <td>{l.entidad ?? '—'}</td>
                  <td>
                    {l.usuario ?? '—'}
                    {l.rol && (
                      <div className="subtle" style={{ fontSize: '0.75rem' }}>
                        {l.rol}
                      </div>
                    )}
                  </td>
                  <td>
                    <div className="actions">
                      <Button size="sm" variant="ghost" onClick={() => setDetalle(l)}>
                        Ver
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AsyncView>

      <Modal
        open={!!detalle}
        title="Detalle del evento"
        onClose={() => setDetalle(null)}
        footer={
          <Button variant="ghost" onClick={() => setDetalle(null)}>
            Cerrar
          </Button>
        }
      >
        {detalle && (
          <div className="stack">
            <dl className="dl">
              <dt>Servicio</dt>
              <dd>{detalle.servicio}</dd>
              <dt>Acción</dt>
              <dd>{detalle.accion}</dd>
              <dt>Entidad</dt>
              <dd>{detalle.entidad ?? '—'}</dd>
              <dt>Usuario</dt>
              <dd>{detalle.usuario ?? '—'}</dd>
              <dt>Rol</dt>
              <dd>{detalle.rol ?? '—'}</dd>
              <dt>IP</dt>
              <dd className="tnum">{detalle.ip ?? '—'}</dd>
              <dt>Fecha</dt>
              <dd>{fmtFecha(detalle.timestamp)}</dd>
            </dl>
            {detalle.datos && (
              <div>
                <div className="muted" style={{ marginBottom: 6 }}>
                  Datos
                </div>
                <pre
                  style={{
                    background: 'var(--surface-2)',
                    padding: 12,
                    borderRadius: 'var(--r-input)',
                    overflow: 'auto',
                    fontSize: '0.8125rem',
                    maxHeight: 300,
                  }}
                >
                  {JSON.stringify(detalle.datos, null, 2)}
                </pre>
              </div>
            )}
          </div>
        )}
      </Modal>
    </>
  );
}
