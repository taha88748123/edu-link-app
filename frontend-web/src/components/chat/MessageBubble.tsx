import { ShieldAlert } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import type { MessageView } from '@/api/chat.api';
import { cn } from '@/lib/utils';
import { formatDateTime } from '@/lib/format';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';

/**
 * Render a single chat message bubble.
 *
 * Anti-bypass invariant: when `moderation.status` is anything other than
 * `CLEAN`, we render a small shield icon next to the bubble with an
 * explanatory tooltip — the backend already strips contact info before we
 * receive the message body.
 */
export function MessageBubble({
  message,
  isOwn,
  locale,
}: {
  message: MessageView;
  isOwn: boolean;
  locale: string;
}) {
  const { t } = useTranslation();
  const status = message.moderation?.status;
  const moderated = status !== null && status !== undefined && status !== 'CLEAN';

  return (
    <div
      className={cn(
        'flex items-end gap-2',
        isOwn ? 'justify-end' : 'justify-start',
      )}
      // Existing test (MessageBubble.test.tsx) queries `message-bubble`; the
      // E2E suite locates messages by id via `[data-message-id]` instead.
      data-testid="message-bubble"
      data-message-id={message.id}
      data-moderation={status ?? 'CLEAN'}
    >
      {moderated ? (
        <TooltipProvider delayDuration={150}>
          <Tooltip>
            <TooltipTrigger asChild>
              <span
                aria-label={t('chat.message.moderation_hint')}
                className="text-amber-600 dark:text-amber-400"
              >
                <ShieldAlert className="h-4 w-4" />
              </span>
            </TooltipTrigger>
            <TooltipContent>{t('chat.message.moderation_hint')}</TooltipContent>
          </Tooltip>
        </TooltipProvider>
      ) : null}

      <div
        className={cn(
          'max-w-[78%] rounded-2xl px-3 py-2 text-sm shadow-sm',
          isOwn
            ? 'bg-primary text-primary-foreground'
            : 'bg-muted text-foreground',
        )}
      >
        {message.body ? (
          <p className="whitespace-pre-wrap break-words">{message.body}</p>
        ) : (
          <p className="italic opacity-70">{t('chat.message.no_body')}</p>
        )}
        <div
          className={cn(
            'mt-1 text-[10px] opacity-70',
            isOwn ? 'text-end' : 'text-start',
          )}
        >
          {formatDateTime(message.createdAt, locale)}
        </div>
      </div>
    </div>
  );
}
