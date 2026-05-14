import { useEffect, useMemo, useRef } from 'react';
import { ArrowLeft } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

import { Alert, AlertDescription } from '@/components/ui/alert';
import { AntiBypassNotice } from '@/components/booking/AntiBypassNotice';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ComposeInput } from '@/components/chat/ComposeInput';
import { MessageBubble } from '@/components/chat/MessageBubble';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Skeleton } from '@/components/ui/skeleton';
import { TypingIndicator } from '@/components/chat/TypingIndicator';
import { useApiError } from '@/hooks/useApiError';
import { useAuthStore } from '@/auth/auth.store';
import {
  useConversation,
  useMarkRead,
  useMessages,
  usePresence,
  useSendMessage,
  useTyping,
} from '@/hooks/queries/useChat';

/**
 * Conversation detail page. Real-time-ish: messages refetched on a 3s
 * interval, presence on 10s. Replace with SockJS+STOMP once /ws/chat is
 * wired through the gateway.
 */
export default function ChatDetailPage() {
  const { conversationId = '' } = useParams<{ conversationId: string }>();
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const me = useAuthStore((s) => s.user);
  const formatApiError = useApiError();

  const conversationQuery = useConversation(conversationId);
  const messagesQuery = useMessages(conversationId);
  const sendMut = useSendMessage();
  const typingMut = useTyping();
  const markReadMut = useMarkRead();

  const conv = conversationQuery.data;
  const peerId = useMemo(() => {
    if (!conv || !me) return null;
    return conv.profId === me.id ? conv.studentId : conv.profId;
  }, [conv, me]);

  const presenceQuery = usePresence(peerId ?? undefined);

  const scrollRef = useRef<HTMLDivElement | null>(null);

  // Auto-scroll to bottom when messages list grows.
  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    el.scrollTop = el.scrollHeight;
  }, [messagesQuery.data?.length]);

  // Mark conversation as read whenever we have new messages.
  const lastReadIdRef = useRef<string | null>(null);
  useEffect(() => {
    if (!conv || !messagesQuery.data || messagesQuery.data.length === 0) return;
    const last = messagesQuery.data[messagesQuery.data.length - 1];
    if (!last) return;
    if (lastReadIdRef.current === last.id) return;
    lastReadIdRef.current = last.id;
    markReadMut.mutate({ id: conv.id, lastReadMessageId: last.id });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [messagesQuery.data, conv?.id]);

  if (conversationQuery.isLoading) {
    return <Skeleton className="h-96 w-full" />;
  }

  if (!conv) {
    return (
      <Alert variant="destructive">
        <AlertDescription>{t('errors.RESOURCE_NOT_FOUND')}</AlertDescription>
      </Alert>
    );
  }

  const peerInitials = (peerId ?? '??').slice(0, 2).toUpperCase();
  const isPreAccept = conv.status === 'PRE_ACCEPT';
  const isClosed = conv.status === 'CLOSED';

  // For v1 the "typing" indicator is one-way — we never receive peer's typing
  // signal via REST. UI shows it only when WS is wired; placeholder stays false.
  // TODO: WebSocket via SockJS+STOMP when chat-service exposes /ws/chat.
  const peerIsTyping = false;

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <Button
          asChild
          size="sm"
          variant="ghost"
          aria-label={t('common.back')}
        >
          <Link to="/chat">
            <ArrowLeft className="h-4 w-4" />
            <span className="ms-1 hidden sm:inline">{t('common.back')}</span>
          </Link>
        </Button>
        <h1 className="text-xl font-semibold">{t('chat.detail_title')}</h1>
      </div>

      <div className="grid gap-4 md:grid-cols-[280px_1fr]">
        {/* Info card — peer identity stays masked while PRE_ACCEPT. */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">{t('chat.info.title')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex items-center gap-3">
              <Avatar className="h-12 w-12">
                <AvatarFallback>{peerInitials}</AvatarFallback>
              </Avatar>
              <div>
                <div className="text-sm font-medium">{peerInitials}</div>
                <div className="text-xs text-muted-foreground">
                  {presenceQuery.data?.online
                    ? t('chat.presence.online')
                    : t('chat.presence.offline')}
                </div>
              </div>
            </div>
            <div className="text-xs">
              <Badge variant="outline">{t(`chat.status.${conv.status}`)}</Badge>
            </div>
            {conv.bookingId ? (
              <div className="text-xs">
                <Link
                  to={`/student/bookings/${conv.bookingId}`}
                  className="text-primary underline-offset-4 hover:underline"
                >
                  {t('chat.info.booking_link')}
                </Link>
              </div>
            ) : null}
          </CardContent>
        </Card>

        {/* Thread card */}
        <Card className="flex h-[60vh] min-h-[480px] flex-col">
          <CardHeader className="border-b py-3">
            <CardTitle className="text-sm">{t('chat.thread_title')}</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-1 flex-col gap-0 overflow-hidden p-0">
            <ScrollArea className="flex-1">
              <div
                ref={scrollRef}
                className="flex flex-col gap-2 p-3"
                data-testid="chat-thread"
              >
                {(messagesQuery.data ?? []).map((m) => (
                  <MessageBubble
                    key={m.id}
                    message={m}
                    isOwn={m.senderId === me?.id}
                    locale={i18n.language}
                  />
                ))}
                {(messagesQuery.data?.length ?? 0) === 0 ? (
                  <p className="py-10 text-center text-sm text-muted-foreground">
                    {t('chat.no_messages')}
                  </p>
                ) : null}
              </div>
            </ScrollArea>

            {isPreAccept ? <AntiBypassNotice className="m-3" /> : null}

            <TypingIndicator peerInitials={peerInitials} show={peerIsTyping} />

            {isClosed ? (
              <Alert className="m-3">
                <AlertDescription>{t('chat.closed_notice')}</AlertDescription>
              </Alert>
            ) : (
              <ComposeInput
                disabled={sendMut.isPending}
                onSend={async (body) => {
                  try {
                    await sendMut.mutateAsync({
                      id: conv.id,
                      body: { type: 'TEXT', body },
                    });
                  } catch (err) {
                    navigate('.', { state: { error: formatApiError(err) } });
                  }
                }}
                onTyping={() => typingMut.mutate(conv.id)}
              />
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
