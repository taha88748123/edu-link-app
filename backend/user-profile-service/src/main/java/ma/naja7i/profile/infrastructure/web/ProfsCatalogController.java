package ma.naja7i.profile.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.naja7i.profile.application.ProfReviewService;
import ma.naja7i.profile.application.ProfileService;
import ma.naja7i.profile.domain.ProfProfile;
import ma.naja7i.profile.domain.Profile;
import ma.naja7i.profile.infrastructure.persistence.ProfCatalogRepository;
import ma.naja7i.profile.infrastructure.persistence.ProfileRepository;
import ma.naja7i.profile.infrastructure.persistence.ReviewStatRepository;
import ma.naja7i.profile.infrastructure.web.dto.ProfPublicView;
import ma.naja7i.profile.infrastructure.web.dto.ReviewView;
import ma.naja7i.profile.infrastructure.web.mapper.ProfileMappers;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.ProfStatus;
import ma.naja7i.shared.dto.SchoolLevel;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
@Tag(name = "Profs Catalog")
public class ProfsCatalogController {

    private final ProfileService profileService;
    private final ProfileRepository profileRepository;
    private final ProfCatalogRepository catalogRepository;
    private final ReviewStatRepository reviewStatRepository;
    private final ProfReviewService reviewService;
    private final ProfileMappers mappers;

    @GetMapping("/profs")
    @Operation(summary = "List validated profs (public catalog)")
    public ApiResponse<List<ProfPublicView>> listProfs(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) SchoolLevel level,
            @RequestParam(required = false) Modality modality,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BigDecimal minStars,
            @RequestParam(defaultValue = "updated") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), sortObj);
        Page<UUID> idsPage = catalogRepository.searchProfIds(subject, level, modality, city, minStars, pageable);

        List<ProfPublicView> views = new ArrayList<>(idsPage.getContent().size());
        for (UUID id : idsPage.getContent()) {
            ProfPublicView v = buildPublicView(id);
            if (v != null) views.add(v);
        }
        return ApiResponse.of(views,
                new ApiResponse.PageMeta(idsPage.getNumber(), idsPage.getSize(), idsPage.getTotalElements()));
    }

    @GetMapping("/profs/{id}")
    @Operation(summary = "Get a prof profile (public view)")
    public ApiResponse<ProfPublicView> getProf(@AuthUser UserContext ctx, @PathVariable UUID id) {
        ProfProfile pp = profileService.findProfProfile(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));

        boolean isSelf = ctx != null && ctx.userId() != null && ctx.userId().equals(id);
        boolean isAdmin = ctx != null && ctx.isAdmin();

        if (pp.getStatus() != ProfStatus.VALIDE && !isSelf && !isAdmin) {
            // Hide existence of non-validated profs from the public.
            throw new BusinessException(ErrorCode.PROFILE_PROF_NOT_VALIDATED);
        }

        ProfPublicView v = buildPublicView(id);
        if (v == null) throw new BusinessException(ErrorCode.PROFILE_NOT_FOUND);

        // Phone/email are stored in auth-service — they're NOT in the public view by design.
        // TODO: when booking-service exists, allow unmasking phone/email for ADMIN OR for
        //  callers who own an ACCEPTEE_PAR_PROF booking with this prof. For v1, masking is unconditional
        //  outside ADMIN (and ADMIN does not get those fields here either since they live in auth-service).
        return ApiResponse.of(v);
    }

    @GetMapping("/profs/{id}/reviews")
    @Operation(summary = "Paginated reviews for a prof")
    public ApiResponse<List<ReviewView>> profReviews(@PathVariable UUID id,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        Pageable p = PageRequest.of(Math.max(0, page),
                Math.min(100, Math.max(1, size)),
                Sort.by(Sort.Order.desc("createdAt")));
        var pageRes = reviewService.listForProf(id, p);
        List<ReviewView> list = pageRes.getContent().stream().map(mappers::toReviewView).toList();
        return ApiResponse.of(list,
                new ApiResponse.PageMeta(pageRes.getNumber(), pageRes.getSize(), pageRes.getTotalElements()));
    }

    // ----------------------------------------------------------------
    private ProfPublicView buildPublicView(UUID id) {
        var profileOpt = profileRepository.findById(id);
        var ppOpt = profileService.findProfProfile(id);
        if (profileOpt.isEmpty() || ppOpt.isEmpty()) return null;
        Profile profile = profileOpt.get();
        ProfProfile pp = ppOpt.get();
        var subjects = profileService.listSubjects(id);
        var modalities = profileService.listModalities(id);
        var locations = profileService.listLocations(id);
        var stat = reviewStatRepository.findById(id);
        BigDecimal avg = stat.map(s -> s.getAvgStars()).orElse(BigDecimal.ZERO);
        long count = stat.map(s -> s.getCount()).orElse(0L);
        return mappers.toPublicView(profile, pp, subjects, modalities, locations, avg, count);
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Order.desc("updated"));
        String[] parts = sort.split(",");
        String prop = parts[0].trim();
        Sort.Direction dir = parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(new Sort.Order(dir, prop));
    }
}
