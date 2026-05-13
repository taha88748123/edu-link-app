package ma.naja7i.profile.application;

import java.io.InputStream;
import java.time.Duration;

/**
 * Abstraction over object storage. Implementations delegate to MinIO/S3.
 */
public interface FileStorageService {

    /**
     * Upload an object and return its (logical) object key.
     */
    StoredObject upload(String prefix,
                        String originalFilename,
                        String contentType,
                        long size,
                        InputStream content);

    /**
     * Generate a pre-signed GET URL for the given object key.
     */
    String presignGet(String objectKey, Duration ttl);

    /**
     * Delete an object — best-effort.
     */
    void delete(String objectKey);

    record StoredObject(String objectKey, String publicUrl) {}
}
