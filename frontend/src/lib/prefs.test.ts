import { describe, it, expect, beforeEach, vi } from 'vitest';
import { getPrefs, setPrefs } from './prefs';

describe('prefs', () => {
  beforeEach(() => localStorage.clear());

  it('devuelve el default cuando no hay nada guardado', () => {
    expect(getPrefs()).toEqual({ pageSize: 15 });
  });

  it('fusiona el patch guardado sobre el default', () => {
    localStorage.setItem('pq_prefs', JSON.stringify({ pageSize: 50 }));
    expect(getPrefs().pageSize).toBe(50);
  });

  it('devuelve el default ante JSON inválido', () => {
    localStorage.setItem('pq_prefs', 'no-es-json');
    expect(getPrefs()).toEqual({ pageSize: 15 });
  });

  it('setPrefs persiste, devuelve el merge y emite el evento', () => {
    const listener = vi.fn();
    window.addEventListener('pq:prefs', listener);
    const next = setPrefs({ pageSize: 25 });
    expect(next.pageSize).toBe(25);
    expect(getPrefs().pageSize).toBe(25);
    expect(listener).toHaveBeenCalledOnce();
    window.removeEventListener('pq:prefs', listener);
  });
});
