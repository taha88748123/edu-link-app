package ma.naja7i.profile.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for MinIO (S3-compatible) storage.
 * Bound from {@code naja7i.minio.*}.
 */
@ConfigurationProperties(prefix = "naja7i.minio")
public record MinioProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        long presignTtlSeconds,
        long maxPhotoBytes,
        long maxDiplomaBytes,
        long maxVideoBytes
) {
    public MinioProperties {
        if (endpoint == null || endpoint.isBlank()) endpoint = "http://localhost:9000";
        if (accessKey == null || accessKey.isBlank()) accessKey = "minioadmin";
        if (secretKey == null || secretKey.isBlank()) secretKey = "minioadmin";
        if (bucket == null || bucket.isBlank()) bucket = "naja7i-profiles";
        if (presignTtlSeconds <= 0) presignTtlSeconds = 15 * 60L; // 15 minutes
        if (maxPhotoBytes <= 0) maxPhotoBytes = 10L * 1024 * 1024;       // 10 MB
        if (maxDiplomaBytes <= 0) maxDiplomaBytes = 25L * 1024 * 1024;   // 25 MB
        if (maxVideoBytes <= 0) maxVideoBytes = 100L * 1024 * 1024;      // 100 MB
    }
}
