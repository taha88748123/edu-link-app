import * as React from 'react';

import { cn } from '@/lib/utils';

/**
 * Pulsing block used as a loading placeholder. Matches the shadcn skeleton
 * primitive but ships with a Tailwind-only animation (`animate-pulse`) — we
 * deliberately avoid adding `tailwindcss-animate` keyframes here.
 */
function Skeleton({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn('animate-pulse rounded-md bg-muted', className)}
      {...props}
    />
  );
}

export { Skeleton };
