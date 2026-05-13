package ma.naja7i.booking.infrastructure.rest;

import lombok.RequiredArgsConstructor;
import ma.naja7i.booking.config.BookingProperties;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

/**
 * Calls {@code calendar-service}'s S2S endpoints with the shared {@code X-Internal-Key}.
 *
 * <p>Service discovery: {@code http://calendar-service} is resolved by the
 * load-balanced {@link RestClient.Builder} from {@link RestClientConfig}.
 *
 * <p>Failure semantics:
 * <ul>
 *   <li>409 on reserve -> {@link ErrorCode#BOOKING_SLOT_NOT_AVAILABLE}</li>
 *   <li>5xx / connection error -> 1 retry, then {@link ErrorCode#INTERNAL_ERROR}
 *       with the sentinel message {@code EXTERNAL_DEPENDENCY_FAILURE}. The shared
 *       {@code ErrorCode} catalog does not include a dedicated
 *       {@code EXTERNAL_DEPENDENCY_FAILURE} code, so we surface 500 with a marker.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class CalendarInternalClient {

    private static final Logger log = LoggerFactory.getLogger(CalendarInternalClient.class);
    private static final String INTERNAL_KEY_HEADER = "X-Internal-Key";
    public static final String EXT_FAILURE_MARKER = "EXTERNAL_DEPENDENCY_FAILURE";

    private final RestClient.Builder restClientBuilder;
    private final BookingProperties properties;

    @Value("${naja7i.booking.calendar-base-url:http://calendar-service}")
    private String calendarBaseUrl;

    private RestClient client() {
        return restClientBuilder
                .baseUrl(calendarBaseUrl)
                .defaultHeader(INTERNAL_KEY_HEADER, properties.getInternalKey())
                .build();
    }

    /** AVAILABLE -> RESERVED_PENDING. Returns the slot snapshot. */
    public CalendarSlotView reserveSlot(UUID slotId, UUID bookingId) {
        return callWithRetry(() -> post(
                "/api/v1/internal/calendar/slots/" + slotId + ":reserve",
                Map.of("bookingId", bookingId)
        ), "reserve");
    }

    /** RESERVED_PENDING -> BOOKED, binding the slot to {@code bookingId}. */
    public CalendarSlotView confirmSlot(UUID slotId, UUID bookingId) {
        return callWithRetry(() -> post(
                "/api/v1/internal/calendar/slots/" + slotId + ":confirm",
                Map.of("bookingId", bookingId)
        ), "confirm");
    }

    /** RESERVED_PENDING|BOOKED -> AVAILABLE. Used on cancel/decline. */
    public CalendarSlotView releaseSlot(UUID slotId, String reason) {
        return callWithRetry(() -> post(
                "/api/v1/internal/calendar/slots/" + slotId + ":release",
                Map.of("reason", reason == null ? "" : reason)
        ), "release");
    }

    // ---- internals

    private CalendarSlotView post(String path, Map<String, Object> body) {
        ApiResponse<CalendarSlotView> response = client().post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    if (res.getStatusCode().value() == 409) {
                        throw new BusinessException(ErrorCode.BOOKING_SLOT_NOT_AVAILABLE,
                                "Slot is not available");
                    }
                    throw new BusinessException(ErrorCode.BOOKING_SLOT_NOT_AVAILABLE,
                            "calendar-service rejected (" + res.getStatusCode() + ")");
                })
                .body(new ParameterizedTypeReference<>() {});
        if (response == null || response.data() == null) {
            throw new BusinessException(ErrorCode.BOOKING_SLOT_NOT_AVAILABLE,
                    "calendar-service returned empty body");
        }
        return response.data();
    }

    @FunctionalInterface
    private interface Call {
        CalendarSlotView run();
    }

    private CalendarSlotView callWithRetry(Call call, String op) {
        try {
            return call.run();
        } catch (BusinessException be) {
            // Business mapping (e.g. 409) -> do not retry.
            throw be;
        } catch (ResourceAccessException | RestClientResponseException firstFail) {
            log.warn("calendar-service {} failed, retrying once: {}", op, firstFail.getMessage());
            try {
                return call.run();
            } catch (BusinessException be) {
                throw be;
            } catch (Exception retryFail) {
                log.error("calendar-service {} failed after retry: {}", op, retryFail.getMessage());
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, EXT_FAILURE_MARKER);
            }
        } catch (Exception ex) {
            log.error("calendar-service {} unexpected error: {}", op, ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, EXT_FAILURE_MARKER);
        }
    }
}
