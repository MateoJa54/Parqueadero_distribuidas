import { useState } from 'react';
import { zonasApi } from '@/api/zonas';
import type { TipoZona, Zona, ZonaRequest } from '@/types';
import { useAsync } from '@/hooks/useAsync';
import { ApiError } from '@/api/client';
import { PageHead } from '@/ui/PageHead';
import { Button } from '@/ui/Button';
import { Input, Select, Textarea } from '@/ui/Input';
import { Modal } from '@/ui/Modal';
import { ActivoBadge, Badge } from '@/ui/Badge';
import { EmptyState, ErrorState, TableSkeleton } from '@/ui/States';
import { useToast } from '@/ui/ToastProvider';

const TIPOS: TipoZona[] = ['VIP', 'REGULAR', 'INTERNA', 'EXTERNA', 'PREFERENCIAL'];
const VACIA: ZonaRequest = { nombre: '', descripcion: '', tipo: 'REGULAR', capacidad: 10 };

export function ZonasPage() {
  const toast = useToast();
  const { data, loading, error, reload } = useAsync(() => zonasApi.list(), []);
  const [modal, setModal] = useState<{ open: boolean; edit?: Zona }>({ open: false });
  const [form, setForm] = useState<ZonaRequest>(VACIA);
  const [errs, setErrs] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const abrirNuevo = () => {
    setForm(VACIA);
    setErrs({});
    setModal({ open: true });
  };
  const abrirEditar = (z: Zona) => {
    setForm({
      nombre: z.nombre,
      descripcion: z.descripcion ?? '',
      tipo: z.tipoZona,
      capacidad: z.capacidad,
    });
    setErrs({});
    setModal({ open: true, edit: z });
  };

  const guardar = async () => {
    const e: Record<string, string> = {};
    if (form.nombre.trim().length < 3) e.nombre = 'Mínimo 3 caracteres.';
    if (form.capacidad < 1 || form.capacidad > 1000) e.capacidad = 'Entre 1 y 1000.';
    setErrs(e);
    if (Object.keys(e).length) return;
    setSaving(true);
    try {
      const payload = { ...form, nombre: form.nombre.trim(), descripcion: form.descripcion?.trim() };
      if (modal.edit) {
        await zonasApi.update(modal.edit.idZona, payload);
        toast.success('Zona actualizada');
      } else {
        await zonasApi.create(payload);
        toast.success('Zona creada');
      }
      setModal({ open: false });
      reload();
    } catch (err) {
      toast.error('No se pudo guardar', err instanceof ApiError ? err.message : undefined);
    } finally {
      setSaving(false);
    }
  };

  const toggleActivo = async (z: Zona) => {
    try {
      if (z.activo) await zonasApi.desactivar(z.idZona);
      else await zonasApi.activar(z.idZona);
      toast.success(z.activo ? 'Zona desactivada' : 'Zona activada');
      reload();
    } catch (err) {
      toast.error('No se pudo cambiar el estado', err instanceof ApiError ? err.message : undefined);
    }
  };

  return (
    <>
      <PageHead
        title="Zonas"
        subtitle="Áreas del parqueadero que agrupan espacios."
        actions={
          <Button onClick={abrirNuevo} icon="＋">
            Nueva zona
          </Button>
        }
      />

      {loading ? (
        <div className="card card-pad">
          <TableSkeleton cols={5} />
        </div>
      ) : error ? (
        <ErrorState message={error} onRetry={reload} />
      ) : (data ?? []).length === 0 ? (
        <EmptyState title="Sin zonas" message="Crea la primera zona." />
      ) : (
        <div className="grid grid-3">
          {(data ?? []).map((z) => (
            <div className="card card-pad" key={z.idZona}>
              <div className="row spread" style={{ marginBottom: 8 }}>
                <h3 style={{ margin: 0 }}>{z.nombre}</h3>
                <ActivoBadge activo={z.activo} />
              </div>
              <div className="row-wrap" style={{ gap: 6, marginBottom: 8 }}>
                <Badge tone="info">{z.tipoZona}</Badge>
                <Badge tone="neutral">Cap. {z.capacidad}</Badge>
                {z.codigo && <Badge tone="neutral">{z.codigo}</Badge>}
              </div>
              <p className="muted" style={{ minHeight: 20 }}>
                {z.descripcion || 'Sin descripción'}
              </p>
              <div className="row" style={{ gap: 8, marginTop: 8 }}>
                <Button size="sm" variant="secondary" onClick={() => abrirEditar(z)}>
                  Editar
                </Button>
                <Button
                  size="sm"
                  variant={z.activo ? 'ghost' : 'secondary'}
                  onClick={() => toggleActivo(z)}
                >
                  {z.activo ? 'Desactivar' : 'Activar'}
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal
        open={modal.open}
        title={modal.edit ? 'Editar zona' : 'Nueva zona'}
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
          <Input
            label="Nombre"
            value={form.nombre}
            onChange={(e) => setForm({ ...form, nombre: e.target.value })}
            error={errs.nombre}
            required
          />
          <div className="grid grid-2">
            <Select
              label="Tipo"
              value={form.tipo}
              onChange={(e) => setForm({ ...form, tipo: e.target.value as TipoZona })}
              options={TIPOS.map((t) => ({ value: t, label: t }))}
              required
            />
            <Input
              label="Capacidad"
              type="number"
              min={1}
              max={1000}
              value={form.capacidad}
              onChange={(e) => setForm({ ...form, capacidad: Number(e.target.value) })}
              error={errs.capacidad}
              required
            />
          </div>
          <Textarea
            label="Descripción"
            value={form.descripcion ?? ''}
            onChange={(e) => setForm({ ...form, descripcion: e.target.value })}
          />
        </div>
      </Modal>
    </>
  );
}
