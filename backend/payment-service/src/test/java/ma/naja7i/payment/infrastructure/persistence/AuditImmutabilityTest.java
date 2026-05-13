package ma.naja7i.payment.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import ma.naja7i.payment.domain.AuditLog;
import ma.naja7i.payment.domain.AuditOperation;
import ma.naja7i.payment.domain.AuditReferenceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CRITICAL safety test: the DB triggers must reject UPDATE and DELETE on
 * {@code audit_log}. Without this defence-in-depth, a buggy Java path could
 * tamper with money history.
 *
 * <p>Skipped without Docker. When Docker is available, this MUST run and pass.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuditImmutabilityTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payment_db")
            .withUsername("payment_user")
            .withPassword("payment_pwd");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
    }

    @Autowired AuditLogRepository auditLogRepository;
    @Autowired EntityManager em;

    private AuditLog persistFreshAuditRow() {
        AuditLog row = AuditLog.builder()
                .userId(UUID.randomUUID())
                .actorId(UUID.randomUUID())
                .operation(AuditOperation.TOPUP)
                .amountMinor(1_000L)
                .balanceBefore(0L)
                .balanceAfter(1_000L)
                .referenceType(AuditReferenceType.TOPUP)
                .referenceId(UUID.randomUUID())
                .reason("test")
                .build();
        AuditLog saved = auditLogRepository.saveAndFlush(row);
        em.clear();
        return saved;
    }

    @Test
    void updateOnAuditLog_isRejectedByTrigger() {
        AuditLog saved = persistFreshAuditRow();

        // The trigger rejects UPDATE -> the JDBC layer surfaces this as a PSQLException
        // wrapped in a PersistenceException by Hibernate.
        assertThatThrownBy(() -> {
            em.createNativeQuery("UPDATE audit_log SET reason = 'tampered' WHERE id = :id")
                    .setParameter("id", saved.getId())
                    .executeUpdate();
            em.flush();
        }).isInstanceOfAny(PersistenceException.class, org.springframework.dao.DataAccessException.class)
                .hasMessageContaining("append-only");
    }

    @Test
    void deleteOnAuditLog_isRejectedByTrigger() {
        AuditLog saved = persistFreshAuditRow();

        assertThatThrownBy(() -> {
            em.createNativeQuery("DELETE FROM audit_log WHERE id = :id")
                    .setParameter("id", saved.getId())
                    .executeUpdate();
            em.flush();
        }).isInstanceOfAny(PersistenceException.class, org.springframework.dao.DataAccessException.class)
                .hasMessageContaining("append-only");
    }

    @Test
    void insertOnAuditLog_isAllowed() {
        AuditLog saved = persistFreshAuditRow();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
