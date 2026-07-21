// Utilidades de formato y validación reutilizables.

export function fmtFecha(iso?: string | null): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return String(iso);
  return d.toLocaleString('es-EC', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function fmtDinero(v?: number | null): string {
  if (v === null || v === undefined) return '—';
  return v.toLocaleString('es-EC', { style: 'currency', currency: 'USD' });
}

export function iniciales(nombre?: string): string {
  if (!nombre) return '??';
  const parts = nombre.trim().split(/\s+/);
  return (parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '');
}

// --- Validaciones cliente (espejo de las del backend, para feedback inmediato) ---

/** Cédula ecuatoriana válida (10 dígitos, provincia, verificador). */
export function esCedulaEc(dni: string): boolean {
  if (!/^\d{10}$/.test(dni)) return false;
  const prov = Number(dni.slice(0, 2));
  if (prov < 1 || prov > 24) return false;
  if (Number(dni[2]) > 5) return false;
  let suma = 0;
  for (let i = 0; i < 9; i++) {
    let m = Number(dni[i]) * (i % 2 === 0 ? 2 : 1);
    if (m > 9) m -= 9;
    suma += m;
  }
  const ver = (10 - (suma % 10)) % 10;
  return ver === Number(dni[9]);
}

export const rgx = {
  username: /^[a-zA-Z0-9._-]+$/,
  password: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{6,}$/,
  email: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  soloLetras: /^[\p{L} ]+$/u,
  telefono: /^\d{7,10}$/,
  placaAuto: /^[A-Z]{3}-\d{4}$/,
  placaMoto: /^[A-Z]{2}-\d{3}[A-Z]$/,
};
