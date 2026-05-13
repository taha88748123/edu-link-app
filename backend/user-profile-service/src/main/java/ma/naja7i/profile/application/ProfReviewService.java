package ma.naja7i.profile.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.profile.domain.Review;
import ma.naja7i.profile.domain.ReviewStat;
import ma.naja7i.profile.infrastructure.messaging.KafkaPublisher;
import ma.naja7i.profile.infrastructure.messaging.payload.ReviewCreatedPayload;
import ma.naja7i.profile.infrastructure.persistence.ProfProfileRepository;
import ma.naja7i.profile.infrastructure.persistence.ReviewRepository;
import ma.naja7i.profile.infrastructure.persistence.ReviewStatRepository;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.events.EventTopics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewStatRepository reviewStatRepository;
    private final ProfProfileRepository profProfileRepository;
    private final KafkaPublisher kafkaPublisher;

    @Transactional(readOnly = true)
    public Page<Review> listForProf(UUID profId, Pageable pageable) {
        return reviewRepository.findByProfId(profId, pageable);
    }

    @Transactional(readOnly = true)
    public ReviewStat statsFor(UUID profId) {
        return reviewStatRepository.findById(profId)
                .orElse(ReviewStat.builder()
                        .profId(profId).avgStars(BigDecimal.ZERO).count(0L).build());
    }

    @Transactional
    public Review createReview(UUID studentId, UUID bookingId, UUID profId, int stars, String comment) {
        if (stars < 1 || stars > 5) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "stars must be 1..5");
        }
        if (profId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "profId required");
        }
        // Ensure the prof profile exists (no FK constraint to auth.users, but we want a real prof here)
        profProfileRepository.findById(profId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));

        // TODO: when booking-service exists, verify the booking is CONFIRMEE,
        //  belongs to studentId+profId, and that no review already exists for it.
        if (bookingId != null
                && reviewRepository.existsByBookingIdAndStudentId(bookingId, studentId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "review already exists for this booking");
        }

        Review review = Review.builder()
                .bookingId(bookingId)
                .profId(profId)
                .studentId(studentId)
                .stars((short) stars)
                .comment(comment)
                .moderated(false)
                .build();
        Review saved = reviewRepository.save(review);

        // Maintain materialized stats in same transaction (simple post-insert recalc)
        recomputeStats(profId);

        kafkaPublisher.publish(
                EventTopics.PROFILE_REVIEW_CREATED,
                profId.toString(),
                new ReviewCreatedPayload(saved.getId(), profId, studentId, saved.getStars(), comment));
        return saved;
    }

    private void recomputeStats(UUID profId) {
        ReviewStat stat = reviewStatRepository.findById(profId).orElseGet(() ->
                ReviewStat.builder()
                        .profId(profId).avgStars(BigDecimal.ZERO).count(0L).build());
        // Aggregate
        var all = reviewRepository.findByProfId(profId,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE));
        long count = all.getTotalElements();
        BigDecimal sum = BigDecimal.ZERO;
        for (Review r : all.getContent()) {
            sum = sum.add(BigDecimal.valueOf(r.getStars()));
        }
        BigDecimal avg = count == 0 ? BigDecimal.ZERO
                : sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        stat.setCount(count);
        stat.setAvgStars(avg);
        reviewStatRepository.save(stat);
    }
}
