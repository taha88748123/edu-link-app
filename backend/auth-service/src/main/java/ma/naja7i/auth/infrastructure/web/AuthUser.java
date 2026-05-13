package ma.naja7i.auth.infrastructure.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation injecting the gateway-provided {@link ma.naja7i.shared.security.UserContext}
 * derived from {@code X-User-Id}, {@code X-User-Role}, {@code X-Locale}, {@code X-Trace-Id} headers.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthUser {
}
