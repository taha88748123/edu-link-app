package ma.naja7i.video.domain;

/**
 * Consent scopes that a participant may grant for a session. RECORDING enables
 * persistent capture; TRANSCRIPT enables speech-to-text persistence.
 */
public enum ConsentScope {
    RECORDING,
    TRANSCRIPT
}
