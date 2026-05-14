import i18n from 'i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import { initReactI18next } from 'react-i18next';

import fr from './locales/fr.json';
import ar from './locales/ar.json';
import en from './locales/en.json';
import { STORAGE_KEYS } from '@/auth/auth.types';

export const SUPPORTED_LOCALES = ['fr', 'ar', 'en'] as const;
export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number];

void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    fallbackLng: 'fr',
    supportedLngs: SUPPORTED_LOCALES as unknown as string[],
    nonExplicitSupportedLngs: true,
    interpolation: { escapeValue: false },
    detection: {
      order: ['querystring', 'localStorage', 'navigator'],
      lookupQuerystring: 'lng',
      lookupLocalStorage: STORAGE_KEYS.locale,
      caches: ['localStorage'],
    },
    resources: {
      fr: { translation: fr },
      ar: { translation: ar },
      en: { translation: en },
    },
  });

function applyDirAndLang(lng: string) {
  if (typeof document === 'undefined') return;
  const norm = (SUPPORTED_LOCALES as readonly string[]).includes(lng) ? lng : 'fr';
  document.documentElement.lang = norm;
  document.documentElement.dir = norm === 'ar' ? 'rtl' : 'ltr';
}

i18n.on('languageChanged', applyDirAndLang);
applyDirAndLang(i18n.language || 'fr');

export default i18n;
