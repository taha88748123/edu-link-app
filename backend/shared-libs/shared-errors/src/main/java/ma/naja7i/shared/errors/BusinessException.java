package ma.naja7i.shared.errors;

import java.util.Map;

/**
 * Base exception thrown by business rules. Always carries a normalised {@link ErrorCode}.
 * Use the static factory methods to attach contextual details.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, Object> details;

    public BusinessException(ErrorCode code) {
        this(code, code.name(), Map.of());
    }

    public BusinessException(ErrorCode code, String message) {
        this(code, message, Map.of());
    }

    public BusinessException(ErrorCode code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public ErrorCode code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }

    public static BusinessException of(ErrorCode code, Map<String, Object> details) {
        return new BusinessException(code, code.name(), details);
    }
}
