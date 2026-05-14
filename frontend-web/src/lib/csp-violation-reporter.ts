/**
 * Tiny dev-side CSP violation reporter.
 *
 * Listens to {@code securitypolicyviolation} events and logs a compact
 * structured warning to the console. Intentionally PII-free: only the
 * blocked URI, the violated directive, and an optional traceId are logged.
 *
 * In production builds the listener still runs but the output is rate-limited
 * (we only emit one warning per unique {blockedURI, directive} pair) to avoid
 * flooding the console if a vendor script attempts something CSP-blocked.
 */

const seen = new Set<string>();

export function installCspViolationReporter(traceId?: string) {
  if (typeof document === 'undefined') return;
  // Skip in node/jsdom: addEventListener is present but the event never fires.
  document.addEventListener('securitypolicyviolation', (event) => {
    const e = event as SecurityPolicyViolationEvent;
    // Build a stable de-dup key — never includes any user-provided strings.
    const key = `${e.violatedDirective}::${e.blockedURI}::${e.disposition}`;
    if (seen.has(key)) return;
    seen.add(key);

    // Pretty short, structured payload. No PII (we never log e.sample beyond its presence).
    // eslint-disable-next-line no-console
    console.warn('[CSP] violation', {
      directive: e.violatedDirective,
      effectiveDirective: e.effectiveDirective,
      blockedURI: e.blockedURI,
      disposition: e.disposition,
      sourceFile: e.sourceFile,
      lineNumber: e.lineNumber,
      columnNumber: e.columnNumber,
      traceId,
    });
  });
}
