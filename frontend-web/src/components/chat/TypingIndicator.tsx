import { useTranslation } from 'react-i18next';

export function TypingIndicator({ peerInitials, show }: { peerInitials: string; show: boolean }) {
  const { t } = useTranslation();
  if (!show) return null;
  return (
    <div className="flex items-center gap-2 px-3 py-1 text-xs text-muted-foreground">
      <span className="inline-flex gap-0.5">
        <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-muted-foreground/50" />
        <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-muted-foreground/50 [animation-delay:150ms]" />
        <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-muted-foreground/50 [animation-delay:300ms]" />
      </span>
      <span>{t('chat.typing', { name: peerInitials })}</span>
    </div>
  );
}
