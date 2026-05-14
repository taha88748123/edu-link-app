/**
 * Best-effort post-run cleanup hooks.
 *
 * v1 strategy: rely on unique-per-run phones so two runs cannot collide.
 * Future versions may call a backend-side `DELETE /api/v1/dev/test/reset`
 * (not yet implemented) to wipe the test DB between runs.
 */

/** Stub: no-op. Kept so spec imports remain stable when we implement reset. */
export async function bestEffortCleanup(): Promise<void> {
  // Intentionally empty — see module comment.
}
