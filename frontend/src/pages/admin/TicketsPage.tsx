import { useCallback, useEffect, useState } from 'react';
import { ticketsApi } from '@/api/tickets';
import { espaciosApi } from '@/api/zonas';
import type { EstadoTicket, Page, Ticket } from '@/types';
import { useAsync } from '@/hooks/useAsync';
import { ApiError } from '@/api/client';
import { PageHead } from '@/ui/PageHead';
import { Button } from '@/ui/Button';
import { Input, Select } from '@/ui/Input';
import { Modal } from '@/ui/Modal';
import { EstadoTicketBadge } from '@/ui/Badge';
import { EmptyState, AsyncView, Loading, TableSkeleton } from '@/ui/States';
import { useToast } from '@/ui/ToastProvider';
import { fmtDinero, fmtFecha, rgx } from '@/lib/format';
import { getPrefs } from '@/lib/prefs';

type Tab = 'ingreso' | 'listado' | 'buscar';
const ESTADOS: EstadoTicket[] = ['ACTIVO', 'PAGADO', 'ANULADO'];

export function TicketsPage() {
  const toast = useToast();
  const [tab, setTab] = useState<Tab>('listado');

  return (
    <>
      <PageHead title="Tickets" subtitle="Ingreso, cobro y anulación de tickets de parqueo." />
      <div className="tabs" role="tablist">
        <button type="button" className={`tab ${tab === 'listado' ? 'active' : ''}`} onClick={() => setTab('listado')} role="tab">
          Listado
        </button>
        <button type="button" className={`tab ${tab === 'ingreso' ? 'active' : ''}`} onClick={() => setTab('ingreso')} role="tab">
          Registrar ingreso
        </button>
        <button type="button" className={`tab ${tab === 'buscar' ? 'active' : ''}`} onClick={() => setTab('buscar')} role="tab">
          Buscar por código
        </button>
      </div>

      {tab === 'ingreso' && <IngresoTab toast={toast} onDone={() => setTab('listado')} />}
      {tab === 'listado' && <ListadoTab toast={toast} />}
      {tab === 'buscar' && <BuscarTab />}
    </>
  );
}

