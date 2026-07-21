import { useMemo, useState } from 'react';
import { personasApi } from '@/api/usuarios';
import type { Persona, PersonaRequest } from '@/types';
import { useAsync } from '@/hooks/useAsync';
import { ApiError } from '@/api/client';
import { PageHead } from '@/ui/PageHead';
import { Button } from '@/ui/Button';
import { Input } from '@/ui/Input';
import { Modal } from '@/ui/Modal';
import { ActivoBadge } from '@/ui/Badge';
import { EmptyState, AsyncView, TableSkeleton } from '@/ui/States';
import { useToast } from '@/ui/ToastProvider';
import { esCedulaEc, rgx } from '@/lib/format';

const VACIA: PersonaRequest = {
  firstName: '',
  middleName: '',
  lastName: '',
  dni: '',
  email: '',
  phone: '',
  address: '',
  nationality: 'Ecuatoriana',
};

export function PersonasPage() {
  const toast = useToast();
  const { data, loading, error, reload } = useAsync(() => personasApi.list(), []);
  const [q, setQ] = useState('');
  const [modal, setModal] = useState<{ open: boolean; edit?: Persona }>({ open: false });
  const [form, setForm] = useState<PersonaRequest>(VACIA);
  const [errs, setErrs] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const lista = useMemo(() => {
    const arr = data ?? [];
    const term = q.trim().toLowerCase();
    if (!term) return arr;
    return arr.filter(
      (p) =>
        `${p.firstName} ${p.lastName}`.toLowerCase().includes(term) ||
        p.dni.includes(term) ||
        p.email.toLowerCase().includes(term),
    );
  }, [data, q]);

  const abrirNuevo = () => {
    setForm(VACIA);
    setErrs({});
    setModal({ open: true });
  };
  const abrirEditar = (p: Persona) => {
    setForm({
      firstName: p.firstName,
      middleName: p.middleName ?? '',
      lastName: p.lastName,
      dni: p.dni,
      email: p.email,
      phone: p.phone ?? '',
      address: p.address ?? '',
      nationality: p.nationality,
    });
    setErrs({});
    setModal({ open: true, edit: p });
  };

  const validar = () => {
    const e: Record<string, string> = {};
    if (!rgx.soloLetras.test(form.firstName) || !form.firstName.trim())
      e.firstName = 'Solo letras, obligatorio.';
    if (!rgx.soloLetras.test(form.lastName) || !form.lastName.trim())
      e.lastName = 'Solo letras, obligatorio.';
    if (!esCedulaEc(form.dni.trim())) e.dni = 'Cédula ecuatoriana inválida.';
    if (!rgx.email.test(form.email.trim())) e.email = 'Correo inválido.';
    if (!rgx.telefono.test(form.phone.trim())) e.phone = '7 a 10 dígitos.';
    if (!rgx.soloLetras.test(form.nationality)) e.nationality = 'Solo letras.';
    setErrs(e);
    return Object.keys(e).length === 0;
  };

  const guardar = async () => {
    if (!validar()) return;
    setSaving(true);
    try {
      const payload: PersonaRequest = {
        ...form,
        dni: form.dni.trim(),
        email: form.email.trim(),
        phone: form.phone.trim(),
        middleName: form.middleName?.trim() || undefined,
        address: form.address?.trim() || undefined,
      };
      if (modal.edit) {
        await personasApi.update(modal.edit.id, payload);
        toast.success('Persona actualizada');
      } else {
        await personasApi.create(payload);
        toast.success('Persona creada');
      }
      setModal({ open: false });
      reload();
    } catch (err) {
      toast.error('No se pudo guardar', err instanceof ApiError ? err.message : undefined);
    } finally {
      setSaving(false);
    }
  };

  const toggleActivo = async (p: Persona) => {
    try {
      if (p.active) await personasApi.desactivar(p.id);
      else await personasApi.activar(p.id);
      toast.success(p.active ? 'Persona desactivada' : 'Persona activada');
      reload();
    } catch (err) {
      toast.error('No se pudo cambiar el estado', err instanceof ApiError ? err.message : undefined);
    }
  };

  return (
    <>
      <PageHead
        title="Personas"
        subtitle="Datos personales base. Un usuario se crea a partir de una persona activa."
        actions={
          <>
            <input
              className="input"
              style={{ width: 220 }}
              placeholder="Buscar por nombre, cédula…"
              value={q}
              onChange={(e) => setQ(e.target.value)}
              aria-label="Buscar personas"
            />
            <Button onClick={abrirNuevo} icon="＋">
              Nueva persona
            </Button>
          </>
        }
      />

      <AsyncView
        loading={loading}
        error={error}
        isEmpty={lista.length === 0}
        onRetry={reload}
        loadingNode={
          <div className="card card-pad">
            <TableSkeleton cols={5} />
          </div>
        }
        emptyNode={
          <EmptyState
            title="Sin personas"
            message={q ? 'No hay coincidencias con tu búsqueda.' : 'Crea la primera persona.'}
            action={!q && <Button onClick={abrirNuevo}>Nueva persona</Button>}
          />
        }
      >
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Cédula</th>
                <th>Correo</th>
                <th>Teléfono</th>
                <th>Estado</th>
                <th style={{ textAlign: 'right' }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {lista.map((p) => (
                <tr key={p.id}>
                  <td>
                    {p.firstName} {p.middleName ? `${p.middleName} ` : ''}
                    {p.lastName}
                  </td>
                  <td className="tnum">{p.dni}</td>
                  <td>{p.email}</td>
                  <td className="tnum">{p.phone ?? '—'}</td>
                  <td>
                    <ActivoBadge activo={p.active} />
                  </td>
                  <td>
                    <div className="actions">
                      <Button size="sm" variant="secondary" onClick={() => abrirEditar(p)}>
                        Editar
                      </Button>
                      <Button
                        size="sm"
                        variant={p.active ? 'ghost' : 'secondary'}
                        onClick={() => toggleActivo(p)}
                      >
                        {p.active ? 'Desactivar' : 'Activar'}
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
        title={modal.edit ? 'Editar persona' : 'Nueva persona'}
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
          <div className="grid grid-2">
            <Input
              label="Primer nombre"
              value={form.firstName}
              onChange={(e) => setForm({ ...form, firstName: e.target.value })}
              error={errs.firstName}
              required
            />
            <Input
              label="Segundo nombre"
              value={form.middleName ?? ''}
              onChange={(e) => setForm({ ...form, middleName: e.target.value })}
            />
          </div>
          <Input
            label="Apellidos"
            value={form.lastName}
            onChange={(e) => setForm({ ...form, lastName: e.target.value })}
            error={errs.lastName}
            required
          />
          <div className="grid grid-2">
            <Input
              label="Cédula"
              value={form.dni}
              maxLength={10}
              onChange={(e) => setForm({ ...form, dni: e.target.value.replace(/\D/g, '') })}
              error={errs.dni}
              disabled={!!modal.edit}
              required
            />
            <Input
              label="Teléfono"
              value={form.phone}
              onChange={(e) => setForm({ ...form, phone: e.target.value.replace(/\D/g, '') })}
              error={errs.phone}
              required
            />
          </div>
          <Input
            label="Correo"
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            error={errs.email}
            required
          />
          <div className="grid grid-2">
            <Input
              label="Nacionalidad"
              value={form.nationality}
              onChange={(e) => setForm({ ...form, nationality: e.target.value })}
              error={errs.nationality}
              required
            />
            <Input
              label="Dirección"
              value={form.address ?? ''}
              onChange={(e) => setForm({ ...form, address: e.target.value })}
            />
          </div>
        </div>
      </Modal>
    </>
  );
}
