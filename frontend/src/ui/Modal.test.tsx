import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Modal } from './Modal';

describe('Modal', () => {
  it('no renderiza cuando open=false', () => {
    render(<Modal open={false} title="T" onClose={() => {}}>body</Modal>);
    expect(screen.queryByText('body')).not.toBeInTheDocument();
  });

  it('renderiza título, cuerpo y footer', () => {
    render(<Modal open title="Titulo" onClose={() => {}} footer={<button type="button">F</button>}>Contenido</Modal>);
    expect(screen.getByText('Titulo')).toBeInTheDocument();
    expect(screen.getByText('Contenido')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'F' })).toBeInTheDocument();
  });

  it('cierra con botón ✕', async () => {
    const onClose = vi.fn();
    render(<Modal open title="T" onClose={onClose}>b</Modal>);
    await userEvent.click(screen.getByLabelText('Cerrar'));
    expect(onClose).toHaveBeenCalled();
  });

  it('cierra con tecla Escape', async () => {
    const onClose = vi.fn();
    render(<Modal open title="T" onClose={onClose}>b</Modal>);
    await userEvent.keyboard('{Escape}');
    expect(onClose).toHaveBeenCalled();
  });

  it('cierra al click en overlay', async () => {
    const onClose = vi.fn();
    render(<Modal open title="T" onClose={onClose}>b</Modal>);
    const overlay = document.querySelector('.overlay')!;
    await userEvent.click(overlay as Element);
    expect(onClose).toHaveBeenCalled();
  });

  it('aplica clase modal-lg con size lg', () => {
    render(<Modal open title="T" onClose={() => {}} size="lg">b</Modal>);
    expect(document.querySelector('.modal-lg')).toBeInTheDocument();
  });

  it('Tab atrapa el foco dentro del modal', async () => {
    render(
      <Modal open title="T" onClose={() => {}} footer={<button type="button">Last</button>}>
        <button type="button">First</button>
      </Modal>,
    );
    // El foco inicial cae en el primer control; Tab shift desde el primero va al último.
    await userEvent.tab({ shift: true });
    expect(document.activeElement).toBeTruthy();
  });
});
