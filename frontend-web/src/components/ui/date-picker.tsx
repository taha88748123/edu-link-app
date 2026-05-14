import { CalendarIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { Button } from './button';
import { Calendar } from './calendar';
import { Popover, PopoverContent, PopoverTrigger } from './popover';
import { cn } from '@/lib/utils';
import { formatDate } from '@/lib/format';

export interface DatePickerProps {
  value?: Date | null;
  onChange?(value: Date | undefined): void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
  fromDate?: Date;
  toDate?: Date;
}

/**
 * Composed Popover + Calendar — drop-in replacement for an `<input type="date">`
 * with locale-aware formatting.
 */
export function DatePicker({
  value,
  onChange,
  placeholder,
  disabled,
  className,
  fromDate,
  toDate,
}: DatePickerProps) {
  const { i18n } = useTranslation();
  const display = value ? formatDate(value, i18n.language) : '';
  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="outline"
          disabled={disabled}
          className={cn(
            'w-full justify-start text-start font-normal',
            !value && 'text-muted-foreground',
            className,
          )}
        >
          <CalendarIcon className="me-2 h-4 w-4" />
          {display || placeholder || '—'}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0" align="start">
        <Calendar
          mode="single"
          selected={value ?? undefined}
          onSelect={onChange}
          startMonth={fromDate}
          endMonth={toDate}
        />
      </PopoverContent>
    </Popover>
  );
}
