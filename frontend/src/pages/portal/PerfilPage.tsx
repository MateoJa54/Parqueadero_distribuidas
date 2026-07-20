import { useEffect, useState } from 'react';
import { useAuth } from '@/auth/context';
import { authApi } from '@/api/auth';
import { usuariosApi } from '@/api/usuarios';
import type { Persona } from '@/types';
import { ApiError } from '@/api/client';
import { PageHead } from '@/ui/PageHead';
import { Button } from '@/ui/Button';
import { Input } from '@/ui/Input';
import { ErrorState, Loading } from '@/ui/States';
import { useToast } from '@/ui/ToastProvider';
import { rgx } from '@/lib/format';

export function PerfilPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [persona, setPersona] = useState<Persona | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [username, setUsername] = useState(user?.username ?? '');
  const [password, setPassword] = useState('');
  const [errs, setErrs] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let alive = true;
    (async () => {
      try {
        const p = await authApi.me();
        if (alive) setPersona(p);
      } catch (err) {
        if (alive) setError(err instanceof ApiError ? err.message : 'Error al cargar tu perfil.');
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, []);

  const guardar = async () => {
    if (!user) return;
    const e: Record<string, string> = {};
    if (username.length < 3 || username.length > 15 || !rgx.username.test(username))
      e.username = '3–15: letras, números, . _ -';
    if (password && (password.length < 6 || !rgx.password.test(password)))
      e.password = '6–30 con mayúscula, minúscula y número.';
    setErrs(e);
    if (Object.keys(e).length) return;
    setSaving(true);
    try {
      await usuariosApi.update(user.idUsuario, {
        idPersona: persona?.id ?? '',
        username: username.trim(),
        password: password || undefined,
      });
      toast.success('Perfil actualizado');
      setPassword('');
    } catch (err) {
      toast.error('No se pudo actualizar', err instanceof ApiError ? err.message : undefined);
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <PageHead title="Mi perfil" subtitle="Tus datos personales y credenciales de acceso." />

      {loading ? (
        <Loading label="Cargando tu perfil…" />
      ) : error ? (
        <ErrorState message={error} />
      ) : (
        <div className="grid grid-2">
          <div className="card card-pad">
            <h3 style={{ marginTop: 0 }}>Datos personales</h3>
            <dl className="dl">
              <dt>Nombre</dt>
              <dd>
                {persona?.firstName} {persona?.middleName ?? ''} {persona?.lastName}
              </dd>
              <dt>Cédula</dt>
              <dd className="tnum">{persona?.dni}</dd>
              <dt>Correo</dt>
              <dd>{persona?.email}</dd>
              <dt>Teléfono</dt>
              <dd className="tnum">{persona?.phone ?? '—'}</dd>
              <dt>Nacionalidad</dt>
              <dd>{persona?.nationality ?? '—'}</dd>
            </dl>
            <p className="subtle" style={{ fontSize: '0.8125rem', marginTop: 8 }}>
              Para cambiar tus datos personales, contacta a la administración.
            </p>
          </div>

          <div className="card card-pad">
            <h3 style={{ marginTop: 0 }}>Acceso</h3>
            <div className="stack">
              <Input
                label="Usuario"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                error={errs.username}
                required
              />
              <Input
                label="Nueva contraseña"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                error={errs.password}
                hint="Deja vacío para conservar la actual."
              />
              <Button onClick={guardar} loading={saving}>
                Guardar cambios
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
