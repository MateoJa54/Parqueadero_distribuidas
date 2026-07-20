import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/context';
import { rutaInicial } from '@/auth/rbac';
import { ApiError } from '@/api/client';
import { Button } from '@/ui/Button';
import { Input, PasswordInput } from '@/ui/Input';
import { useTheme } from '@/ui/ThemeProvider';
import { esCedulaEc, rgx } from '@/lib/format';

interface Errores {
  firstName?: string;
  middleName?: string;
  lastName?: string;
  dni?: string;
  email?: string;
  phone?: string;
  address?: string;
  nationality?: string;
  username?: string;
  password?: string;
  confirm?: string;
}

const soloLetras = /^[\p{L} ]+$/u;

export function RegistroPage() {
  const { registrarCompleto } = useAuth();
  const { theme, toggle } = useTheme();
  const navigate = useNavigate();

  // Asistente de 2 pasos: 1) datos personales, 2) crear credenciales de acceso.
  const [paso, setPaso] = useState<1 | 2>(1);

  // Paso 1: datos de la persona (identidad).
  const [firstName, setFirstName] = useState('');
  const [middleName, setMiddleName] = useState('');
  const [lastName, setLastName] = useState('');
  const [dni, setDni] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');
  const [nationality, setNationality] = useState('Ecuatoriana');

  // Paso 2: credenciales del usuario (acceso).
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');

  const [errores, setErrores] = useState<Errores>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // Paso 1: valida los datos personales (mismas reglas que el backend).
  const validarDatos = (): boolean => {
    const e: Errores = {};
    if (!firstName.trim() || !soloLetras.test(firstName.trim()) || firstName.trim().length > 30)
      e.firstName = 'Solo letras, máximo 30 caracteres.';
    if (middleName.trim() && (!soloLetras.test(middleName.trim()) || middleName.trim().length > 30))
      e.middleName = 'Solo letras, máximo 30 caracteres.';
    if (!lastName.trim() || !soloLetras.test(lastName.trim()) || lastName.trim().length > 30)
      e.lastName = 'Solo letras, máximo 30 caracteres.';
    if (!esCedulaEc(dni.trim())) e.dni = 'Cédula ecuatoriana inválida (10 dígitos).';
    if (!rgx.email.test(email.trim())) e.email = 'Correo no válido.';
    else if (email.trim().length > 50) e.email = 'Máximo 50 caracteres.';
    if (!/^\d{7,10}$/.test(phone.trim())) e.phone = 'Entre 7 y 10 dígitos numéricos.';
    if (address.trim().length > 255) e.address = 'Máximo 255 caracteres.';
    if (!nationality.trim() || !soloLetras.test(nationality.trim()) || nationality.trim().length > 30)
      e.nationality = 'Solo letras, máximo 30 caracteres.';
    setErrores(e);
    return Object.keys(e).length === 0;
  };

  const validarCredenciales = (): boolean => {
    const e: Errores = {};
    if (username.length < 3 || username.length > 15 || !rgx.username.test(username))
      e.username = '3–15 caracteres: letras, números, . _ -';
    if (password.length < 6 || password.length > 30 || !rgx.password.test(password))
      e.password = '6–30 caracteres con mayúscula, minúscula y número.';
    if (confirm !== password) e.confirm = 'Las contraseñas no coinciden.';
    setErrores(e);
    return Object.keys(e).length === 0;
  };

  const irAPaso2 = (ev: React.FormEvent) => {
    ev.preventDefault();
    setError(null);
    if (validarDatos()) setPaso(2);
  };

  const volverAPaso1 = () => {
    setError(null);
    setErrores({});
    setPaso(1);
  };

  const onSubmit = async (ev: React.FormEvent) => {
    ev.preventDefault();
    setError(null);
    if (!validarCredenciales()) return;
    setLoading(true);
    try {
      const u = await registrarCompleto({
        firstName: firstName.trim(),
        middleName: middleName.trim() || undefined,
        lastName: lastName.trim(),
        dni: dni.trim(),
        email: email.trim(),
        phone: phone.trim(),
        address: address.trim() || undefined,
        nationality: nationality.trim(),
        username: username.trim(),
        password,
      });
      navigate(rutaInicial(u.roles), { replace: true });
    } catch (err) {
      setError(
        err instanceof ApiError
          ? err.message
          : 'No pudimos completar el registro. Intenta más tarde.',
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrap">
      <button
        className="icon-btn"
        onClick={toggle}
        aria-label="Cambiar tema"
        style={{ position: 'fixed', top: 16, right: 16 }}
      >
        {theme === 'light' ? '🌙' : '☀'}
      </button>
      <div className="card auth-card wide">
        <div className="card-body">
          <div className="auth-head">
            <div className="logo" aria-hidden>
              P
            </div>
            <h2>Crea tu cuenta</h2>
            <p>
              {paso === 1
                ? 'Paso 1 de 2 — Cuéntanos tus datos personales.'
                : 'Paso 2 de 2 — Elige tus credenciales de acceso.'}
            </p>
          </div>

          {/* Indicador de pasos */}
          <div aria-hidden style={{ display: 'flex', gap: 8, margin: '0 0 16px' }}>
            <span
              style={{
                flex: 1,
                height: 4,
                borderRadius: 999,
                background: 'var(--primary, #4f46e5)',
              }}
            />
            <span
              style={{
                flex: 1,
                height: 4,
                borderRadius: 999,
                background: paso === 2 ? 'var(--primary, #4f46e5)' : 'var(--border, #d1d5db)',
              }}
            />
          </div>

          {error && (
            <div className="alert alert-danger" role="alert" style={{ marginBottom: 16 }}>
              <span aria-hidden>⚠</span>
              {error}
            </div>
          )}

          {paso === 1 ? (
            <form className="stack" onSubmit={irAPaso2} noValidate>
              <div className="grid grid-2" style={{ gap: 12 }}>
                <Input
                  label="Primer nombre"
                  placeholder="Juan"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  error={errores.firstName}
                  required
                  autoFocus
                />
                <Input
                  label="Segundo nombre"
                  placeholder="Carlos"
                  value={middleName}
                  onChange={(e) => setMiddleName(e.target.value)}
                  error={errores.middleName}
                  hint="Opcional"
                />
              </div>
              <Input
                label="Apellidos"
                placeholder="Pérez López"
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                error={errores.lastName}
                required
              />
              <div className="grid grid-2" style={{ gap: 12 }}>
                <Input
                  label="Cédula"
                  inputMode="numeric"
                  maxLength={10}
                  placeholder="1710034065"
                  value={dni}
                  onChange={(e) => setDni(e.target.value.replace(/\D/g, ''))}
                  error={errores.dni}
                  required
                />
                <Input
                  label="Teléfono"
                  inputMode="numeric"
                  maxLength={10}
                  placeholder="0991234567"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value.replace(/\D/g, ''))}
                  error={errores.phone}
                  required
                />
              </div>
              <Input
                label="Correo electrónico"
                type="email"
                placeholder="tu@correo.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                error={errores.email}
                required
              />
              <Input
                label="Dirección"
                placeholder="Av. Principal 123 y Secundaria"
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                error={errores.address}
                hint="Opcional"
              />
              <Input
                label="Nacionalidad"
                placeholder="Ecuatoriana"
                value={nationality}
                onChange={(e) => setNationality(e.target.value)}
                error={errores.nationality}
                required
              />
              <Button type="submit" block>
                Continuar
              </Button>
            </form>
          ) : (
            <form className="stack" onSubmit={onSubmit} noValidate>
              <div className="muted" style={{ fontSize: '0.8125rem', marginBottom: 4 }}>
                {firstName} {lastName} · <strong>{dni}</strong> · {email}
              </div>
              <Input
                label="Usuario"
                placeholder="tu.usuario"
                autoComplete="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                error={errores.username}
                required
                autoFocus
              />
              <PasswordInput
                label="Contraseña"
                placeholder="••••••••"
                autoComplete="new-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                error={errores.password}
                hint="Mínimo 6 caracteres, con mayúscula, minúscula y número."
                required
              />
              <PasswordInput
                label="Confirmar contraseña"
                placeholder="••••••••"
                autoComplete="new-password"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                error={errores.confirm}
                required
              />
              <div className="grid grid-2" style={{ gap: 12 }}>
                <Button type="button" variant="secondary" block onClick={volverAPaso1}>
                  Atrás
                </Button>
                <Button type="submit" block loading={loading}>
                  Crear mi cuenta
                </Button>
              </div>
            </form>
          )}

          <hr className="divider" style={{ margin: '20px 0' }} />
          <p className="muted" style={{ textAlign: 'center', fontSize: '0.875rem' }}>
            ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
