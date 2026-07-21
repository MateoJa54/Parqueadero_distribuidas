import { useMemo, useState } from 'react';
import { espaciosApi } from '@/api/zonas';
import type { TipoEspacio } from '@/types';
import { useAsync } from '@/hooks/useAsync';
import { PageHead } from '@/ui/PageHead';
import { Badge } from '@/ui/Badge';
import { Select } from '@/ui/Input';
import { Button } from '@/ui/Button';
import { EmptyState, AsyncView, Loading } from '@/ui/States';

const TIPOS: TipoEspacio[] = ['MOTO', 'AUTO', 'BUSETA'];

export function DisponibilidadPage() {
  const { data, loading, error, reload } = useAsync(() => espaciosApi.disponibles(), []);
  const [tipo, setTipo] = useState<TipoEspacio | ''>('');

  const lista = useMemo(() => {
    let arr = data ?? [];
    if (tipo) arr = arr.filter((e) => e.tipo === tipo);
    return arr;
  }, [data, tipo]);

  const porTipo = useMemo(() => {
    const m: Record<string, number> = {};
    for (const e of data ?? []) m[e.tipo] = (m[e.tipo] ?? 0) + 1;
    return m;
  }, [data]);

  return (
    <>
      <PageHead
        title="Disponibilidad"
        subtitle="Espacios libres en este momento."
        actions={
          <>
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
            <div className="card card-pad" style={{ minWidth: 140 }}>
              <div className="stat-label">Total libres</div>
              <div className="stat-value tnum">{(data ?? []).length}</div>
            </div>
            {TIPOS.map((t) => (
              <div className="card card-pad" style={{ minWidth: 120 }} key={t}>
                <div className="stat-label">{t}</div>
                <div className="stat-value tnum">{porTipo[t] ?? 0}</div>
              </div>
            ))}
          </div>

          {lista.length === 0 ? (
            <EmptyState
              title="Sin espacios disponibles"
              message="No hay espacios libres con ese filtro por ahora."
            />
          ) : (
            <div className="grid grid-4">
              {lista.map((e) => (
                <div
                  className="card card-pad"
                  key={e.id}
                  style={{ borderLeft: '3px solid var(--success)' }}
                >
                  <div className="row spread">
                    <strong>{e.codigo}</strong>
                    <Badge tone="success">Libre</Badge>
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
