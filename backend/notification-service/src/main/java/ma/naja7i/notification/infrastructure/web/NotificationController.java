package ma.naja7i.notification.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ma.naja7i.notification.application.DispatchRequest;
import ma.naja7i.notification.application.NotificationDispatcher;
import ma.naja7i.notification.domain.Channel;
import ma.naja7i.notification.domain.Notification;
import ma.naja7i.notification.domain.NotificationStatus;
import ma.naja7i.notification.infrastructure.persistence.NotificationRepository;
import ma.naja7i.notification.infrastructure.web.dto.NotificationView;
import ma.naja7i.notification.infrastructure.web.dto.SendNotificationRequest;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public surface of the notification-service. Most traffic happens via Kafka — these
 * three endpoints exist for : (1) ad-hoc internal sends, (2) end-user history,
 * (3) admin auditing.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "notifications", description = "Multi-channel notification orchestration")
public class NotificationController {

    private final NotificationDispatcher dispatcher;
    private final NotificationRepository repo;

    public NotificationController(NotificationDispatcher dispatcher, NotificationRepository repo) {
        this.dispatcher = dispatcher;
        this.repo = repo;
    }

    @Operation(summary = "Send a notification directly (internal / admin only)")
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<List<NotificationView>>> send(
            @Valid @RequestBody SendNotificationRequest body,
            HttpServletRequest req) {
        requireInternalOrAdmin(req);
        List<Notification> created = dispatcher.dispatch(DispatchRequest.builder()
                .userId(body.userId())
                .channels(body.channels())
                .templateKey(body.templateKey())
                .locale(body.locale() == null ? "fr" : body.locale())
                .params(body.params())
                .build());
        return ResponseEntity.ok(ApiResponse.of(created.stream().map(NotificationView::from).toList()));
    }

    @Operation(summary = "Caller's recent notifications (paginated)")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<NotificationView>>> mine(
            HttpServletRequest req,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserContext ctx = requireUser(req);
        Page<Notification> p = repo.findByUserIdOrderByCreatedAtDesc(ctx.userId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.of(
                p.getContent().stream().map(NotificationView::from).toList(),
                new ApiResponse.PageMeta(page, size, p.getTotalElements())));
    }

    @Operation(summary = "Admin : filterable notification log")
    @GetMapping("/admin/log")
    public ResponseEntity<ApiResponse<List<NotificationView>>> adminLog(
            HttpServletRequest req,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Channel channel,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        requireAdmin(req);
        Page<Notification> p = repo.search(userId, channel, status, from, to, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.of(
                p.getContent().stream().map(NotificationView::from).toList(),
                new ApiResponse.PageMeta(page, size, p.getTotalElements())));
    }

    // --- guards ---------------------------------------------------------

    private static UserContext requireUser(HttpServletRequest req) {
        Object ctx = req.getAttribute(HeaderAuthFilter.ATTR_USER_CONTEXT);
        if (ctx instanceof UserContext uc) return uc;
        throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "Missing user context headers");
    }

    private static void requireAdmin(HttpServletRequest req) {
        UserContext uc = requireUser(req);
        if (uc.role() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE, "Admin role required");
        }
    }

    private static void requireInternalOrAdmin(HttpServletRequest req) {
        Boolean internal = (Boolean) req.getAttribute(InternalKeyFilter.ATTR_INTERNAL);
        if (Boolean.TRUE.equals(internal)) return;
        Object ctx = req.getAttribute(HeaderAuthFilter.ATTR_USER_CONTEXT);
        if (ctx instanceof UserContext uc && uc.role() == Role.ADMIN) return;
        throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE,
                "Endpoint requires X-Internal-Key or ADMIN role");
    }
}
