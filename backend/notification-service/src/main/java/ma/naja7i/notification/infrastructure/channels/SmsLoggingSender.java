package ma.naja7i.notification.infrastructure.channels;

import lombok.extern.slf4j.Slf4j;
import ma.naja7i.notification.domain.Channel;
import ma.naja7i.notification.domain.Notification;
import ma.naja7i.notification.domain.SendOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Default SMS adapter — writes the message to stdout / logs only. Active when
 * {@code naja7i.notification.providers.sms.driver=logging} (the default).
 */
@Component
@ConditionalOnProperty(name = "naja7i.notification.providers.sms.driver", havingValue = "logging", matchIfMissing = true)
@Slf4j
public class SmsLoggingSender implements NotificationChannelSender {

    @Override
    public Channel channel() {
        return Channel.SMS;
    }

    @Override
    public SendOutcome send(Notification notif) {
        log.info("[SMS:LOG] userId={} template={} body=\"{}\"",
                notif.getUserId(), notif.getTemplateKey(), notif.getBody());
        return SendOutcome.ok("log-" + UUID.randomUUID());
    }
}
