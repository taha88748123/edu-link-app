package ma.naja7i.notification.infrastructure.channels;

import lombok.extern.slf4j.Slf4j;
import ma.naja7i.notification.domain.Channel;
import ma.naja7i.notification.domain.Notification;
import ma.naja7i.notification.domain.SendOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "naja7i.notification.providers.push.driver", havingValue = "logging", matchIfMissing = true)
@Slf4j
public class PushLoggingSender implements NotificationChannelSender {

    @Override
    public Channel channel() {
        return Channel.PUSH;
    }

    @Override
    public SendOutcome send(Notification notif) {
        log.info("[PUSH:LOG] userId={} template={} subject=\"{}\" body=\"{}\"",
                notif.getUserId(), notif.getTemplateKey(), notif.getSubject(), notif.getBody());
        return SendOutcome.ok("log-" + UUID.randomUUID());
    }
}
