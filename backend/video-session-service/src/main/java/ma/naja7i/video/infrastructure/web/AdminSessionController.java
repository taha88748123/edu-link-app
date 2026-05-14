package ma.naja7i.video.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.video.application.SessionService;
import ma.naja7i.video.domain.Session;
import ma.naja7i.video.domain.SessionStatus;
import ma.naja7i.video.infrastructure.web.dto.SessionView;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Admin-only session operations. Path prefix matches the SecurityConfig rule
 * (/api/v1/video/admin/**) so role-gating still applies even if a future
 * controller omits the @PreAuthorize annotation.
 */
@RestController
@RequestMapping("/api/v1/video/admin/sessions")
@RequiredArgsConstructor
@Tag(name = "Sessions — Admin", description = "Admin browse and lifecycle override")
public class AdminSessionController {

    private final SessionService sessionService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all sessions (paginated, filterable)")
    public ApiResponse<List<SessionView>> list(@RequestParam(required = false) SessionStatus status,
                                               @RequestParam(required = false) Instant from,
                                               @RequestParam(required = false) Instant to,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        Page<Session> result = sessionService.listForAdmin(status, from, to, page, size);
        return ApiResponse.of(
                result.map(SessionView::from).getContent(),
                new ApiResponse.PageMeta(result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @GetMapping("/{sessionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fetch any session (admin override)")
    public ApiResponse<SessionView> get(@PathVariable("sessionId") UUID sessionId) {
        return ApiResponse.of(SessionView.from(sessionService.get(sessionId)));
    }

    @PostMapping("/{sessionId}:expire")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Force a SCHEDULED session to EXPIRED")
    public ApiResponse<SessionView> expire(@PathVariable("sessionId") UUID sessionId) {
        Session s = sessionService.adminExpire(sessionId);
        return ApiResponse.of(SessionView.from(s));
    }
}
