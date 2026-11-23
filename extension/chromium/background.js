import {classifyTab} from "./report.js";

let session = null;
let sequence = 0;
let busy = false;
let revision = 0;
let lastStatus = "Sin configurar";

async function config() {
  const c = await chrome.storage.local.get(["port", "token"]);
  if (!Number.isInteger(c.port) || c.port < 1024 || c.port > 65535 || !/^[0-9a-f]{64}$/.test(c.token ?? "")) return null;
  return c;
}

async function post(c, path, body) {
  return fetch(`http://127.0.0.1:${c.port}/v1/${path}`, {
    method: "POST", headers: {"Content-Type": "application/json", "X-Deriva-Token": c.token},
    body: JSON.stringify(body), cache: "no-store", credentials: "omit", signal: AbortSignal.timeout(1500)
  });
}

async function observe() {
  const windows = await chrome.windows.getAll({windowTypes: ["normal"]});
  const focused = windows.filter(w => w.focused);
  if (focused.length !== 1 || focused[0].incognito) return {state: "UNKNOWN", host: null};
  const w = focused[0];
  const tabs = await chrome.tabs.query({windowId: w.id, active: true});
  if (tabs.length !== 1) return {state: "UNKNOWN", host: null};
  const verified = await chrome.windows.get(w.id);
  return classifyTab(verified, tabs[0]);
}

async function sample() {
  if (busy) return; // No backlog and no replay of old observations.
  busy = true;
  const version = revision;
  try {
    const c = await config();
    if (!c) { lastStatus = "Sin configurar"; return; }
    if (!session) {
      const handshake = await post(c, "session", {browser: "chrome"});
      if (!handshake.ok) { lastStatus = `Rechazado (${handshake.status})`; return; }
      session = (await handshake.json()).session;
      sequence = 0;
    }
    let report = await observe();
    if (version !== revision) report = {state: "UNKNOWN", host: null};
    const response = await post(c, "tab", {browser: "chrome", session, seq: ++sequence, ...report});
    if (response.status === 409) session = null;
    lastStatus = response.ok ? "Bridge conectado; captura deshabilitada" : `Rechazado (${response.status})`;
  } catch {
    session = null;
    lastStatus = "Bridge no disponible"; // Never log URLs, hosts, tokens or request bodies.
  } finally { busy = false; }
}

function changed() { revision++; void sample(); }
chrome.tabs.onActivated.addListener(changed);
chrome.tabs.onUpdated.addListener((_id, info) => { if (info.url !== undefined || info.status === "complete") changed(); });
chrome.windows.onFocusChanged.addListener(changed);
chrome.tabs.onRemoved.addListener(changed);
chrome.runtime.onStartup.addListener(changed);
chrome.runtime.onInstalled.addListener(() => { void chrome.alarms.create("resample", {periodInMinutes: 0.5}); changed(); });
chrome.alarms.onAlarm.addListener(alarm => { if (alarm.name === "resample") changed(); });
chrome.storage.onChanged.addListener((_changes, area) => { if (area === "local") { session = null; changed(); } });
chrome.action.onClicked.addListener(() => { void chrome.runtime.openOptionsPage(); });
chrome.runtime.onMessage.addListener((message, _sender, respond) => {
  if (message?.type === "status") { respond({status: lastStatus, captureEnabled: false}); }
});
// Best effort while the worker runs. Sleeping workers do not extend the 3 s server expiry.
setInterval(() => { void sample(); }, 1000);
void sample();
