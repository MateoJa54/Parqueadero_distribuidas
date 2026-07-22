import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, useTheme } from './ThemeProvider';

function Probe() {
  const { theme, toggle } = useTheme();
  return (
    <div>
      <span data-testid="theme">{theme}</span>
      <button type="button" onClick={toggle}>toggle</button>
    </div>
  );
}

describe('ThemeProvider', () => {
  beforeEach(() => localStorage.clear());

  it('usa tema guardado', () => {
    localStorage.setItem('pq_theme', 'dark');
    render(<ThemeProvider><Probe /></ThemeProvider>);
    expect(screen.getByTestId('theme')).toHaveTextContent('dark');
  });

  it('toggle alterna y persiste', async () => {
    localStorage.setItem('pq_theme', 'light');
    render(<ThemeProvider><Probe /></ThemeProvider>);
    await userEvent.click(screen.getByText('toggle'));
    expect(screen.getByTestId('theme')).toHaveTextContent('dark');
    expect(localStorage.getItem('pq_theme')).toBe('dark');
    expect(document.documentElement.dataset.theme).toBe('dark');
  });

  it('useTheme lanza fuera del provider', () => {
    expect(() => render(<Probe />)).toThrow('useTheme dentro de <ThemeProvider>');
  });
});
