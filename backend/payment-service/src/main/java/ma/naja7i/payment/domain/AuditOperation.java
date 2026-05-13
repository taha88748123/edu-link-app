package ma.naja7i.payment.domain;

/**
 * Operation kind for an {@link AuditLog} row. Matches the DB CHECK constraint
 * in V1__init.sql exactly.
 */
public enum AuditOperation {
    TOPUP,
    ESCROW_LOCK,
    ESCROW_RELEASE,
    REFUND,
    PAYOUT,
    ADMIN_ADJUST,
    ADMIN_CREDIT,
    ADMIN_DEBIT
}
