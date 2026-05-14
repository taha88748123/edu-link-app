import { NavLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Calendar,
  ClipboardCheck,
  ClipboardList,
  Coins,
  CreditCard,
  Gavel,
  LayoutDashboard,
  MessageSquare,
  ScrollText,
  Tag,
  Wallet,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

import type { Role } from '@/api/types';
import { cn } from '@/lib/utils';
import { useAuthStore } from '@/auth/auth.store';

interface NavItem {
  to: string;
  labelKey: string;
  icon: LucideIcon;
  /** When true (Vague A), the item is visible but disabled. */
  soon?: boolean;
}

const STUDENT_NAV: NavItem[] = [
  { to: '/student', labelKey: 'nav.dashboard', icon: LayoutDashboard },
  { to: '/student/bookings', labelKey: 'nav.bookings', icon: ClipboardList, soon: true },
  { to: '/student/wallet', labelKey: 'nav.wallet', icon: Wallet, soon: true },
  { to: '/student/chat', labelKey: 'nav.chat', icon: MessageSquare, soon: true },
];

const PROF_NAV: NavItem[] = [
  { to: '/prof', labelKey: 'nav.dashboard', icon: LayoutDashboard },
  { to: '/prof/calendar', labelKey: 'nav.calendar', icon: Calendar, soon: true },
  { to: '/prof/pricing', labelKey: 'nav.pricing', icon: Tag, soon: true },
  { to: '/prof/bookings', labelKey: 'nav.bookings', icon: ClipboardList, soon: true },
  { to: '/prof/wallet', labelKey: 'nav.wallet', icon: Wallet, soon: true },
  { to: '/prof/chat', labelKey: 'nav.chat', icon: MessageSquare, soon: true },
];

const ADMIN_NAV: NavItem[] = [
  { to: '/admin', labelKey: 'nav.dashboard', icon: LayoutDashboard },
  { to: '/admin/validations', labelKey: 'nav.validations', icon: ClipboardCheck, soon: true },
  { to: '/admin/topups', labelKey: 'nav.topups', icon: Coins, soon: true },
  { to: '/admin/payouts', labelKey: 'nav.payouts', icon: CreditCard, soon: true },
  { to: '/admin/disputes', labelKey: 'nav.disputes', icon: Gavel, soon: true },
  { to: '/admin/audit', labelKey: 'nav.audit', icon: ScrollText, soon: true },
];

function navFor(role: Role | null | undefined): NavItem[] {
  switch (role) {
    case 'PROF':
      return PROF_NAV;
    case 'ADMIN':
      return ADMIN_NAV;
    case 'STUDENT':
    case 'PARENT':
    default:
      return STUDENT_NAV;
  }
}

export function Sidebar() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const items = navFor(user?.role);

  return (
    <aside className="hidden border-e bg-card md:flex md:w-60 md:flex-col">
      <nav className="flex flex-1 flex-col gap-1 p-4">
        {items.map((item) => (
          <SidebarLink key={item.to} item={item} label={t(item.labelKey)} soonLabel={t('common.soon')} />
        ))}
      </nav>
    </aside>
  );
}

function SidebarLink({
  item,
  label,
  soonLabel,
}: {
  item: NavItem;
  label: string;
  soonLabel: string;
}) {
  const Icon = item.icon;
  if (item.soon) {
    return (
      <div
        aria-disabled
        className="flex cursor-not-allowed items-center justify-between rounded-md px-3 py-2 text-sm text-muted-foreground opacity-60"
      >
        <span className="flex items-center gap-2">
          <Icon className="h-4 w-4" />
          {label}
        </span>
        <span className="rounded bg-muted px-1.5 py-0.5 text-[10px] font-medium uppercase">
          {soonLabel}
        </span>
      </div>
    );
  }
  return (
    <NavLink
      to={item.to}
      end
      className={({ isActive }) =>
        cn(
          'flex items-center gap-2 rounded-md px-3 py-2 text-sm transition-colors',
          isActive
            ? 'bg-primary/10 text-primary'
            : 'text-foreground hover:bg-accent hover:text-accent-foreground',
        )
      }
    >
      <Icon className="h-4 w-4" />
      {label}
    </NavLink>
  );
}
