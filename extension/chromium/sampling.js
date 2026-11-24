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
    // A genuine UNKNOWN (incognito, lost focus, closed tab, stable invalid URL) is
    // reported immediately even if an event raced the read: never delay a privacy
    // boundary. NORMAL/INTERNAL observations are re-read when the revision changed,
    // because they may describe the previous tab.
    if (!report.retry && (version === currentRevision() || report.state === "UNKNOWN")) return report;
    if (attempt < attempts - 1) await delay();
  }
  // Bounded fallback: the latest fresh read. If it is UNKNOWN, the caller
  // reports UNKNOWN; privacy is never relaxed by this retry.
  return observe();
}
