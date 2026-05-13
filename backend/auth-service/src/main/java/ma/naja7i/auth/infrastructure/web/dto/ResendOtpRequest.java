package ma.naja7i.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import ma.naja7i.auth.domain.OtpChannel;

import java.util.UUID;

public record ResendOtpRequest(
        @NotNull
        UUID userId,

        @NotNull
        OtpChannel channel
) {}
