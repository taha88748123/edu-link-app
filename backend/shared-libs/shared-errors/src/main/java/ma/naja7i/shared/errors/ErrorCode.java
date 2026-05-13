package ma.naja7i.shared.errors;

import org.springframework.http.HttpStatus;

/**
 * Normalised, stable error codes used by every NAJA7I microservice.
 * Sync with docs/architecture/shared-contracts.md.
 */
public enum ErrorCode {

    // ---- AUTH
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    AUTH_OTP_EXPIRED(HttpStatus.UNAUTHORIZED),
    AUTH_OTP_INVALID(HttpStatus.UNAUTHORIZED),
    AUTH_USER_SUSPENDED(HttpStatus.FORBIDDEN),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED),
    AUTH_FORBIDDEN_ROLE(HttpStatus.FORBIDDEN),

    // ---- PROFILE
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND),
    PROFILE_PROF_NOT_VALIDATED(HttpStatus.FORBIDDEN),
    PROFILE_DIPLOMA_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY),
    PROFILE_QCM_FAILED(HttpStatus.UNPROCESSABLE_ENTITY),

    // ---- CALENDAR
    CALENDAR_SLOT_NOT_FOUND(HttpStatus.NOT_FOUND),
    CALENDAR_SLOT_CONFLICT(HttpStatus.CONFLICT),
    CALENDAR_TIMEZONE_INVALID(HttpStatus.BAD_REQUEST),

    // ---- BOOKING
    BOOKING_SLOT_NOT_AVAILABLE(HttpStatus.CONFLICT),
    BOOKING_INVALID_TRANSITION(HttpStatus.CONFLICT),
    BOOKING_TTL_EXPIRED(HttpStatus.GONE),
    BOOKING_CONFIRM_NOT_TERMINATED(HttpStatus.UNPROCESSABLE_ENTITY),

    // ---- PAYMENT
    PAYMENT_INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_ENTITY),
    PAYMENT_TOPUP_PROOF_REQUIRED(HttpStatus.BAD_REQUEST),
    PAYMENT_ESCROW_NOT_FOUND(HttpStatus.NOT_FOUND),
    PAYMENT_PAYOUT_NOT_ELIGIBLE(HttpStatus.UNPROCESSABLE_ENTITY),
    PAYMENT_NEGATIVE_BALANCE_BLOCKED(HttpStatus.UNPROCESSABLE_ENTITY),

    // ---- CHAT
    CHAT_CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND),
    CHAT_BYPASS_DETECTED(HttpStatus.UNPROCESSABLE_ENTITY),
    CHAT_ATTACHMENT_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE),

    // ---- VIDEO
    VIDEO_SESSION_NOT_LIVE(HttpStatus.CONFLICT),
    VIDEO_CONSENT_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY),

    // ---- AI
    AI_UPSTREAM_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT),
    AI_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS),

    // ---- COMMON
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String messageKey() {
        return "errors." + name().toLowerCase().replace('_', '.');
    }
}
