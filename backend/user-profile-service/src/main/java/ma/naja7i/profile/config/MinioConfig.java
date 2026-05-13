package ma.naja7i.profile.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    private final MinioProperties props;

    public MinioConfig(MinioProperties props) {
        this.props = props;
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(props.endpoint())
                .credentials(props.accessKey(), props.secretKey())
                .build();
    }

    @PostConstruct
    public void ensureBucketExists() {
        try {
            MinioClient client = minioClient();
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(props.bucket()).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(props.bucket()).build());
                log.info("Created MinIO bucket '{}'", props.bucket());
            } else {
                log.info("MinIO bucket '{}' already exists", props.bucket());
            }
        } catch (Exception e) {
            // Do not crash boot if MinIO is unavailable; uploads will fail loudly later.
            log.warn("Could not ensure MinIO bucket '{}' (endpoint={}): {}",
                    props.bucket(), props.endpoint(), e.getMessage());
        }
    }
}
