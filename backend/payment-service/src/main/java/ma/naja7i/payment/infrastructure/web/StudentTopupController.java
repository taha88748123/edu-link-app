package ma.naja7i.payment.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.payment.application.TopupService;
import ma.naja7i.payment.domain.Topup;
import ma.naja7i.payment.domain.TopupStatus;
import ma.naja7i.payment.infrastructure.web.dto.CreateTopupRequest;
import ma.naja7i.payment.infrastructure.web.dto.TopupView;
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
@RequestMapping("/api/v1/payment/topups")
@RequiredArgsConstructor
@Tag(name = "Top-ups — Student", description = "Submit and list top-up requests")
public class StudentTopupController {

    private final TopupService topupService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a top-up request")
    public ApiResponse<TopupView> submit(@AuthUser UserContext ctx,
                                         @Valid @RequestBody CreateTopupRequest req) {
        long minor = req.amountMad().movePointRight(2).longValueExact();
        Topup t = topupService.submit(ctx.userId(), req.method(), minor, req.proofUrl());
        return ApiResponse.of(TopupView.from(t));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "List my top-up requests")
    public ApiResponse<List<TopupView>> myTopups(@AuthUser UserContext ctx,
                                                 @RequestParam(required = false) TopupStatus status,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        Page<Topup> result = topupService.listForStudent(ctx.userId(), status, page, size);
        return ApiResponse.of(
                result.map(TopupView::from).getContent(),
                new ApiResponse.PageMeta(result.getNumber(), result.getSize(), result.getTotalElements()));
    }
}
