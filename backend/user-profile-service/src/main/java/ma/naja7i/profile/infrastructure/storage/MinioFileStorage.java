package ma.naja7i.profile.infrastructure.storage;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import ma.naja7i.profile.application.FileStorageService;
import ma.naja7i.profile.config.MinioProperties;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class MinioFileStorage implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioFileStorage.class);

    private final MinioClient client;
    private final MinioProperties props;

    public MinioFileStorage(MinioClient client, MinioProperties props) {
        this.client = client;
        this.props = props;
    }

    @Override
    public StoredObject upload(String prefix, String originalFilename, String contentType,
                               long size, InputStream content) {
        String safeName = sanitize(originalFilename);
        String objectKey = (prefix == null ? "misc" : prefix.replaceAll("/+$", ""))
                + "/" + UUID.randomUUID() + "-" + safeName;
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(props.bucket())
                    .object(objectKey)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .stream(content, size, -1)
                    .build());
            String publicUrl = presignGet(objectKey, Duration.ofSeconds(props.presignTtlSeconds()));
            return new StoredObject(objectKey, publicUrl);
        } catch (Exception e) {
            log.error("MinIO upload failed for {}: {}", objectKey, e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to upload file");
        }
    }

    @Override
    public String presignGet(String objectKey, Duration ttl) {
        try {
            int seconds = (int) Math.min(ttl.getSeconds(), 7 * 24 * 3600L);
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(props.bucket())
                    .object(objectKey)
                    .expiry(seconds, TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            log.warn("MinIO presign failed for {}: {}", objectKey, e.getMessage());
            return null;
        }
    }

    @Override
    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return;
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(props.bucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.warn("MinIO delete failed for {}: {}", objectKey, e.getMessage());
        }
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) return "file";
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
