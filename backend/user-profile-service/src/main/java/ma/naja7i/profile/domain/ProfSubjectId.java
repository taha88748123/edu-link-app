package ma.naja7i.profile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.naja7i.shared.dto.SchoolLevel;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProfSubjectId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "subject_code", nullable = false, length = 32)
    private String subjectCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private SchoolLevel level;
}
