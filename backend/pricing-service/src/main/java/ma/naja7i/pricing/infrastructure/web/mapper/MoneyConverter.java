package ma.naja7i.pricing.infrastructure.web.mapper;

import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Edge conversions between human-readable MAD strings ("150.00") and machine-friendly
 * centimes ({@code long amountMinor}). Centralised here to keep the rounding rule consistent.
 *
 * <p>Rule: HALF_UP rounding, 2 decimals — the smallest unit of MAD currency.</p>
 */
public final class MoneyConverter {

    private MoneyConverter() {}

    /** Parse a decimal MAD string into centimes. Throws {@link BusinessException} on invalid input. */
    public static long mad2Minor(String mad) {
        if (mad == null || mad.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "amount required",
                    Map.of("field", "amountMad"));
        }
        BigDecimal bd;
        try {
            bd = new BigDecimal(mad);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "invalid decimal: " + mad,
                    Map.of("field", "amountMad"));
        }
        if (bd.signum() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "amount must be non-negative",
                    Map.of("field", "amountMad"));
        }
        return bd.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    /** Pretty-print centimes as a 2-decimal MAD string. */
    public static String minor2Mad(long minor) {
        return BigDecimal.valueOf(minor)
                .movePointLeft(2)
                .setScale(2, RoundingMode.UNNECESSARY)
                .toPlainString();
    }
}
