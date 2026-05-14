package ma.naja7i.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-conversation unread counters. The two slots are dedicated to the two participants
 * of the conversation. Keep this simple structure rather than an open map to avoid
 * Mongo schema surprises when participants change role.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCounts {

    @Builder.Default
    private int profUnread = 0;

    @Builder.Default
    private int studentUnread = 0;

    public static UnreadCounts zero() {
        return UnreadCounts.builder().profUnread(0).studentUnread(0).build();
    }
}
