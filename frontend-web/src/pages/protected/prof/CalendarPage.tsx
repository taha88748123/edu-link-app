import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, ChevronRight, Plus, Trash2, Wand2 } from 'lucide-react';

import type {
  AvailabilityRuleView,
  CreateAvailabilityRuleRequest,
  SlotView,
} from '@/api/calendar.api';
import type { Recurrence } from '@/api/enums';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
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
import { toast } from '@/components/ui/use-toast';
import { WeekGrid } from '@/components/calendar/WeekGrid';
import {
  useBlockSlot,
  useCreateRule,
  useDeleteRule,
  useGenerateSlots,
  useMyAvailabilityRules,
  useMySlots,
  useUnblockSlot,
} from '@/hooks/queries/useCalendar';
import { useApiError } from '@/hooks/useApiError';

function mondayOf(d: Date): Date {
  const x = new Date(d);
  const day = x.getDay(); // 0=Sun..6=Sat
  const diff = (day + 6) % 7; // days to subtract to reach Monday
  x.setDate(x.getDate() - diff);
  x.setHours(0, 0, 0, 0);
  return x;
}

export default function CalendarPage() {
  const { t } = useTranslation();
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">{t('calendar.title')}</h1>
      <Tabs defaultValue="slots">
        <TabsList>
          <TabsTrigger value="slots">{t('calendar.tabs.slots')}</TabsTrigger>
          <TabsTrigger value="rules">{t('calendar.tabs.rules')}</TabsTrigger>
        </TabsList>
        <TabsContent value="slots">
          <SlotsTab />
        </TabsContent>
        <TabsContent value="rules">
          <RulesTab />
        </TabsContent>
      </Tabs>
    </div>
  );
}

// -------------------------- SLOTS TAB --------------------------
function SlotsTab() {
  const { t } = useTranslation();
  const apiError = useApiError();
  const [weekStart, setWeekStart] = useState(() => mondayOf(new Date()));
  const weekEnd = useMemo(() => {
    const e = new Date(weekStart);
    e.setDate(e.getDate() + 7);
    return e;
  }, [weekStart]);

  const slotsQuery = useMySlots({
    from: weekStart.toISOString(),
    to: weekEnd.toISOString(),
    size: 200,
  });
  const generateMut = useGenerateSlots();

  const [selected, setSelected] = useState<SlotView | null>(null);

  async function onGenerate() {
    try {
      const r = await generateMut.mutateAsync();
      toast({ description: t('calendar.slots.generated', { created: r.created, skipped: r.skipped }) });
    } catch (err) {
      toast({ variant: 'destructive', description: apiError(err) });
    }
  }

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0">
        <div className="flex items-center gap-2">
          <Button size="icon" variant="outline" onClick={() => setWeekStart(addDays(weekStart, -7))}>
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <Button size="sm" variant="outline" onClick={() => setWeekStart(mondayOf(new Date()))}>
            {t('calendar.slots.today')}
          </Button>
          <Button size="icon" variant="outline" onClick={() => setWeekStart(addDays(weekStart, 7))}>
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
        <Button onClick={onGenerate} disabled={generateMut.isPending} className="gap-2">
          <Wand2 className="h-4 w-4" />
          {t('calendar.slots.generate')}
        </Button>
      </CardHeader>
      <CardContent>
        {slotsQuery.isLoading ? (
          <Skeleton className="h-64 w-full" />
        ) : (
          <WeekGrid
            weekStart={weekStart}
            slots={slotsQuery.data ?? []}
            onSlotClick={(s) => setSelected(s)}
          />
        )}
        <SlotActionsDialog
          slot={selected}
          onClose={() => setSelected(null)}
        />
      </CardContent>
    </Card>
  );
}

function addDays(d: Date, n: number): Date {
  const x = new Date(d);
  x.setDate(x.getDate() + n);
  return x;
}

function SlotActionsDialog({ slot, onClose }: { slot: SlotView | null; onClose(): void }) {
  const { t } = useTranslation();
  const apiError = useApiError();
  const blockMut = useBlockSlot();
  const unblockMut = useUnblockSlot();
  if (!slot) return null;

  async function block() {
    if (!slot) return;
    try {
      await blockMut.mutateAsync(slot.id);
      onClose();
    } catch (err) {
      toast({ variant: 'destructive', description: apiError(err) });
    }
  }
  async function unblock() {
    if (!slot) return;
    try {
      await unblockMut.mutateAsync(slot.id);
      onClose();
    } catch (err) {
      toast({ variant: 'destructive', description: apiError(err) });
    }
  }

  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('calendar.slots.block_dialog_title')}</DialogTitle>
        </DialogHeader>
        <div className="space-y-2 text-sm">
          <Row label={t('common.date')} value={new Date(slot.startAt).toLocaleString()} />
          <Row label={t('common.status')} value={t(`calendar.status.${slot.status}`)} />
        </div>
        <DialogFooter>
          {slot.status === 'AVAILABLE' && (
            <Button variant="destructive" onClick={block} disabled={blockMut.isPending}>
              {t('calendar.slots.block')}
            </Button>
          )}
          {slot.status === 'BLOCKED' && (
            <Button onClick={unblock} disabled={unblockMut.isPending}>
              {t('calendar.slots.unblock')}
            </Button>
          )}
          <Button variant="outline" onClick={onClose}>
            {t('common.close')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-baseline justify-between">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium">{value}</span>
    </div>
  );
}

