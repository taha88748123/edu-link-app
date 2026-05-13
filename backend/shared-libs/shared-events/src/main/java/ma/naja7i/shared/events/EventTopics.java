package ma.naja7i.shared.events;

/**
 * Single source of truth for Kafka topic names.
 * Keep in sync with docs/architecture/events-catalog.md.
 */
public final class EventTopics {

    private EventTopics() {}

    // --- auth
    public static final String AUTH_USER_REGISTERED   = "naja7i.auth.user.registered.v1";
    public static final String AUTH_OTP_REQUESTED     = "naja7i.auth.otp.requested.v1";
    public static final String AUTH_USER_LOGGED_IN    = "naja7i.auth.user.logged_in.v1";
    public static final String AUTH_PASSWORD_RESET    = "naja7i.auth.password.reset_requested.v1";
    public static final String AUTH_USER_SUSPENDED    = "naja7i.auth.user.suspended.v1";

    // --- profile
    public static final String PROFILE_PROF_SUBMITTED = "naja7i.profile.prof.submitted.v1";
    public static final String PROFILE_PROF_EVALUATED = "naja7i.profile.prof.evaluated.v1";
    public static final String PROFILE_PROF_VALIDATED = "naja7i.profile.prof.validated.v1";
    public static final String PROFILE_PROF_REJECTED  = "naja7i.profile.prof.rejected.v1";
    public static final String PROFILE_REVIEW_CREATED = "naja7i.profile.review.created.v1";

    // --- calendar
    public static final String CALENDAR_SLOT_CREATED  = "naja7i.calendar.slot.created.v1";
    public static final String CALENDAR_SLOT_BOOKED   = "naja7i.calendar.slot.booked.v1";
    public static final String CALENDAR_SLOT_RELEASED = "naja7i.calendar.slot.released.v1";

    // --- booking
    public static final String BOOKING_REQUESTED      = "naja7i.booking.requested.v1";
    public static final String BOOKING_ACCEPTED       = "naja7i.booking.accepted.v1";
    public static final String BOOKING_DECLINED       = "naja7i.booking.declined.v1";
    public static final String BOOKING_STARTED        = "naja7i.booking.started.v1";
    public static final String BOOKING_ENDED          = "naja7i.booking.ended.v1";
    public static final String BOOKING_CONFIRMED      = "naja7i.booking.confirmed.v1";
    public static final String BOOKING_CANCELLED      = "naja7i.booking.cancelled.v1";
    public static final String BOOKING_DISPUTED       = "naja7i.booking.disputed.v1";
    public static final String BOOKING_DISPUTE_RESOLVED = "naja7i.booking.dispute_resolved.v1";

    // --- payment
    public static final String PAYMENT_TOPUP_SUBMITTED = "naja7i.payment.topup.submitted.v1";
    public static final String PAYMENT_TOPUP_DECIDED   = "naja7i.payment.topup.decided.v1";
    public static final String PAYMENT_ESCROW_LOCKED   = "naja7i.payment.escrow.locked.v1";
    public static final String PAYMENT_ESCROW_RELEASED = "naja7i.payment.escrow.released.v1";
    public static final String PAYMENT_PAYOUT_REQUESTED = "naja7i.payment.payout.requested.v1";
    public static final String PAYMENT_PAYOUT_DECIDED   = "naja7i.payment.payout.decided.v1";
    public static final String PAYMENT_BALANCE_ADJUSTED = "naja7i.payment.balance.adjusted.v1";

    // --- chat
    public static final String CHAT_MESSAGE_POSTED    = "naja7i.chat.message.posted.v1";

    // --- video
    public static final String VIDEO_SESSION_STARTED  = "naja7i.video.session.started.v1";
    public static final String VIDEO_SESSION_ENDED    = "naja7i.video.session.ended.v1";

    // --- notification (observability)
    public static final String NOTIF_DELIVERED = "naja7i.notification.delivered.v1";
    public static final String NOTIF_FAILED    = "naja7i.notification.failed.v1";
}
