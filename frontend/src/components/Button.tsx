import type { ButtonHTMLAttributes, ReactNode } from 'react';

export type ButtonVariant = 'primary' | 'secondary' | 'tertiary' | 'outlined' | 'error' | 'link';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  block?: boolean;
  small?: boolean;
  icon?: boolean;
  loading?: boolean;
  children?: ReactNode;
}

export function Button({
  variant = 'secondary',
  block,
  small,
  icon,
  loading,
  className,
  children,
  disabled,
  type = 'button',
  ...rest
}: ButtonProps) {
  const classes = [
    'btn',
    variant !== 'secondary' ? `btn--${variant}` : '',
    block ? 'btn--block' : '',
    small ? 'btn--small' : '',
    icon ? 'btn--icon' : '',
    className ?? '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <button type={type} className={classes} disabled={disabled || loading} aria-busy={loading || undefined} {...rest}>
      {loading && <span className="spinner" aria-hidden="true" />}
      {children}
    </button>
  );
}
