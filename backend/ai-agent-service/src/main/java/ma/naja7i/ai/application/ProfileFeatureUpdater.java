package ma.naja7i.ai.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.ai.domain.MatchingFeatures;
import ma.naja7i.ai.infrastructure.persistence.MatchingFeaturesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Reacts to {@code naja7i.profile.prof.validated.v1} by upserting a
 * minimal PROF feature row. We do NOT fetch the prof's full profile yet
 * (v2 will recompute features from user-profile-service), but the row's
 * mere presence enables MATCHING to discover the prof.
 */
@Component
@RequiredArgsConstructor
public class ProfileFeatureUpdater {

    private static final Logger log = LoggerFactory.getLogger(ProfileFeatureUpdater.class);

    /** Empty feature blob used on first insert — recomputed in v2. */
    private static final String EMPTY_PROF_FEATURES =
            "{\"subjects\":[],\"levels\":[],\"modalities\":[],\"rating\":0,\"avg_price_minor\":0,\"city\":\"\"}";

    private final MatchingFeaturesRepository repository;

    @Transactional
    public void onValidated(UUID userId) {
        if (userId == null) {
            log.warn("onValidated called with null userId — ignoring");
            return;
        }
        Optional<MatchingFeatures> existing = repository.findById(userId);
        if (existing.isPresent()) {
            MatchingFeatures row = existing.get();
            row.setComputedAt(Instant.now());
            // Force the role to PROF in case it was previously a STUDENT row
            // (defensive — should never happen in practice).
            row.setRole("PROF");
            repository.saveAndFlush(row);
            log.debug("Refreshed matching_features.computed_at for prof {}", userId);
            return;
        }
        MatchingFeatures fresh = MatchingFeatures.builder()
                .userId(userId)
                .role("PROF")
                .features(EMPTY_PROF_FEATURES)
                .computedAt(Instant.now())
                .version(0)
                .build();
        repository.saveAndFlush(fresh);
        log.info("Created matching_features row for newly-validated prof {}", userId);
    }
}
