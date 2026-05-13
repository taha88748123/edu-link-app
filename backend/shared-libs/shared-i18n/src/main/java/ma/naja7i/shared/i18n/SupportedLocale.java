package ma.naja7i.shared.i18n;

import java.util.Arrays;
import java.util.Locale;

public enum SupportedLocale {
    FR("fr", false),
    AR("ar", true),
    EN("en", false);

    private final String code;
    private final boolean rtl;

    SupportedLocale(String code, boolean rtl) {
        this.code = code;
        this.rtl = rtl;
    }

    public String code() { return code; }
    public boolean rtl() { return rtl; }
    public Locale toLocale() { return Locale.forLanguageTag(code); }

    public static SupportedLocale fromCodeOrDefault(String code) {
        if (code == null) return FR;
        return Arrays.stream(values())
                .filter(l -> l.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(FR);
    }
}
