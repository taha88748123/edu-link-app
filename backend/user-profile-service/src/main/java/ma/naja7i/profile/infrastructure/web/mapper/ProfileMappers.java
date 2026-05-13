package ma.naja7i.profile.infrastructure.web.mapper;

import ma.naja7i.profile.domain.Diploma;
import ma.naja7i.profile.domain.ProfLocation;
import ma.naja7i.profile.domain.ProfModalityEntity;
import ma.naja7i.profile.domain.ProfProfile;
import ma.naja7i.profile.domain.ProfSubject;
import ma.naja7i.profile.domain.Profile;
import ma.naja7i.profile.domain.Review;
import ma.naja7i.profile.domain.StudentInterest;
import ma.naja7i.profile.domain.StudentProfile;
import ma.naja7i.profile.infrastructure.web.dto.DiplomaView;
import ma.naja7i.profile.infrastructure.web.dto.ProfPrivateView;
import ma.naja7i.profile.infrastructure.web.dto.ProfPublicView;
import ma.naja7i.profile.infrastructure.web.dto.ProfileView;
import ma.naja7i.profile.infrastructure.web.dto.ReviewView;
import ma.naja7i.profile.infrastructure.web.dto.StudentView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ProfileMappers {

    @Mapping(target = "prof", ignore = true)
    @Mapping(target = "student", ignore = true)
    ProfileView toProfileView(Profile p);

    DiplomaView toDiplomaView(Diploma d);

    ReviewView toReviewView(Review r);

    default ProfPrivateView toPrivateView(ProfProfile pp,
                                          List<ProfSubject> subjects,
                                          List<ProfModalityEntity> modalities,
                                          List<ProfLocation> locations,
                                          List<Diploma> diplomas,
                                          BigDecimal avgStars,
                                          long reviewCount) {
        return new ProfPrivateView(
                pp.getUserId(),
                pp.getStatus(),
                pp.getYearsExperience(),
                pp.getIntroVideoUrl(),
                pp.getAiQcmScore(),
                pp.getAdminNotes(),
                pp.getSubmittedAt(),
                pp.getValidatedAt(),
                pp.getValidatedBy(),
                subjects.stream()
                        .map(s -> new ProfPrivateView.SubjectView(
                                s.getId().getSubjectCode(), s.getId().getLevel()))
                        .toList(),
                modalities.stream().map(m -> m.getId().getModality()).toList(),
                locations.stream()
                        .map(l -> new ProfPrivateView.LocationView(l.getId().getCity(), l.getRegion()))
                        .toList(),
                diplomas.stream().map(this::toDiplomaView).toList(),
                avgStars,
                reviewCount
        );
    }

    default ProfPublicView toPublicView(Profile profile,
                                        ProfProfile pp,
                                        List<ProfSubject> subjects,
                                        List<ProfModalityEntity> modalities,
                                        List<ProfLocation> locations,
                                        BigDecimal avgStars,
                                        long reviewCount) {
        return new ProfPublicView(
                profile.getUserId(),
                profile.getDisplayName(),
                profile.getPhotoUrl(),
                profile.getBio(),
                pp.getYearsExperience(),
                pp.getIntroVideoUrl(),
                subjects.stream()
                        .map(s -> new ProfPublicView.SubjectView(
                                s.getId().getSubjectCode(), s.getId().getLevel()))
                        .toList(),
                modalities.stream().map(m -> m.getId().getModality()).toList(),
                locations.stream()
                        .map(l -> new ProfPublicView.LocationView(l.getId().getCity(), l.getRegion()))
                        .toList(),
                avgStars,
                reviewCount
        );
    }

    default StudentView toStudentView(StudentProfile sp, List<StudentInterest> interests) {
        if (sp == null) return null;
        return new StudentView(
                sp.getUserId(),
                sp.getSchoolLevel(),
                sp.getSchoolName(),
                sp.getTargetExam(),
                sp.getGoals(),
                interests == null ? List.of() :
                        interests.stream().map(i -> i.getId().getSubjectCode()).toList()
        );
    }
}
