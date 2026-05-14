import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';

import type { SlotView } from '@/api/calendar.api';
import type { SlotStatus } from '@/api/enums';
import { cn } from '@/lib/utils';

export interface WeekGridProps {
  /** Monday of the displayed week (00:00 local). */
  weekStart: Date;
  slots: SlotView[];
  onSlotClick?(slot: SlotView): void;
}

const HOURS = Array.from({ length: 14 }, (_, i) => i + 7); // 7am..8pm

const STATUS_CLASS: Record<SlotStatus, string> = {
  AVAILABLE: 'bg-emerald-100 text-emerald-900 hover:bg-emerald-200 dark:bg-emerald-900/40 dark:text-emerald-200',
  BOOKED: 'bg-sky-100 text-sky-900 hover:bg-sky-200 dark:bg-sky-900/40 dark:text-sky-200',
  BLOCKED: 'bg-zinc-200 text-zinc-800 hover:bg-zinc-300 dark:bg-zinc-700 dark:text-zinc-200',
  RESERVED_PENDING: 'bg-amber-100 text-amber-900 hover:bg-amber-200 dark:bg-amber-900/40 dark:text-amber-200',
};

function fmtHour(h: number): string {
  return `${h.toString().padStart(2, '0')}:00`;
}

function startOfDay(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}

/**
 * Weekly grid (7 day columns x 14 hour rows). Each slot is rendered in its
 * day column at a top offset proportional to its start hour. We deliberately
 * keep the layout CSS-only — no virtualisation — because a prof rarely has
 * more than a few dozen slots per week.
 */
export function WeekGrid({ weekStart, slots, onSlotClick }: WeekGridProps) {
  const { t } = useTranslation();
  const days = useMemo(() => {
    const monday = startOfDay(weekStart);
    return Array.from({ length: 7 }, (_, i) => {
      const d = new Date(monday);
      d.setDate(monday.getDate() + i);
      return d;
    });
  }, [weekStart]);

  return (
    <div className="overflow-x-auto rounded-md border">
      <div className="grid min-w-[720px] grid-cols-[60px_repeat(7,minmax(0,1fr))] text-xs">
        {/* header row */}
        <div className="border-b bg-muted/40 p-2" />
        {days.map((d, i) => (
          <div key={d.toISOString()} className="border-b bg-muted/40 p-2 text-center font-medium">
            <div>{t(`weekday_short.${i}`)}</div>
            <div className="text-muted-foreground">{d.getDate()}</div>
          </div>
        ))}

        {/* body rows */}
        {HOURS.map((h) => (
          <RowGroup key={h} hour={h} days={days} slots={slots} onSlotClick={onSlotClick} />
        ))}
      </div>
    </div>
  );
}

function RowGroup({
  hour,
  days,
  slots,
  onSlotClick,
}: {
  hour: number;
  days: Date[];
  slots: SlotView[];
  onSlotClick?(slot: SlotView): void;
}) {
  return (
    <>
      <div className="border-t border-e px-2 py-1 text-muted-foreground">{fmtHour(hour)}</div>
      {days.map((d) => {
        const cellStart = new Date(d);
        cellStart.setHours(hour, 0, 0, 0);
        const cellEnd = new Date(d);
        cellEnd.setHours(hour + 1, 0, 0, 0);
        const cellSlots = slots.filter((s) => {
          const sStart = new Date(s.startAt);
          return sStart >= cellStart && sStart < cellEnd;
        });
        return (
          <div
            key={`${d.toISOString()}-${hour}`}
            className="relative min-h-[44px] border-s border-t"
          >
            {cellSlots.map((s) => (
              <SlotChip key={s.id} slot={s} onClick={() => onSlotClick?.(s)} />
            ))}
          </div>
        );
      })}
    </>
  );
}

function SlotChip({ slot, onClick }: { slot: SlotView; onClick(): void }) {
  const start = new Date(slot.startAt);
  const end = new Date(slot.endAt);
  const label = `${start.getHours().toString().padStart(2, '0')}:${start
    .getMinutes()
    .toString()
    .padStart(2, '0')}–${end.getHours().toString().padStart(2, '0')}:${end
    .getMinutes()
    .toString()
    .padStart(2, '0')}`;
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'm-1 block w-[calc(100%-0.5rem)] rounded px-1 py-0.5 text-[10px] transition',
        STATUS_CLASS[slot.status],
      )}
    >
      {label}
    </button>
  );
}
