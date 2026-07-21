import { useAuth } from '@/auth/context';
import { puede } from '@/auth/rbac';
import { espaciosApi, zonasApi } from '@/api/zonas';
import { ticketsApi } from '@/api/tickets';
import { vehiculosApi } from '@/api/vehiculos';
import { useAsync } from '@/hooks/useAsync';
import { PageHead } from '@/ui/PageHead';
import { AsyncView } from '@/ui/States';
import { fmtDinero } from '@/lib/format';

function StatCard({
  label,
  value,
  icon,
  tone,
}: {
  readonly label: string;
  readonly value: string | number;
  readonly icon: string;
  readonly tone: string;
}) {
  return (
    <div className="card stat">
      <div className="row spread">
        <div>
          <div className="stat-label">{label}</div>
          <div className="stat-value tnum">{value}</div>
        </div>
        <div className="stat-ico" style={{ background: `var(--${tone}-soft)`, color: `var(--${tone})` }} aria-hidden>
          {icon}
        </div>
      </div>
    </div>
  );
}

export function DashboardPage() {
  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const puedeVerCatalogo = puede(roles, 'zonas');

  const espacios = useAsync(() => espaciosApi.list(), []);
  const zonas = useAsync(() => (puedeVerCatalogo ? zonasApi.list() : Promise.resolve([])), []);
  const activos = useAsync(() => ticketsApi.listar({ estado: 'ACTIVO', size: 1 }), []);
  const pagados = useAsync(() => ticketsApi.listar({ estado: 'PAGADO', size: 200 }), []);
  const vehiculos = useAsync(
    () => (puede(roles, 'vehiculos') ? vehiculosApi.list() : Promise.resolve([])),
    [],
  );

  const espaciosList = espacios.data ?? [];
  const disponibles = espaciosList.filter((e) => e.estado === 'DISPONIBLE').length;
  const ocupados = espaciosList.filter((e) => e.estado === 'OCUPADO').length;
  const recaudado = (pagados.data?.content ?? []).reduce(
    (acc, t) => acc + (t.valorRecaudado ?? 0),
    0,
  );

  return (
    <>
      <PageHead
        title={`Hola, ${user?.username} 👋`}
        subtitle="Resumen operativo del parqueadero"
      />
      <div className="grid grid-4">
        <StatCard
          label="Tickets activos"
          value={activos.loading ? '…' : activos.data?.totalElements ?? 0}
          icon="🎟"
          tone="info"
        />
        <StatCard
          label="Espacios disponibles"
          value={espacios.loading ? '…' : disponibles}
          icon="✓"
          tone="success"
        />
        <StatCard
          label="Espacios ocupados"
          value={espacios.loading ? '…' : ocupados}
          icon="●"
          tone="danger"
        />
        {puedeVerCatalogo && (
          <StatCard
            label="Zonas"
            value={zonas.loading ? '…' : zonas.data?.length ?? 0}
            icon="◫"
            tone="primary"
          />
        )}
        {puede(roles, 'vehiculos') && (
          <StatCard
            label="Vehículos"
            value={vehiculos.loading ? '…' : vehiculos.data?.length ?? 0}
            icon="🚗"
            tone="primary"
          />
        )}
        <StatCard
          label="Recaudado (pagados)"
          value={pagados.loading ? '…' : fmtDinero(recaudado)}
          icon="$"
          tone="success"
        />
      </div>

      <div className="card card-pad" style={{ marginTop: 24 }}>
        <h3 style={{ marginBottom: 12 }}>Ocupación por espacio</h3>
        <AsyncView
          loading={espacios.loading}
          isEmpty={espaciosList.length === 0}
          loadingNode={<p className="muted">Cargando…</p>}
          emptyNode={<p className="muted">No hay espacios registrados.</p>}
        >
          <div className="row-wrap" style={{ gap: 8 }}>
            {espaciosList.slice(0, 60).map((e) => {
              let bg = 'var(--warning-soft)';
              if (e.estado === 'DISPONIBLE') bg = 'var(--success-soft)';
              else if (e.estado === 'OCUPADO') bg = 'var(--danger-soft)';
              let fg = 'var(--warning)';
              if (e.estado === 'DISPONIBLE') fg = 'var(--success)';
              else if (e.estado === 'OCUPADO') fg = 'var(--danger)';
              return (
              <span
                key={e.id}
                className="badge"
                title={`${e.codigo} · ${e.estado}`}
                style={{
                  background: bg,
                  color: fg,
                }}
              >
                {e.codigo}
              </span>
              );
            })}
          </div>
        </AsyncView>
      </div>
    </>
  );
}
