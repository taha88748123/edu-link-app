import { useTranslation } from 'react-i18next';

/**
 * Minimal spinner shown while a lazy-loaded page chunk is being fetched.
 * Kept dependency-light so it is bundled in the initial chunk (no UI lib imports).
 */
export function LoadingScreen() {
  const { t } = useTranslation();
  return (
    <div
      role="status"
      aria-live="polite"
      className="flex min-h-[60vh] flex-col items-center justify-center gap-3 text-muted-foreground"
    >
      <div
        aria-hidden
        className="h-8 w-8 animate-spin rounded-full border-2 border-current border-t-transparent"
      />
      <span className="text-sm">{t('common.loading')}</span>
    </div>
  );
}
