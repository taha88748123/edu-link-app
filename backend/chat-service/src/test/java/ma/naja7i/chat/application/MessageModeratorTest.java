package ma.naja7i.chat.application;

import ma.naja7i.chat.domain.ConversationStatus;
import ma.naja7i.chat.domain.ModerationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Anti-bypass — the most security-critical unit test in chat-service.
 * Verifies that Moroccan phone numbers, emails, off-platform URLs and
 * suspicious keyword/digit combos are masked while the conversation is
 * still PRE_ACCEPT.
 */
class MessageModeratorTest {

    private final MessageModerator mod = new MessageModerator();

    @Test
    void preAccept_masksStrictMoroccanPhone() {
        var r = mod.moderate("call me +212612345678 plz", ConversationStatus.PRE_ACCEPT);
        assertThat(r.getStatus()).isEqualTo(ModerationStatus.MASKED);
        assertThat(r.getSanitizedBody()).doesNotContain("612345678");
        assertThat(r.getFlags()).contains("PHONE_PATTERN");
    }

    @Test
    void preAccept_masksSpacedPhone() {
        var r = mod.moderate("contact 06 12 34 56 78", ConversationStatus.PRE_ACCEPT);
        assertThat(r.getStatus()).isEqualTo(ModerationStatus.MASKED);
        assertThat(r.getSanitizedBody()).doesNotContain("06 12 34");
        assertThat(r.getFlags()).contains("PHONE_PATTERN");
    }

    @Test
    void preAccept_masksDottedPhone() {
        var r = mod.moderate("06.12.34.56.78 ok ?", ConversationStatus.PRE_ACCEPT);
        assertThat(r.getStatus()).isEqualTo(ModerationStatus.MASKED);
        assertThat(r.getSanitizedBody()).doesNotContain("06.12.34");
    }

    @Test
    void preAccept_masksEmail() {
        var r = mod.moderate("ecris-moi a foo.bar@gmail.com merci", ConversationStatus.PRE_ACCEPT);
        assertThat(r.getStatus()).isEqualTo(ModerationStatus.MASKED);
        assertThat(r.getSanitizedBody()).doesNotContain("foo.bar@gmail.com");
        assertThat(r.getFlags()).contains("EMAIL_PATTERN");
    }

    @Test
    void preAccept_masksWhatsAppUrl() {
        var r = mod.moderate("https://wa.me/+212612345678", ConversationStatus.PRE_ACCEPT);
        assertThat(r.getStatus()).isEqualTo(ModerationStatus.MASKED);
        assertThat(r.getSanitizedBody()).doesNotContain("wa.me");
    }

    @Test
    void preAccept_benignText_isCleanAndPassesThrough() {
        var r = mod.moderate("Bonjour, à quelle heure ce soir ?", ConversationStatus.PRE_ACCEPT);
        assertThat(r.getStatus()).isEqualTo(ModerationStatus.CLEAN);
        assertThat(r.getSanitizedBody()).isEqualTo("Bonjour, à quelle heure ce soir ?");
        assertThat(r.getFlags()).isEmpty();
    }

    @Test
    void postAccept_neverMasks() {
        var r = mod.moderate("mon numéro c'est 0612345678", ConversationStatus.POST_ACCEPT);
        assertThat(r.getStatus()).isEqualTo(ModerationStatus.CLEAN);
        assertThat(r.getSanitizedBody()).contains("0612345678");
    }

    @Test
    void preAccept_multiplePatterns_flagsAllPlusReturnsMaskedBody() {
        var r = mod.moderate("06 12 34 56 78 ou foo@bar.com", ConversationStatus.PRE_ACCEPT);
        assertThat(r.getStatus()).isEqualTo(ModerationStatus.MASKED);
        assertThat(r.getFlags()).contains("PHONE_PATTERN", "EMAIL_PATTERN");
    }
}
