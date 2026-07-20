import type { ReactNode } from 'react';
import { Button } from './Button';

// Estados de vista: loading (skeleton), empty, error. Siempre presentes en las listas.

export function Loading({ label = 'Cargando…' }: { label?: string }) {
  return (
    <div className="state">
      <div className="spinner" />
      <p className="muted">{label}</p>
    </div>
  );
}

export function TableSkeleton({ cols = 4, rows = 5 }: { cols?: number; rows?: number }) {
  return (
    <div className="stack" style={{ gap: 8 }}>
      {Array.from({ length: rows }).map((_, r) => (
        <div key={r} className="row" style={{ gap: 12 }}>
          {Array.from({ length: cols }).map((_, c) => (
            <div
              key={c}
              className="skeleton"
              style={{ height: 18, flex: c === 0 ? 2 : 1 }}
            />
          ))}
        </div>
      ))}
    </div>
  );
}

export function EmptyState({
  icon = '📭',
  title,
  message,
  action,
}: {
  icon?: string;
  title: string;
  message?: string;
  action?: ReactNode;
}) {
  return (
    <div className="state">
      <div className="state-icon" aria-hidden>
        {icon}
      </div>
      <h4>{title}</h4>
      {message && <p className="muted">{message}</p>}
      {action}
    </div>
  );
}

export function ErrorState({
  message,
  onRetry,
}: {
  message: string;
  onRetry?: () => void;
}) {
  return (
    <div className="state">
      <div className="state-icon" aria-hidden>
        ⚠️
      </div>
      <h4>Ocurrió un error</h4>
      <p className="muted">{message}</p>
      {onRetry && (
        <Button variant="secondary" onClick={onRetry}>
          Reintentar
        </Button>
      )}
    </div>
  );
}
