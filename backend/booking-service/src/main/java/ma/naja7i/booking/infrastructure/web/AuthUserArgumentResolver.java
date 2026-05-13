package ma.naja7i.booking.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.security.JwtClaims;
import ma.naja7i.shared.security.UserContext;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

@Component
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUser.class)
                && UserContext.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "No request bound");
        }
        String userIdHeader = request.getHeader(JwtClaims.HEADER_USER_ID);
        String roleHeader   = request.getHeader(JwtClaims.HEADER_USER_ROLE);
        if (userIdHeader == null || roleHeader == null) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID,
                    "Missing X-User-Id / X-User-Role headers from gateway");
        }
        UUID userId;
        try {
            userId = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "Invalid X-User-Id header");
        }
        Role role;
        try {
            role = Role.valueOf(roleHeader);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE, "Unknown role: " + roleHeader);
        }
        String locale = request.getHeader(JwtClaims.HEADER_LOCALE);
        String traceId = request.getHeader(JwtClaims.HEADER_TRACE_ID);
        return new UserContext(userId, role, locale == null ? "fr" : locale,
                traceId == null ? UUID.randomUUID().toString() : traceId);
    }
}
