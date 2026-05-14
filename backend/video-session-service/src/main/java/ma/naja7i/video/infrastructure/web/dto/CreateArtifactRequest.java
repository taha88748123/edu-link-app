package ma.naja7i.video.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ma.naja7i.video.domain.ArtifactType;

/**
 * Body of {@code POST /api/v1/video/sessions/{id}/artifacts}. {@code content}
 * is an opaque JSON blob (already-stringified by the producer / frontend).
 */
public record CreateArtifactRequest(
        @NotNull ArtifactType type,
        @NotBlank String content
) {
}
