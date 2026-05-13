package ma.naja7i.calendar.config;

import lombok.Getter;
import lombok.Setter;
import ma.naja7i.shared.dto.Modality;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Service-level configuration read from {@code naja7i.calendar.*}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "naja7i.calendar")
public class CalendarProperties {

    /** Shared secret between booking-service and calendar-service for {@code /api/v1/internal/**}. */
    private String internalKey = "dev-internal-key";

    /** TTL of the {@code RESERVED_PENDING} state (auto-release after this duration). */
    private int ttlMinutes = 10;

    /** How far ahead the materializer builds slots from active rules. */
    private int generationWindowDays = 60;

    /** Default slot duration when a rule covers a larger window. */
    private int defaultSlotDurationMinutes = 60;

    /** Default modality applied to materialized slots (overridable per rule in v2). */
    private Modality defaultModality = Modality.ONLINE;
}
