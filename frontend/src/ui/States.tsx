import type { ReactNode } from 'react';
import { Button } from './Button';

// Estados de vista: loading (skeleton), empty, error. Siempre presentes en las listas.

export function Loading({ label = 'Cargando…' }: { readonly label?: string }) {
  return (
    <div className="state">
      <div className="spinner" />
      <p className="muted">{label}</p>
    </div>
  );
}

export function TableSkeleton({ cols = 4, rows = 5 }: { readonly cols?: number; readonly rows?: number }) {
  return (
    <div className="stack" style={{ gap: 8 }}>
      {Array.from({ length: rows }).map((_, r) => (
        <div key={`row-${r}`} className="row" style={{ gap: 12 }}>
          {Array.from({ length: cols }).map((_, c) => (
            <div
              key={`col-${r}-${c}`}
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
  readonly icon?: string;
  readonly title: string;
  readonly message?: string;
  readonly action?: ReactNode;
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
  readonly message: string;
  readonly onRetry?: () => void;
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

interface AsyncViewProps {
  readonly loading: boolean;
  readonly error?: string | null;
  readonly isEmpty?: boolean;
  readonly onRetry?: () => void;
  readonly loadingNode?: ReactNode;
  readonly emptyNode?: ReactNode;
  readonly children: ReactNode;
}

/**
 * Renderiza el estado adecuado (cargando / error / vacío / contenido) evitando
 * ternarios anidados en cada página. Equivalente al patrón
 * `loading ? … : error ? … : empty ? … : contenido`.
 */
export function AsyncView({
  loading,
  error,
  isEmpty,
  onRetry,
  loadingNode,
  emptyNode,
  children,
}: AsyncViewProps) {
  if (loading) return <>{loadingNode ?? <Loading />}</>;
  if (error) return <ErrorState message={error} onRetry={onRetry} />;
  if (isEmpty) return <>{emptyNode ?? <EmptyState title="Sin registros" />}</>;
  return <>{children}</>;
}
