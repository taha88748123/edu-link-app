package ma.naja7i.notification.domain;

/**
 * Result returned by a {@code NotificationChannelSender#send}. {@code success} drives
 * whether the row is marked SENT or pushed to the retry queue.
 */
public record SendOutcome(boolean success, String providerMessageId, String error) {

    public static SendOutcome ok(String providerMessageId) {
        return new SendOutcome(true, providerMessageId, null);
    }

    public static SendOutcome failed(String error) {
        return new SendOutcome(false, null, error);
    }
}