// ---- Registrar ingreso ----
function IngresoTab({ toast, onDone }: { readonly toast: ReturnType<typeof useToast>; readonly onDone: () => void }) {
  const disponibles = useAsync(() => espaciosApi.disponibles(), []);
  const [placa, setPlaca] = useState('');
  const [idEspacio, setIdEspacio] = useState('');
  const [errs, setErrs] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [creado, setCreado] = useState<Ticket | null>(null);

  const registrar = async () => {
    const e: Record<string, string> = {};
    const p = placa.toUpperCase().trim();
    if (!rgx.placaAuto.test(p) && !rgx.placaMoto.test(p)) e.placa = 'Placa inválida.';
    if (!idEspacio) e.idEspacio = 'Selecciona un espacio disponible.';
    setErrs(e);
    if (Object.keys(e).length) return;
    setSaving(true);
    try {
      const t = await ticketsApi.ingreso({ placa: p, idEspacio });
      toast.success('Ingreso registrado', `Ticket ${t.codigo}`);
      setCreado(t);
      setPlaca('');
      setIdEspacio('');
      disponibles.reload();
    } catch (err) {
      toast.error('No se pudo registrar', err instanceof ApiError ? err.message : undefined);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="card card-pad" style={{ maxWidth: 560 }}>
      {creado && (
        <output className="alert alert-success" style={{ marginBottom: 16, display: 'block' }}>
          <span aria-hidden>✓</span>
          {' '}
          Ticket <strong>{creado.codigo}</strong> creado para {creado.placa}.
          <Button size="sm" variant="ghost" onClick={onDone} style={{ marginLeft: 'auto' }}>
            Ver listado
          </Button>
        </output>
      )}
      <div className="stack">
        <Input
          label="Placa del vehículo"
          value={placa}
          onChange={(e) => setPlaca(e.target.value.toUpperCase())}
          error={errs.placa}
          hint="Auto ABC-1234 · Moto AB-123C"
          required
        />
        {disponibles.loading ? (
          <Loading label="Cargando espacios…" />
        ) : (
          <Select
            label="Espacio disponible"
            placeholder="Selecciona…"
            value={idEspacio}
            onChange={(e) => setIdEspacio(e.target.value)}
            error={errs.idEspacio}
            options={(disponibles.data ?? []).map((es) => ({
              value: es.id,
            label: (() => { const zonaStr = es.nombreZona ? ` · ${es.nombreZona}` : ''; return `${es.codigo} · ${es.tipo}${zonaStr}`; })(),
            }))}
            required
          />
        )}
        <Button onClick={registrar} loading={saving} disabled={!(disponibles.data ?? []).length}>
          Registrar ingreso
        </Button>
        {!(disponibles.data ?? []).length && !disponibles.loading && (
          <p className="muted">No hay espacios disponibles en este momento.</p>
        )}
      </div>
    </div>
  );
}

// ---- Listado paginado + cobro/anulación ----
function ListadoTab({ toast }: { readonly toast: ReturnType<typeof useToast> }) {
  const [estado, setEstado] = useState<EstadoTicket | ''>('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<Ticket> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [anular, setAnular] = useState<{ open: boolean; ticket?: Ticket; motivo: string }>({
    open: false,
    motivo: '',
  });
  const [busy, setBusy] = useState<string | null>(null);

  const cargar = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await ticketsApi.listar({ estado: estado || undefined, page, size: getPrefs().pageSize });
      setData(res);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Error al cargar tickets.');
    } finally {
      setLoading(false);
    }
  }, [estado, page]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const cobrar = async (t: Ticket) => {
    setBusy(t.id);
    try {
      const r = await ticketsApi.pagar(t.id);
      toast.success('Ticket pagado', `${fmtDinero(r.valorRecaudado)}`);
      cargar();
    } catch (err) {
      toast.error('No se pudo cobrar', err instanceof ApiError ? err.message : undefined);
    } finally {
      setBusy(null);
    }
  };

  const confirmarAnular = async () => {
    if (!anular.ticket || anular.motivo.trim().length < 5) return;
    setBusy(anular.ticket.id);
    try {
      await ticketsApi.anular(anular.ticket.id, anular.motivo.trim());
      toast.success('Ticket anulado');
      setAnular({ open: false, motivo: '' });
      cargar();
    } catch (err) {
      toast.error('No se pudo anular', err instanceof ApiError ? err.message : undefined);
    } finally {
      setBusy(null);
    }
  };

  return (
    <>
      <div className="row-wrap" style={{ marginBottom: 16 }}>
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
      </div>

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
        emptyNode={<EmptyState title="Sin tickets" message="No hay tickets con ese filtro." />}
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
                  <th>Estado</th>
                  <th className="num">Valor</th>
                  <th style={{ textAlign: 'right' }}>Acciones</th>
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
                    <td>
                      <EstadoTicketBadge estado={t.estadoTicket} />
                    </td>
                    <td className="num tnum">{fmtDinero(t.valorRecaudado)}</td>
                    <td>
                      <div className="actions">
                        {t.estadoTicket === 'ACTIVO' && (
                          <>
                            <Button
                              size="sm"
                              onClick={() => cobrar(t)}
                              loading={busy === t.id}
                            >
                              Cobrar
                            </Button>
                            <Button
                              size="sm"
                              variant="danger"
                              onClick={() => setAnular({ open: true, ticket: t, motivo: '' })}
                            >
                              Anular
                            </Button>
                          </>
                        )}
                        {t.estadoTicket !== 'ACTIVO' && <span className="subtle">—</span>}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="row spread" style={{ marginTop: 16 }}>
            <span className="muted">
              Página {(data?.number ?? 0) + 1} de {Math.max(1, data?.totalPages ?? 1)} · {data?.totalElements ?? 0} tickets
            </span>
            <div className="row" style={{ gap: 8 }}>
              <Button
                size="sm"
                variant="secondary"
                disabled={(data?.first ?? true) || page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Anterior
              </Button>
              <Button
                size="sm"
                variant="secondary"
                disabled={data?.last ?? true}
                onClick={() => setPage((p) => p + 1)}
              >
                Siguiente
              </Button>
            </div>
          </div>
        </>
      </AsyncView>

      <Modal
        open={anular.open}
        title={`Anular ticket ${anular.ticket?.codigo ?? ''}`}
        onClose={() => setAnular({ open: false, motivo: '' })}
        footer={
          <>
            <Button variant="ghost" onClick={() => setAnular({ open: false, motivo: '' })}>
              Cancelar
            </Button>
            <Button
              variant="danger"
              onClick={confirmarAnular}
              loading={busy === anular.ticket?.id}
              disabled={anular.motivo.trim().length < 5}
            >
              Anular ticket
            </Button>
          </>
        }
      >
        <Input
          label="Motivo de anulación"
          value={anular.motivo}
          onChange={(e) => setAnular({ ...anular, motivo: e.target.value })}
          hint="Mínimo 5 caracteres."
          required
        />
      </Modal>
    </>
  );
}

// ---- Buscar por código ----
function BuscarTab() {
  const [codigo, setCodigo] = useState('');
  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const buscar = async () => {
    if (!codigo.trim()) return;
    setLoading(true);
    setError(null);
    setTicket(null);
    try {
      const t = await ticketsApi.porCodigo(codigo.trim());
      setTicket(t);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se encontró el ticket.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 560 }}>
      <div className="row" style={{ gap: 8, marginBottom: 16 }}>
        <input
          className="input"
          placeholder="Código del ticket"
          value={codigo}
          onChange={(e) => setCodigo(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && buscar()}
          aria-label="Código del ticket"
        />
        <Button onClick={buscar} loading={loading}>
          Buscar
        </Button>
      </div>
      {error && (
        <div className="alert alert-danger" role="alert">
          <span aria-hidden>⚠</span>
          {error}
        </div>
      )}
      {ticket && (
        <div className="card card-pad">
          <div className="row spread" style={{ marginBottom: 12 }}>
            <h3 style={{ margin: 0 }}>{ticket.codigo}</h3>
            <EstadoTicketBadge estado={ticket.estadoTicket} />
          </div>
          <dl className="dl">
            <dt>Placa</dt>
            <dd className="tnum">{ticket.placa}</dd>
            <dt>Espacio</dt>
            <dd>{ticket.codigoEspacio ?? '—'}</dd>
            <dt>Ingreso</dt>
            <dd>{fmtFecha(ticket.fechaHoraIngreso)}</dd>
            <dt>Salida</dt>
            <dd>{fmtFecha(ticket.fechaHoraSalida)}</dd>
            <dt>Valor</dt>
            <dd>{fmtDinero(ticket.valorRecaudado)}</dd>
            {ticket.motivoAnulacion && (
              <>
                <dt>Motivo anulación</dt>
                <dd>{ticket.motivoAnulacion}</dd>
              </>
            )}
          </dl>
        </div>
      )}
    </div>
  );
}
