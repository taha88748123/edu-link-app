import { Send } from 'lucide-react';
import { useCallback, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';

/**
 * Multi-line compose box. Enter submits, Shift+Enter inserts newline.
 *
 * `onTyping` is invoked at most once every {@link TYPING_THROTTLE_MS} ms so
 * the chat-service /typing endpoint isn't hammered on every keystroke.
 */
const TYPING_THROTTLE_MS = 3_000;

export function ComposeInput({
  disabled,
  onSend,
  onTyping,
}: {
  disabled?: boolean;
  onSend: (body: string) => Promise<unknown> | unknown;
  onTyping?: () => void;
}) {
  const { t } = useTranslation();
  const [value, setValue] = useState('');
  const [sending, setSending] = useState(false);
  const lastTypingAtRef = useRef<number>(0);

  const handleSend = useCallback(async () => {
    const trimmed = value.trim();
    if (!trimmed || sending || disabled) return;
    setSending(true);
    try {
      await onSend(trimmed);
      setValue('');
    } finally {
      setSending(false);
    }
  }, [value, sending, disabled, onSend]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      void handleSend();
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setValue(e.target.value);
    if (onTyping) {
      const now = Date.now();
      if (now - lastTypingAtRef.current > TYPING_THROTTLE_MS) {
        lastTypingAtRef.current = now;
        onTyping();
      }
    }
  };

  return (
    <div className="flex items-end gap-2 border-t bg-background p-3">
      <Textarea
        value={value}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        placeholder={t('chat.compose_placeholder')}
        disabled={disabled || sending}
        rows={2}
        className="resize-none"
        aria-label={t('chat.compose_placeholder')}
      />
      <Button
        type="button"
        onClick={() => void handleSend()}
        disabled={disabled || sending || value.trim().length === 0}
        aria-label={t('chat.send')}
      >
        <Send className="h-4 w-4" />
        <span className="ms-1 hidden sm:inline">{t('chat.send')}</span>
      </Button>
    </div>
  );
}
