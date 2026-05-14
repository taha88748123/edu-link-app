import { Link } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

import { LocaleSwitcher } from './LocaleSwitcher';
import { ThemeToggle } from './ThemeToggle';

/**
 * Centered narrow shell used by all public auth pages and the landing page.
 * Hosts the locale switcher + theme toggle in the top-right corner.
 */
export function PublicShell({ children }: { children: ReactNode }) {
  const { t } = useTranslation();
  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="flex items-center justify-between px-4 py-3 sm:px-8">
        <Link to="/" className="flex items-center gap-2">
          <div className="flex h-9 w-9 items-center justify-center rounded-md bg-primary text-primary-foreground font-bold">
            N
          </div>
          <div className="flex flex-col leading-tight">
            <span className="font-semibold">{t('app.name')}</span>
            <span className="text-xs text-muted-foreground">{t('app.tagline')}</span>
          </div>
        </Link>
        <div className="flex items-center gap-1">
          <LocaleSwitcher />
          <ThemeToggle />
        </div>
      </header>
      <main className="flex flex-1 items-center justify-center px-4 py-6">
        <div className="w-full max-w-md">{children}</div>
      </main>
      <footer className="px-4 py-4 text-center text-xs text-muted-foreground">
        © {new Date().getFullYear()} {t('app.name')}
      </footer>
    </div>
  );
}
