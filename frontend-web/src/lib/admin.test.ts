import { describe, expect, it } from 'vitest';

import { disputeResolutionValidation, formatAuditOperation } from './admin';

describe('formatAuditOperation', () => {
  it('returns the i18n key for known operations', () => {
    expect(formatAuditOperation('TOPUP')).toBe('payment.operation.TOPUP');
    expect(formatAuditOperation('ESCROW_RELEASE')).toBe('payment.operation.ESCROW_RELEASE');
    expect(formatAuditOperation('ADMIN_DEBIT')).toBe('payment.operation.ADMIN_DEBIT');
  });

  it('returns an em-dash for null / undefined / empty', () => {
    expect(formatAuditOperation(null)).toBe('—');
    expect(formatAuditOperation(undefined)).toBe('—');
    expect(formatAuditOperation('')).toBe('—');
  });

  it('passes through unknown operation names', () => {
    expect(formatAuditOperation('FUTURE_OP')).toBe('payment.operation.FUTURE_OP');
  });
});

describe('disputeResolutionValidation', () => {
  it('accepts REFUND without amounts', () => {
    expect(disputeResolutionValidation({ decision: 'REFUND' })).toBeNull();
  });

  it('accepts PAY_PROF without amounts', () => {
    expect(disputeResolutionValidation({ decision: 'PAY_PROF' })).toBeNull();
  });

  it('rejects SPLIT when prof share is missing', () => {
    expect(
      disputeResolutionValidation({ decision: 'SPLIT', studentRefundMad: '50' }),
    ).toBe('admin.disputes.errors.prof_share_required');
  });

  it('rejects SPLIT when student refund is missing', () => {
    expect(
      disputeResolutionValidation({ decision: 'SPLIT', profShareMad: '30' }),
    ).toBe('admin.disputes.errors.student_refund_required');
  });

  it('rejects SPLIT when amounts are not positive', () => {
    expect(
      disputeResolutionValidation({
        decision: 'SPLIT',
        profShareMad: '0',
        studentRefundMad: '50',
      }),
    ).toBe('admin.disputes.errors.prof_share_required');
    expect(
      disputeResolutionValidation({
        decision: 'SPLIT',
        profShareMad: '30',
        studentRefundMad: '-5',
      }),
    ).toBe('admin.disputes.errors.student_refund_required');
  });

  it('accepts a fully-specified SPLIT', () => {
    expect(
      disputeResolutionValidation({
        decision: 'SPLIT',
        profShareMad: '70',
        studentRefundMad: '30',
      }),
    ).toBeNull();
  });

  it('rejects an unknown decision', () => {
    expect(
      disputeResolutionValidation({
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        decision: 'NUKE' as any,
      }),
    ).toBe('admin.disputes.errors.invalid_decision');
  });
});
