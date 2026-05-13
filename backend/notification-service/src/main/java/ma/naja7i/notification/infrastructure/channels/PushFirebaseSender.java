package ma.naja7i.notification.infrastructure.channels;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import ma.naja7i.notification.config.NotificationProperties;
import ma.naja7i.notification.domain.Channel;
import ma.naja7i.notification.domain.SendOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;

/**
 * Real Firebase Cloud Messaging push adapter. Active when
 * {@code naja7i.notification.providers.push.driver=real}. The credentials are
 * a service-account JSON file path on disk, supplied through {@code apiKey}.
 */
@Component
@ConditionalOnProperty(name = "naja7i.notification.providers.push.driver", havingValue = "real")
@Slf4j
public class PushFirebaseSender implements NotificationChannelSender {

    private final NotificationProperties props;

    public PushFirebaseSender(NotificationProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() throws Exception {
        if (FirebaseApp.getApps().isEmpty()) {
            String path = props.getProviders().getPush().getApiKey();
            if (path == null) {
                throw new IllegalStateException(
                        "naja7i.notification.providers.push.driver=real requires apiKey (service-account JSON path)");
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new FileInputStream(path)))
                    .build();
            FirebaseApp.initializeApp(options);
        }
    }

    @Override
    public Channel channel() {
        return Channel.PUSH;
    }

    @Override
    public SendOutcome send(ma.naja7i.notification.domain.Notification notif) {
        try {
            // v1: deviceToken not yet resolved from user-profile-service. The push
            // adapter expects payload to carry "deviceToken" (set by the REST send
            // endpoint when testing) or the API call will be skipped.
            String token = extractToken(notif);
            if (token == null) {
                return SendOutcome.failed("No deviceToken available for user " + notif.getUserId());
            }
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(notif.getSubject() == null ? "" : notif.getSubject())
                            .setBody(notif.getBody() == null ? "" : notif.getBody())
                            .build())
                    .build();
            String msgId = FirebaseMessaging.getInstance().send(message);
            return SendOutcome.ok(msgId);
        } catch (Exception ex) {
            log.warn("Firebase push send failed: {}", ex.getMessage());
            return SendOutcome.failed(ex.getMessage());
        }
    }

    private String extractToken(ma.naja7i.notification.domain.Notification notif) {
        String payload = notif.getPayload();
        if (payload != null && payload.contains("\"deviceToken\"")) {
            int idx = payload.indexOf("\"deviceToken\"");
            int colon = payload.indexOf(':', idx);
            int quote1 = payload.indexOf('"', colon);
            int quote2 = payload.indexOf('"', quote1 + 1);
            if (quote1 > 0 && quote2 > quote1) return payload.substring(quote1 + 1, quote2);
        }
        return null;
    }
}
