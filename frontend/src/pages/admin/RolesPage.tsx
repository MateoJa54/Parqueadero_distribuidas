import { useState } from 'react';
import { rolesApi } from '@/api/usuarios';
import type { RolEntity, RolRequest } from '@/types';
import { useAsync } from '@/hooks/useAsync';
import { useAuth } from '@/auth/context';
import { ApiError } from '@/api/client';
import { PageHead } from '@/ui/PageHead';
import { Button } from '@/ui/Button';
import { Input, Textarea } from '@/ui/Input';
import { Modal } from '@/ui/Modal';
import { ActivoBadge } from '@/ui/Badge';
import { EmptyState, AsyncView, TableSkeleton } from '@/ui/States';
import { useToast } from '@/ui/ToastProvider';

export function RolesPage() {
  const toast = useToast();
  const { hasRole } = useAuth();
  const { data, loading, error, reload } = useAsync(() => rolesApi.list(), []);
  const [modal, setModal] = useState<{ open: boolean; edit?: RolEntity }>({ open: false });
  const [form, setForm] = useState<RolRequest>({ name: '', description: '' });
  const [errs, setErrs] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  // Roles reservados que no debe ver/desactivar cada quien:
  // - ROOT en sesión: se oculta el propio rol ROOT.
  // - ADMIN en sesión: se ocultan ROOT y ADMIN.
  const lista = (data ?? []).filter((r) => {
    const nombre = r.name.toUpperCase();
    if (hasRole('ROOT')) return nombre !== 'ROOT';
    if (hasRole('ADMIN')) return nombre !== 'ROOT' && nombre !== 'ADMIN';
    return true;
  });

  const abrirNuevo = () => {
    setForm({ name: '', description: '' });
    setErrs({});
    setModal({ open: true });
  };
  const abrirEditar = (r: RolEntity) => {
    setForm({ name: r.name, description: r.description ?? '' });
    setErrs({});
    setModal({ open: true, edit: r });
  };

  const guardar = async () => {
    const e: Record<string, string> = {};
    if (!/^[A-Z_]{3,20}$/.test(form.name.trim()))
      e.name = 'MAYÚSCULAS y guion bajo, 3–20 caracteres.';
    setErrs(e);
    if (Object.keys(e).length) return;
    setSaving(true);
    try {
      const payload = { name: form.name.trim().toUpperCase(), description: form.description?.trim() };
      if (modal.edit) {
        await rolesApi.update(modal.edit.id, payload);
        toast.success('Rol actualizado');
      } else {
        await rolesApi.create(payload);
        toast.success('Rol creado');
      }
      setModal({ open: false });
      reload();
    } catch (err) {
      toast.error('No se pudo guardar', err instanceof ApiError ? err.message : undefined);
    } finally {
      setSaving(false);
    }
  };

  const toggleActivo = async (r: RolEntity) => {
    try {
      if (r.active) await rolesApi.desactivar(r.id);
      else await rolesApi.activar(r.id);
      toast.success(r.active ? 'Rol desactivado' : 'Rol activado');
      reload();
    } catch (err) {
      toast.error('No se pudo cambiar el estado', err instanceof ApiError ? err.message : undefined);
    }
  };

  return (
    <>
      <PageHead
        title="Roles"
        subtitle="Catálogo de roles del sistema."
        actions={
          <Button onClick={abrirNuevo} icon="＋">
            Nuevo rol
          </Button>
        }
      />

      <AsyncView
        loading={loading}
        error={error}
        isEmpty={lista.length === 0}
        onRetry={reload}
        loadingNode={
          <div className="card card-pad">
            <TableSkeleton cols={3} />
          </div>
        }
        emptyNode={<EmptyState title="Sin roles" message="Crea el primer rol." />}
      >
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Descripción</th>
                <th>Estado</th>
                <th style={{ textAlign: 'right' }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {lista.map((r) => (
                <tr key={r.id}>
                  <td>
                    <span className="chip">{r.name}</span>
                  </td>
                  <td>{r.description ?? '—'}</td>
                  <td>
                    <ActivoBadge activo={r.active} />
                  </td>
                  <td>
                    <div className="actions">
                      <Button size="sm" variant="secondary" onClick={() => abrirEditar(r)}>
                        Editar
                      </Button>
                      <Button
                        size="sm"
                        variant={r.active ? 'ghost' : 'secondary'}
                        onClick={() => toggleActivo(r)}
                      >
                        {r.active ? 'Desactivar' : 'Activar'}
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
        open={modal.open}
        title={modal.edit ? 'Editar rol' : 'Nuevo rol'}
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
            label="Nombre del rol"
            placeholder="RECAUDADOR"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value.toUpperCase() })}
            error={errs.name}
            disabled={!!modal.edit}
            required
          />
          <Textarea
            label="Descripción"
            value={form.description ?? ''}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
          />
        </div>
      </Modal>
    </>
  );
}
