package ma.naja7i.payment.application;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import ma.naja7i.payment.domain.AuditLog;
import ma.naja7i.payment.infrastructure.persistence.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Read-side service for the immutable audit log. Used by both STUDENT/PROF (my
 * own audit) and ADMIN (anyone's audit + CSV export).
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    public static final int MAX_PAGE_SIZE = 500;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLog> listForUser(UUID userId, Instant from, Instant to, int page, int size) {
        Pageable pageable = clamp(page, size);
        return auditLogRepository.findByUserIdAndCreatedAtBetween(userId, from, to, pageable);
    }

    /**
     * Streams audit rows as CSV directly into the response writer. Caller is
     * expected to have set the appropriate {@code Content-Type} / Content-
     * Disposition headers. We don't buffer the whole result set in memory; this
     * is one big SQL fetch but writing rows line-by-line.
     */
    @Transactional(readOnly = true)
    public void exportCsv(UUID userIdOrNull, Instant from, Instant to,
                          HttpServletResponse response) throws IOException {
        List<AuditLog> rows = auditLogRepository.findForExport(userIdOrNull, from, to);
        PrintWriter w = response.getWriter();
        w.println("id,user_id,actor_id,operation,amount_minor,balance_before,balance_after," +
                "reference_type,reference_id,reason,trace_id,created_at");
        for (AuditLog r : rows) {
            w.print(r.getId());
            w.print(',');
            w.print(r.getUserId());
            w.print(',');
            w.print(r.getActorId());
            w.print(',');
            w.print(r.getOperation());
            w.print(',');
            w.print(r.getAmountMinor());
            w.print(',');
            w.print(r.getBalanceBefore());
            w.print(',');
            w.print(r.getBalanceAfter());
            w.print(',');
            w.print(r.getReferenceType());
            w.print(',');
            w.print(r.getReferenceId());
            w.print(',');
            w.print(csvEscape(r.getReason()));
            w.print(',');
            w.print(r.getTraceId() == null ? "" : r.getTraceId());
            w.print(',');
            w.println(r.getCreatedAt() == null ? "" : ISO.format(r.getCreatedAt()));
        }
        w.flush();
        log.info("audit_log CSV export streamed: rows={}", rows.size());
    }

    private static String csvEscape(String s) {
        if (s == null) return "";
        boolean needsQuoting = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String escaped = s.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }

    private static Pageable clamp(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        return PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    }
}
