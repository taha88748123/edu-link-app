package ma.naja7i.notification.infrastructure.channels;

import ma.naja7i.notification.domain.Channel;
import ma.naja7i.notification.domain.Notification;
import ma.naja7i.notification.domain.SendOutcome;

/**
 * Strategy interface implemented by each channel adapter. Returns a {@link SendOutcome}
 * — never throws : transient failures are signalled via {@code SendOutcome.failed(...)}
 * so the dispatcher can decide whether to retry.
 */
public interface NotificationChannelSender {

    Channel channel();

    SendOutcome send(Notification notif);
}
