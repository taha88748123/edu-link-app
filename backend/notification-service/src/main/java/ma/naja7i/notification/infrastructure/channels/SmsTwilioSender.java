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
 * Real Twilio SMS adapter. Activated when {@code naja7i.notification.providers.sms.driver=real}.
 *
 * <p>v1 does NOT call user-profile-service to resolve the destination MSISDN — the body
 * is logged and a Twilio request would need a real phone number. The wiring here is a
 * compilable stub : credentials are loaded but {@code send()} requires
 * {@code notif.getPayload()} to carry the destination number under key {@code "to"}.
 * Once user-profile-service exposes phone lookup, replace by a real fetch.</p>
 */
@Component
@ConditionalOnProperty(name = "naja7i.notification.providers.sms.driver", havingValue = "real")
@Slf4j
public class SmsTwilioSender implements NotificationChannelSender {

    private final NotificationProperties props;

    public SmsTwilioSender(NotificationProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        NotificationProperties.Provider p = props.getProviders().getSms();
        if (p.getAccountSid() == null || p.getAuthToken() == null) {
            throw new IllegalStateException(
                    "naja7i.notification.providers.sms.driver=real requires accountSid + authToken");
        }
        Twilio.init(p.getAccountSid(), p.getAuthToken());
    }

    @Override
    public Channel channel() {
        return Channel.SMS;
    }

    @Override
    public SendOutcome send(Notification notif) {
        try {
            String to = extractTo(notif);
            String from = props.getProviders().getSms().getFromNumber();
            Message msg = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(from),
                    notif.getBody() == null ? "" : notif.getBody()
            ).create();
            return SendOutcome.ok(msg.getSid());
        } catch (Exception ex) {
            log.warn("Twilio SMS send failed: {}", ex.getMessage());
            return SendOutcome.failed(ex.getMessage());
        }
    }

    private String extractTo(Notification notif) {
        // payload is opaque JSON in v1 — destination resolution is a TODO once
        // user-profile-service exposes a phone lookup. We accept a "to" hint
        // injected by the REST send endpoint for early E2E tests.
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
