import { useMemo, useState } from 'react';
import { personasApi, usuariosApi } from '@/api/usuarios';
import type { Persona, Usuario } from '@/types';
import { useAsync } from '@/hooks/useAsync';
import { ApiError } from '@/api/client';
import { PageHead } from '@/ui/PageHead';
import { Button } from '@/ui/Button';
import { Input, Select } from '@/ui/Input';
import { Modal } from '@/ui/Modal';
import { ActivoBadge } from '@/ui/Badge';
import { EmptyState, AsyncView, TableSkeleton } from '@/ui/States';
import { useToast } from '@/ui/ToastProvider';
import { fmtFecha, rgx } from '@/lib/format';

export function UsuariosPage() {
  const toast = useToast();
  const usuarios = useAsync(() => usuariosApi.list(), []);
  const personas = useAsync(() => personasApi.list(), []);
  const [q, setQ] = useState('');
  const [modal, setModal] = useState<{ open: boolean; edit?: Usuario }>({ open: false });
  const [idPersona, setIdPersona] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [errs, setErrs] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const personasMap = useMemo(() => {
    const m = new Map<string, Persona>();
    for (const p of personas.data ?? []) m.set(p.id, p);
    return m;
  }, [personas.data]);

  // Personas activas SIN usuario aún (para crear).
  const personasDisponibles = useMemo(() => {
    const conUsuario = new Set((usuarios.data ?? []).map((u) => u.idPersona));
    return (personas.data ?? []).filter((p) => p.active && !conUsuario.has(p.id));
  }, [personas.data, usuarios.data]);

  const lista = useMemo(() => {
    const arr = usuarios.data ?? [];
    const term = q.trim().toLowerCase();
    if (!term) return arr;
    return arr.filter(
      (u) =>
        u.username.toLowerCase().includes(term) ||
        u.nombreCompleto.toLowerCase().includes(term),
    );
  }, [usuarios.data, q]);

  const abrirNuevo = () => {
    setIdPersona('');
    setUsername('');
    setPassword('');
    setErrs({});
    setModal({ open: true });
  };
  const abrirEditar = (u: Usuario) => {
    setIdPersona(u.idPersona);
    setUsername(u.username);
    setPassword('');
    setErrs({});
    setModal({ open: true, edit: u });
  };

  const validar = () => {
    const e: Record<string, string> = {};
    if (!modal.edit && !idPersona) e.idPersona = 'Selecciona una persona.';
    if (username.length < 3 || username.length > 15 || !rgx.username.test(username))
      e.username = '3–15: letras, números, . _ -';
    if (modal.edit) {
      if (password && (password.length < 6 || !rgx.password.test(password)))
        e.password = '6–30 con mayúscula, minúscula y número (o vacío para conservar).';
    } else {
      if (password.length < 6 || !rgx.password.test(password))
        e.password = '6–30 con mayúscula, minúscula y número.';
    }
    setErrs(e);
    return Object.keys(e).length === 0;
  };

  const guardar = async () => {
    if (!validar()) return;
    setSaving(true);
    try {
      if (modal.edit) {
        await usuariosApi.update(modal.edit.id, {
          idPersona: modal.edit.idPersona,
          username: username.trim(),
          password: password || undefined,
        });
        toast.success('Usuario actualizado');
      } else {
        await usuariosApi.create({ idPersona, username: username.trim(), password });
        toast.success('Usuario creado');
      }
      setModal({ open: false });
      usuarios.reload();
    } catch (err) {
      toast.error('No se pudo guardar', err instanceof ApiError ? err.message : undefined);
    } finally {
      setSaving(false);
    }
  };

  const toggleActivo = async (u: Usuario) => {
    try {
      if (u.active) await usuariosApi.desactivar(u.id);
      else await usuariosApi.activar(u.id);
      toast.success(u.active ? 'Usuario desactivado' : 'Usuario activado');
      usuarios.reload();
    } catch (err) {
      toast.error('No se pudo cambiar el estado', err instanceof ApiError ? err.message : undefined);
    }
  };

  const loading = usuarios.loading || personas.loading;

  return (
    <>
      <PageHead
        title="Usuarios"
        subtitle="Credenciales de acceso vinculadas a una persona."
        actions={
          <>
            <input
              className="input"
              style={{ width: 220 }}
              placeholder="Buscar usuario…"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              aria-label="Buscar usuarios"
            />
            <Button onClick={abrirNuevo} icon="＋">
              Nuevo usuario
            </Button>
          </>
        }
      />

      <AsyncView
        loading={loading}
        error={usuarios.error}
        isEmpty={lista.length === 0}
        onRetry={usuarios.reload}
        loadingNode={
          <div className="card card-pad">
            <TableSkeleton cols={4} />
          </div>
        }
        emptyNode={
          <EmptyState title="Sin usuarios" message="Crea el primer usuario a partir de una persona activa." />
        }
      >
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Usuario</th>
                <th>Persona</th>
                <th>Último acceso</th>
                <th>Estado</th>
                <th style={{ textAlign: 'right' }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {lista.map((u) => (
                <tr key={u.id}>
                  <td>
                    <strong>{u.username}</strong>
                  </td>
                  <td>
                    {u.nombreCompleto}
                    <div className="subtle" style={{ fontSize: '0.75rem' }}>
                      {personasMap.get(u.idPersona)?.dni ?? ''}
                    </div>
                  </td>
                  <td className="tnum">{fmtFecha(u.lastLogin)}</td>
                  <td>
                    <ActivoBadge activo={u.active} />
                  </td>
                  <td>
                    <div className="actions">
                      <Button size="sm" variant="secondary" onClick={() => abrirEditar(u)}>
                        Editar
                      </Button>
                      <Button
                        size="sm"
                        variant={u.active ? 'ghost' : 'secondary'}
                        onClick={() => toggleActivo(u)}
                      >
                        {u.active ? 'Desactivar' : 'Activar'}
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
        title={modal.edit ? 'Editar usuario' : 'Nuevo usuario'}
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
          {modal.edit ? (
            <Input label="Persona" value={modal.edit.nombreCompleto} disabled />
          ) : personasDisponibles.length === 0 ? (
            <div className="alert alert-warning">
              <span aria-hidden>⚠</span>
              No hay personas activas sin usuario. Crea una persona primero.
            </div>
          ) : (
            <Select
              label="Persona"
              placeholder="Selecciona una persona…"
              value={idPersona}
              onChange={(e) => setIdPersona(e.target.value)}
              error={errs.idPersona}
              required
              options={personasDisponibles.map((p) => ({
                value: p.id,
                label: `${p.firstName} ${p.lastName} · ${p.dni}`,
              }))}
            />
          )}
          <Input
            label="Usuario"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            error={errs.username}
            required
          />
          <Input
            label={modal.edit ? 'Nueva contraseña (opcional)' : 'Contraseña'}
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={errs.password}
            hint={modal.edit ? 'Deja vacío para conservar la actual.' : undefined}
            required={!modal.edit}
          />
        </div>
      </Modal>
    </>
  );
}
