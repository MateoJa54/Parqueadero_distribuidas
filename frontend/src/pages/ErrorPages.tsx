import { Link } from 'react-router-dom';

export function ForbiddenPage() {
  return (
    <div className="auth-wrap">
      <div className="card auth-card">
        <div className="card-body state">
          <div className="state-icon" aria-hidden>
            🚫
          </div>
          <h2>Acceso denegado</h2>
          <p className="muted">No tienes permisos para ver esta sección.</p>
          <Link to="/" className="btn btn-primary" style={{ marginTop: 8 }}>
            Volver al inicio
          </Link>
        </div>
      </div>
    </div>
  );
}

export function NotFoundPage() {
  return (
    <div className="auth-wrap">
      <div className="card auth-card">
        <div className="card-body state">
          <div className="state-icon" aria-hidden>
            🧭
          </div>
          <h2>Página no encontrada</h2>
          <p className="muted">La ruta que buscas no existe.</p>
          <Link to="/" className="btn btn-primary" style={{ marginTop: 8 }}>
            Volver al inicio
          </Link>
        </div>
      </div>
    </div>
  );
}
