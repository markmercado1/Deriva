export function classifyTab(window, tab) {
  const unknown = {state: "UNKNOWN", host: null};
  if (!window || !window.focused || window.incognito || window.type !== "normal"
      || !tab || tab.incognito || !tab.active || tab.windowId !== window.id) return unknown;
  try {
    const url = new URL(tab.url);
    if (url.protocol !== "https:" && url.protocol !== "http:") return {state: "INTERNAL", host: null};
    let host = url.hostname.toLowerCase().replace(/^www\./, "");
    if (!host || host.length > 253 || !host.split(".").every(label => /^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$/.test(label))) return unknown;
    return {state: "NORMAL", host};
  } catch { return unknown; }
}