// -------------------------- RULES TAB --------------------------
function RulesTab() {
  const { t } = useTranslation();
  const rulesQuery = useMyAvailabilityRules();
  const [open, setOpen] = useState(false);
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0">
        <div>
          <CardTitle className="text-lg">{t('calendar.rules.title')}</CardTitle>
          <p className="text-xs text-muted-foreground">{t('calendar.rules.subtitle')}</p>
        </div>
        <Button onClick={() => setOpen(true)} className="gap-2">
          <Plus className="h-4 w-4" />
          {t('calendar.rules.add')}
        </Button>
      </CardHeader>
      <CardContent>
        {rulesQuery.isLoading ? (
          <Skeleton className="h-32 w-full" />
        ) : (rulesQuery.data ?? []).length === 0 ? (
          <p className="text-sm text-muted-foreground">{t('calendar.rules.no_rules')}</p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('calendar.rules.recurrence')}</TableHead>
                <TableHead>{t('calendar.rules.day')}</TableHead>
                <TableHead>{t('calendar.rules.start_time')}</TableHead>
                <TableHead>{t('calendar.rules.end_time')}</TableHead>
                <TableHead>{t('calendar.rules.active')}</TableHead>
                <TableHead className="text-end">{t('common.actions')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {(rulesQuery.data ?? []).map((r) => (
                <RuleRow key={r.id} rule={r} />
              ))}
            </TableBody>
          </Table>
        )}
        <RuleDialog open={open} onOpenChange={setOpen} />
      </CardContent>
    </Card>
  );
}

function RuleRow({ rule }: { rule: AvailabilityRuleView }) {
  const { t } = useTranslation();
  const apiError = useApiError();
  const deleteMut = useDeleteRule();
  async function onDelete() {
    try {
      await deleteMut.mutateAsync(rule.id);
    } catch (err) {
      toast({ variant: 'destructive', description: apiError(err) });
    }
  }
  return (
    <TableRow>
      <TableCell>
        <Badge variant={rule.recurrence === 'WEEKLY' ? 'info' : 'secondary'}>
          {t(`calendar.rules.${rule.recurrence}`)}
        </Badge>
      </TableCell>
      <TableCell>
        {rule.recurrence === 'WEEKLY' && rule.dayOfWeek != null
          ? t(`weekday.${rule.dayOfWeek}`)
          : rule.date ?? '—'}
      </TableCell>
      <TableCell className="font-mono">{rule.startTime}</TableCell>
      <TableCell className="font-mono">{rule.endTime}</TableCell>
      <TableCell>{rule.active ? t('common.yes') : t('common.no')}</TableCell>
      <TableCell className="text-end">
        <Button
          size="icon"
          variant="ghost"
          onClick={onDelete}
          disabled={deleteMut.isPending}
        >
          <Trash2 className="h-4 w-4" />
        </Button>
      </TableCell>
    </TableRow>
  );
}

function RuleDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange(o: boolean): void;
}) {
  const { t } = useTranslation();
  const apiError = useApiError();
  const createMut = useCreateRule();

  const [recurrence, setRecurrence] = useState<Recurrence>('WEEKLY');
  const [dayOfWeek, setDayOfWeek] = useState('0');
  const [date, setDate] = useState('');
  const [startTime, setStartTime] = useState('09:00');
  const [endTime, setEndTime] = useState('11:00');

  async function onSubmit() {
    const body: CreateAvailabilityRuleRequest = {
      recurrence,
      startTime,
      endTime,
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'Africa/Casablanca',
      dayOfWeek: recurrence === 'WEEKLY' ? Number(dayOfWeek) : null,
      date: recurrence === 'ONE_OFF' && date ? date : null,
    };
    try {
      await createMut.mutateAsync(body);
      onOpenChange(false);
    } catch (err) {
      toast({ variant: 'destructive', description: apiError(err) });
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('calendar.rules.add')}</DialogTitle>
        </DialogHeader>
        <div className="space-y-3">
          <div className="space-y-1.5">
            <Label>{t('calendar.rules.recurrence')}</Label>
            <Select value={recurrence} onValueChange={(v) => setRecurrence(v as Recurrence)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="WEEKLY">{t('calendar.rules.WEEKLY')}</SelectItem>
                <SelectItem value="ONE_OFF">{t('calendar.rules.ONE_OFF')}</SelectItem>
              </SelectContent>
            </Select>
          </div>
          {recurrence === 'WEEKLY' ? (
            <div className="space-y-1.5">
              <Label>{t('calendar.rules.day')}</Label>
              <Select value={dayOfWeek} onValueChange={setDayOfWeek}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {[0, 1, 2, 3, 4, 5, 6].map((d) => (
                    <SelectItem key={d} value={String(d)}>
                      {t(`weekday.${d}`)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          ) : (
            <div className="space-y-1.5">
              <Label>{t('calendar.rules.date')}</Label>
              <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
            </div>
          )}
          <div className="grid grid-cols-2 gap-2">
            <div className="space-y-1.5">
              <Label>{t('calendar.rules.start_time')}</Label>
              <Input type="time" value={startTime} onChange={(e) => setStartTime(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label>{t('calendar.rules.end_time')}</Label>
              <Input type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)} />
            </div>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            {t('common.cancel')}
          </Button>
          <Button onClick={onSubmit} disabled={createMut.isPending}>
            {t('common.save')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
