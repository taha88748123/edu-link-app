package ma.naja7i.video.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.security.UserContext;
import ma.naja7i.video.application.ArtifactService;
import ma.naja7i.video.domain.SessionArtifact;
import ma.naja7i.video.infrastructure.web.dto.ArtifactView;
import ma.naja7i.video.infrastructure.web.dto.CreateArtifactRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/video/sessions/{sessionId}/artifacts")
@RequiredArgsConstructor
@Tag(name = "Session Artifacts", description = "Whiteboard / quiz / note / code snapshots")
public class ArtifactController {

    private final ArtifactService artifactService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT','PROF','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Attach an artifact to this session")
    public ApiResponse<ArtifactView> create(@AuthUser UserContext ctx,
                                            @PathVariable("sessionId") UUID sessionId,
                                            @Valid @RequestBody CreateArtifactRequest req) {
        SessionArtifact a = artifactService.create(sessionId, ctx.userId(), ctx.role(),
                req.type(), req.content());
        return ApiResponse.of(ArtifactView.from(a));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','PROF','ADMIN')")
    @Operation(summary = "List artifacts for this session (chronological)")
    public ApiResponse<List<ArtifactView>> list(@AuthUser UserContext ctx,
                                                @PathVariable("sessionId") UUID sessionId) {
        List<SessionArtifact> rows = artifactService.list(sessionId, ctx.userId(), ctx.role());
        return ApiResponse.of(rows.stream().map(ArtifactView::from).toList());
    }
}
