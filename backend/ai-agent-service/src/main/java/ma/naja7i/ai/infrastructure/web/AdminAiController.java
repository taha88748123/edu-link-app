package ma.naja7i.ai.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.naja7i.ai.domain.AiAgent;
import ma.naja7i.ai.domain.AiCallLog;
import ma.naja7i.ai.infrastructure.persistence.AiCallLogRepository;
import ma.naja7i.ai.infrastructure.web.dto.AiCallEntryView;
import ma.naja7i.ai.infrastructure.web.dto.AiCallSummaryView;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Read-only admin views for the AI cost/observability dashboard.
 */
@RestController
@RequestMapping("/api/v1/ai/admin")
@RequiredArgsConstructor
@Tag(name = "AI — Admin", description = "Cost and observability views over ai_calls_log")
public class AdminAiController {

    private final AiCallLogRepository repository;

    @GetMapping("/calls")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Paginated calls within a time window, optionally filtered by agent")
    public ApiResponse<List<AiCallEntryView>> listCalls(@RequestParam(required = false) AiAgent agent,
                                                        @RequestParam(required = false) Instant from,
                                                        @RequestParam(required = false) Instant to,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        if (size <= 0 || size > 200) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "size must be in [1, 200]", Map.of("size", size));
        }
        Instant effectiveFrom = from != null ? from : Instant.EPOCH;
        Instant effectiveTo   = to   != null ? to   : Instant.now().plus(1, ChronoUnit.DAYS);
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AiCallLog> result = (agent == null)
                ? repository.findByCreatedAtBetween(effectiveFrom, effectiveTo, pageable)
                : repository.findByAgentAndCreatedAtBetween(agent, effectiveFrom, effectiveTo, pageable);
        return ApiResponse.of(
                result.map(AiCallEntryView::from).getContent(),
                new ApiResponse.PageMeta(result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @GetMapping("/calls/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aggregated KPIs per agent for the requested window")
    public ApiResponse<List<AiCallSummaryView>> summary(@RequestParam(required = false) Instant from,
                                                        @RequestParam(required = false) Instant to) {
        Instant effectiveFrom = from != null ? from : Instant.EPOCH;
        Instant effectiveTo   = to   != null ? to   : Instant.now().plus(1, ChronoUnit.DAYS);
        List<Object[]> raw = repository.summarise(effectiveFrom, effectiveTo);
        List<AiCallSummaryView> views = new ArrayList<>(raw.size());
        for (Object[] r : raw) {
            String agent = r[0] == null ? null
                    : (r[0] instanceof AiAgent a ? a.name() : r[0].toString());
            long count = r[1] == null ? 0L : ((Number) r[1]).longValue();
            BigDecimal totalCost = r[2] == null ? BigDecimal.ZERO
                    : (r[2] instanceof BigDecimal b ? b : new BigDecimal(r[2].toString()));
            double avgLatency = r[3] == null ? 0.0 : ((Number) r[3]).doubleValue();
            double successRate = r[4] == null ? 0.0 : ((Number) r[4]).doubleValue();
            views.add(new AiCallSummaryView(agent, count, totalCost, avgLatency, successRate));
        }
        return ApiResponse.of(views);
    }
}
