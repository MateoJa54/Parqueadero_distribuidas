import { useMemo, useState } from 'react';
import { espaciosApi, zonasApi } from '@/api/zonas';
import type { Espacio, EspacioRequest, EstadoEspacio, TipoEspacio, Zona } from '@/types';
import { useAsync } from '@/hooks/useAsync';
import { ApiError } from '@/api/client';
import { PageHead } from '@/ui/PageHead';
import { Button } from '@/ui/Button';
import { Input, Select } from '@/ui/Input';
import { Modal } from '@/ui/Modal';
import { ActivoBadge, EstadoEspacioBadge } from '@/ui/Badge';
import { EmptyState, ErrorState, TableSkeleton } from '@/ui/States';
import { useToast } from '@/ui/ToastProvider';

const TIPOS: TipoEspacio[] = ['MOTO', 'AUTO', 'BUSETA'];
const ESTADOS: EstadoEspacio[] = ['DISPONIBLE', 'OCUPADO', 'RESERVADO', 'MANTENIMIENTO'];

export function EspaciosPage() {
  const toast = useToast();
  const espacios = useAsync(() => espaciosApi.list(), []);
  const zonas = useAsync(() => zonasApi.list(), []);
  const [filtroEstado, setFiltroEstado] = useState<EstadoEspacio | ''>('');
  const [filtroZona, setFiltroZona] = useState('');
  const [modal, setModal] = useState<{ open: boolean; edit?: Espacio }>({ open: false });
  const [estadoModal, setEstadoModal] = useState<{ open: boolean; esp?: Espacio; estado: EstadoEspacio }>(
    { open: false, estado: 'DISPONIBLE' },
  );
  const [form, setForm] = useState<EspacioRequest>({ idZona: '', descripcion: '', tipo: 'AUTO', estado: 'DISPONIBLE' });
  const [errs, setErrs] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const zonasMap = useMemo(() => {
    const m = new Map<string, Zona>();
    for (const z of zonas.data ?? []) m.set(z.idZona, z);
    return m;
  }, [zonas.data]);

  const lista = useMemo(() => {
    let arr = espacios.data ?? [];
    if (filtroEstado) arr = arr.filter((e) => e.estado === filtroEstado);
    if (filtroZona) arr = arr.filter((e) => e.idZona === filtroZona);
    return arr;
  }, [espacios.data, filtroEstado, filtroZona]);

  const abrirNuevo = () => {
    setForm({ idZona: filtroZona || '', descripcion: '', tipo: 'AUTO', estado: 'DISPONIBLE' });
    setErrs({});
    setModal({ open: true });
  };
  const abrirEditar = (e: Espacio) => {
    setForm({ idZona: e.idZona, descripcion: e.descripcion ?? '', tipo: e.tipo, estado: e.estado });
    setErrs({});
    setModal({ open: true, edit: e });
  };

  const guardar = async () => {
    const e: Record<string, string> = {};
    if (!form.idZona) e.idZona = 'Selecciona una zona.';
    setErrs(e);
    if (Object.keys(e).length) return;
    setSaving(true);
    try {
      const payload: EspacioRequest = {
        idZona: form.idZona,
        tipo: form.tipo,
        descripcion: form.descripcion?.trim() || undefined,
        estado: form.estado,
      };
      if (modal.edit) {
        await espaciosApi.update(modal.edit.id, payload);
        toast.success('Espacio actualizado');
      } else {
        await espaciosApi.create(payload);
        toast.success('Espacio creado');
      }
      setModal({ open: false });
      espacios.reload();
    } catch (err) {
      toast.error('No se pudo guardar', err instanceof ApiError ? err.message : undefined);
    } finally {
      setSaving(false);
    }
  };

  const cambiarEstado = async () => {
    if (!estadoModal.esp) return;
    setSaving(true);
    try {
      await espaciosApi.cambiarEstado(estadoModal.esp.id, estadoModal.estado);
      toast.success('Estado actualizado');
      setEstadoModal({ open: false, estado: 'DISPONIBLE' });
      espacios.reload();
    } catch (err) {
      toast.error('No se pudo cambiar el estado', err instanceof ApiError ? err.message : undefined);
    } finally {
      setSaving(false);
    }
  };

  const toggleActivo = async (e: Espacio) => {
    try {
      if (e.activo) await espaciosApi.desactivar(e.id);
      else await espaciosApi.activar(e.id);
      toast.success(e.activo ? 'Espacio desactivado' : 'Espacio activado');
      espacios.reload();
    } catch (err) {
      toast.error('No se pudo cambiar el estado', err instanceof ApiError ? err.message : undefined);
    }
  };

  const loading = espacios.loading || zonas.loading;

  return (
    <>
      <PageHead
        title="Espacios"
        subtitle="Plazas individuales de estacionamiento. El código se genera automáticamente."
        actions={
          <>
            <Select
              value={filtroZona}
              onChange={(e) => setFiltroZona(e.target.value)}
              placeholder="Todas las zonas"
              aria-label="Filtrar por zona"
              options={(zonas.data ?? []).map((z) => ({ value: z.idZona, label: z.nombre }))}
            />
            <Select
              value={filtroEstado}
              onChange={(e) => setFiltroEstado(e.target.value as EstadoEspacio | '')}
              placeholder="Todos los estados"
              aria-label="Filtrar por estado"
              options={ESTADOS.map((s) => ({ value: s, label: s }))}
            />
            <Button onClick={abrirNuevo} icon="＋">
              Nuevo espacio
            </Button>
          </>
        }
      />

      {loading ? (
        <div className="card card-pad">
          <TableSkeleton cols={5} />
        </div>
      ) : espacios.error ? (
        <ErrorState message={espacios.error} onRetry={espacios.reload} />
      ) : lista.length === 0 ? (
        <EmptyState title="Sin espacios" message="No hay espacios con esos filtros." />
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Código</th>
                <th>Zona</th>
                <th>Tipo</th>
                <th>Estado</th>
                <th>Activo</th>
                <th style={{ textAlign: 'right' }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {lista.map((e) => (
                <tr key={e.id}>
                  <td>
                    <strong>{e.codigo}</strong>
                    {e.descripcion && (
                      <div className="subtle" style={{ fontSize: '0.75rem' }}>
                        {e.descripcion}
                      </div>
                    )}
                  </td>
                  <td>{e.nombreZona ?? zonasMap.get(e.idZona)?.nombre ?? '—'}</td>
                  <td>{e.tipo}</td>
                  <td>
                    <EstadoEspacioBadge estado={e.estado} />
                  </td>
                  <td>
                    <ActivoBadge activo={e.activo} />
                  </td>
                  <td>
                    <div className="actions">
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() =>
                          setEstadoModal({ open: true, esp: e, estado: e.estado })
                        }
                      >
                        Estado
                      </Button>
                      <Button size="sm" variant="secondary" onClick={() => abrirEditar(e)}>
                        Editar
                      </Button>
                      <Button
                        size="sm"
                        variant={e.activo ? 'ghost' : 'secondary'}
                        onClick={() => toggleActivo(e)}
                      >
                        {e.activo ? 'Desactivar' : 'Activar'}
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
        open={modal.open}
        title={modal.edit ? 'Editar espacio' : 'Nuevo espacio'}
        onClose={() => setModal({ open: false })}
        footer={
          <>
            <Button variant="ghost" onClick={() => setModal({ open: false })}>
              Cancelar
            </Button>
            <Button onClick={guardar} loading={saving}>
              Guardar
            </Button>
          </>
        }
      >
        <div className="stack">
          <Select
            label="Zona"
            placeholder="Selecciona…"
            value={form.idZona}
            onChange={(e) => setForm({ ...form, idZona: e.target.value })}
            error={errs.idZona}
            options={(zonas.data ?? [])
              .filter((z) => z.activo)
              .map((z) => ({ value: z.idZona, label: z.nombre }))}
            required
          />
          <div className="grid grid-2">
            <Select
              label="Tipo"
              value={form.tipo}
              onChange={(e) => setForm({ ...form, tipo: e.target.value as TipoEspacio })}
              options={TIPOS.map((t) => ({ value: t, label: t }))}
              required
            />
            <Select
              label="Estado inicial"
              value={form.estado}
              onChange={(e) => setForm({ ...form, estado: e.target.value as EstadoEspacio })}
              options={ESTADOS.map((s) => ({ value: s, label: s }))}
            />
          </div>
          <Input
            label="Descripción"
            value={form.descripcion ?? ''}
            onChange={(e) => setForm({ ...form, descripcion: e.target.value })}
          />
        </div>
      </Modal>

      <Modal
        open={estadoModal.open}
        title={`Cambiar estado · ${estadoModal.esp?.codigo ?? ''}`}
        onClose={() => setEstadoModal({ open: false, estado: 'DISPONIBLE' })}
        footer={
          <>
            <Button variant="ghost" onClick={() => setEstadoModal({ open: false, estado: 'DISPONIBLE' })}>
              Cancelar
            </Button>
            <Button onClick={cambiarEstado} loading={saving}>
              Aplicar
            </Button>
          </>
        }
      >
        <Select
          label="Nuevo estado"
          value={estadoModal.estado}
          onChange={(e) =>
            setEstadoModal({ ...estadoModal, estado: e.target.value as EstadoEspacio })
          }
          options={ESTADOS.map((s) => ({ value: s, label: s }))}
        />
      </Modal>
    </>
  );
}
