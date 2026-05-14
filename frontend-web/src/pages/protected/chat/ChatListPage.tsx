import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import type { ConversationStatus } from '@/api/chat.api';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { ConversationListItem } from '@/components/chat/ConversationListItem';
import { useAuthStore } from '@/auth/auth.store';
import { useMyConversations } from '@/hooks/queries/useChat';

type Tab = 'ALL' | ConversationStatus;
const TABS: Tab[] = ['ALL', 'PRE_ACCEPT', 'POST_ACCEPT', 'CLOSED'];

export default function ChatListPage() {
  const { t, i18n } = useTranslation();
  const me = useAuthStore((s) => s.user);
  const [tab, setTab] = useState<Tab>('ALL');

  const filters = tab === 'ALL' ? {} : { status: tab };
  const conversationsQuery = useMyConversations(filters);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">{t('chat.title')}</h1>

      <Tabs value={tab} onValueChange={(v) => setTab(v as Tab)}>
        <TabsList>
          {TABS.map((s) => (
            <TabsTrigger key={s} value={s}>
              {s === 'ALL' ? t('common.all') : t(`chat.status.${s}`)}
            </TabsTrigger>
          ))}
        </TabsList>
      </Tabs>

      {conversationsQuery.isLoading ? (
        <Skeleton className="h-64 w-full" />
      ) : (conversationsQuery.data?.length ?? 0) === 0 ? (
        <Card>
          <CardContent className="py-10 text-center text-sm text-muted-foreground">
            {t('chat.empty')}
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-2">
          {(conversationsQuery.data ?? []).map((c) => (
            <ConversationListItem
              key={c.id}
              conversation={c}
              myUserId={me?.id ?? null}
              locale={i18n.language}
            />
          ))}
        </div>
      )}
    </div>
  );
}
