package ma.naja7i.video.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.security.UserContext;
import ma.naja7i.video.application.SessionService;
import ma.naja7i.video.domain.Session;
import ma.naja7i.video.domain.SessionStatus;
import ma.naja7i.video.infrastructure.web.dto.ConsentRequest;
import ma.naja7i.video.infrastructure.web.dto.SessionTokenResponse;
import ma.naja7i.video.infrastructure.web.dto.SessionView;
import ma.naja7i.video.livekit.LiveKitTokenIssuer;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Participant-facing video session endpoints. Strict participant / admin
 * checks are enforced inside {@link SessionService}; this controller only
 * does role-gating ({@code STUDENT / PROF / ADMIN}) and DTO mapping.
 */
@RestController
@RequestMapping("/api/v1/video/sessions")
@RequiredArgsConstructor
@Tag(name = "Sessions", description = "Video session lifecycle, tokens, consent")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/{sessionId}/token")
    @PreAuthorize("hasAnyRole('STUDENT','PROF','ADMIN')")
    @Operation(summary = "Issue a LiveKit JWT for the authenticated caller")
    public ApiResponse<SessionTokenResponse> issueToken(@AuthUser UserContext ctx,
                                                        @PathVariable("sessionId") UUID sessionId) {
        LiveKitTokenIssuer.Token token = sessionService.issueToken(sessionId, ctx.userId(), ctx.role());
        return ApiResponse.of(SessionTokenResponse.from(token));
    }

    @PostMapping("/{sessionId}:start")
    @PreAuthorize("hasAnyRole('STUDENT','PROF','ADMIN')")
    @Operation(summary = "Transition a SCHEDULED session to LIVE")
    public ApiResponse<SessionView> start(@AuthUser UserContext ctx,
                                          @PathVariable("sessionId") UUID sessionId) {
        Session s = sessionService.start(sessionId, ctx.userId(), ctx.role());
        return ApiResponse.of(SessionView.from(s));
    }

    @PostMapping("/{sessionId}:end")
    @PreAuthorize("hasAnyRole('STUDENT','PROF','ADMIN')")
    @Operation(summary = "Transition a LIVE session to ENDED")
    public ApiResponse<SessionView> end(@AuthUser UserContext ctx,
                                        @PathVariable("sessionId") UUID sessionId) {
        Session s = sessionService.end(sessionId, ctx.userId(), ctx.role());
        return ApiResponse.of(SessionView.from(s));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT','PROF','ADMIN')")
    @Operation(summary = "List my sessions (paginated)")
    public ApiResponse<List<SessionView>> mySessions(@AuthUser UserContext ctx,
                                                     @RequestParam(required = false) SessionStatus status,
                                                     @RequestParam(required = false) Instant from,
                                                     @RequestParam(required = false) Instant to,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        Page<Session> result = sessionService.listForCaller(ctx.userId(), ctx.role(),
                status, from, to, page, size);
        return ApiResponse.of(
                result.map(SessionView::from).getContent(),
                new ApiResponse.PageMeta(result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @GetMapping("/{sessionId}")
    @PreAuthorize("hasAnyRole('STUDENT','PROF','ADMIN')")
    @Operation(summary = "Fetch a single session I have access to")
    public ApiResponse<SessionView> get(@AuthUser UserContext ctx,
                                        @PathVariable("sessionId") UUID sessionId) {
        Session s = sessionService.getOwned(sessionId, ctx.userId(), ctx.role());
        return ApiResponse.of(SessionView.from(s));
    }

    @PostMapping("/{sessionId}/consent")
    @PreAuthorize("hasAnyRole('STUDENT','PROF','ADMIN')")
    @Operation(summary = "Grant a consent (e.g. RECORDING) for this session")
    public ApiResponse<SessionView> grantConsent(@AuthUser UserContext ctx,
                                                 @PathVariable("sessionId") UUID sessionId,
                                                 @Valid @RequestBody ConsentRequest req) {
        Session s = sessionService.grantConsent(sessionId, ctx.userId(), ctx.role(), req.scope());
        return ApiResponse.of(SessionView.from(s));
    }
}
