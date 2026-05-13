package ma.naja7i.notification.infrastructure.web.dto;

import ma.naja7i.notification.domain.Channel;
import ma.naja7i.notification.domain.Notification;
import ma.naja7i.notification.domain.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-side projection of {@link Notification}. The raw {@code payload} JSON is omitted
 * to avoid leaking template variables (which may contain PII like reset tokens).
 */
public record NotificationView(
        Long id,
        UUID userId,
        Channel channel,
        String templateKey,
        String locale,
        String subject,
        String body,
        NotificationStatus status,
        String providerMessageId,
        String error,
        short attempts,
        Instant createdAt,
        Instant sentAt,
        Instant deliveredAt
) {

    public static NotificationView from(Notification n) {
        return new NotificationView(
                n.getId(), n.getUserId(), n.getChannel(), n.getTemplateKey(), n.getLocale(),
                n.getSubject(), n.getBody(), n.getStatus(), n.getProviderMessageId(),
                n.getError(), n.getAttempts(), n.getCreatedAt(), n.getSentAt(), n.getDeliveredAt());
    }
}
