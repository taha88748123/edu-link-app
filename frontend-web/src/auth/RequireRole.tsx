import { Navigate } from 'react-router-dom';
import type { ReactNode } from 'react';

import type { Role } from '@/api/types';
import { useAuthStore } from './auth.store';

/**
 * Renders `children` only if the authenticated user's role is in `allow`,
 * otherwise redirects to `/forbidden`.
 *
 * Assumes it is nested under {@link RequireAuth}.
 */
export function RequireRole({ allow, children }: { allow: Role[]; children: ReactNode }) {
  const user = useAuthStore((s) => s.user);

  if (!user) {
    // RequireAuth not yet ready — render nothing rather than flashing /forbidden.
    return null;
  }

  if (!allow.includes(user.role)) {
    return <Navigate to="/forbidden" replace />;
  }

  return <>{children}</>;
}
