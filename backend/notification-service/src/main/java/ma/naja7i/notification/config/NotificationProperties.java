package ma.naja7i.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Root config object for the notification-service. Bound under {@code naja7i.notification.*}.
 * All sub-objects default to {@code logging} drivers so the service can boot with no
 * external credentials.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "naja7i.notification")
public class NotificationProperties {

    /** Shared secret expected on the {@code X-Internal-Key} header for service-to-service calls. */
    private String internalKey = "dev-internal-key";

    /** Producer name embedded in published {@code DomainEvent.producer}. */
    private String producer = "notification-service";

    private Providers providers = new Providers();
    private Retry retry = new Retry();
    private AdminAlerts adminAlerts = new AdminAlerts();

    @Getter
    @Setter
    public static class Providers {
        private Provider sms   = new Provider();
        private Provider wa    = new Provider();
        private Provider email = new Provider();
        private Provider push  = new Provider();
    }

    @Getter
    @Setter
    public static class Provider {
        /** {@code logging|smtp|real}. Default {@code logging} keeps the service safe to start. */
        private String driver = "logging";
        private String fromNumber;     // for sms / wa : Twilio "from"
        private String fromEmail;      // for email
        private String accountSid;     // Twilio
        private String authToken;      // Twilio
        private String apiKey;         // SendGrid / FCM service-account path
    }

    @Getter
    @Setter
    public static class Retry {
        /** Maximum attempts before marking notification FAILED. */
        private int maxAttempts = 5;
        /** Backoff steps in seconds — {30s, 2m, 10m, 1h, 6h}. */
        private long[] backoffSeconds = {30, 120, 600, 3600, 21600};
    }

    @Getter
    @Setter
    public static class AdminAlerts {
        /** Pseudo-userId used to address all platform admins until a real admin directory exists. */
        private String inboxKey = "admin@naja7i";
        /** Locale used for admin-targeted templates. */
        private String locale = "fr";
        /** Channel used for admin alerts (default: EMAIL — logged in dev). */
        private String channel = "EMAIL";
    }
}
