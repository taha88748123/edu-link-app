package ma.naja7i.payment.infrastructure.web.dto;

import ma.naja7i.payment.domain.Balance;

import java.math.BigDecimal;

public record BalanceView(String balanceMad, long balanceMinor, String currency) {

    public static BalanceView from(Balance b) {
        long minor = b.getBalanceMinor() == null ? 0L : b.getBalanceMinor();
        return new BalanceView(BigDecimal.valueOf(minor).movePointLeft(2).toPlainString(),
                minor, b.getCurrency() == null ? "MAD" : b.getCurrency());
    }
}
