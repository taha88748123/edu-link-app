package ma.naja7i.notification.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import ma.naja7i.notification.config.NotificationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Inspects {@code X-Internal-Key} and stores a boolean attribute the controllers
 * use to authorise service-to-service calls without a user JWT.
 */
@Component
@Slf4j
public class InternalKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Key";
    public static final String ATTR_INTERNAL = "naja7i.internalCall";

    private final NotificationProperties props;

    public InternalKeyFilter(NotificationProperties props) {
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String supplied = req.getHeader(HEADER);
        if (supplied != null && supplied.equals(props.getInternalKey())) {
            req.setAttribute(ATTR_INTERNAL, true);
        }
        chain.doFilter(req, res);
    }
}
