package ma.naja7i.profile.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.naja7i.profile.application.AdminProfService;
import ma.naja7i.profile.application.DiplomaService;
import ma.naja7i.profile.application.ProfileService;
import ma.naja7i.profile.domain.Diploma;
import ma.naja7i.profile.domain.ProfProfile;
import ma.naja7i.profile.infrastructure.persistence.ReviewStatRepository;
import ma.naja7i.profile.infrastructure.web.dto.AdminDecisionRequest;
import ma.naja7i.profile.infrastructure.web.dto.ProfPrivateView;
import ma.naja7i.profile.infrastructure.web.mapper.ProfileMappers;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/profs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin — Profs")
public class AdminProfController {

    private final AdminProfService adminProfService;
    private final ProfileService profileService;
    private final DiplomaService diplomaService;
    private final ReviewStatRepository reviewStatRepository;
    private final ProfileMappers mappers;

    @GetMapping("/pending")
    @Operation(summary = "List profs awaiting validation")
    public ApiResponse<List<ProfPrivateView>> listPending(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)),
                Sort.by(Sort.Order.asc("submittedAt")));
        Page<ProfProfile> pendings = adminProfService.listPending(pageable);
        List<ProfPrivateView> views = pendings.getContent().stream()
                .map(pp -> buildPrivateView(pp.getUserId()))
                .filter(v -> v != null)
                .toList();
        return ApiResponse.of(views,
                new ApiResponse.PageMeta(pendings.getNumber(), pendings.getSize(), pendings.getTotalElements()));
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "Validate a prof (status → VALIDE)")
    public ApiResponse<ProfPrivateView> validate(@AuthUser UserContext ctx,
                                                 @PathVariable UUID id,
                                                 @RequestBody(required = false) AdminDecisionRequest req) {
        adminProfService.validate(id, ctx.userId(), req == null ? null : req.notes());
        return ApiResponse.of(buildPrivateView(id));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a prof (status → REJETE)")
    public ApiResponse<ProfPrivateView> reject(@AuthUser UserContext ctx,
                                               @PathVariable UUID id,
                                               @RequestBody(required = false) AdminDecisionRequest req) {
        adminProfService.reject(id, ctx.userId(), req == null ? null : req.reason());
        return ApiResponse.of(buildPrivateView(id));
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend a prof (status → SUSPENDU)")
    public ApiResponse<ProfPrivateView> suspend(@AuthUser UserContext ctx,
                                                @PathVariable UUID id,
                                                @RequestBody(required = false) AdminDecisionRequest req) {
        adminProfService.suspend(id, ctx.userId(), req == null ? null : req.reason());
        return ApiResponse.of(buildPrivateView(id));
    }

    // -------------------------------------------------------
    private ProfPrivateView buildPrivateView(UUID userId) {
        return profileService.findProfProfile(userId).map(pp -> {
            var subjects = profileService.listSubjects(userId);
            var modalities = profileService.listModalities(userId);
            var locations = profileService.listLocations(userId);
            List<Diploma> diplomas = diplomaService.listForUser(userId);
            var stat = reviewStatRepository.findById(userId);
            BigDecimal avg = stat.map(s -> s.getAvgStars()).orElse(BigDecimal.ZERO);
            long count = stat.map(s -> s.getCount()).orElse(0L);
            return mappers.toPrivateView(pp, subjects, modalities, locations, diplomas, avg, count);
        }).orElse(null);
    }
}
