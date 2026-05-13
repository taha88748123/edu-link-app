package ma.naja7i.payment.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.payment.domain.Balance;
import ma.naja7i.payment.infrastructure.persistence.BalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Read-side convenience for "what is my balance?". Always returns a value: a
 * user with no rows yet has implicit zero balance.
 */
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;

    @Transactional(readOnly = true)
    public Balance getOrZero(UUID userId) {
        return balanceRepository.findById(userId).orElseGet(() ->
                Balance.builder()
                        .userId(userId)
                        .balanceMinor(0L)
                        .currency("MAD")
                        .version(0)
                        .build());
    }
}
