import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';

type ToastKind = 'success' | 'error';

interface ToastItem {
  id: number;
  kind: ToastKind;
  message: string;
}

interface ToastApi {
  success: (message: string, durationMs?: number) => void;
  error: (message: string, durationMs?: number) => void;
}

const ToastContext = createContext<ToastApi | null>(null);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const nextId = useRef(1);

  const push = useCallback((kind: ToastKind, message: string, durationMs: number) => {
    const id = nextId.current++;
    let added = false;
    setToasts((current) => {
      // An identical message that is still visible is not stacked a second time.
      if (current.some((t) => t.kind === kind && t.message === message)) return current;
      added = true;
      return [...current, { id, kind, message }];
    });
    window.setTimeout(() => {
      if (added) setToasts((current) => current.filter((t) => t.id !== id));
    }, durationMs);
  }, []);

  const api = useMemo<ToastApi>(
    () => ({
      success: (message, durationMs = 3500) => push('success', message, durationMs),
      error: (message, durationMs = 5000) => push('error', message, durationMs),
    }),
    [push],
  );

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toast-region" role="status" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast toast--${t.kind}`}>
            {t.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastApi {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within a ToastProvider');
  return ctx;
}
