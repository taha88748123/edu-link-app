package ma.naja7i.video.livekit;

import java.time.Instant;
import java.util.UUID;

/**
 * Mints LiveKit room-grant tokens. The contract is intentionally narrow: callers
 * (the session service) supply user / room / role / TTL and we hand back an
 * opaque token string that the frontend forwards to a real LiveKit deployment.
 *
 * <p>Two implementations exist:
 * <ul>
 *   <li>{@link JjwtLiveKitTokenIssuer}    — real HS256 JWT compatible with LiveKit.</li>
 *   <li>{@link LoggingLiveKitTokenIssuer} — stub returning a fake token; useful for tests.</li>
 * </ul>
 */
public interface LiveKitTokenIssuer {

    /**
     * @param userId       canonical user UUID (becomes {@code sub})
     * @param roomName     LiveKit room name (must match the session row)
     * @param roleLabel    PII-free participant label (e.g. {@code "PROF"} / {@code "STUDENT"})
     * @param canPublish   true if the user may publish A/V tracks
     * @param canSubscribe true if the user may subscribe to others' tracks
     * @param expiresAt    JWT exp (also LiveKit grant TTL)
     * @return opaque token to be embedded in the LiveKit connect URL
     */
    Token mint(UUID userId,
               String roomName,
               String roleLabel,
               boolean canPublish,
               boolean canSubscribe,
               Instant expiresAt);

    /** Issued token + the LiveKit URL the frontend should connect to. */
    record Token(String token, String livekitUrl, String roomName, Instant expiresAt) {
    }
}
