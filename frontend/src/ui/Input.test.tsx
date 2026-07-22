import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Field, Input, PasswordInput, Select, Textarea, Combobox } from './Input';

describe('Field', () => {
  it('muestra label con asterisco requerido', () => {
    render(<Field label="Nombre" required htmlFor="x"><input id="x" /></Field>);
    expect(screen.getByText('Nombre')).toBeInTheDocument();
    expect(screen.getByText('*')).toBeInTheDocument();
  });

  it('muestra error con role alert (prioriza sobre hint)', () => {
    render(<Field label="L" error="Malo" hint="Ayuda"><input /></Field>);
    expect(screen.getByRole('alert')).toHaveTextContent('Malo');
    expect(screen.queryByText('Ayuda')).not.toBeInTheDocument();
  });

  it('muestra hint cuando no hay error', () => {
    render(<Field label="L" hint="Ayuda"><input /></Field>);
    expect(screen.getByText('Ayuda')).toBeInTheDocument();
  });

  it('sin label no renderiza label', () => {
    render(<Field><input aria-label="solo" /></Field>);
    expect(screen.getByLabelText('solo')).toBeInTheDocument();
  });
});

describe('Input', () => {
  it('renderiza y acepta texto; marca aria-invalid con error', async () => {
    render(<Input label="Correo" error="req" />);
    const input = screen.getByLabelText('Correo') as HTMLInputElement;
    expect(input).toHaveAttribute('aria-invalid', 'true');
    await userEvent.type(input, 'hola');
    expect(input.value).toBe('hola');
  });

  it('respeta id explícito', () => {
    render(<Input label="X" id="mi-id" />);
    expect(document.getElementById('mi-id')).toBeInTheDocument();
  });
});

describe('PasswordInput', () => {
  it('alterna visibilidad de la contraseña', async () => {
    render(<PasswordInput label="Clave" />);
    const input = screen.getByLabelText('Clave') as HTMLInputElement;
    expect(input.type).toBe('password');
    await userEvent.click(screen.getByLabelText('Mostrar contraseña'));
    expect(input.type).toBe('text');
    await userEvent.click(screen.getByLabelText('Ocultar contraseña'));
    expect(input.type).toBe('password');
  });
});

describe('Select', () => {
  const options = [{ value: 'a', label: 'A' }, { value: 'b', label: 'B' }];

  it('renderiza opciones y placeholder', () => {
    render(<Select label="Sel" options={options} placeholder="Elige" />);
    expect(screen.getByRole('option', { name: 'Elige' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'A' })).toBeInTheDocument();
  });

  it('permite seleccionar un valor', async () => {
    render(<Select label="Sel" options={options} />);
    await userEvent.selectOptions(screen.getByLabelText('Sel'), 'b');
    expect((screen.getByLabelText('Sel') as HTMLSelectElement).value).toBe('b');
  });
});

describe('Textarea', () => {
  it('acepta texto multilinea', async () => {
    render(<Textarea label="Notas" />);
    const ta = screen.getByLabelText('Notas') as HTMLTextAreaElement;
    await userEvent.type(ta, 'linea');
    expect(ta.value).toBe('linea');
  });
});

describe('Combobox', () => {
  const options = [
    { value: '1', label: 'Uno' },
    { value: '2', label: 'Dos' },
    { value: '3', label: 'Tres' },
  ];

  it('muestra la etiqueta seleccionada cuando está cerrado', () => {
    render(<Combobox label="C" options={options} value="2" onChange={() => {}} />);
    expect((screen.getByRole('combobox') as HTMLInputElement).value).toBe('Dos');
  });

  it('filtra opciones al teclear y elige con clic', async () => {
    const onChange = vi.fn();
    render(<Combobox label="C" options={options} value="" onChange={onChange} />);
    const input = screen.getByRole('combobox');
    await userEvent.click(input);
    await userEvent.type(input, 'tre');
    expect(screen.getByRole('option', { name: 'Tres' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Uno' })).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('option', { name: 'Tres' }));
    expect(onChange).toHaveBeenCalledWith('3');
  });

  it('muestra emptyText sin coincidencias', async () => {
    render(<Combobox label="C" options={options} value="" onChange={() => {}} emptyText="Nada" />);
    const input = screen.getByRole('combobox');
    await userEvent.click(input);
    await userEvent.type(input, 'zzz');
    expect(screen.getByText('Nada')).toBeInTheDocument();
  });

  it('navega con teclado y elige con Enter', async () => {
    const onChange = vi.fn();
    render(<Combobox label="C" options={options} value="" onChange={onChange} />);
    const input = screen.getByRole('combobox');
    input.focus();
    await userEvent.keyboard('{ArrowDown}');
    await userEvent.keyboard('{ArrowDown}');
    await userEvent.keyboard('{ArrowUp}');
    await userEvent.keyboard('{Enter}');
    expect(onChange).toHaveBeenCalledWith('2');
  });

  it('Escape cierra el desplegable', async () => {
    render(<Combobox label="C" options={options} value="" onChange={() => {}} />);
    const input = screen.getByRole('combobox');
    await userEvent.click(input);
    expect(screen.getByRole('listbox')).toBeInTheDocument();
    await userEvent.keyboard('{Escape}');
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
  });

  it('clic fuera cierra el desplegable', async () => {
    render(
      <div>
        <Combobox label="C" options={options} value="" onChange={() => {}} />
        <button type="button">fuera</button>
      </div>,
    );
    await userEvent.click(screen.getByRole('combobox'));
    expect(screen.getByRole('listbox')).toBeInTheDocument();
    await userEvent.click(screen.getByText('fuera'));
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
  });

  it('deshabilitado no abre', async () => {
    render(<Combobox label="C" options={options} value="" onChange={() => {}} disabled />);
    const input = screen.getByRole('combobox');
    await userEvent.click(input);
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
  });
});
