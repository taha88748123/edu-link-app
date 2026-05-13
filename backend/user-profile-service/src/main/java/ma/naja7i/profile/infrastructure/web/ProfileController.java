package ma.naja7i.profile.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.profile.application.DiplomaService;
import ma.naja7i.profile.application.FileStorageService;
import ma.naja7i.profile.application.ProfileService;
import ma.naja7i.profile.domain.Diploma;
import ma.naja7i.profile.domain.ProfProfile;
import ma.naja7i.profile.domain.Profile;
import ma.naja7i.profile.domain.StudentProfile;
import ma.naja7i.profile.infrastructure.persistence.ReviewStatRepository;
import ma.naja7i.profile.infrastructure.web.dto.CreateDiplomaRequest;
import ma.naja7i.profile.infrastructure.web.dto.DiplomaView;
import ma.naja7i.profile.infrastructure.web.dto.PhotoUploadResponse;
import ma.naja7i.profile.infrastructure.web.dto.ProfPrivateView;
import ma.naja7i.profile.infrastructure.web.dto.ProfileView;
import ma.naja7i.profile.infrastructure.web.dto.StudentView;
import ma.naja7i.profile.infrastructure.web.dto.UpdateProfileRequest;
import ma.naja7i.profile.infrastructure.web.dto.UpsertProfProfileRequest;
import ma.naja7i.profile.infrastructure.web.dto.UpsertStudentProfileRequest;
import ma.naja7i.profile.infrastructure.web.mapper.ProfileMappers;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.security.UserContext;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile (me)", description = "Self-service profile endpoints")
public class ProfileController {

    private final ProfileService profileService;
    private final DiplomaService diplomaService;
    private final FileStorageService fileStorage;
    private final UploadValidator uploadValidator;
    private final ReviewStatRepository reviewStatRepository;
    private final ProfileMappers mappers;

    // ----------------------------------------------------------------
    // GET /profile/me
    // ----------------------------------------------------------------
    @GetMapping("/me")
    @Operation(summary = "Get caller's profile (role-shaped)")
    public ApiResponse<ProfileView> getMe(@AuthUser UserContext ctx) {
        Profile p = profileService.getProfileOrThrow(ctx.userId());
        ProfileView base = mappers.toProfileView(p);
        ProfPrivateView profView = null;
        StudentView studentView = null;
        switch (p.getRole()) {
            case PROF -> profView = buildPrivateView(p.getUserId());
            case STUDENT -> studentView = buildStudentView(p.getUserId());
            default -> { /* admin / parent: just common view */ }
        }
        return ApiResponse.of(new ProfileView(
                base.userId(), base.role(), base.displayName(), base.photoUrl(),
                base.bio(), base.preferredLanguage(), base.notificationPrefs(),
                base.createdAt(), base.updatedAt(), profView, studentView));
    }

    // ----------------------------------------------------------------
    // PUT /profile/me
    // ----------------------------------------------------------------
    @PutMapping("/me")
    @Operation(summary = "Update common profile fields")
    public ApiResponse<ProfileView> updateMe(@AuthUser UserContext ctx,
                                             @Valid @org.springframework.web.bind.annotation.RequestBody UpdateProfileRequest req) {
        Profile p = profileService.updateCommon(ctx.userId(), req);
        return ApiResponse.of(mappers.toProfileView(p));
    }

    // ----------------------------------------------------------------
    // POST /profile/me/photo (multipart)
    // ----------------------------------------------------------------
    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload caller's profile photo")
    public ApiResponse<PhotoUploadResponse> uploadPhoto(@AuthUser UserContext ctx,
                                                        @RequestPart("file") MultipartFile file)
            throws IOException {
        uploadValidator.validatePhoto(file);
        FileStorageService.StoredObject stored = fileStorage.upload(
                "photos/" + ctx.userId(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream());
        profileService.setPhoto(ctx.userId(), stored.publicUrl());
        return ApiResponse.of(new PhotoUploadResponse(stored.objectKey(), stored.publicUrl()));
    }

    // ----------------------------------------------------------------
    // PUT /profile/me/prof — PROF only
    // ----------------------------------------------------------------
    @PutMapping("/me/prof")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "Upsert prof-specific profile")
    public ApiResponse<ProfPrivateView> upsertProf(@AuthUser UserContext ctx,
                                                   @Valid @org.springframework.web.bind.annotation.RequestBody UpsertProfProfileRequest req) {
        profileService.upsertProfProfile(ctx.userId(), req);
        return ApiResponse.of(buildPrivateView(ctx.userId()));
    }

    // ----------------------------------------------------------------
    // POST /profile/me/prof/diplomas (multipart)
    // ----------------------------------------------------------------
    @PostMapping(value = "/me/prof/diplomas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "Upload a diploma PDF")
    public ApiResponse<DiplomaView> uploadDiploma(@AuthUser UserContext ctx,
                                                  @RequestPart("file") MultipartFile file,
                                                  @RequestParam("title") String title,
                                                  @RequestParam(value = "institution", required = false) String institution,
                                                  @RequestParam(value = "year", required = false) Integer year)
            throws IOException {
        uploadValidator.validateDiploma(file);
        FileStorageService.StoredObject stored = fileStorage.upload(
                "diplomas/" + ctx.userId(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream());
        Diploma d = diplomaService.create(ctx.userId(), title, institution, year, stored);
        return ApiResponse.of(mappers.toDiplomaView(d));
    }

    @DeleteMapping("/me/prof/diplomas/{id}")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "Delete a diploma")
    public ApiResponse<Void> deleteDiploma(@AuthUser UserContext ctx, @PathVariable UUID id) {
        diplomaService.delete(ctx.userId(), id);
        return ApiResponse.of(null);
    }

    // ----------------------------------------------------------------
    // PUT /profile/me/student — STUDENT only
    // ----------------------------------------------------------------
    @PutMapping("/me/student")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Upsert student-specific profile")
    public ApiResponse<StudentView> upsertStudent(@AuthUser UserContext ctx,
                                                  @Valid @org.springframework.web.bind.annotation.RequestBody UpsertStudentProfileRequest req) {
        profileService.upsertStudentProfile(ctx.userId(), req);
        return ApiResponse.of(buildStudentView(ctx.userId()));
    }

    // ----------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------
    private ProfPrivateView buildPrivateView(UUID userId) {
        Optional<ProfProfile> ppOpt = profileService.findProfProfile(userId);
        if (ppOpt.isEmpty()) return null;
        ProfProfile pp = ppOpt.get();
        var subjects = profileService.listSubjects(userId);
        var modalities = profileService.listModalities(userId);
        var locations = profileService.listLocations(userId);
        List<Diploma> diplomas = diplomaService.listForUser(userId);
        var stat = reviewStatRepository.findById(userId);
        BigDecimal avg = stat.map(s -> s.getAvgStars()).orElse(BigDecimal.ZERO);
        long count = stat.map(s -> s.getCount()).orElse(0L);
        return mappers.toPrivateView(pp, subjects, modalities, locations, diplomas, avg, count);
    }

    private StudentView buildStudentView(UUID userId) {
        Optional<StudentProfile> spOpt = profileService.findStudentProfile(userId);
        if (spOpt.isEmpty()) return null;
        return mappers.toStudentView(spOpt.get(), profileService.listInterests(userId));
    }
}
