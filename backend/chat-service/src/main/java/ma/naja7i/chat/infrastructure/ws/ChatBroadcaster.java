package ma.naja7i.chat.infrastructure.ws;

import lombok.extern.slf4j.Slf4j;
import ma.naja7i.chat.domain.Message;
import ma.naja7i.chat.infrastructure.web.dto.MessageView;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Thin facade around {@link SimpMessagingTemplate} that pushes new chat
 * messages to subscribers of {@code /topic/conversations/{id}/messages}.
 *
 * <p>The template bean is injected lazily through an {@link ObjectProvider}
 * because the WebSocket broker is conditional on
 * {@code naja7i.chat.ws.enabled=true}. When the broker is disabled the
 * broadcaster is a no-op (instead of throwing).
 *
 * <p>NOTE: this broadcast is intentionally fire-and-forget; if it fails the
 * message has already been persisted and the REST GET path serves the new
 * message on next refetch. Kafka events are emitted independently in
 * {@link ma.naja7i.chat.application.MessageService}.
 */
@Component
@Slf4j
public class ChatBroadcaster {

    public static final String TOPIC_TEMPLATE = "/topic/conversations/%s/messages";

    private final ObjectProvider<SimpMessagingTemplate> templateProvider;
    private final boolean enabled;

    public ChatBroadcaster(ObjectProvider<SimpMessagingTemplate> templateProvider,
                           @Value("${naja7i.chat.ws.enabled:true}") boolean enabled) {
        this.templateProvider = templateProvider;
        this.enabled = enabled;
    }

    public void broadcastMessage(UUID conversationId, Message msg) {
        if (!enabled) return;
        SimpMessagingTemplate template = templateProvider.getIfAvailable();
        if (template == null) {
            return;
        }
        try {
            MessageView view = MessageView.of(msg);
            String dest = String.format(TOPIC_TEMPLATE, conversationId);
            template.convertAndSend(dest, view);
        } catch (Exception ex) {
            // Never fail the parent transaction over a broadcast hiccup.
            log.debug("WS broadcast failed for conv={} msg={}: {}",
                    conversationId, msg.getId(), ex.getMessage());
        }
    }
}
