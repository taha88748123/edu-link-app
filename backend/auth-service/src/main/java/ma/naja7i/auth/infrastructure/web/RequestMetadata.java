package ma.naja7i.auth.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;

final class RequestMetadata {

    private RequestMetadata() {}

    static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        return request.getRemoteAddr();
    }

    static String userAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua == null ? "unknown" : ua;
    }
}
