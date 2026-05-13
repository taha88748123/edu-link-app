package ma.naja7i.profile.config;

import ma.naja7i.shared.errors.GlobalExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Imports the shared {@link GlobalExceptionHandler} so business exceptions are mapped to
 * the uniform {@code ApiResponse} envelope.
 */
@Configuration
@Import(GlobalExceptionHandler.class)
public class ErrorHandlerImport {
}
