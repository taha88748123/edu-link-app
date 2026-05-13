package ma.naja7i.notification.infrastructure.messaging.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.naja7i.shared.events.DomainEvent;

/**
 * Tiny helper that converts the loosely-typed {@code payload} carried by an inbound
 * {@link DomainEvent} into the strongly-typed consumer-side mirror record.
 *
 * <p>The Spring-kafka {@code JsonDeserializer} hydrates {@code payload} as a
 * {@code LinkedHashMap}, so we re-bind it with Jackson here.</p>
 */
public final class EventPayloads {

    private EventPayloads() {}

    public static <T> T as(DomainEvent<?> event, Class<T> type, ObjectMapper mapper) {
        if (event == null || event.payload() == null) return null;
        return mapper.convertValue(event.payload(), type);
    }
}
