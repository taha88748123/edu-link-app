package ma.naja7i.auth.domain;

public final class AuditAction {

    private AuditAction() {}

    public static final String REGISTER         = "REGISTER";
    public static final String OTP_VERIFIED     = "OTP_VERIFIED";
    public static final String OTP_RESENT       = "OTP_RESENT";
    public static final String LOGIN_SUCCESS    = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILURE    = "LOGIN_FAILURE";
    public static final String TOKEN_REFRESHED  = "TOKEN_REFRESHED";
    public static final String LOGOUT           = "LOGOUT";
    public static final String PASSWORD_RESET_REQUESTED = "PASSWORD_RESET_REQUESTED";
    public static final String PASSWORD_RESET   = "PASSWORD_RESET";
    public static final String SUSPENDED        = "SUSPENDED";
}
