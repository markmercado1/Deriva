const form = document.getElementById("settings");
const port = document.getElementById("port");
const token = document.getElementById("token");
const status = document.getElementById("status");
const saved = await chrome.storage.local.get(["port", "token"]);
port.value = saved.port ?? 47821;
token.value = saved.token ?? "";
form.addEventListener("submit", async event => {
  event.preventDefault();
  const value = Number(port.value);
  if (!Number.isInteger(value) || value < 1024 || value > 65535 || !/^[0-9a-f]{64}$/.test(token.value)) {
    status.textContent = "Revisa puerto y token hexadecimal de 64 caracteres."; return;
  }
  await chrome.storage.local.set({port: value, token: token.value});
  status.textContent = "Conexión guardada. Captura deshabilitada.";
});
setInterval(async () => {
  try { status.textContent = (await chrome.runtime.sendMessage({type: "status"})).status; }
  catch { status.textContent = "Worker no disponible"; }
}, 2000);
