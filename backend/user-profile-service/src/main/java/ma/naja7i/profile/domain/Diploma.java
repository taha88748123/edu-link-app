package ma.naja7i.profile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prof_diplomas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Diploma {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 256)
    private String title;

    @Column(length = 256)
    private String institution;

    @Column(name = "year")
    private Integer year;

    @Column(name = "file_url", nullable = false, length = 1024)
    private String fileUrl;

    @Column(name = "object_key", nullable = false, length = 1024)
    private String objectKey;

    @Column(name = "ocr_text", columnDefinition = "text")
    private String ocrText;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
