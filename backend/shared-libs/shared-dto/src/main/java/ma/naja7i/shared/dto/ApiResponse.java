package ma.naja7i.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(T data, PageMeta meta, ApiError error) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, null, null);
    }

    public static <T> ApiResponse<T> of(T data, PageMeta meta) {
        return new ApiResponse<>(data, meta, null);
    }

    public static <T> ApiResponse<T> error(ApiError error) {
        return new ApiResponse<>(null, null, error);
    }

    public record PageMeta(int page, int size, long total) {}

    public record ApiError(String code, String message, String messageKey, Object details, String traceId) {}
}
