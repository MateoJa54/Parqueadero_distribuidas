import { useCallback, useEffect, useState } from 'react';
import { ticketsApi } from '@/api/tickets';
import { ApiError } from '@/api/client';
import type { EstadoTicket, Page, Ticket } from '@/types';
import { PageHead } from '@/ui/PageHead';
import { Button } from '@/ui/Button';
import { Select } from '@/ui/Input';
import { EstadoTicketBadge } from '@/ui/Badge';
import { AsyncView, EmptyState, TableSkeleton } from '@/ui/States';
import { fmtFecha, fmtDinero } from '@/lib/format';

const ESTADOS: EstadoTicket[] = ['ACTIVO', 'PAGADO', 'ANULADO'];

export function MisTicketsPage() {
  const [estado, setEstado] = useState<EstadoTicket | ''>('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<Ticket> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const cargar = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await ticketsApi.misTickets({ estado: estado || undefined, page, size: 20 });
      setData(res);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Error al cargar tus tickets.');
    } finally {
      setLoading(false);
    }
  }, [estado, page]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  return (
    <>
      <PageHead
        title="Mis tickets"
        subtitle="Historial de tus tickets de parqueadero."
        actions={
          <>
            <Select
              value={estado}
              onChange={(e) => {
                setEstado(e.target.value as EstadoTicket | '');
                setPage(0);
              }}
              placeholder="Todos los estados"
              aria-label="Filtrar por estado"
              options={ESTADOS.map((s) => ({ value: s, label: s }))}
            />
            <Button variant="secondary" size="sm" onClick={cargar}>
              Refrescar
            </Button>
          </>
        }
      />

      <AsyncView
        loading={loading}
        error={error}
        isEmpty={!data || data.content.length === 0}
        onRetry={cargar}
        loadingNode={
          <div className="card card-pad">
            <TableSkeleton cols={6} />
          </div>
        }
        emptyNode={<EmptyState title="Sin tickets" message="Todavía no tienes tickets registrados." />}
      >
        <>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Placa</th>
                  <th>Espacio</th>
                  <th>Ingreso</th>
                  <th>Salida</th>
                  <th>Estado</th>
                  <th className="num">Valor</th>
                </tr>
              </thead>
              <tbody>
                {(data?.content ?? []).map((t) => (
                  <tr key={t.id}>
                    <td>
                      <strong>{t.codigo}</strong>
                    </td>
                    <td className="tnum">{t.placa}</td>
                    <td>{t.codigoEspacio ?? '—'}</td>
                    <td className="tnum">{fmtFecha(t.fechaHoraIngreso)}</td>
                    <td className="tnum">{fmtFecha(t.fechaHoraSalida)}</td>
                    <td>
                      <EstadoTicketBadge estado={t.estadoTicket} />
                    </td>
                    <td className="num tnum">{fmtDinero(t.valorRecaudado)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="row spread" style={{ marginTop: 16 }}>
            <span className="muted">
              Página {(data?.number ?? 0) + 1} de {Math.max(1, data?.totalPages ?? 1)} ·{' '}
              {data?.totalElements ?? 0} tickets
            </span>
            <div className="row" style={{ gap: 8 }}>
              <Button
                size="sm"
                variant="secondary"
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Anterior
              </Button>
              <Button
                size="sm"
                variant="secondary"
                disabled={(data?.number ?? 0) + 1 >= (data?.totalPages ?? 1)}
                onClick={() => setPage((p) => p + 1)}
              >
                Siguiente
              </Button>
            </div>
          </div>
        </>
      </AsyncView>
    </>
  );
}
