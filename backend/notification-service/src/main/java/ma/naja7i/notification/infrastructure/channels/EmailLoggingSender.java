package ma.naja7i.notification.infrastructure.channels;

import lombok.extern.slf4j.Slf4j;
import ma.naja7i.notification.domain.Channel;
import ma.naja7i.notification.domain.Notification;
import ma.naja7i.notification.domain.SendOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Default email adapter — only logs. To send via Mailhog SMTP set
 * {@code naja7i.notification.providers.email.driver=smtp}.
 */
@Component
@ConditionalOnProperty(name = "naja7i.notification.providers.email.driver", havingValue = "logging", matchIfMissing = true)
@Slf4j
public class EmailLoggingSender implements NotificationChannelSender {

    @Override
    public Channel channel() {
        return Channel.EMAIL;
    }

    @Override
    public SendOutcome send(Notification notif) {
        log.info("[EMAIL:LOG] userId={} template={} subject=\"{}\" body=\"{}\"",
                notif.getUserId(), notif.getTemplateKey(), notif.getSubject(), notif.getBody());
        return SendOutcome.ok("log-" + UUID.randomUUID());
    }
}
