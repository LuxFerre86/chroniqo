import { useId, useState } from 'react';
import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react';
import { Icon } from './Icon';

interface FieldShellProps {
  id: string;
  label?: string;
  required?: boolean;
  helperText?: string;
  error?: string | null;
  readOnly?: boolean;
  className?: string;
  children: ReactNode;
}

function FieldShell({ id, label, required, helperText, error, readOnly, className, children }: FieldShellProps) {
  const classes = ['field', error ? 'field--invalid' : '', readOnly ? 'field--readonly' : '', className ?? '']
    .filter(Boolean)
    .join(' ');
  return (
    <div className={classes}>
      {label && (
        <label className="field__label" htmlFor={id}>
          {label}
          {required && (
            <span className="field__required" aria-hidden="true">
              •
            </span>
          )}
        </label>
      )}
      {children}
      {error ? (
        <div className="field__error" id={`${id}-error`} role="alert">
          {error}
        </div>
      ) : (
        helperText && (
          <div className="field__helper" id={`${id}-helper`}>
            {helperText}
          </div>
        )
      )}
    </div>
  );
}

function describedBy(id: string, error?: string | null, helperText?: string): string | undefined {
  if (error) return `${id}-error`;
  if (helperText) return `${id}-helper`;
  return undefined;
}

interface BaseFieldProps {
  label?: string;
  helperText?: string;
  error?: string | null;
}

export interface TextFieldProps extends BaseFieldProps, Omit<InputHTMLAttributes<HTMLInputElement>, 'className'> {
  className?: string;
}

export function TextField({ label, helperText, error, className, required, readOnly, id, ...rest }: TextFieldProps) {
  const autoId = useId();
  const fieldId = id ?? autoId;
  return (
    <FieldShell
      id={fieldId}
      label={label}
      required={required}
      helperText={helperText}
      error={error}
      readOnly={readOnly}
      className={className}
    >
      <div className="field__control">
        <input
          id={fieldId}
          className="field__input"
          required={required}
          readOnly={readOnly}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy(fieldId, error, helperText)}
          {...rest}
        />
      </div>
    </FieldShell>
  );
}

export function PasswordField({ label, helperText, error, className, required, id, ...rest }: TextFieldProps) {
  const autoId = useId();
  const fieldId = id ?? autoId;
  const [visible, setVisible] = useState(false);
  return (
    <FieldShell id={fieldId} label={label} required={required} helperText={helperText} error={error} className={className}>
      <div className="field__control">
        <input
          id={fieldId}
          className="field__input"
          type={visible ? 'text' : 'password'}
          required={required}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy(fieldId, error, helperText)}
          {...rest}
        />
        <button
          type="button"
          className="field__adornment"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? 'Hide password' : 'Show password'}
          aria-pressed={visible}
        >
          <Icon name={visible ? 'eye-off' : 'eye'} />
        </button>
      </div>
    </FieldShell>
  );
}

export interface TextAreaFieldProps extends BaseFieldProps, Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'className'> {
  className?: string;
}

export function TextAreaField({ label, helperText, error, className, required, id, ...rest }: TextAreaFieldProps) {
  const autoId = useId();
  const fieldId = id ?? autoId;
  return (
    <FieldShell id={fieldId} label={label} required={required} helperText={helperText} error={error} className={className}>
      <div className="field__control">
        <textarea
          id={fieldId}
          className="field__input"
          required={required}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy(fieldId, error, helperText)}
          {...rest}
        />
      </div>
    </FieldShell>
  );
}

export interface SelectOption {
  value: string;
  label: string;
}

export interface SelectFieldProps extends BaseFieldProps, Omit<SelectHTMLAttributes<HTMLSelectElement>, 'className' | 'children'> {
  options: SelectOption[];
  /** Label of the empty option; when omitted no empty option is rendered. */
  placeholder?: string;
  className?: string;
}

export function SelectField({ label, helperText, error, className, required, options, placeholder, id, ...rest }: SelectFieldProps) {
  const autoId = useId();
  const fieldId = id ?? autoId;
  return (
    <FieldShell id={fieldId} label={label} required={required} helperText={helperText} error={error} className={className}>
      <div className="field__control">
        <select
          id={fieldId}
          className="field__input"
          required={required}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy(fieldId, error, helperText)}
          {...rest}
        >
          {placeholder !== undefined && <option value="">{placeholder}</option>}
          {options.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      </div>
    </FieldShell>
  );
}

export interface NumberFieldProps extends BaseFieldProps {
  value: number | null;
  onChange: (value: number | null) => void;
  min?: number;
  max?: number;
  step?: number;
  label?: string;
  required?: boolean;
  id?: string;
  className?: string;
  disabled?: boolean;
}

/** Integer input with minus/plus step buttons (matches the former Vaadin IntegerField). */
export function NumberField({ label, helperText, error, className, required, value, onChange, min, max, step = 1, id, disabled }: NumberFieldProps) {
  const autoId = useId();
  const fieldId = id ?? autoId;
  const clamp = (n: number) => Math.min(max ?? Infinity, Math.max(min ?? -Infinity, n));
  const bump = (delta: number) => onChange(clamp((value ?? 0) + delta));
  return (
    <FieldShell id={fieldId} label={label} required={required} helperText={helperText} error={error} className={className}>
      <div className="stepper">
        <button
          type="button"
          className="stepper__button"
          aria-label="Decrease"
          disabled={disabled || (min !== undefined && value !== null && value <= min)}
          onClick={() => bump(-step)}
        >
          −
        </button>
        <input
          id={fieldId}
          className="field__input"
          type="number"
          inputMode="numeric"
          value={value ?? ''}
          min={min}
          max={max}
          step={step}
          required={required}
          disabled={disabled}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy(fieldId, error, helperText)}
          onChange={(e) => onChange(e.target.value === '' ? null : Number(e.target.value))}
        />
        <button
          type="button"
          className="stepper__button"
          aria-label="Increase"
          disabled={disabled || (max !== undefined && value !== null && value >= max)}
          onClick={() => bump(step)}
        >
          +
        </button>
      </div>
    </FieldShell>
  );
}

export interface CheckboxProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'className'> {
  label: string;
}

export function Checkbox({ label, ...rest }: CheckboxProps) {
  return (
    <label className="checkbox">
      <input type="checkbox" {...rest} />
      <span>{label}</span>
    </label>
  );
}
