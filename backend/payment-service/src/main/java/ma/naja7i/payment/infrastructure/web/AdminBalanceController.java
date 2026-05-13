package ma.naja7i.payment.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.payment.application.AdminBalanceService;
import ma.naja7i.payment.domain.AuditLog;
import ma.naja7i.payment.domain.AuditOperation;
import ma.naja7i.payment.infrastructure.web.dto.AdminAdjustRequest;
import ma.naja7i.payment.infrastructure.web.dto.AuditEntryView;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.security.UserContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment/admin/balance")
@RequiredArgsConstructor
@Tag(name = "Balance — Admin", description = "Manual balance adjustments (extremely sensitive)")
public class AdminBalanceController {

    private final AdminBalanceService adminBalanceService;

    @PostMapping("/{userId}:adjust")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually credit / debit / adjust a user's wallet")
    public ApiResponse<AuditEntryView> adjust(@AuthUser UserContext ctx,
                                              @PathVariable("userId") UUID userId,
                                              @Valid @RequestBody AdminAdjustRequest req) {
        long deltaMinor = req.deltaMad().movePointRight(2).longValueExact();
        AuditOperation op = AuditOperation.valueOf(req.operation());
        AuditLog row = adminBalanceService.adjust(userId, deltaMinor, op,
                ctx.userId(), req.reason(), safeTraceId(ctx));
        return ApiResponse.of(AuditEntryView.from(row));
    }

    private static UUID safeTraceId(UserContext ctx) {
        if (ctx == null || ctx.traceId() == null) return null;
        try {
            return UUID.fromString(ctx.traceId());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
