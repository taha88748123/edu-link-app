package ma.naja7i.chat.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.naja7i.chat.application.PresenceService;
import ma.naja7i.chat.infrastructure.web.dto.PresenceView;
import ma.naja7i.shared.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat/presence")
@RequiredArgsConstructor
@Tag(name = "Chat — Presence", description = "Online indicator backed by Redis TTL keys")
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Whether the user is online and remaining TTL of the heartbeat key.")
    public ApiResponse<PresenceView> get(@PathVariable("userId") UUID userId) {
        boolean online = presenceService.isOnline(userId);
        Long ttl = presenceService.presenceTtlSeconds(userId);
        return ApiResponse.of(new PresenceView(userId, online, ttl));
    }

    @PostMapping("/heartbeat")
    @PreAuthorize("hasAnyRole('STUDENT','PROF','ADMIN')")
    @Operation(summary = "Heartbeat for the caller — refreshes their presence TTL.")
    public ApiResponse<PresenceView> heartbeat(@AuthUser ma.naja7i.shared.security.UserContext ctx) {
        presenceService.heartbeat(ctx.userId());
        Long ttl = presenceService.presenceTtlSeconds(ctx.userId());
        return ApiResponse.of(new PresenceView(ctx.userId(), true, ttl));
    }
}
