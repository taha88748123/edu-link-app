package ma.naja7i.profile.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.profile.domain.ProfLocation;
import ma.naja7i.profile.domain.ProfLocationId;
import ma.naja7i.profile.domain.ProfModalityEntity;
import ma.naja7i.profile.domain.ProfModalityId;
import ma.naja7i.profile.domain.ProfProfile;
import ma.naja7i.profile.domain.ProfSubject;
import ma.naja7i.profile.domain.ProfSubjectId;
import ma.naja7i.profile.domain.Profile;
import ma.naja7i.profile.domain.StudentInterest;
import ma.naja7i.profile.domain.StudentInterestId;
import ma.naja7i.profile.domain.StudentProfile;
import ma.naja7i.profile.domain.TargetExam;
import ma.naja7i.profile.infrastructure.messaging.KafkaPublisher;
import ma.naja7i.profile.infrastructure.messaging.payload.ProfSubmittedPayload;
import ma.naja7i.profile.infrastructure.persistence.DiplomaRepository;
import ma.naja7i.profile.infrastructure.persistence.ProfLocationRepository;
import ma.naja7i.profile.infrastructure.persistence.ProfModalityRepository;
import ma.naja7i.profile.infrastructure.persistence.ProfProfileRepository;
import ma.naja7i.profile.infrastructure.persistence.ProfSubjectRepository;
import ma.naja7i.profile.infrastructure.persistence.ProfileRepository;
import ma.naja7i.profile.infrastructure.persistence.StudentInterestRepository;
import ma.naja7i.profile.infrastructure.persistence.StudentProfileRepository;
import ma.naja7i.profile.infrastructure.web.dto.UpdateProfileRequest;
import ma.naja7i.profile.infrastructure.web.dto.UpsertProfProfileRequest;
import ma.naja7i.profile.infrastructure.web.dto.UpsertStudentProfileRequest;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.ProfStatus;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.events.EventTopics;
import ma.naja7i.shared.i18n.SupportedLocale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfProfileRepository profProfileRepository;
    private final ProfSubjectRepository profSubjectRepository;
    private final ProfModalityRepository profModalityRepository;
    private final ProfLocationRepository profLocationRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentInterestRepository studentInterestRepository;
    private final DiplomaRepository diplomaRepository;
    private final KafkaPublisher kafkaPublisher;

    // -------------------------------------------------------------------------
    // user.registered consumer hook
    // -------------------------------------------------------------------------
    @Transactional
    public Profile ensureProfileExists(UUID userId, Role role, String locale) {
        if (userId == null) throw new IllegalArgumentException("userId required");
        Optional<Profile> existing = profileRepository.findById(userId);
        if (existing.isPresent()) return existing.get();

        Profile p = Profile.builder()
                .userId(userId)
                .role(role == null ? Role.STUDENT : role)
                .preferredLanguage(SupportedLocale.fromCodeOrDefault(locale))
                .build();
        return profileRepository.save(p);
    }

    // -------------------------------------------------------------------------
    // Reads
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public Profile getProfileOrThrow(UUID userId) {
        return profileRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Optional<ProfProfile> findProfProfile(UUID userId) {
        return profProfileRepository.findById(userId);
    }

    @Transactional(readOnly = true)
    public Optional<StudentProfile> findStudentProfile(UUID userId) {
        return studentProfileRepository.findById(userId);
    }

    @Transactional(readOnly = true)
    public List<ProfSubject> listSubjects(UUID userId) {
        return profSubjectRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<ProfModalityEntity> listModalities(UUID userId) {
        return profModalityRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<ProfLocation> listLocations(UUID userId) {
        return profLocationRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<StudentInterest> listInterests(UUID userId) {
        return studentInterestRepository.findByUserId(userId);
    }

    // -------------------------------------------------------------------------
    // Common profile update
    // -------------------------------------------------------------------------
    @Transactional
    public Profile updateCommon(UUID userId, UpdateProfileRequest req) {
        Profile p = getProfileOrThrow(userId);
        if (req.displayName() != null) p.setDisplayName(req.displayName());
        if (req.bio() != null) p.setBio(req.bio());
        if (req.photoUrl() != null) p.setPhotoUrl(req.photoUrl());
        if (req.preferredLanguage() != null) p.setPreferredLanguage(req.preferredLanguage());
        if (req.notificationPrefs() != null) p.setNotificationPrefs(req.notificationPrefs());
        return profileRepository.save(p);
    }

    @Transactional
    public Profile setPhoto(UUID userId, String url) {
        Profile p = getProfileOrThrow(userId);
        p.setPhotoUrl(url);
        return profileRepository.save(p);
    }

    // -------------------------------------------------------------------------
    // Prof upsert
    // -------------------------------------------------------------------------
    @Transactional
    public ProfProfile upsertProfProfile(UUID userId, UpsertProfProfileRequest req) {
        Profile parent = getProfileOrThrow(userId);
        if (parent.getRole() != Role.PROF) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE, "Only PROF can upsert prof profile");
        }

        ProfProfile pp = profProfileRepository.findById(userId).orElseGet(() ->
                ProfProfile.builder().userId(userId).status(ProfStatus.EN_ATTENTE).build());
        boolean firstSubmission = pp.getSubmittedAt() == null;

        if (req.yearsExperience() != null) pp.setYearsExperience(req.yearsExperience());
        if (req.introVideoUrl() != null) pp.setIntroVideoUrl(req.introVideoUrl());

        // Replace subject list
        if (req.subjects() != null) {
            profSubjectRepository.deleteByUserId(userId);
            Set<ProfSubjectId> dedup = new HashSet<>();
            for (UpsertProfProfileRequest.SubjectEntry s : req.subjects()) {
                if (s == null || s.code() == null || s.level() == null) continue;
                ProfSubjectId id = new ProfSubjectId(userId, s.code(), s.level());
                if (dedup.add(id)) {
                    profSubjectRepository.save(new ProfSubject(id));
                }
            }
        }

        // Replace modalities
        if (req.modalities() != null) {
            profModalityRepository.deleteByUserId(userId);
            Set<Modality> dedup = new HashSet<>();
            for (Modality m : req.modalities()) {
                if (m == null) continue;
                if (dedup.add(m)) {
                    profModalityRepository.save(new ProfModalityEntity(new ProfModalityId(userId, m)));
                }
            }
        }

        // Replace locations (city/region)
        if (req.locations() != null) {
            profLocationRepository.deleteByUserId(userId);
            for (UpsertProfProfileRequest.LocationEntry loc : req.locations()) {
                if (loc == null || loc.city() == null || loc.city().isBlank()) continue;
                ProfLocation entity = ProfLocation.builder()
                        .id(new ProfLocationId(userId, loc.city()))
                        .region(loc.region())
                        .build();
                profLocationRepository.save(entity);
            }
        }

        if (firstSubmission) {
            pp.setSubmittedAt(java.time.Instant.now());
        }

        ProfProfile saved = profProfileRepository.save(pp);

        if (firstSubmission) {
            // Publish profile.prof.submitted on first ever submission
            List<String> codes = profSubjectRepository.findByUserId(userId).stream()
                    .map(ps -> ps.getId().getSubjectCode())
                    .toList();
            int diplomasCount = (int) diplomaRepository.countByUserId(userId);
            kafkaPublisher.publish(
                    EventTopics.PROFILE_PROF_SUBMITTED,
                    userId.toString(),
                    new ProfSubmittedPayload(userId, codes, diplomasCount));
        }
        return saved;
    }

    // -------------------------------------------------------------------------
    // Student upsert
    // -------------------------------------------------------------------------
    @Transactional
    public StudentProfile upsertStudentProfile(UUID userId, UpsertStudentProfileRequest req) {
        Profile parent = getProfileOrThrow(userId);
        if (parent.getRole() != Role.STUDENT) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE, "Only STUDENT can upsert student profile");
        }
        StudentProfile sp = studentProfileRepository.findById(userId).orElseGet(() ->
                StudentProfile.builder().userId(userId).build());

        if (req.schoolLevel() != null) sp.setSchoolLevel(req.schoolLevel());
        if (req.schoolName() != null) sp.setSchoolName(req.schoolName());
        if (req.targetExam() != null) sp.setTargetExam(req.targetExam());
        else if (sp.getTargetExam() == null) sp.setTargetExam(TargetExam.AUCUN);
        if (req.goals() != null) sp.setGoals(req.goals());

        StudentProfile saved = studentProfileRepository.save(sp);

        if (req.interests() != null) {
            studentInterestRepository.deleteByUserId(userId);
            Set<String> dedup = new HashSet<>();
            for (String code : req.interests()) {
                if (code == null || code.isBlank()) continue;
                if (dedup.add(code)) {
                    studentInterestRepository.save(
                            new StudentInterest(new StudentInterestId(userId, code)));
                }
            }
        }
        return saved;
    }
}
