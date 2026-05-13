package ma.naja7i.shared.dto;

import java.math.BigDecimal;

/**
 * Amount stored in minor units (centimes) — never use float/double for money.
 * MAD has 2 decimals, so 12.50 MAD == new Money(1250, MAD).
 */
public record Money(long amountMinor, Currency currency) {

    public Money {
        if (currency == null) throw new IllegalArgumentException("currency must not be null");
    }

    public static Money mad(long centimes) {
        return new Money(centimes, Currency.MAD);
    }

    public static Money madFromMajor(BigDecimal majorAmount) {
        long minor = majorAmount.movePointRight(2).longValueExact();
        return new Money(minor, Currency.MAD);
    }

    public BigDecimal toMajor() {
        return BigDecimal.valueOf(amountMinor).movePointLeft(2);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amountMinor + other.amountMinor, currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amountMinor - other.amountMinor, currency);
    }

    public boolean isPositive() {
        return amountMinor > 0;
    }

    public boolean isZero() {
        return amountMinor == 0;
    }

    private void requireSameCurrency(Money other) {
        if (other.currency != currency) {
            throw new IllegalArgumentException("currency mismatch: " + currency + " vs " + other.currency);
        }
    }
}
