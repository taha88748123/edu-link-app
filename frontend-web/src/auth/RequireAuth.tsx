import { useEffect, type ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';

import { authApi } from '@/api/auth.api';
import { useAuthStore } from './auth.store';

/**
 * Gate for authenticated routes. Behaviour:
 * 1. If the store has no access token → redirect to `/login?next=<current>`.
 * 2. Otherwise, fetch `GET /api/v1/auth/me` once and rehydrate `user` into the store.
 *    The axios 401 interceptor will handle stale-token refresh in the background.
 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const location = useLocation();
  const accessToken = useAuthStore((s) => s.accessToken);
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);

  const meQuery = useQuery({
    queryKey: ['me'],
    queryFn: () => authApi.me(),
    enabled: Boolean(accessToken) && !user,
    retry: false,
    staleTime: 5 * 60 * 1000,
  });

  useEffect(() => {
    if (meQuery.data) {
      setUser(meQuery.data);
    }
  }, [meQuery.data, setUser]);

  if (!accessToken) {
    const next = encodeURIComponent(location.pathname + location.search);
    return <Navigate to={`/login?next=${next}`} replace />;
  }

  // While we fetch the user for the first time, render a minimal placeholder.
  if (!user && meQuery.isFetching) {
    return (
      <div className="flex min-h-screen items-center justify-center text-muted-foreground">
        …
      </div>
    );
  }

  return <>{children}</>;
}
