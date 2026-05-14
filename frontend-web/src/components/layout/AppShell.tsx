import type { ReactNode } from 'react';

import { Header } from './Header';
import { Sidebar } from './Sidebar';

/**
 * Authenticated layout: header on top, sidebar on the side (md+), main content in the center.
 */
export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col bg-background">
      <Header />
      <div className="flex flex-1">
        <Sidebar />
        <main className="flex-1 overflow-x-hidden">
          <div className="container py-6">{children}</div>
        </main>
      </div>
    </div>
  );
}
