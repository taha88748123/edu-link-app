import { DayPicker, type DayPickerProps } from 'react-day-picker';

import { cn } from '@/lib/utils';

export type CalendarProps = DayPickerProps;

/**
 * Minimal react-day-picker wrapper styled with Tailwind to roughly match
 * shadcn/ui's calendar. RDP v10 uses CSS modules + `classNames` slots; we
 * override the bits we want themed and let the rest fall through.
 */
function Calendar({ className, classNames, ...props }: CalendarProps) {
  return (
    <DayPicker
      showOutsideDays
      className={cn('p-3', className)}
      classNames={{
        months: 'flex flex-col sm:flex-row gap-4',
        month: 'space-y-2',
        month_caption: 'flex justify-center pt-1 relative items-center',
        caption_label: 'text-sm font-medium',
        nav: 'flex items-center justify-between absolute inset-x-0 px-1',
        button_previous: cn(
          'inline-flex h-7 w-7 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-accent-foreground',
        ),
        button_next: cn(
          'inline-flex h-7 w-7 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-accent-foreground',
        ),
        month_grid: 'w-full border-collapse',
        weekdays: 'flex',
        weekday: 'text-muted-foreground rounded-md w-9 font-normal text-[0.8rem]',
        week: 'flex w-full mt-2',
        day: cn(
          'relative p-0 text-center text-sm h-9 w-9 focus-within:relative focus-within:z-20',
        ),
        day_button: cn(
          'inline-flex h-9 w-9 items-center justify-center rounded-md font-normal aria-selected:opacity-100 hover:bg-accent hover:text-accent-foreground',
        ),
        selected:
          'bg-primary text-primary-foreground hover:bg-primary hover:text-primary-foreground focus:bg-primary focus:text-primary-foreground',
        today: 'bg-accent text-accent-foreground',
        outside: 'text-muted-foreground opacity-50',
        disabled: 'text-muted-foreground opacity-50',
        ...classNames,
      }}
      {...props}
    />
  );
}
Calendar.displayName = 'Calendar';

export { Calendar };
