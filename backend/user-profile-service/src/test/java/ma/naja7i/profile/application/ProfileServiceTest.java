package ma.naja7i.profile.application;

import ma.naja7i.profile.domain.Profile;
import ma.naja7i.profile.domain.ProfProfile;
import ma.naja7i.profile.infrastructure.messaging.KafkaPublisher;
import ma.naja7i.profile.infrastructure.persistence.DiplomaRepository;
import ma.naja7i.profile.infrastructure.persistence.ProfLocationRepository;
import ma.naja7i.profile.infrastructure.persistence.ProfModalityRepository;
import ma.naja7i.profile.infrastructure.persistence.ProfProfileRepository;
import ma.naja7i.profile.infrastructure.persistence.ProfSubjectRepository;
import ma.naja7i.profile.infrastructure.persistence.ProfileRepository;
import ma.naja7i.profile.infrastructure.persistence.StudentInterestRepository;
import ma.naja7i.profile.infrastructure.persistence.StudentProfileRepository;
import ma.naja7i.profile.infrastructure.web.dto.UpsertProfProfileRequest;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.ProfStatus;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.dto.SchoolLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileServiceTest {

    private ProfileRepository profileRepository;
    private ProfProfileRepository profProfileRepository;
    private ProfSubjectRepository profSubjectRepository;
    private ProfModalityRepository profModalityRepository;
    private ProfLocationRepository profLocationRepository;
    private StudentProfileRepository studentProfileRepository;
    private StudentInterestRepository studentInterestRepository;
    private DiplomaRepository diplomaRepository;
    private KafkaPublisher kafkaPublisher;

    private ProfileService service;

    @BeforeEach
    void setUp() {
        profileRepository = mock(ProfileRepository.class);
        profProfileRepository = mock(ProfProfileRepository.class);
        profSubjectRepository = mock(ProfSubjectRepository.class);
        profModalityRepository = mock(ProfModalityRepository.class);
        profLocationRepository = mock(ProfLocationRepository.class);
        studentProfileRepository = mock(StudentProfileRepository.class);
        studentInterestRepository = mock(StudentInterestRepository.class);
        diplomaRepository = mock(DiplomaRepository.class);
        kafkaPublisher = mock(KafkaPublisher.class);

        service = new ProfileService(
                profileRepository, profProfileRepository, profSubjectRepository,
                profModalityRepository, profLocationRepository, studentProfileRepository,
                studentInterestRepository, diplomaRepository, kafkaPublisher);
    }

    @Test
    void ensureProfileExists_createsWhenAbsent() {
        UUID uid = UUID.randomUUID();
        when(profileRepository.findById(uid)).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile created = service.ensureProfileExists(uid, Role.PROF, "fr");

        assertThat(created.getUserId()).isEqualTo(uid);
        assertThat(created.getRole()).isEqualTo(Role.PROF);
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void ensureProfileExists_idempotent() {
        UUID uid = UUID.randomUUID();
        Profile existing = Profile.builder().userId(uid).role(Role.PROF).build();
        when(profileRepository.findById(uid)).thenReturn(Optional.of(existing));

        Profile result = service.ensureProfileExists(uid, Role.PROF, "fr");

        assertThat(result).isSameAs(existing);
        verify(profileRepository, never()).save(any());
    }

    @Test
    void upsertProfProfile_firstSubmission_publishesEvent() {
        UUID uid = UUID.randomUUID();
        Profile parent = Profile.builder().userId(uid).role(Role.PROF).build();
        when(profileRepository.findById(uid)).thenReturn(Optional.of(parent));
        when(profProfileRepository.findById(uid)).thenReturn(Optional.empty());
        when(profProfileRepository.save(any(ProfProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profSubjectRepository.findByUserId(uid)).thenReturn(List.of());
        when(diplomaRepository.countByUserId(uid)).thenReturn(0L);

        UpsertProfProfileRequest req = new UpsertProfProfileRequest(
                3, "https://video", List.of(
                new UpsertProfProfileRequest.SubjectEntry("MATH", SchoolLevel.LYCEE)),
                List.of(Modality.ONLINE),
                List.of(new UpsertProfProfileRequest.LocationEntry("Casablanca", null)));

        service.upsertProfProfile(uid, req);

        verify(kafkaPublisher, times(1)).publish(anyString(), eq(uid.toString()), any());
        ArgumentCaptor<ProfProfile> captor = ArgumentCaptor.forClass(ProfProfile.class);
        verify(profProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProfStatus.EN_ATTENTE);
        assertThat(captor.getValue().getSubmittedAt()).isNotNull();
    }

    @Test
    void upsertProfProfile_secondSubmission_doesNotRepublish() {
        UUID uid = UUID.randomUUID();
        Profile parent = Profile.builder().userId(uid).role(Role.PROF).build();
        ProfProfile existing = ProfProfile.builder()
                .userId(uid).status(ProfStatus.EN_ATTENTE)
                .submittedAt(java.time.Instant.now().minusSeconds(60)).build();

        when(profileRepository.findById(uid)).thenReturn(Optional.of(parent));
        when(profProfileRepository.findById(uid)).thenReturn(Optional.of(existing));
        when(profProfileRepository.save(any(ProfProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UpsertProfProfileRequest req = new UpsertProfProfileRequest(
                4, null, List.of(), List.of(), List.of());

        service.upsertProfProfile(uid, req);

        verify(kafkaPublisher, never()).publish(anyString(), anyString(), any());
    }
}
