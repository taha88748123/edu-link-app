package ma.naja7i.profile.infrastructure.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for controller parameters of type
 * {@link ma.naja7i.shared.security.UserContext}, injected from the gateway-provided headers.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface AuthUser { }
