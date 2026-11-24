/**
 * Re-reads the focused tab until the observation is stable.
 *
 * A revision change means an event arrived while the tab was being read, so the
 * observation may describe the previous tab. A retryable UNKNOWN means the URL
 * is not ready while the tab is loading. In both cases a fresh read is used
 * instead of reporting a false UNKNOWN. Every attempt reads the tab again; a
 * previous host is never reused.
 */
export async function settleObservation(observe, currentRevision, delay, attempts = 3) {
  for (let attempt = 0; attempt < attempts; attempt++) {
    const version = currentRevision();
    const report = await observe();
    if (version === currentRevision() && !report.retry) return report;
    if (attempt < attempts - 1) await delay();
  }
  // Bounded fallback: the latest fresh read. If it is UNKNOWN, the caller
  // reports UNKNOWN; privacy is never relaxed by this retry.
  return observe();
}
