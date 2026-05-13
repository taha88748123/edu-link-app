package ma.naja7i.profile.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.profile.domain.Diploma;
import ma.naja7i.profile.infrastructure.persistence.DiplomaRepository;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiplomaService {

    private final DiplomaRepository diplomaRepository;
    private final FileStorageService fileStorage;

    @Transactional(readOnly = true)
    public List<Diploma> listForUser(UUID userId) {
        return diplomaRepository.findByUserId(userId);
    }

    @Transactional
    public Diploma create(UUID userId, String title, String institution, Integer year,
                          FileStorageService.StoredObject stored) {
        Diploma d = Diploma.builder()
                .userId(userId)
                .title(title)
                .institution(institution)
                .year(year)
                .fileUrl(stored.publicUrl())
                .objectKey(stored.objectKey())
                .verified(false)
                .build();
        return diplomaRepository.save(d);
    }

    @Transactional
    public void delete(UUID userId, UUID diplomaId) {
        Diploma d = diplomaRepository.findById(diplomaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!d.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE, "Diploma does not belong to caller");
        }
        fileStorage.delete(d.getObjectKey());
        diplomaRepository.delete(d);
    }
}
