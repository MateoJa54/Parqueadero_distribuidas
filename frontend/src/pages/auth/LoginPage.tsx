import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/context';
import { rutaInicial } from '@/auth/rbac';
import { ApiError } from '@/api/client';
import { Button } from '@/ui/Button';
import { Input, PasswordInput } from '@/ui/Input';
import { useTheme } from '@/ui/ThemeProvider';

export function LoginPage() {
  const { login } = useAuth();
  const { theme, toggle } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const u = await login(username.trim(), password);
      const from = (location.state as { from?: string })?.from;
      navigate(from ?? rutaInicial(u.roles), { replace: true });
    } catch (err) {
      setError(
        err instanceof ApiError ? err.message : 'No se pudo iniciar sesión. Intenta de nuevo.',
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrap">
      <button
        type="button"
        className="icon-btn"
        onClick={toggle}
        aria-label="Cambiar tema"
        style={{ position: 'fixed', top: 16, right: 16 }}
      >
        {theme === 'light' ? '🌙' : '☀'}
      </button>
      <div className="card auth-card">
        <div className="card-body">
          <div className="auth-head">
            <div className="logo" aria-hidden>
              P
            </div>
            <h2>Bienvenido de nuevo</h2>
            <p>Ingresa tus credenciales para continuar</p>
          </div>

          {error && (
            <div className="alert alert-danger" role="alert" style={{ marginBottom: 16 }}>
              <span aria-hidden>⚠</span>
              {error}
            </div>
          )}

          <form className="stack" onSubmit={onSubmit} noValidate>
            <Input
              label="Usuario"
              placeholder="tu.usuario"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              autoFocus
            />
            <PasswordInput
              label="Contraseña"
              placeholder="••••••••"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            <Button type="submit" block loading={loading} disabled={!username || !password}>
              Iniciar sesión
            </Button>
          </form>

          <hr className="divider" style={{ margin: '20px 0' }} />
          <p className="muted" style={{ textAlign: 'center', fontSize: '0.875rem' }}>
            ¿Eres cliente nuevo?{' '}
            <Link to="/registro">Crea tu cuenta</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
