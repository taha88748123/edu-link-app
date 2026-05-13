package ma.naja7i.auth.events.payload;

import ma.naja7i.auth.domain.OtpChannel;

import java.time.Instant;
import java.util.UUID;

public record OtpRequestedPayload(
        UUID userId,
        String phone,
        String code,
        OtpChannel channel,
        Instant expiresAt,
        String locale
) {}
