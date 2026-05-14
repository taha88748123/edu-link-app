import { Link } from 'react-router-dom';
import type { LucideIcon } from 'lucide-react';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';

export function AdminKpiCard({
  icon: Icon,
  title,
  value,
  to,
  loading,
}: {
  icon?: LucideIcon;
  title: string;
  value: number | string;
  to?: string;
  loading?: boolean;
}) {
  const content = (
    <Card className="h-full transition-colors hover:bg-accent/40">
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">{title}</CardTitle>
        {Icon ? <Icon className="h-4 w-4 text-muted-foreground" /> : null}
      </CardHeader>
      <CardContent>
        {loading ? <Skeleton className="h-8 w-12" /> : <div className="text-2xl font-semibold">{value}</div>}
      </CardContent>
    </Card>
  );

  if (to) {
    return (
      <Link to={to} className="block">
        {content}
      </Link>
    );
  }
  return content;
}
