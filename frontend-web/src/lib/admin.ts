import type { DisputeDecision, ResolveDisputeRequest } from '@/api/admin.api';

/**
 * Localisation key for an audit operation enum value. Mirrors
 * `payment.operation.*` so existing FR/AR/EN keys (TOPUP, ESCROW_*, REFUND,
 * PAYOUT, ADMIN_*) keep working. Unknown operations fall back to the raw enum.
 */
export function formatAuditOperation(operation: string | null | undefined): string {
  if (!operation) return '—';
  return `payment.operation.${operation}`;
}

/**
 * Validate a dispute resolution before sending it to the API.
 *
 * - REFUND  : no amounts required (full refund to student).
 * - PAY_PROF: no amounts required (full release to prof).
 * - SPLIT   : both `profShareMad` and `studentRefundMad` must be > 0.
 *
 * Returns null when valid, otherwise an i18n error key (caller is responsible
 * for translating).
 */
export function disputeResolutionValidation(
  body: ResolveDisputeRequest,
): string | null {
  const allowed: DisputeDecision[] = ['REFUND', 'PAY_PROF', 'SPLIT'];
  if (!allowed.includes(body.decision)) {
    return 'admin.disputes.errors.invalid_decision';
  }
  if (body.decision === 'SPLIT') {
    if (!isPositiveAmount(body.profShareMad)) {
      return 'admin.disputes.errors.prof_share_required';
    }
    if (!isPositiveAmount(body.studentRefundMad)) {
      return 'admin.disputes.errors.student_refund_required';
    }
  }
  return null;
}

function isPositiveAmount(raw: string | undefined): boolean {
  if (raw === undefined || raw === null) return false;
  const trimmed = String(raw).trim();
  if (trimmed.length === 0) return false;
  const n = Number(trimmed);
  return Number.isFinite(n) && n > 0;
}

/**
 * Trigger a browser download for a Blob (audit CSV export, etc.).
 *
 * Exported separately from `lib/format.ts` because it has a DOM side-effect
 * and we want the unit tests on `formatAuditOperation` to stay pure.
 */
export function downloadBlob(blob: Blob, filename: string): void {
  if (typeof window === 'undefined') return;
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.style.display = 'none';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
