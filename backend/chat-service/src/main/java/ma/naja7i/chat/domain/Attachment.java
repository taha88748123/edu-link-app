package ma.naja7i.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reference to an external object (MinIO-hosted URL is stored, never the binary itself).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {

    private String url;
    private String mime;
    private long sizeBytes;
}
