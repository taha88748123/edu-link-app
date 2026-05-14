package ma.naja7i.video.domain;

/**
 * Artifact categories that can be attached to a video session. Stored as
 * VARCHAR in DB and validated by a CHECK constraint.
 */
public enum ArtifactType {
    WHITEBOARD,
    QUIZ,
    NOTE,
    CODE
}
