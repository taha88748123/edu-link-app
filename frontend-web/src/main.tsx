import React from 'react';
import ReactDOM from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import './styles/globals.css';
import './i18n/config';
import { router } from './routes';
import { useAuthStore } from './auth/auth.store';
import { Toaster } from './components/ui/toaster';
import { installCspViolationReporter } from './lib/csp-violation-reporter';

// Surface any CSP block in the browser console — invaluable when tightening the
// policy. The reporter is a no-op in jsdom/node test envs.
installCspViolationReporter();

// Single TanStack Query client for the whole app.
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

// Hydrate tokens from localStorage as early as possible.
useAuthStore.getState().hydrate();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
      <Toaster />
    </QueryClientProvider>
  </React.StrictMode>,
);
