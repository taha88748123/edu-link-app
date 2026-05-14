package ma.naja7i.chat.application;

import ma.naja7i.chat.domain.ConversationStatus;
import ma.naja7i.chat.domain.ModerationStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Synchronous regex-based moderator. Applied to any user-typed message while the
 * surrounding conversation is in {@link ConversationStatus#PRE_ACCEPT}.
 *
 * <p><b>Patterns recognised</b> (case-insensitive where applicable) :
 * <ul>
 *   <li>Moroccan phone numbers (strict {@code +212...} / {@code 0[5-7]...}, and the
 *       loose spaced/dotted variants seen in chat).</li>
 *   <li>Email addresses.</li>
 *   <li>URLs to known off-platform channels (wa.me, t.me, instagram.com).</li>
 *   <li>Suspicious keywords (whatsapp, instagram, telegram, gmail) — only flagged
 *       when combined with a numeric or {@code @} sequence in the same message.</li>
 * </ul>
 *
 * <p>The component is deterministic and stateless; it does not call any external
 * service. The async {@link BypassAnalyzer} runs after persistence.
 */
@Component
public class MessageModerator {

    /** Strict Moroccan numbers: +212 5/6/7 + 8 digits, or 0 5/6/7 + 8 digits. */
    private static final Pattern PHONE_STRICT = Pattern.compile(
            "(?:(?:\\+|00)212|0)\\s?[5-7](?:[\\s.-]?\\d){8}");

    /** Loose spaced / dotted Moroccan number (e.g. {@code 06 12 34 56 78}, {@code 06.12.34.56.78}). */
    private static final Pattern PHONE_LOOSE = Pattern.compile(
            "0[5-7](?:[\\s.-]?\\d{2}){4}");

    /** Email — basic but greedy enough for chat-typed values. */
    private static final Pattern EMAIL = Pattern.compile(
            "[\\w.+-]+@[\\w-]+\\.[\\w.-]+");

    /** Known off-platform handle URLs. */
    private static final Pattern WHATSAPP_URL = Pattern.compile(
            "(?i)(?:https?://)?wa\\.me/\\+?\\d{6,15}");

    private static final Pattern TELEGRAM_URL = Pattern.compile(
            "(?i)(?:https?://)?t\\.me/[\\w._-]+");

    private static final Pattern INSTAGRAM_URL = Pattern.compile(
            "(?i)(?:https?://)?(?:www\\.)?instagram\\.com/[\\w._-]+");

    /** Words that, combined with a number or '@', are suspicious. */
    private static final Pattern SUSPICIOUS_WORDS = Pattern.compile(
            "(?i)\\b(whats?app|insta(?:gram)?|telegram|tele|gmail|outlook|hotmail|yahoo)\\b");

    /** Any 6+ digit sequence — used as a corroborator for suspicious keywords. */
    private static final Pattern DIGIT_RUN = Pattern.compile("\\d{6,}");

    /**
     * Apply the moderation pipeline.
     *
     * @param body              message body as typed
     * @param conversationStatus current conversation status
     * @return result with sanitized body + flags
     */
    public ModeratorResult moderate(String body, ConversationStatus conversationStatus) {
        if (body == null) {
            return ModeratorResult.clean("");
        }
        // POST_ACCEPT / CLOSED — no masking, deliver as-is.
        if (conversationStatus != ConversationStatus.PRE_ACCEPT) {
            return ModeratorResult.clean(body);
        }

        String working = body;
        Set<String> flags = new LinkedHashSet<>();
        List<String> masked = new ArrayList<>();

        // 1. URLs first (otherwise the embedded numbers in wa.me would be matched as phones).
        working = maskAll(working, WHATSAPP_URL, "***", "WHATSAPP_URL", flags, masked);
        working = maskAll(working, TELEGRAM_URL, "***", "TELEGRAM_URL", flags, masked);
        working = maskAll(working, INSTAGRAM_URL, "***", "INSTAGRAM_URL", flags, masked);

        // 2. Email
        working = maskAll(working, EMAIL, "***@***.***", "EMAIL_PATTERN", flags, masked);

        // 3. Phones — strict first, then loose; both replaced with "***".
        working = maskAll(working, PHONE_STRICT, "***", "PHONE_PATTERN", flags, masked);
        working = maskAll(working, PHONE_LOOSE, "***", "PHONE_PATTERN", flags, masked);

        // 4. Suspicious keyword combined with a long digit run -> flag (but do not mask
        //    the word itself; the digits will already have been masked above if Moroccan).
        if (SUSPICIOUS_WORDS.matcher(body).find() && DIGIT_RUN.matcher(body).find()) {
            flags.add("SUSPICIOUS_KEYWORD_WITH_DIGITS");
        }

        ModerationStatus status = flags.isEmpty() ? ModerationStatus.CLEAN : ModerationStatus.MASKED;
        return ModeratorResult.builder()
                .sanitizedBody(working)
                .originalBody(body)
                .status(status)
                .flags(new ArrayList<>(flags))
                .maskedFields(masked)
                .build();
    }

    private String maskAll(String text, Pattern pattern, String replacement,
                           String flag, Set<String> flags, List<String> masked) {
        Matcher m = pattern.matcher(text);
        if (!m.find()) return text;
        StringBuilder sb = new StringBuilder(text.length());
        int last = 0;
        // First match already located by .find().
        do {
            sb.append(text, last, m.start());
            sb.append(replacement);
            // Persist a redacted echo so admins know SOMETHING was scrubbed (length only).
            masked.add(redactedToken(m.group()));
            last = m.end();
        } while (m.find());
        sb.append(text, last, text.length());
        flags.add(flag);
        return sb.toString();
    }

    /**
     * Renders a "shape-only" redacted token: keep the first 2 chars then asterisks
     * (e.g. {@code "06****"} for a phone, {@code "fo***"} for an email local-part).
     * This lets admins reason about hit shape without storing the raw PII in
     * {@code maskedFields}.
     */
    private static String redactedToken(String original) {
        if (original == null) return "***";
        if (original.length() <= 2) return "***";
        return original.substring(0, 2) + "***";
    }
}
