package ma.naja7i.payment.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.payment.application.TopupService;
import ma.naja7i.payment.domain.Topup;
import ma.naja7i.payment.domain.TopupStatus;
import ma.naja7i.payment.infrastructure.web.dto.TopupDecideRequest;
import ma.naja7i.payment.infrastructure.web.dto.TopupView;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment/admin/topups")
@RequiredArgsConstructor
@Tag(name = "Top-ups — Admin", description = "Review and decide pending top-ups")
public class AdminTopupController {

    private final TopupService topupService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List top-ups (default: pending)")
    public ApiResponse<List<TopupView>> list(@RequestParam(required = false, defaultValue = "EN_ATTENTE_ADMIN") TopupStatus status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        Page<Topup> result = topupService.listForAdmin(status, page, size);
        return ApiResponse.of(
                result.map(TopupView::from).getContent(),
                new ApiResponse.PageMeta(result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @PostMapping("/{id}:decide")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Decide a top-up (APPROVE / MODIFY / REJECT)")
    public ApiResponse<TopupView> decide(@AuthUser UserContext ctx,
                                         @PathVariable("id") UUID topupId,
                                         @Valid @RequestBody TopupDecideRequest req) {
        Long approvedMinor = null;
        if ("MODIFY".equals(req.decision())) {
            if (req.approvedAmountMad() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "approvedAmountMad required for MODIFY",
                        Map.of("topupId", topupId));
            }
            approvedMinor = req.approvedAmountMad().movePointRight(2).longValueExact();
        }
        UUID traceId = safeTraceId(ctx);
        Topup t = topupService.decide(topupId, ctx.userId(), req.decision(),
                approvedMinor, req.notes(), traceId);
        return ApiResponse.of(TopupView.from(t));
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
