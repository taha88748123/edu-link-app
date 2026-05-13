package ma.naja7i.profile.infrastructure.web.dto;

import ma.naja7i.profile.domain.TargetExam;
import ma.naja7i.shared.dto.SchoolLevel;

import java.util.List;
import java.util.UUID;

public record StudentView(
        UUID userId,
        SchoolLevel schoolLevel,
        String schoolName,
        TargetExam targetExam,
        String goals,
        List<String> interests
) { }
