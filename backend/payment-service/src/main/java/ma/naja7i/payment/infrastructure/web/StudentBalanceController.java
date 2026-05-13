package ma.naja7i.payment.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.naja7i.payment.application.AuditService;
import ma.naja7i.payment.application.BalanceService;
import ma.naja7i.payment.domain.AuditLog;
import ma.naja7i.payment.infrastructure.web.dto.AuditEntryView;
import ma.naja7i.payment.infrastructure.web.dto.BalanceView;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Wallet balance + my audit-log views. Reachable by STUDENT or PROF; each role
 * sees ONLY its own data because the controller queries by {@code ctx.userId()}.
 */
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Tag(name = "Balance — Me", description = "Balance and audit for the authenticated user")
public class StudentBalanceController {

    private final BalanceService balanceService;
    private final AuditService auditService;

    @GetMapping("/balance/me")
    @PreAuthorize("hasAnyRole('STUDENT','PROF','ADMIN')")
    @Operation(summary = "My current wallet balance")
    public ApiResponse<BalanceView> myBalance(@AuthUser UserContext ctx) {
        return ApiResponse.of(BalanceView.from(balanceService.getOrZero(ctx.userId())));
    }

    @GetMapping("/audit/me")
    @PreAuthorize("hasAnyRole('STUDENT','PROF','ADMIN')")
    @Operation(summary = "My audit log entries (paginated)")
    public ApiResponse<List<AuditEntryView>> myAudit(@AuthUser UserContext ctx,
                                                     @RequestParam(required = false) Instant from,
                                                     @RequestParam(required = false) Instant to,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> result = auditService.listForUser(ctx.userId(), from, to, page, size);
        return ApiResponse.of(
                result.map(AuditEntryView::from).getContent(),
                new ApiResponse.PageMeta(result.getNumber(), result.getSize(), result.getTotalElements()));
    }
}
