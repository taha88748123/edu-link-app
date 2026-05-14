// Minimal toast store inspired by the shadcn/ui reference implementation.
// We keep a small subset (no `update`, no actions API) — enough for Vague A error notifications.
import * as React from 'react';

type ToastVariant = 'default' | 'destructive';

export interface ToastProps {
  id: string;
  title?: React.ReactNode;
  description?: React.ReactNode;
  variant?: ToastVariant;
  open: boolean;
  onOpenChange?(open: boolean): void;
}

type Toast = Omit<ToastProps, 'id' | 'open' | 'onOpenChange'>;

const TOAST_LIMIT = 4;
const TOAST_REMOVE_DELAY = 5000;

const listeners: Array<(state: State) => void> = [];

interface State {
  toasts: ToastProps[];
}

let memoryState: State = { toasts: [] };

function dispatch(next: (s: State) => State) {
  memoryState = next(memoryState);
  listeners.forEach((l) => l(memoryState));
}

let count = 0;
function genId() {
  count = (count + 1) % Number.MAX_SAFE_INTEGER;
  return count.toString();
}

const removeTimers = new Map<string, ReturnType<typeof setTimeout>>();

function scheduleRemoval(toastId: string) {
  if (removeTimers.has(toastId)) return;
  const t = setTimeout(() => {
    removeTimers.delete(toastId);
    dispatch((s) => ({ toasts: s.toasts.filter((x) => x.id !== toastId) }));
  }, TOAST_REMOVE_DELAY);
  removeTimers.set(toastId, t);
}

export function toast(t: Toast) {
  const id = genId();
  const onOpenChange = (open: boolean) => {
    if (!open) scheduleRemoval(id);
  };
  dispatch((s) => ({
    toasts: [
      { ...t, id, open: true, onOpenChange },
      ...s.toasts,
    ].slice(0, TOAST_LIMIT),
  }));
  scheduleRemoval(id);
  return { id };
}

export function dismiss(id?: string) {
  dispatch((s) => ({
    toasts: s.toasts.map((x) => (id && x.id !== id ? x : { ...x, open: false })),
  }));
}

export function useToast() {
  const [state, setState] = React.useState<State>(memoryState);
  React.useEffect(() => {
    listeners.push(setState);
    return () => {
      const idx = listeners.indexOf(setState);
      if (idx !== -1) listeners.splice(idx, 1);
    };
  }, []);
  return { ...state, toast, dismiss };
}
