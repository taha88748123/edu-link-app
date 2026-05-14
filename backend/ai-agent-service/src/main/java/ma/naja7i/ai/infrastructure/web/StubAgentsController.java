package ma.naja7i.ai.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ma.naja7i.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The five non-MATCHING agents are scoped for v2. We expose stable URIs
 * that return {@code 501 NOT_IMPLEMENTED} so frontend clients can wire up
 * the integration today and switch to the real handlers transparently.
 */
@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "Stubs (v2)", description = "Future AI agents — return HTTP 501 in v1")
public class StubAgentsController {

    @PostMapping("/qcm/generate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate a QCM (v2 — returns 501)")
    public ResponseEntity<ApiResponse<Void>> qcmGenerate() {
        return notImplemented("QCM");
    }

    @PostMapping("/moderation/analyse")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Moderate a chat message (v2 — returns 501)")
    public ResponseEntity<ApiResponse<Void>> moderationAnalyse() {
        return notImplemented("MODERATION");
    }

    @PostMapping("/smart-reply/suggest")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Suggest a smart reply (v2 — returns 501)")
    public ResponseEntity<ApiResponse<Void>> smartReplySuggest() {
        return notImplemented("SMART_REPLY");
    }

    @PostMapping("/revision/generate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate a revision summary (v2 — returns 501)")
    public ResponseEntity<ApiResponse<Void>> revisionGenerate() {
        return notImplemented("REVISION");
    }

    @PostMapping("/tutor/chat")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Free-form tutoring chat (v2 — returns 501)")
    public ResponseEntity<ApiResponse<Void>> tutorChat() {
        return notImplemented("TUTOR");
    }

    private static ResponseEntity<ApiResponse<Void>> notImplemented(String agent) {
        ApiResponse.ApiError error = new ApiResponse.ApiError(
                "NOT_IMPLEMENTED",
                "Agent " + agent + " is scoped for v2",
                "errors.ai.not_implemented",
                null,
                null);
        return ResponseEntity.status(501).body(ApiResponse.error(error));
    }
}
