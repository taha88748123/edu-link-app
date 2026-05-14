import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

import type { ConversationStatus, ConversationView } from '@/api/chat.api';
import { Badge, type BadgeVariant } from '@/components/ui/badge';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { cn } from '@/lib/utils';
import { formatDateTime } from '@/lib/format';

const STATUS_VARIANT: Record<ConversationStatus, BadgeVariant> = {
  PRE_ACCEPT: 'warning',
  POST_ACCEPT: 'success',
  CLOSED: 'outline',
};

export function ConversationListItem({
  conversation,
  myUserId,
  locale,
}: {
  conversation: ConversationView;
  myUserId: string | null;
  locale: string;
}) {
  const { t } = useTranslation();
  const peerId =
    myUserId && myUserId === conversation.profId
      ? conversation.studentId
      : conversation.profId;
  const peerInitials = peerId.slice(0, 2).toUpperCase();

  return (
    <Link
      to={`/chat/${conversation.id}`}
      className={cn(
        'flex items-center gap-3 rounded-md border bg-card p-3 transition-colors hover:bg-accent',
      )}
    >
      <Avatar className="h-10 w-10">
        <AvatarFallback>{peerInitials}</AvatarFallback>
      </Avatar>
      <div className="min-w-0 flex-1">
        <div className="flex items-center justify-between gap-2">
          <span className="truncate text-sm font-medium" data-testid="conv-peer">
            {peerId.slice(0, 8)}…
          </span>
          {conversation.lastMessageAt ? (
            <span className="shrink-0 text-[10px] text-muted-foreground">
              {formatDateTime(conversation.lastMessageAt, locale)}
            </span>
          ) : null}
        </div>
        <div className="mt-1 flex items-center gap-2">
          <Badge variant={STATUS_VARIANT[conversation.status]}>
            {t(`chat.status.${conversation.status}`)}
          </Badge>
          {conversation.unreadCount > 0 ? (
            <span
              className="rounded-full bg-primary px-2 py-0.5 text-[10px] font-semibold text-primary-foreground"
              aria-label={t('chat.unread_count', { count: conversation.unreadCount })}
            >
              {conversation.unreadCount}
            </span>
          ) : null}
        </div>
      </div>
    </Link>
  );
}
