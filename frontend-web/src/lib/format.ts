import type { Locale } from '@/api/types';

const localeMap: Record<Locale, string> = {
  fr: 'fr-MA',
  ar: 'ar-MA',
  en: 'en-MA',
};

function resolveLocale(locale?: string): string {
  if (!locale) return 'fr-MA';
  return localeMap[locale as Locale] ?? locale;
}

/**
 * Format an amount stored as minor units (centimes) as MAD.
 * Example: `formatMoneyMad(15000)` → "150,00 MAD" with `fr` locale.
 */
export function formatMoneyMad(amountMinor: number, locale: string = 'fr'): string {
  const major = amountMinor / 100;
  return new Intl.NumberFormat(resolveLocale(locale), {
    style: 'currency',
    currency: 'MAD',
    currencyDisplay: 'code',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(major);
}

/** Format a Date (or ISO string) using the active locale (date only). */
export function formatDate(value: string | Date, locale: string = 'fr'): string {
  const d = typeof value === 'string' ? new Date(value) : value;
  if (Number.isNaN(d.getTime())) return '';
  return new Intl.DateTimeFormat(resolveLocale(locale), {
    dateStyle: 'medium',
  }).format(d);
}

/** Format a Date (or ISO string) with date + time. */
export function formatDateTime(value: string | Date, locale: string = 'fr'): string {
  const d = typeof value === 'string' ? new Date(value) : value;
  if (Number.isNaN(d.getTime())) return '';
  return new Intl.DateTimeFormat(resolveLocale(locale), {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(d);
}

/** "JD" from "Jane Doe" — used in the avatar fallback. */
export function initials(name: string | null | undefined): string {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  if (parts.length === 1) return parts[0][0].toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}
