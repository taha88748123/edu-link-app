package ma.naja7i.profile.application;

import ma.naja7i.profile.domain.ProfProfile;
import ma.naja7i.profile.domain.Review;
import ma.naja7i.profile.domain.ReviewStat;
import ma.naja7i.profile.infrastructure.messaging.KafkaPublisher;
import ma.naja7i.profile.infrastructure.persistence.ProfProfileRepository;
import ma.naja7i.profile.infrastructure.persistence.ReviewRepository;
import ma.naja7i.profile.infrastructure.persistence.ReviewStatRepository;
import ma.naja7i.shared.dto.ProfStatus;
import ma.naja7i.shared.errors.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfReviewServiceTest {

    private ReviewRepository reviewRepository;
    private ReviewStatRepository reviewStatRepository;
    private ProfProfileRepository profProfileRepository;
    private KafkaPublisher kafkaPublisher;
    private ProfReviewService service;

    @BeforeEach
    void setUp() {
        reviewRepository = mock(ReviewRepository.class);
        reviewStatRepository = mock(ReviewStatRepository.class);
        profProfileRepository = mock(ProfProfileRepository.class);
        kafkaPublisher = mock(KafkaPublisher.class);
        service = new ProfReviewService(reviewRepository, reviewStatRepository,
                profProfileRepository, kafkaPublisher);
    }

    @Test
    void createReview_updatesStatsAndPublishes() {
        UUID profId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        when(profProfileRepository.findById(profId))
                .thenReturn(Optional.of(ProfProfile.builder().userId(profId).status(ProfStatus.VALIDE).build()));
        when(reviewRepository.existsByBookingIdAndStudentId(bookingId, studentId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        Review existing = Review.builder().id(UUID.randomUUID()).profId(profId).studentId(studentId).stars((short) 4).build();
        Review newest  = Review.builder().id(UUID.randomUUID()).profId(profId).studentId(studentId).stars((short) 5).build();
        Page<Review> page = new PageImpl<>(List.of(existing, newest));
        when(reviewRepository.findByProfId(eq(profId), any(Pageable.class))).thenReturn(page);
        when(reviewStatRepository.findById(profId)).thenReturn(Optional.empty());
        when(reviewStatRepository.save(any(ReviewStat.class))).thenAnswer(inv -> inv.getArgument(0));

        Review saved = service.createReview(studentId, bookingId, profId, 5, "great");

        assertThat(saved.getStars()).isEqualTo((short) 5);

        ArgumentCaptor<ReviewStat> statCap = ArgumentCaptor.forClass(ReviewStat.class);
        verify(reviewStatRepository).save(statCap.capture());
        assertThat(statCap.getValue().getCount()).isEqualTo(2L);
        assertThat(statCap.getValue().getAvgStars()).isEqualByComparingTo(new BigDecimal("4.50"));

        verify(kafkaPublisher, times(1)).publish(anyString(), eq(profId.toString()), any());
    }

    @Test
    void createReview_rejectsOutOfRangeStars() {
        UUID profId = UUID.randomUUID();
        assertThatThrownBy(() -> service.createReview(UUID.randomUUID(), null, profId, 6, "x"))
                .isInstanceOf(BusinessException.class);
    }
}
