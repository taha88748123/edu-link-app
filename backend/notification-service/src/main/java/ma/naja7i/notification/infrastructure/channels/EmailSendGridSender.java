package ma.naja7i.notification.infrastructure.channels;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import ma.naja7i.notification.config.NotificationProperties;
import ma.naja7i.notification.domain.Channel;
import ma.naja7i.notification.domain.Notification;
import ma.naja7i.notification.domain.SendOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Real SendGrid adapter. Active when {@code naja7i.notification.providers.email.driver=real}.
 * Recipient resolution from {@code payload.to} (same v1 limitation as Twilio adapters).
 */
@Component
@ConditionalOnProperty(name = "naja7i.notification.providers.email.driver", havingValue = "real")
@Slf4j
public class EmailSendGridSender implements NotificationChannelSender {

    private final NotificationProperties props;

    public EmailSendGridSender(NotificationProperties props) {
        this.props = props;
        if (props.getProviders().getEmail().getApiKey() == null) {
            throw new IllegalStateException(
                    "naja7i.notification.providers.email.driver=real requires apiKey");
        }
    }

    @Override
    public Channel channel() {
        return Channel.EMAIL;
    }

    @Override
    public SendOutcome send(Notification notif) {
        try {
            NotificationProperties.Provider p = props.getProviders().getEmail();
            Email from = new Email(p.getFromEmail() == null ? "noreply@naja7i.local" : p.getFromEmail());
            Email to = new Email(extractTo(notif));
            Content content = new Content("text/plain", notif.getBody() == null ? "" : notif.getBody());
            Mail mail = new Mail(from, notif.getSubject() == null ? "" : notif.getSubject(), to, content);

            SendGrid sg = new SendGrid(p.getApiKey());
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                return SendOutcome.ok(response.getHeaders().getOrDefault("X-Message-Id",
                        "sg-" + UUID.randomUUID()));
            }
            return SendOutcome.failed("SendGrid status=" + response.getStatusCode() + " body=" + response.getBody());
        } catch (Exception ex) {
            log.warn("SendGrid send failed: {}", ex.getMessage());
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
        return "user-" + notif.getUserId() + "@naja7i.local";
    }
}
