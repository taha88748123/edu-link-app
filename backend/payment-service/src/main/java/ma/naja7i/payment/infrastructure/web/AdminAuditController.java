package ma.naja7i.payment.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import ma.naja7i.payment.application.AuditService;
import ma.naja7i.payment.domain.AuditLog;
import ma.naja7i.payment.infrastructure.web.dto.AuditEntryView;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment/admin/audit")
@RequiredArgsConstructor
@Tag(name = "Audit — Admin", description = "Read and export the immutable audit log")
public class AdminAuditController {

    private final AuditService auditService;

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List a user's audit log entries")
    public ApiResponse<List<AuditEntryView>> userAudit(@PathVariable("userId") UUID userId,
                                                       @RequestParam(required = false) Instant from,
                                                       @RequestParam(required = false) Instant to,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> result = auditService.listForUser(userId, from, to, page, size);
        return ApiResponse.of(
                result.map(AuditEntryView::from).getContent(),
                new ApiResponse.PageMeta(result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @PostMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Stream the audit log range as CSV")
    public void export(@RequestParam(required = false) UUID userId,
                       @RequestParam(required = false) Instant from,
                       @RequestParam(required = false) Instant to,
                       @RequestParam(defaultValue = "CSV") String format,
                       HttpServletResponse response) throws IOException {
        if (!"CSV".equalsIgnoreCase(format)) {
            // v1 only supports CSV streaming; PDF/Excel are a future async job.
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Only CSV format is supported in v1",
                    Map.of("format", format));
        }
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setHeader("Content-Disposition",
                "attachment; filename=\"audit-log-export.csv\"");
        auditService.exportCsv(userId, from, to, response);
    }
}
