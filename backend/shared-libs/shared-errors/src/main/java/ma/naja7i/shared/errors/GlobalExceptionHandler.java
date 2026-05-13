package ma.naja7i.shared.errors;

import ma.naja7i.shared.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Standard {@code @RestControllerAdvice} importable in any service via
 * {@code @Import(GlobalExceptionHandler.class)}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        log.warn("Business error: {} — {}", ex.code(), ex.getMessage());
        return build(ex.code(), ex.getMessage(), ex.details());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        return build(ErrorCode.VALIDATION_FAILED, "Validation failed", Map.of("fields", fieldErrors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(ErrorCode.INTERNAL_ERROR, "Internal error", Map.of());
    }

    private ResponseEntity<ApiResponse<Void>> build(ErrorCode code, String message, Map<String, Object> details) {
        String traceId = traceId();
        ApiResponse.ApiError error = new ApiResponse.ApiError(
                code.name(), message, code.messageKey(), details, traceId);
        return ResponseEntity.status(code.httpStatus()).body(ApiResponse.error(error));
    }

    private String traceId() {
        try {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                Object t = attrs.getAttribute("X-Trace-Id", RequestAttributes.SCOPE_REQUEST);
                if (t != null) return t.toString();
            }
        } catch (Exception ignored) { /* no request context */ }
        return UUID.randomUUID().toString();
    }
}
