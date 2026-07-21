import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ToastProvider, useToast } from './ToastProvider';

function Probe() {
  const t = useToast();
  return (
    <div>
      <button type="button" onClick={() => t.success('Ok', 'detalle')}>ok</button>
      <button type="button" onClick={() => t.error('Err')}>err</button>
      <button type="button" onClick={() => t.info('Info')}>info</button>
      <button type="button" onClick={() => t.push({ type: 'warning', title: 'W' })}>warn</button>
    </div>
  );
}

describe('ToastProvider', () => {
  afterEach(() => vi.useRealTimers());

  it('muestra toast success con título y mensaje', async () => {
    render(<ToastProvider><Probe /></ToastProvider>);
    await userEvent.click(screen.getByText('ok'));
    expect(screen.getByText('Ok')).toBeInTheDocument();
    expect(screen.getByText('detalle')).toBeInTheDocument();
  });

  it('error e info se muestran', async () => {
    render(<ToastProvider><Probe /></ToastProvider>);
    await userEvent.click(screen.getByText('err'));
    await userEvent.click(screen.getByText('info'));
    expect(screen.getByText('Err')).toBeInTheDocument();
    expect(screen.getByText('Info')).toBeInTheDocument();
  });

  it('permite cerrar un toast', async () => {
    render(<ToastProvider><Probe /></ToastProvider>);
    await userEvent.click(screen.getByText('warn'));
    expect(screen.getByText('W')).toBeInTheDocument();
    await userEvent.click(screen.getByLabelText('Cerrar notificación'));
    await waitFor(() => expect(screen.queryByText('W')).not.toBeInTheDocument());
  });

  it('el toast se autoelimina tras el timeout', async () => {
    vi.useFakeTimers();
    render(<ToastProvider><Probe /></ToastProvider>);
    act(() => { screen.getByText('ok').click(); });
    expect(screen.getByText('Ok')).toBeInTheDocument();
    act(() => { vi.advanceTimersByTime(4300); });
    expect(screen.queryByText('Ok')).not.toBeInTheDocument();
  });

  it('useToast lanza fuera del provider', () => {
    expect(() => render(<Probe />)).toThrow('useToast dentro de <ToastProvider>');
  });
});
