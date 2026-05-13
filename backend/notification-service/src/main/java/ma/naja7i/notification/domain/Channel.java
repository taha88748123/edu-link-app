package ma.naja7i.notification.domain;

/**
 * Channels supported by the notification-service.
 * Mirrors {@code notification_log.channel} in {@code notification_db}.
 */
public enum Channel {
    PUSH,
    SMS,
    WA,
    EMAIL
}
