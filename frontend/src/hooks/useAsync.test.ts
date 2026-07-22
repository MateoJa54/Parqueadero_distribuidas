import { describe, it, expect } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { useAsync } from './useAsync';
import { ApiError } from '@/api/client';

describe('useAsync', () => {
  it('resuelve datos', async () => {
    const { result } = renderHook(() => useAsync(() => Promise.resolve(42)));
    expect(result.current.loading).toBe(true);
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.data).toBe(42);
    expect(result.current.error).toBeNull();
  });

  it('captura mensaje de ApiError', async () => {
    const { result } = renderHook(() => useAsync(() => Promise.reject(new ApiError(500, 'boom'))));
    await waitFor(() => expect(result.current.error).toBe('boom'));
    expect(result.current.data).toBeNull();
  });

  it('captura mensaje de Error genérico', async () => {
    const { result } = renderHook(() => useAsync(() => Promise.reject(new Error('generico'))));
    await waitFor(() => expect(result.current.error).toBe('generico'));
  });

  it('reload vuelve a ejecutar fn', async () => {
    let n = 0;
    const { result } = renderHook(() => useAsync(() => Promise.resolve(++n)));
    await waitFor(() => expect(result.current.data).toBe(1));
    act(() => result.current.reload());
    await waitFor(() => expect(result.current.data).toBe(2));
  });
});
