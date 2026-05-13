package ma.naja7i.profile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "prof_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfLocation {

    @EmbeddedId
    private ProfLocationId id;

    @Column(name = "region", length = 128)
    private String region;
}
