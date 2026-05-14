import { useTranslation } from 'react-i18next';

import { ApiClientError } from '@/api/client';

/**
 * Map an unknown error (typically thrown by a TanStack mutation) to a localized
 * human-readable message. Order of resolution:
 *   1. If it's an `ApiClientError`, try the `errors.<CODE>` i18n key.
 *   2. Then the backend-provided `message` (already localized by `shared-i18n`).
 *   3. Finally fall back to `errors.generic`.
 */
export function useApiError() {
  const { t } = useTranslation();
  return (err: unknown): string => {
    if (err instanceof ApiClientError) {
      const code = err.code;
      if (code) {
        const key = `errors.${code}`;
        const translated = t(key, { defaultValue: '' });
        if (translated && translated !== key) return translated;
      }
      if (err.message) return err.message;
    }
    if (err instanceof Error && err.message) {
      return err.message;
    }
    return t('errors.generic');
  };
}
