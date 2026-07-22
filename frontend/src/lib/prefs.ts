// Preferencias de interfaz persistidas en localStorage. Editables desde Configuración.

const KEY = 'pq_prefs';

export interface Prefs {
  /** Filas por página en tablas paginadas (tickets). */
  pageSize: number;
}

const DEFAULT: Prefs = { pageSize: 15 };

export function getPrefs(): Prefs {
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return DEFAULT;
    return { ...DEFAULT, ...(JSON.parse(raw) as Partial<Prefs>) };
  } catch {
    return DEFAULT;
  }
}

export function setPrefs(patch: Partial<Prefs>): Prefs {
  const next = { ...getPrefs(), ...patch };
  localStorage.setItem(KEY, JSON.stringify(next));
  window.dispatchEvent(new CustomEvent('pq:prefs', { detail: next }));
  return next;
}
