package ma.naja7i.ai.application;

import ma.naja7i.ai.domain.MatchingFeatures;
import ma.naja7i.ai.infrastructure.persistence.MatchingFeaturesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileFeatureUpdaterTest {

    @Mock MatchingFeaturesRepository repository;

    ProfileFeatureUpdater updater;

    UUID profId;

    @BeforeEach
    void setup() {
        updater = new ProfileFeatureUpdater(repository);
        profId = UUID.randomUUID();
    }

    @Test
    void firstEvent_createsNewRow() {
        when(repository.findById(profId)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(MatchingFeatures.class))).thenAnswer(i -> i.getArgument(0));

        updater.onValidated(profId);

        ArgumentCaptor<MatchingFeatures> captor = ArgumentCaptor.forClass(MatchingFeatures.class);
        verify(repository).saveAndFlush(captor.capture());
        MatchingFeatures saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(profId);
        assertThat(saved.getRole()).isEqualTo("PROF");
        assertThat(saved.getFeatures()).isNotBlank();
        assertThat(saved.getComputedAt()).isNotNull();
    }

    @Test
    void secondEvent_sameUser_bumpsComputedAt() throws InterruptedException {
        Instant initial = Instant.now().minusSeconds(60);
        MatchingFeatures existing = MatchingFeatures.builder()
                .userId(profId)
                .role("PROF")
                .features("{\"subjects\":[]}")
                .computedAt(initial)
                .version(1)
                .build();
        when(repository.findById(profId)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(any(MatchingFeatures.class))).thenAnswer(i -> i.getArgument(0));

        updater.onValidated(profId);

        assertThat(existing.getComputedAt()).isAfter(initial);
        assertThat(existing.getRole()).isEqualTo("PROF");
        verify(repository).saveAndFlush(existing);
    }

    @Test
    void nullUserId_isIgnored() {
        updater.onValidated(null);
        verify(repository, never()).saveAndFlush(any(MatchingFeatures.class));
    }
}
