import { useMemo, useState } from 'react';
import { asignacionesRolApi, rolesApi, usuariosApi } from '@/api/usuarios';
import type { AsignacionRol, Usuario } from '@/types';
import { useAsync } from '@/hooks/useAsync';
import { ApiError } from '@/api/client';
import { PageHead } from '@/ui/PageHead';
import { Button } from '@/ui/Button';
import { Select } from '@/ui/Input';
import { Modal } from '@/ui/Modal';
import { ActivoBadge } from '@/ui/Badge';
import { EmptyState, ErrorState, TableSkeleton } from '@/ui/States';
import { useToast } from '@/ui/ToastProvider';

export function AsignacionRolesPage() {
  const toast = useToast();
  const asignaciones = useAsync(() => asignacionesRolApi.list(), []);
  const usuarios = useAsync(() => usuariosApi.list(), []);
  const roles = useAsync(() => rolesApi.list(), []);
  const [modal, setModal] = useState(false);
  const [idUser, setIdUser] = useState('');
  const [idRole, setIdRole] = useState('');
  const [saving, setSaving] = useState(false);

  const usuariosMap = useMemo(() => {
    const m = new Map<string, Usuario>();
    for (const u of usuarios.data ?? []) m.set(u.id, u);
    return m;
  }, [usuarios.data]);

  const asignar = async () => {
    if (!idUser || !idRole) {
      toast.error('Selecciona usuario y rol');
      return;
    }
    setSaving(true);
    try {
      await asignacionesRolApi.asignar({ idUser, idRole });
      toast.success('Rol asignado');
      setModal(false);
      setIdUser('');
      setIdRole('');
      asignaciones.reload();
    } catch (err) {
      toast.error('No se pudo asignar', err instanceof ApiError ? err.message : undefined);
    } finally {
      setSaving(false);
    }
  };

  const toggle = async (a: AsignacionRol) => {
    try {
      if (a.active) await asignacionesRolApi.desactivar(a.idUser, a.idRole);
      else await asignacionesRolApi.activar(a.idUser, a.idRole);
      toast.success(a.active ? 'Asignación desactivada' : 'Asignación activada');
      asignaciones.reload();
    } catch (err) {
      toast.error('No se pudo cambiar el estado', err instanceof ApiError ? err.message : undefined);
    }
  };

  const loading = asignaciones.loading || usuarios.loading || roles.loading;

  return (
    <>
      <PageHead
        title="Asignación de roles"
        subtitle="Vincula roles a usuarios. Un usuario puede tener varios roles."
        actions={
          <Button onClick={() => setModal(true)} icon="＋">
            Asignar rol
          </Button>
        }
      />

      {loading ? (
        <div className="card card-pad">
          <TableSkeleton cols={3} />
        </div>
      ) : asignaciones.error ? (
        <ErrorState message={asignaciones.error} onRetry={asignaciones.reload} />
      ) : (asignaciones.data ?? []).length === 0 ? (
        <EmptyState title="Sin asignaciones" message="Asigna el primer rol a un usuario." />
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Usuario</th>
                <th>Rol</th>
                <th>Estado</th>
                <th style={{ textAlign: 'right' }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {(asignaciones.data ?? []).map((a) => (
                <tr key={`${a.idUser}-${a.idRole}`}>
                  <td>{a.username || usuariosMap.get(a.idUser)?.username || a.idUser}</td>
                  <td>
                    <span className="chip">{a.rol}</span>
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
        title="Asignar rol a usuario"
        onClose={() => setModal(false)}
        footer={
          <>
            <Button variant="ghost" onClick={() => setModal(false)}>
              Cancelar
            </Button>
            <Button onClick={asignar} loading={saving}>
              Asignar
            </Button>
          </>
        }
      >
        <div className="stack">
          <Select
            label="Usuario"
            placeholder="Selecciona…"
            value={idUser}
            onChange={(e) => setIdUser(e.target.value)}
            options={(usuarios.data ?? [])
              .filter((u) => u.active)
              .map((u) => ({ value: u.id, label: `${u.username} · ${u.nombreCompleto}` }))}
            required
          />
          <Select
            label="Rol"
            placeholder="Selecciona…"
            value={idRole}
            onChange={(e) => setIdRole(e.target.value)}
            options={(roles.data ?? [])
              .filter((r) => r.active)
              .map((r) => ({ value: r.id, label: r.name }))}
            required
          />
        </div>
      </Modal>
    </>
  );
}
