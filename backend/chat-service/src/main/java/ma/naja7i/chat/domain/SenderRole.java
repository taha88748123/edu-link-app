package ma.naja7i.chat.domain;

/**
 * Restricted projection of {@link ma.naja7i.shared.dto.Role} : only the two parties
 * that may post into a chat conversation are valid sender roles.
 */
public enum SenderRole {
    PROF, STUDENT
}
