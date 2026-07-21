import { createContext, useCallback, useContext, useMemo, useState } from 'react';

type ToastType = 'success' | 'danger' | 'info' | 'warning';
interface Toast {
  id: string;
  type: ToastType;
  title: string;
  msg?: string;
}
interface ToastCtx {
  push: (t: Omit<Toast, 'id'>) => void;
  success: (title: string, msg?: string) => void;
  error: (title: string, msg?: string) => void;
  info: (title: string, msg?: string) => void;
}
const Ctx = createContext<ToastCtx | null>(null);

const ICON: Record<ToastType, string> = {
  success: '✓',
  danger: '✕',
  info: 'ℹ',
  warning: '⚠',
};

export function ToastProvider({ children }: { readonly children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const remove = useCallback((id: string) => {
    setToasts((ts) => ts.filter((t) => t.id !== id));
  }, []);

  const push = useCallback(
    (t: Omit<Toast, 'id'>) => {
      const id = crypto.randomUUID();
      setToasts((ts) => [...ts, { ...t, id }]);
      window.setTimeout(() => remove(id), 4200);
    },
    [remove],
  );

  const value = useMemo<ToastCtx>(
    () => ({
      push,
      success: (title, msg) => push({ type: 'success', title, msg }),
      error: (title, msg) => push({ type: 'danger', title, msg }),
      info: (title, msg) => push({ type: 'info', title, msg }),
    }),
    [push],
  );

  return (
    <Ctx.Provider value={value}>
      {children}
      <section className="toast-region" aria-live="polite" aria-label="Notificaciones">
        {toasts.map((t) => (
          <output key={t.id} className={`toast toast-${t.type}`} htmlFor="">
            <span aria-hidden>{ICON[t.type]}</span>
            <div className="grow">
              <div className="toast-title">{t.title}</div>
              {t.msg && <div className="toast-msg">{t.msg}</div>}
            </div>
            <button
              type="button"
              className="btn-ghost btn-sm"
              onClick={() => remove(t.id)}
              aria-label="Cerrar notificación"
            >
              ✕
            </button>
          </output>
        ))}
      </section>
    </Ctx.Provider>
  );
}

export function useToast() {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error('useToast dentro de <ToastProvider>');
  return ctx;
}
