import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from './context';
import { puede, rutaInicial, type Permiso } from './rbac';

/** Exige sesión activa. Sin sesión → /login (recordando destino). */
export function RequireAuth() {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="state" style={{ minHeight: '100vh' }}>
        <div className="spinner" />
        <p className="muted">Cargando sesión…</p>
      </div>
    );
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <Outlet />;
}

/** Protege una rama por permiso concreto. Sin permiso → 403 (no filtra la existencia). */
export function RequirePermiso({ permiso }: { permiso: Permiso }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (!puede(user.roles, permiso)) {
    return <Navigate to="/403" replace />;
  }
  return <Outlet />;
}

/** Redirige la raíz al home correcto según rol. */
export function HomeRedirect() {
  const { user, loading } = useAuth();
  if (loading) return null;
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={rutaInicial(user.roles)} replace />;
}
