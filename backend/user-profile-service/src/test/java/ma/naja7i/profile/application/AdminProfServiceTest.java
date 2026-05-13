package ma.naja7i.profile.application;

import ma.naja7i.profile.domain.ProfProfile;
import ma.naja7i.profile.infrastructure.messaging.KafkaPublisher;
import ma.naja7i.profile.infrastructure.persistence.ProfProfileRepository;
import ma.naja7i.shared.dto.ProfStatus;
import ma.naja7i.shared.errors.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminProfServiceTest {

    private ProfProfileRepository profProfileRepository;
    private KafkaPublisher kafkaPublisher;
    private AdminProfService service;

    @BeforeEach
    void setUp() {
        profProfileRepository = mock(ProfProfileRepository.class);
        kafkaPublisher = mock(KafkaPublisher.class);
        service = new AdminProfService(profProfileRepository, kafkaPublisher);
    }

    @Test
    void validate_transitionsToValideAndPublishes() {
        UUID profId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        ProfProfile pp = ProfProfile.builder().userId(profId).status(ProfStatus.EN_ATTENTE).build();
        when(profProfileRepository.findById(profId)).thenReturn(Optional.of(pp));
        when(profProfileRepository.save(any(ProfProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfProfile result = service.validate(profId, adminId, "ok");

        assertThat(result.getStatus()).isEqualTo(ProfStatus.VALIDE);
        assertThat(result.getValidatedAt()).isNotNull();
        assertThat(result.getValidatedBy()).isEqualTo(adminId);
        verify(kafkaPublisher, times(1)).publish(anyString(), eq(profId.toString()), any());
    }

    @Test
    void validate_rejectsAlreadyValidated() {
        UUID profId = UUID.randomUUID();
        ProfProfile pp = ProfProfile.builder().userId(profId).status(ProfStatus.VALIDE).build();
        when(profProfileRepository.findById(profId)).thenReturn(Optional.of(pp));

        assertThatThrownBy(() -> service.validate(profId, UUID.randomUUID(), null))
                .isInstanceOf(BusinessException.class);

        verify(kafkaPublisher, never()).publish(anyString(), anyString(), any());
    }

    @Test
    void reject_transitionsToRejeteAndPublishes() {
        UUID profId = UUID.randomUUID();
        ProfProfile pp = ProfProfile.builder().userId(profId).status(ProfStatus.EN_ATTENTE).build();
        when(profProfileRepository.findById(profId)).thenReturn(Optional.of(pp));
        when(profProfileRepository.save(any(ProfProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfProfile result = service.reject(profId, UUID.randomUUID(), "not enough experience");

        assertThat(result.getStatus()).isEqualTo(ProfStatus.REJETE);
        assertThat(result.getAdminNotes()).contains("experience");
        verify(kafkaPublisher, times(1)).publish(anyString(), eq(profId.toString()), any());
    }
}
