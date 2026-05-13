package ma.naja7i.payment.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.payment.application.PayoutService;
import ma.naja7i.payment.domain.Payout;
import ma.naja7i.payment.domain.PayoutStatus;
import ma.naja7i.payment.infrastructure.web.dto.PayoutDecideRequest;
import ma.naja7i.payment.infrastructure.web.dto.PayoutMarkPaidRequest;
import ma.naja7i.payment.infrastructure.web.dto.PayoutView;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment/admin/payouts")
@RequiredArgsConstructor
@Tag(name = "Payouts — Admin", description = "Approve / reject / mark-paid payouts")
public class AdminPayoutController {

    private final PayoutService payoutService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List payouts (default: REQUESTED)")
    public ApiResponse<List<PayoutView>> list(@RequestParam(required = false, defaultValue = "REQUESTED") PayoutStatus status,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        Page<Payout> result = payoutService.listForAdmin(status, page, size);
        return ApiResponse.of(
                result.map(PayoutView::from).getContent(),
                new ApiResponse.PageMeta(result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @PostMapping("/{id}:decide")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Decide a payout (APPROVE -> debits balance, REJECT -> no-op)")
    public ApiResponse<PayoutView> decide(@AuthUser UserContext ctx,
                                          @PathVariable("id") UUID payoutId,
                                          @Valid @RequestBody PayoutDecideRequest req) {
        Payout p = payoutService.decide(payoutId, ctx.userId(), req.decision(),
                req.notes(), safeTraceId(ctx));
        return ApiResponse.of(PayoutView.from(p));
    }

    @PostMapping("/{id}:markPaid")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark an APPROVED payout as PAID (bank transfer confirmed)")
    public ApiResponse<PayoutView> markPaid(@AuthUser UserContext ctx,
                                            @PathVariable("id") UUID payoutId,
                                            @Valid @RequestBody PayoutMarkPaidRequest req) {
        Payout p = payoutService.markPaid(payoutId, ctx.userId(), req.paymentRef());
        return ApiResponse.of(PayoutView.from(p));
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
