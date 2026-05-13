package ma.naja7i.auth.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.auth.application.AuthService;
import ma.naja7i.auth.infrastructure.web.dto.SuspendRequest;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.security.UserContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-only auth operations")
public class AdminController {

    private final AuthService authService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/{id}/suspend")
    @Operation(summary = "Suspend a user account (ADMIN only)")
    public ApiResponse<Void> suspend(
            @PathVariable("id") UUID id,
            @Valid @RequestBody(required = false) SuspendRequest body,
            @AuthUser UserContext me,
            HttpServletRequest request) {
        String reason = body == null ? null : body.reason();
        authService.suspend(
                id,
                reason,
                me.userId(),
                RequestMetadata.clientIp(request),
                RequestMetadata.userAgent(request));
        return ApiResponse.<Void>of(null);
    }
}
