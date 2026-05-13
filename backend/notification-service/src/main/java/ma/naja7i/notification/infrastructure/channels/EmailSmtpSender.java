package ma.naja7i.notification.infrastructure.channels;

import lombok.extern.slf4j.Slf4j;
import ma.naja7i.notification.config.NotificationProperties;
import ma.naja7i.notification.domain.Channel;
import ma.naja7i.notification.domain.Notification;
import ma.naja7i.notification.domain.SendOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Sends email via Spring's {@link JavaMailSender}, configured to talk to localhost:1025
 * (Mailhog) in dev. Activated when {@code naja7i.notification.providers.email.driver=smtp}.
 *
 * <p>For prod replace by the SendGrid implementation (driver=real).</p>
 */
@Component
@ConditionalOnProperty(name = "naja7i.notification.providers.email.driver", havingValue = "smtp")
@Slf4j
public class EmailSmtpSender implements NotificationChannelSender {

    private final JavaMailSender mailSender;
    private final NotificationProperties props;

    public EmailSmtpSender(JavaMailSender mailSender, NotificationProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    @Override
    public Channel channel() {
        return Channel.EMAIL;
    }

    @Override
    public SendOutcome send(Notification notif) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(props.getProviders().getEmail().getFromEmail() != null
                    ? props.getProviders().getEmail().getFromEmail()
                    : "noreply@naja7i.local");
            // v1 : the recipient address is not yet resolved from user-profile-service.
            // We send to a dev-only sink so that Mailhog still captures the message.
            msg.setTo("user-" + notif.getUserId() + "@naja7i.local");
            msg.setSubject(notif.getSubject() == null ? "" : notif.getSubject());
            msg.setText(notif.getBody() == null ? "" : notif.getBody());
            mailSender.send(msg);
            return SendOutcome.ok("smtp-" + UUID.randomUUID());
        } catch (Exception ex) {
            log.warn("SMTP send failed: {}", ex.getMessage());
            return SendOutcome.failed(ex.getMessage());
        }
    }
}
