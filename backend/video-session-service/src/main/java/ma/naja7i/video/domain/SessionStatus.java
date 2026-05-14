package ma.naja7i.video.domain;

/**
 * Session lifecycle status. SCHEDULED is the initial state once a booking is
 * accepted; LIVE/ENDED are driven by participant :start / :end actions; FAILED
 * and EXPIRED are admin/sweep terminal states.
 */
public enum SessionStatus {
    SCHEDULED,
    LIVE,
    ENDED,
    FAILED,
    EXPIRED
}
