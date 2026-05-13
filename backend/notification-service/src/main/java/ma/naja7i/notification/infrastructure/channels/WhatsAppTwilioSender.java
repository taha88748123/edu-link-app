package ma.naja7i.notification.infrastructure.channels;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import ma.naja7i.notification.config.NotificationProperties;
import ma.naja7i.notification.domain.Channel;
import ma.naja7i.notification.domain.Notification;
import ma.naja7i.notification.domain.SendOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Real Twilio WhatsApp adapter — uses the {@code whatsapp:} prefix on both
 * {@code from} and {@code to}. Activated when
 * {@code naja7i.notification.providers.wa.driver=real}.
 */
@Component
@ConditionalOnProperty(name = "naja7i.notification.providers.wa.driver", havingValue = "real")
@Slf4j
public class WhatsAppTwilioSender implements NotificationChannelSender {

    private final NotificationProperties props;

    public WhatsAppTwilioSender(NotificationProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        NotificationProperties.Provider p = props.getProviders().getWa();
        if (p.getAccountSid() == null || p.getAuthToken() == null) {
            throw new IllegalStateException(
                    "naja7i.notification.providers.wa.driver=real requires accountSid + authToken");
        }
        Twilio.init(p.getAccountSid(), p.getAuthToken());
    }

    @Override
    public Channel channel() {
        return Channel.WA;
    }

    @Override
    public SendOutcome send(Notification notif) {
        try {
            String to = "whatsapp:" + extractTo(notif);
            String from = "whatsapp:" + props.getProviders().getWa().getFromNumber();
            Message msg = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(from),
                    notif.getBody() == null ? "" : notif.getBody()
            ).create();
            return SendOutcome.ok(msg.getSid());
        } catch (Exception ex) {
            log.warn("Twilio WA send failed: {}", ex.getMessage());
            return SendOutcome.failed(ex.getMessage());
        }
    }

    private String extractTo(Notification notif) {
        String payload = notif.getPayload();
        if (payload != null && payload.contains("\"to\"")) {
            int idx = payload.indexOf("\"to\"");
            int colon = payload.indexOf(':', idx);
            int quote1 = payload.indexOf('"', colon);
            int quote2 = payload.indexOf('"', quote1 + 1);
            if (quote1 > 0 && quote2 > quote1) return payload.substring(quote1 + 1, quote2);
        }
        throw new IllegalArgumentException("Destination phone (to) not resolvable in payload");
    }
}
