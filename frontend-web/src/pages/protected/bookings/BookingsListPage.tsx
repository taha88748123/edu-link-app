import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useMemo } from 'react';

import type { BookingView } from '@/api/bookings.api';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import { BookingStatusBadge } from '@/components/booking/BookingStatusBadge';
import { useMyBookings } from '@/hooks/queries/useBookings';
import { useAuthStore } from '@/auth/auth.store';
import { bookingTab, type BookingTab } from '@/lib/booking';
import { formatDateTime, formatMoneyMad } from '@/lib/format';

/**
 * Shared bookings list. Same backend endpoint (`/api/v1/bookings/me`) is used
 * for STUDENT and PROF; the gateway scopes the result by role. We just adjust
 * column labels.
 */
export default function BookingsListPage({ basePath }: { basePath: '/student' | '/prof' }) {
  const { t, i18n } = useTranslation();
  const role = useAuthStore((s) => s.user?.role);
  const isProfView = role === 'PROF';

  const bookingsQuery = useMyBookings({ size: 100 });

  const grouped = useMemo(() => {
    const result: Record<BookingTab, BookingView[]> = {
      upcoming: [],
      past: [],
      cancelled: [],
    };
    for (const b of bookingsQuery.data ?? []) {
      result[bookingTab(b.status)].push(b);
    }
    return result;
  }, [bookingsQuery.data]);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">{t('booking.list_title')}</h1>

      {bookingsQuery.isLoading ? (
        <Skeleton className="h-64 w-full" />
      ) : (
        <Tabs defaultValue="upcoming">
          <TabsList>
            <TabsTrigger value="upcoming">{t('booking.tabs.upcoming')}</TabsTrigger>
            <TabsTrigger value="past">{t('booking.tabs.past')}</TabsTrigger>
            <TabsTrigger value="cancelled">{t('booking.tabs.cancelled')}</TabsTrigger>
          </TabsList>

          {(['upcoming', 'past', 'cancelled'] as BookingTab[]).map((tab) => (
            <TabsContent key={tab} value={tab}>
              {grouped[tab].length === 0 ? (
                <Card>
                  <CardContent className="py-10 text-center text-sm text-muted-foreground">
                    {t(`booking.empty_${tab}`)}
                  </CardContent>
                </Card>
              ) : (
                <Card>
                  <CardContent className="p-0">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>{t('booking.columns.subject')}</TableHead>
                          <TableHead>
                            {isProfView ? t('booking.columns.student') : t('booking.columns.with')}
                          </TableHead>
                          <TableHead>{t('booking.columns.when')}</TableHead>
                          <TableHead>{t('booking.columns.amount')}</TableHead>
                          <TableHead>{t('booking.columns.status')}</TableHead>
                          <TableHead className="text-end">{t('common.actions')}</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {grouped[tab].map((b) => (
                          <TableRow key={b.id}>
                            <TableCell className="font-medium">
                              {b.subjectCode}{' '}
                              <span className="text-xs text-muted-foreground">
                                · {t(`level.${b.level}`)}
                              </span>
                            </TableCell>
                            <TableCell className="text-xs text-muted-foreground">
                              {isProfView
                                ? b.studentId.slice(0, 8) + '…'
                                : b.profId.slice(0, 8) + '…'}
                            </TableCell>
                            <TableCell>{formatDateTime(b.scheduledStart, i18n.language)}</TableCell>
                            <TableCell>{formatMoneyMad(b.totalAmountMinor, i18n.language)}</TableCell>
                            <TableCell>
                              <BookingStatusBadge status={b.status} />
                            </TableCell>
                            <TableCell className="text-end">
                              <Button asChild size="sm" variant="outline">
                                <Link to={`${basePath}/bookings/${b.id}`}>{t('booking.view')}</Link>
                              </Button>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </CardContent>
                </Card>
              )}
            </TabsContent>
          ))}
        </Tabs>
      )}
    </div>
  );
}
