package ma.naja7i.notification.infrastructure.persistence;

import ma.naja7i.notification.domain.Channel;
import ma.naja7i.notification.domain.Notification;
import ma.naja7i.notification.domain.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("""
              select n from Notification n
              where (:userId  is null or n.userId  = :userId)
                and (:channel is null or n.channel = :channel)
                and (:status  is null or n.status  = :status)
                and (:from    is null or n.createdAt >= :from)
                and (:to      is null or n.createdAt <= :to)
              order by n.createdAt desc
            """)
    Page<Notification> search(
            @Param("userId") UUID userId,
            @Param("channel") Channel channel,
            @Param("status") NotificationStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
