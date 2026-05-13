package ma.naja7i.notification.domain;

/**
 * Canonical template keys shipped with v1. Filenames under
 * {@code src/main/resources/templates/{key}/{locale}.json}.
 */
public final class TemplateKey {

    private TemplateKey() {}

    public static final String OTP_CODE               = "otp_code";
    public static final String WELCOME_PROF           = "welcome_PROF";
    public static final String WELCOME_STUDENT        = "welcome_STUDENT";
    public static final String PASSWORD_RESET         = "password_reset";
    public static final String PROF_VALIDATED         = "prof_validated";
    public static final String PROF_REJECTED          = "prof_rejected";
    public static final String ADMIN_NEW_PROF_PENDING = "admin_new_prof_pending";
    public static final String ADMIN_NEW_TOPUP        = "admin_new_topup";
    public static final String TOPUP_DECIDED          = "topup_decided";
}
