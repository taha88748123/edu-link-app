package ma.naja7i.booking.infrastructure.rest;

import lombok.RequiredArgsConstructor;
import ma.naja7i.booking.config.BookingProperties;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Calls {@code pricing-service}'s server-to-server quote endpoint to lock the price
 * of a booking before persisting it.
 *
 * <p>Same retry / failure semantics as {@link CalendarInternalClient}.
 */
@Component
@RequiredArgsConstructor
public class PricingInternalClient {

    private static final Logger log = LoggerFactory.getLogger(PricingInternalClient.class);
    private static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

    private final RestClient.Builder restClientBuilder;
    private final BookingProperties properties;

    @Value("${naja7i.booking.pricing-base-url:http://pricing-service}")
    private String pricingBaseUrl;

    private RestClient client() {
        return restClientBuilder
                .baseUrl(pricingBaseUrl)
                .defaultHeader(INTERNAL_KEY_HEADER, properties.getInternalKey())
                .build();
    }

    public QuoteResult quote(UUID profId, String subjectCode, SchoolLevel level, Modality modality,
                             int durationMinutes, Integer dayOfWeek, UUID packId) {
        Map<String, Object> body = new HashMap<>();
        body.put("profId", profId);
        body.put("subjectCode", subjectCode);
        body.put("level", level);
        body.put("modality", modality);
        body.put("durationMinutes", durationMinutes);
        if (dayOfWeek != null) body.put("dayOfWeek", dayOfWeek);
        if (packId != null)    body.put("packId", packId);

        try {
            return callWithRetry(body);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception ex) {
            log.error("pricing-service quote unexpected error: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, CalendarInternalClient.EXT_FAILURE_MARKER);
        }
    }

    private QuoteResult callWithRetry(Map<String, Object> body) {
        try {
            return doCall(body);
        } catch (BusinessException be) {
            throw be;
        } catch (ResourceAccessException | RestClientResponseException firstFail) {
            log.warn("pricing-service quote failed, retrying once: {}", firstFail.getMessage());
            try {
                return doCall(body);
            } catch (BusinessException be) {
                throw be;
            } catch (Exception retryFail) {
                log.error("pricing-service quote failed after retry: {}", retryFail.getMessage());
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                        CalendarInternalClient.EXT_FAILURE_MARKER);
            }
        }
    }

    private QuoteResult doCall(Map<String, Object> body) {
        ApiResponse<QuoteResult> response = client().post()
                .uri("/api/v1/internal/pricing/quote")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                            "pricing-service rejected quote (" + res.getStatusCode() + ")");
                })
                .body(new ParameterizedTypeReference<>() {});
        if (response == null || response.data() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "pricing-service returned empty body");
        }
        return response.data();
    }
}
