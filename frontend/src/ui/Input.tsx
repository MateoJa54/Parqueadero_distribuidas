import type {
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from 'react';
import { useEffect, useId, useMemo, useRef, useState } from 'react';

interface FieldWrap {
  /** Etiqueta visible. Si se omite, provee `aria-label` en el control para accesibilidad. */
  label?: string;
  required?: boolean;
  hint?: string;
  error?: string;
}

export function Field({
  label,
  required,
  hint,
  error,
  children,
  htmlFor,
}: FieldWrap & { children: ReactNode; htmlFor?: string }) {
  return (
    <div className="field">
      {label && (
        <label htmlFor={htmlFor}>
          {label}
          {required && <span className="req" aria-hidden>*</span>}
        </label>
      )}
      {children}
      {error ? (
        <span className="error" role="alert">
          <span aria-hidden>⚠</span>
          {error}
        </span>
      ) : hint ? (
        <span className="hint">{hint}</span>
      ) : null}
    </div>
  );
}

type InputProps = InputHTMLAttributes<HTMLInputElement> & FieldWrap;

export function Input({ label, required, hint, error, id, ...rest }: InputProps) {
  const auto = useId();
  const fid = id ?? auto;
  return (
    <Field label={label} required={required} hint={hint} error={error} htmlFor={fid}>
      <input
        id={fid}
        className="input"
        aria-invalid={!!error}
        aria-required={required}
        {...rest}
      />
    </Field>
  );
}

type PasswordProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> & FieldWrap;

/** Campo de contraseña con botón de ojo para mostrar/ocultar el texto. */
export function PasswordInput({ label, required, hint, error, id, ...rest }: PasswordProps) {
  const auto = useId();
  const fid = id ?? auto;
  const [visible, setVisible] = useState(false);
  return (
    <Field label={label} required={required} hint={hint} error={error} htmlFor={fid}>
      <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
        <input
          id={fid}
          className="input"
          type={visible ? 'text' : 'password'}
          aria-invalid={!!error}
          aria-required={required}
          style={{ paddingRight: 44, width: '100%' }}
          {...rest}
        />
        <button
          type="button"
          className="icon-btn"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
          aria-pressed={visible}
          title={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
          tabIndex={-1}
          style={{
            position: 'absolute',
            right: 4,
            border: 'none',
            background: 'transparent',
            cursor: 'pointer',
          }}
        >
          {visible ? '🙈' : '👁'}
        </button>
      </div>
    </Field>
  );
}

type SelectProps = SelectHTMLAttributes<HTMLSelectElement> &
  FieldWrap & { options: { value: string; label: string }[]; placeholder?: string };

export function Select({
  label,
  required,
  hint,
  error,
  id,
  options,
  placeholder,
  ...rest
}: SelectProps) {
  const auto = useId();
  const fid = id ?? auto;
  return (
    <Field label={label} required={required} hint={hint} error={error} htmlFor={fid}>
      <select id={fid} className="select" aria-invalid={!!error} {...rest}>
        {placeholder && <option value="">{placeholder}</option>}
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
    </Field>
  );
}

type TextareaProps = TextareaHTMLAttributes<HTMLTextAreaElement> & FieldWrap;

export function Textarea({ label, required, hint, error, id, ...rest }: TextareaProps) {
  const auto = useId();
  const fid = id ?? auto;
  return (
    <Field label={label} required={required} hint={hint} error={error} htmlFor={fid}>
      <textarea id={fid} className="textarea" aria-invalid={!!error} {...rest} />
    </Field>
  );
}

export interface ComboboxOption {
  value: string;
  label: string;
}

type ComboboxProps = FieldWrap & {
  options: ComboboxOption[];
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  emptyText?: string;
  disabled?: boolean;
  id?: string;
};

/**
 * Campo único que combina escritura y selección: al teclear filtra las opciones
 * y muestra un desplegable navegable con teclado (↑ ↓ Enter Esc) o clic.
 */
export function Combobox({
  label,
  required,
  hint,
  error,
  id,
  options,
  value,
  onChange,
  placeholder,
  emptyText = 'Sin coincidencias',
  disabled,
}: ComboboxProps) {
  const auto = useId();
  const fid = id ?? auto;
  const listId = `${fid}-list`;
  const wrapRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [active, setActive] = useState(0);

  const selected = useMemo(() => options.find((o) => o.value === value), [options, value]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return options;
    return options.filter((o) => o.label.toLowerCase().includes(q));
  }, [options, query]);

  // Texto mostrado: mientras el desplegable está abierto usa lo tecleado;
  // cerrado muestra la etiqueta seleccionada.
  const shownText = open ? query : selected?.label ?? '';

  useEffect(() => {
    if (!open) return;
    const onDocClick = (e: MouseEvent) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', onDocClick);
    return () => document.removeEventListener('mousedown', onDocClick);
  }, [open]);

  const abrir = () => {
    if (disabled) return;
    setQuery('');
    setActive(Math.max(0, filtered.findIndex((o) => o.value === value)));
    setOpen(true);
  };

  const elegir = (opt: ComboboxOption) => {
    onChange(opt.value);
    setOpen(false);
    setQuery('');
  };

  const onKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (disabled) return;
    if (!open && (e.key === 'ArrowDown' || e.key === 'Enter')) {
      abrir();
      return;
    }
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActive((i) => Math.min(i + 1, filtered.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActive((i) => Math.max(i - 1, 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      const opt = filtered[active];
      if (opt) elegir(opt);
    } else if (e.key === 'Escape') {
      setOpen(false);
      setQuery('');
    }
  };

  return (
    <Field label={label} required={required} hint={hint} error={error} htmlFor={fid}>
      <div ref={wrapRef} style={{ position: 'relative' }}>
        <input
          id={fid}
          className="input"
          role="combobox"
          aria-expanded={open}
          aria-controls={listId}
          aria-autocomplete="list"
          aria-invalid={!!error}
          aria-required={required}
          autoComplete="off"
          disabled={disabled}
          placeholder={placeholder}
          value={shownText}
          onFocus={abrir}
          onClick={abrir}
          onChange={(e) => {
            setQuery(e.target.value);
            setActive(0);
            if (!open) setOpen(true);
          }}
          onKeyDown={onKeyDown}
          style={{ paddingRight: 34, width: '100%' }}
        />
        <span
          aria-hidden
          style={{
            position: 'absolute',
            right: 12,
            top: '50%',
            transform: 'translateY(-50%)',
            pointerEvents: 'none',
            opacity: 0.6,
            fontSize: 12,
          }}
        >
          ▾
        </span>
        {open && (
          <ul
            id={listId}
            role="listbox"
            style={{
              position: 'absolute',
              zIndex: 30,
              top: 'calc(100% + 4px)',
              left: 0,
              right: 0,
              maxHeight: 240,
              overflowY: 'auto',
              margin: 0,
              padding: 4,
              listStyle: 'none',
              background: 'var(--surface)',
              border: '1px solid var(--border)',
              borderRadius: 10,
              boxShadow: 'var(--shadow-lg)',
            }}
          >
            {filtered.length === 0 ? (
              <li
                style={{
                  padding: '8px 10px',
                  color: 'var(--muted, #888)',
                  fontSize: 14,
                }}
              >
                {emptyText}
              </li>
            ) : (
              filtered.map((o, i) => {
                const isActive = i === active;
                const isSelected = o.value === value;
                return (
                  <li
                    key={o.value}
                    role="option"
                    aria-selected={isSelected}
                    onMouseEnter={() => setActive(i)}
                    onMouseDown={(e) => {
                      e.preventDefault();
                      elegir(o);
                    }}
                    style={{
                      padding: '8px 10px',
                      borderRadius: 8,
                      cursor: 'pointer',
                      fontSize: 14,
                      background: isActive ? 'var(--surface-2)' : 'transparent',
                      fontWeight: isSelected ? 600 : 400,
                    }}
                  >
                    {o.label}
                  </li>
                );
              })
            )}
          </ul>
        )}
      </div>
    </Field>
  );
}
