import { useMemo, useState } from 'react';
import { asignVehiculoApi } from '@/api/asignaciones';
import { vehiculosApi } from '@/api/vehiculos';
import { usuariosApi } from '@/api/usuarios';
import type { Assignment, AssignmentType, Usuario, Vehiculo } from '@/types';
import { useAsync } from '@/hooks/useAsync';
import { ApiError } from '@/api/client';
import { PageHead } from '@/ui/PageHead';
import { Button } from '@/ui/Button';
import { Input, Select, Combobox } from '@/ui/Input';
import { Modal } from '@/ui/Modal';
import { ActivoBadge, Badge, EstadoAsignacionBadge } from '@/ui/Badge';
import { EmptyState, ErrorState, Loading } from '@/ui/States';
import { useToast } from '@/ui/ToastProvider';

const TIPOS: AssignmentType[] = ['PROPIETARIO', 'AUTORIZADO', 'TEMPORAL'];

type FiltroEstado = 'TODAS' | 'ACTIVAS' | 'INACTIVAS';

export function AsignacionesVehiculosPage() {
  const toast = useToast();
  const asignaciones = useAsync(() => asignVehiculoApi.listAll(), []);
  const vehiculos = useAsync(() => vehiculosApi.list(true), []);
  const usuarios = useAsync(() => usuariosApi.list(), []);

  const [busqueda, setBusqueda] = useState('');
  const [filtroEstado, setFiltroEstado] = useState<FiltroEstado>('TODAS');

  const [modal, setModal] = useState(false);
  const [vehicleId, setVehicleId] = useState('');
  const [userId, setUserId] = useState('');
  const [assignmentType, setAssignmentType] = useState<AssignmentType>('PROPIETARIO');
  const [alias, setAlias] = useState('');
  const [observation, setObservation] = useState('');
  const [saving, setSaving] = useState(false);

  const usuariosMap = useMemo(() => {
    const m = new Map<string, Usuario>();
    for (const u of usuarios.data ?? []) m.set(u.id, u);
    return m;
  }, [usuarios.data]);

  const vehiculosMap = useMemo(() => {
    const m = new Map<string, Vehiculo>();
    for (const v of vehiculos.data ?? []) m.set(v.id, v);
    return m;
  }, [vehiculos.data]);

  const filtradas = useMemo(() => {
    const q = busqueda.trim().toLowerCase();
    return (asignaciones.data ?? []).filter((a) => {
      if (filtroEstado === 'ACTIVAS' && !a.active) return false;
      if (filtroEstado === 'INACTIVAS' && a.active) return false;
      if (!q) return true;
      const u = usuariosMap.get(a.userId);
      const v = vehiculosMap.get(a.vehicleId);
      const campos = [
        u?.username,
        u?.nombreCompleto,
        v?.placa,
        v?.marca,
        v?.modelo,
        a.assignmentType,
        a.status,
        a.vehicleAlias ?? '',
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return campos.includes(q);
    });
  }, [asignaciones.data, busqueda, filtroEstado, usuariosMap, vehiculosMap]);

  const abrirModal = () => {
    setVehicleId('');
    setUserId('');
    setAssignmentType('PROPIETARIO');
    setAlias('');
    setObservation('');
    setModal(true);
  };

  const crear = async () => {
    if (!vehicleId || !userId) {
      toast.error('Selecciona vehículo y usuario');
      return;
    }
    setSaving(true);
    try {
      await asignVehiculoApi.create({
        userId,
        vehicleId,
        assignmentType,
        vehicleAlias: alias.trim() || undefined,
        observation: observation.trim() || undefined,
      });
      toast.success('Asignación creada');
      setModal(false);
      asignaciones.reload();
    } catch (err) {
      toast.error('No se pudo asignar', err instanceof ApiError ? err.message : undefined);
    } finally {
      setSaving(false);
    }
  };

  const toggle = async (a: Assignment) => {
    try {
      if (a.active) await asignVehiculoApi.desactivar(a.userId, a.vehicleId);
      else await asignVehiculoApi.activar(a.userId, a.vehicleId);
      toast.success(a.active ? 'Asignación desactivada' : 'Asignación activada');
      asignaciones.reload();
    } catch (err) {
      toast.error('No se pudo cambiar el estado', err instanceof ApiError ? err.message : undefined);
    }
  };

  const nombreVehiculo = (id: string) => {
    const v = vehiculosMap.get(id);
    return v ? `${v.placa} · ${v.marca} ${v.modelo}` : id;
  };

  return (
    <>
      <PageHead
        title="Asignaciones de vehículos"
        subtitle="Vincula vehículos con sus propietarios y conductores autorizados."
        actions={
          <Button onClick={abrirModal} icon="＋">
            Nueva asignación
          </Button>
        }
      />

      <div className="card card-pad" style={{ marginBottom: 20 }}>
        <div className="row-wrap" style={{ gap: 12, alignItems: 'flex-end' }}>
          <div style={{ flex: '1 1 260px' }}>
            <Input
              label="Buscar"
              placeholder="Placa, usuario, tipo, estado…"
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
            />
          </div>
          <div style={{ flex: '0 0 200px' }}>
            <Select
              label="Estado"
              value={filtroEstado}
              onChange={(e) => setFiltroEstado(e.target.value as FiltroEstado)}
              options={[
                { value: 'TODAS', label: 'Todas' },
                { value: 'ACTIVAS', label: 'Solo activas' },
                { value: 'INACTIVAS', label: 'Solo inactivas' },
              ]}
            />
          </div>
          <Button variant="ghost" onClick={() => asignaciones.reload()}>
            Actualizar
          </Button>
        </div>
      </div>

      {asignaciones.loading ? (
        <Loading label="Cargando asignaciones…" />
      ) : asignaciones.error ? (
        <ErrorState message={asignaciones.error} onRetry={() => asignaciones.reload()} />
      ) : (asignaciones.data ?? []).length === 0 ? (
        <EmptyState
          title="Sin asignaciones"
          message="Todavía no hay asignaciones de vehículos."
          action={<Button onClick={abrirModal}>Nueva asignación</Button>}
        />
      ) : filtradas.length === 0 ? (
        <EmptyState title="Sin resultados" message="Ninguna asignación coincide con el filtro." />
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Usuario</th>
                <th>Vehículo</th>
                <th>Tipo</th>
                <th>Alias</th>
                <th>Estado</th>
                <th>Activa</th>
                <th style={{ textAlign: 'right' }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {filtradas.map((a) => (
                <tr key={`${a.userId}-${a.vehicleId}`}>
                  <td>{usuariosMap.get(a.userId)?.username ?? a.userId}</td>
                  <td>{nombreVehiculo(a.vehicleId)}</td>
                  <td>
                    <Badge tone="info">{a.assignmentType}</Badge>
                  </td>
                  <td>{a.vehicleAlias ?? '—'}</td>
                  <td>
                    <EstadoAsignacionBadge estado={a.status} />
                  </td>
                  <td>
                    <ActivoBadge activo={a.active} />
                  </td>
                  <td>
                    <div className="actions">
                      <Button
                        size="sm"
                        variant={a.active ? 'ghost' : 'secondary'}
                        onClick={() => toggle(a)}
                      >
                        {a.active ? 'Desactivar' : 'Activar'}
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal
        open={modal}
        title="Nueva asignación"
        onClose={() => setModal(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setModal(false)}>
              Cancelar
            </Button>
            <Button onClick={crear} loading={saving}>
              Asignar
            </Button>
          </>
        }
      >
        <div className="stack">
          <Combobox
            label="Vehículo"
            placeholder={
              vehiculos.loading ? 'Cargando vehículos…' : 'Escribe placa, marca o modelo…'
            }
            value={vehicleId}
            onChange={setVehicleId}
            options={(vehiculos.data ?? []).map((v) => ({
              value: v.id,
              label: `${v.placa} · ${v.marca} ${v.modelo}`,
            }))}
            emptyText="Ningún vehículo coincide"
            required
          />
          <Combobox
            label="Usuario"
            placeholder="Escribe usuario o nombre…"
            value={userId}
            onChange={setUserId}
            options={(usuarios.data ?? [])
              .filter((u) => u.active)
              .map((u) => ({ value: u.id, label: `${u.username} · ${u.nombreCompleto}` }))}
            emptyText="Ningún usuario coincide"
            required
          />
          <Select
            label="Tipo de asignación"
            value={assignmentType}
            onChange={(e) => setAssignmentType(e.target.value as AssignmentType)}
            options={TIPOS.map((t) => ({ value: t, label: t }))}
          />
          <Input label="Alias (opcional)" value={alias} onChange={(e) => setAlias(e.target.value)} />
          <Input
            label="Observación (opcional)"
            value={observation}
            onChange={(e) => setObservation(e.target.value)}
          />
        </div>
      </Modal>
    </>
  );
}
