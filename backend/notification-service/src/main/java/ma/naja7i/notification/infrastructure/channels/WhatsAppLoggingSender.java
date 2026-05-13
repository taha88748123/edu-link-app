package ma.naja7i.notification.infrastructure.channels;

import lombok.extern.slf4j.Slf4j;
import ma.naja7i.notification.domain.Channel;
import ma.naja7i.notification.domain.Notification;
import ma.naja7i.notification.domain.SendOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "naja7i.notification.providers.wa.driver", havingValue = "logging", matchIfMissing = true)
@Slf4j
public class WhatsAppLoggingSender implements NotificationChannelSender {

    @Override
    public Channel channel() {
        return Channel.WA;
    }

    @Override
    public SendOutcome send(Notification notif) {
        log.info("[WA:LOG] userId={} template={} body=\"{}\"",
                notif.getUserId(), notif.getTemplateKey(), notif.getBody());
        return SendOutcome.ok("log-" + UUID.randomUUID());
    }
}
