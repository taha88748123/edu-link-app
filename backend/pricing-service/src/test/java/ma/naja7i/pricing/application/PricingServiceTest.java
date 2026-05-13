package ma.naja7i.pricing.application;

import ma.naja7i.pricing.domain.ProfPricing;
import ma.naja7i.pricing.infrastructure.persistence.ProfPricingRepository;
import ma.naja7i.pricing.infrastructure.web.dto.CreateRateRequest;
import ma.naja7i.pricing.infrastructure.web.dto.UpdateRateRequest;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PricingServiceTest {

    private ProfPricingRepository repo;
    private PricingService service;

    private final UUID profId = UUID.randomUUID();
    private final UUID otherProf = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repo = mock(ProfPricingRepository.class);
        service = new PricingService(repo);
    }

    @Test
    void create_persists_centimesFromMadString() {
        when(repo.save(any(ProfPricing.class))).thenAnswer(inv -> inv.getArgument(0));
        CreateRateRequest req = new CreateRateRequest(
                "MATH", SchoolLevel.LYCEE, Modality.ONLINE, 60, null, "150.00");

        ProfPricing saved = service.create(profId, req);

        ArgumentCaptor<ProfPricing> cap = ArgumentCaptor.forClass(ProfPricing.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getAmountMinor()).isEqualTo(15000L);
        assertThat(cap.getValue().getProfId()).isEqualTo(profId);
        assertThat(cap.getValue().isActive()).isTrue();
        assertThat(saved.getAmountMinor()).isEqualTo(15000L);
    }

    @Test
    void update_rejectsForeignOwnership_withResourceNotFound() {
        UUID rateId = UUID.randomUUID();
        when(repo.findById(rateId)).thenReturn(Optional.of(existingRate(rateId, otherProf)));

        UpdateRateRequest req = new UpdateRateRequest(
                "MATH", SchoolLevel.LYCEE, Modality.ONLINE, 60, null, "200.00", null);

        assertThatThrownBy(() -> service.update(profId, rateId, req))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).code())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void update_appliesNewFields_when_owner_matches() {
        UUID rateId = UUID.randomUUID();
        when(repo.findById(rateId)).thenReturn(Optional.of(existingRate(rateId, profId)));
        when(repo.save(any(ProfPricing.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateRateRequest req = new UpdateRateRequest(
                "PHYS", SchoolLevel.COLLEGE, Modality.DOMICILE, 90, 2, "180.50", false);

        ProfPricing saved = service.update(profId, rateId, req);

        assertThat(saved.getSubjectCode()).isEqualTo("PHYS");
        assertThat(saved.getLevel()).isEqualTo(SchoolLevel.COLLEGE);
        assertThat(saved.getModality()).isEqualTo(Modality.DOMICILE);
        assertThat(saved.getDurationMinutes()).isEqualTo((short) 90);
        assertThat(saved.getDayOfWeek()).isEqualTo((short) 2);
        assertThat(saved.getAmountMinor()).isEqualTo(18050L);
        assertThat(saved.isActive()).isFalse();
    }

    @Test
    void softDelete_setsActiveFalse_when_owner_matches() {
        UUID rateId = UUID.randomUUID();
        ProfPricing existing = existingRate(rateId, profId);
        when(repo.findById(rateId)).thenReturn(Optional.of(existing));
        when(repo.save(any(ProfPricing.class))).thenAnswer(inv -> inv.getArgument(0));

        service.softDelete(profId, rateId);

        assertThat(existing.isActive()).isFalse();
    }

    @Test
    void softDelete_foreignOwner_throwsResourceNotFound() {
        UUID rateId = UUID.randomUUID();
        when(repo.findById(rateId)).thenReturn(Optional.of(existingRate(rateId, otherProf)));

        assertThatThrownBy(() -> service.softDelete(profId, rateId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).code())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    private static ProfPricing existingRate(UUID id, UUID ownerProf) {
        return ProfPricing.builder()
                .id(id)
                .profId(ownerProf)
                .subjectCode("MATH")
                .level(SchoolLevel.LYCEE)
                .modality(Modality.ONLINE)
                .durationMinutes((short) 60)
                .dayOfWeek(null)
                .amountMinor(15000L)
                .currency("MAD")
                .active(true)
                .version(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
