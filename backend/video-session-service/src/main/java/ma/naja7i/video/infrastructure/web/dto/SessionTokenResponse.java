package ma.naja7i.video.infrastructure.web.dto;

import ma.naja7i.video.livekit.LiveKitTokenIssuer;

import java.time.Instant;

/**
 * Response of {@code POST /api/v1/video/sessions/{id}/token}. The frontend
 * passes {@code token} and {@code livekitUrl} to the LiveKit client SDK.
 */
public record SessionTokenResponse(String token, String livekitUrl, String roomName, Instant expiresAt) {

    public static SessionTokenResponse from(LiveKitTokenIssuer.Token t) {
        return new SessionTokenResponse(t.token(), t.livekitUrl(), t.roomName(), t.expiresAt());
    }
}
