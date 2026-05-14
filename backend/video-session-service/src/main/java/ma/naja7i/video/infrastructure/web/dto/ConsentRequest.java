package ma.naja7i.video.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import ma.naja7i.video.domain.ConsentScope;

/** Body of {@code POST /api/v1/video/sessions/{id}/consent}. */
public record ConsentRequest(@NotNull ConsentScope scope) {
}
