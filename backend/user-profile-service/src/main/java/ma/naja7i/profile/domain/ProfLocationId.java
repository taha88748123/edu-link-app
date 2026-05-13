package ma.naja7i.profile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProfLocationId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "city", nullable = false, length = 128)
    private String city;
}
