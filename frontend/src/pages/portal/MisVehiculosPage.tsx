import { useAuth } from '@/auth/context';
import { propietariosApi } from '@/api/asignaciones';
import { useAsync } from '@/hooks/useAsync';
import { PageHead } from '@/ui/PageHead';
import { ActivoBadge, Badge } from '@/ui/Badge';
import { EmptyState, ErrorState, Loading } from '@/ui/States';

export function MisVehiculosPage() {
  const { user } = useAuth();
  const { data, loading, error, reload } = useAsync(
    () => (user ? propietariosApi.vehiculos(user.idUsuario) : Promise.resolve([])),
    [user?.idUsuario],
  );

  return (
    <>
      <PageHead title="Mis vehículos" subtitle="Vehículos asignados a tu cuenta." />

      {loading ? (
        <Loading label="Cargando tus vehículos…" />
      ) : error ? (
        <ErrorState message={error} onRetry={reload} />
      ) : (data ?? []).length === 0 ? (
        <EmptyState
          title="Aún no tienes vehículos"
          message="Cuando la administración te asigne un vehículo, aparecerá aquí."
        />
      ) : (
        <div className="grid grid-3">
          {(data ?? []).map((v) => (
            <div className="card card-pad" key={v.id}>
              <div className="row spread" style={{ marginBottom: 8 }}>
                <h3 className="tnum" style={{ margin: 0 }}>
                  {v.placa}
                </h3>
                <ActivoBadge activo={v.activo} />
              </div>
              <p style={{ margin: '0 0 8px' }}>
                {v.marca} {v.modelo}
              </p>
              <div className="row-wrap" style={{ gap: 6 }}>
                <Badge tone="info">{v.tipo}</Badge>
                {v.color && <Badge tone="neutral">{v.color}</Badge>}
                {v.anio && <Badge tone="neutral">{v.anio}</Badge>}
                {v.clasificacion && <Badge tone="neutral">{v.clasificacion}</Badge>}
              </div>
            </div>
          ))}
        </div>
      )}
    </>
  );
}
