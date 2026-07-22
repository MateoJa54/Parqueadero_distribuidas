import { useMemo, useState } from 'react';
import { espaciosApi } from '@/api/zonas';
import type { EstadoEspacio, TipoEspacio } from '@/types';
import { useAsync } from '@/hooks/useAsync';
import { PageHead } from '@/ui/PageHead';
import { Badge, EstadoEspacioBadge } from '@/ui/Badge';
import { Select } from '@/ui/Input';
import { Button } from '@/ui/Button';
import { EmptyState, AsyncView, Loading } from '@/ui/States';

const TIPOS: TipoEspacio[] = ['MOTO', 'AUTO', 'BUSETA'];
const ESTADOS: EstadoEspacio[] = ['DISPONIBLE', 'OCUPADO', 'RESERVADO', 'MANTENIMIENTO'];

const BORDE_ESTADO: Record<EstadoEspacio, string> = {
  DISPONIBLE: 'var(--success)',
  OCUPADO: 'var(--danger)',
  RESERVADO: 'var(--warning)',
  MANTENIMIENTO: 'var(--border)',
};

export function DisponibilidadPage() {
  const { data, loading, error, reload } = useAsync(() => espaciosApi.list(), []);
  const [tipo, setTipo] = useState<TipoEspacio | ''>('');
  const [estado, setEstado] = useState<EstadoEspacio | ''>('');

  // Solo espacios operativos (activos) para la vista del cliente.
  const activos = useMemo(() => (data ?? []).filter((e) => e.activo), [data]);

  const lista = useMemo(() => {
    let arr = activos;
    if (tipo) arr = arr.filter((e) => e.tipo === tipo);
    if (estado) arr = arr.filter((e) => e.estado === estado);
    return arr;
  }, [activos, tipo, estado]);

  const porEstado = useMemo(() => {
    const m: Record<string, number> = {};
    for (const e of activos) m[e.estado] = (m[e.estado] ?? 0) + 1;
    return m;
  }, [activos]);

  return (
    <>
      <PageHead
        title="Disponibilidad"
        subtitle="Estado de los espacios en este momento."
        actions={
          <>
            <Select
              value={estado}
              onChange={(e) => setEstado(e.target.value as EstadoEspacio | '')}
              placeholder="Todos los estados"
              aria-label="Filtrar por estado"
              options={ESTADOS.map((s) => ({ value: s, label: s }))}
            />
            <Select
              value={tipo}
              onChange={(e) => setTipo(e.target.value as TipoEspacio | '')}
              placeholder="Todos los tipos"
              aria-label="Filtrar por tipo"
              options={TIPOS.map((t) => ({ value: t, label: t }))}
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
        onRetry={reload}
        loadingNode={<Loading label="Consultando disponibilidad…" />}
      >
        <>
          <div className="row-wrap" style={{ gap: 12, marginBottom: 20 }}>
            <div className="card card-pad" style={{ minWidth: 120 }}>
              <div className="stat-label">Total</div>
              <div className="stat-value tnum">{activos.length}</div>
            </div>
            {ESTADOS.map((s) => (
              <div className="card card-pad" style={{ minWidth: 120 }} key={s}>
                <div className="stat-label">{s}</div>
                <div className="stat-value tnum">{porEstado[s] ?? 0}</div>
              </div>
            ))}
          </div>

          {lista.length === 0 ? (
            <EmptyState
              title="Sin espacios"
              message="No hay espacios con ese filtro por ahora."
            />
          ) : (
            <div className="grid grid-4">
              {lista.map((e) => (
                <div
                  className="card card-pad"
                  key={e.id}
                  style={{ borderLeft: `3px solid ${BORDE_ESTADO[e.estado]}` }}
                >
                  <div className="row spread">
                    <strong>{e.codigo}</strong>
                    <EstadoEspacioBadge estado={e.estado} />
                  </div>
                  <div className="row-wrap" style={{ gap: 6, marginTop: 8 }}>
                    <Badge tone="info">{e.tipo}</Badge>
                    {e.nombreZona && <Badge tone="neutral">{e.nombreZona}</Badge>}
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      </AsyncView>
    </>
  );
}
