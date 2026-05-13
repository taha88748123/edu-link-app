package ma.naja7i.payment.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.payment.application.PayoutService;
import ma.naja7i.payment.domain.Payout;
import ma.naja7i.payment.domain.PayoutStatus;
import ma.naja7i.payment.infrastructure.web.dto.CreatePayoutRequest;
import ma.naja7i.payment.infrastructure.web.dto.PayoutView;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payment/payouts")
@RequiredArgsConstructor
@Tag(name = "Payouts — Prof", description = "Request and list payouts")
public class ProfPayoutController {

    private final PayoutService payoutService;

    @PostMapping
    @PreAuthorize("hasRole('PROF')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Request a payout to my bank account")
    public ApiResponse<PayoutView> request(@AuthUser UserContext ctx,
                                           @Valid @RequestBody CreatePayoutRequest req) {
        long minor = req.amountMad().movePointRight(2).longValueExact();
        Payout p = payoutService.request(ctx.userId(), minor, req.ibanRef());
        return ApiResponse.of(PayoutView.from(p));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "List my payout requests")
    public ApiResponse<List<PayoutView>> myPayouts(@AuthUser UserContext ctx,
                                                   @RequestParam(required = false) PayoutStatus status,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        Page<Payout> result = payoutService.listForProf(ctx.userId(), status, page, size);
        return ApiResponse.of(
                result.map(PayoutView::from).getContent(),
                new ApiResponse.PageMeta(result.getNumber(), result.getSize(), result.getTotalElements()));
    }
}
