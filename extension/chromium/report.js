export function inspectTab(window, tab) {
  const unknown = (retry = false) => ({state: "UNKNOWN", host: null, retry});
  if (!window || !window.focused || window.incognito || window.type !== "normal") return unknown();
  if (!tab || tab.incognito || !tab.active || tab.windowId !== window.id) return unknown();
  try {
    const url = new URL(tab.url);
    if (url.protocol !== "https:" && url.protocol !== "http:") return {state: "INTERNAL", host: null, retry: false};
    let host = url.hostname.toLowerCase().replace(/^www\./, "");
    if (!host || host.length > 253 || !host.split(".").every(label => /^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$/.test(label))) return unknown();
    return {state: "NORMAL", host, retry: false};
  } catch {
    // While a tab is loading, its URL can be empty for a fraction of a second.
    // That transient is retryable; a stable invalid URL is still unknown.
    return unknown(tab.status === "loading");
  }
}

export function classifyTab(window, tab) {
  const {state, host} = inspectTab(window, tab);
  return {state, host};
}
