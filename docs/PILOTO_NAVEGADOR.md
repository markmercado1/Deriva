# F1.2 — piloto de extensión y bridge

El piloto recibe reportes **en memoria**, no registra actividad ni escribe hosts en disco. CaptureEnabled es siempre false. No sustituye la validación D4 ni la correlación con la ventana activa de Windows (pendiente).

## Ejecutar y probar manualmente

1. Usar Chrome con un perfil de prueba sin cuenta ni navegación personal. Abrir chrome://extensions, habilitar modo desarrollador y cargar descomprimida la carpeta `C:\AppTImeControl\extension\chromium`. No habilitar incógnito: el manifiesto lo prohíbe.
2. Copiar el identificador de 32 letras mostrado por Chrome. Se permite exactamente ese origen en el servidor; no usar un ID de ejemplo.
3. Compilar desde C:\AppTImeControl:

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
node --test extension/tests/report.test.mjs
powershell -NoProfile -File .\scripts\Start-BridgePilot.ps1 -ExtensionId IDENTIFICADOR_REAL
```

El script genera un token aleatorio en work/pilot/bridge-token.txt, ignorado por Git. No imprimirlo en logs ni pegarlo en chats. El proceso queda activo hasta Ctrl+C. No ejecuta el navegador ni modifica perfiles.

4. Abrir las opciones de la extensión (clic en su icono), indicar puerto 47821 y pegar el token local. Guardar. Esperar el estado “Bridge conectado; captura deshabilitada”. Si aparece 403, conservar la protección: registrar el Origin real desde DevTools de la extensión sin copiar headers/token a documentación. No abrir CORS para hacer pasar la prueba.
5. Con sitios de prueba sin cuenta, comprobar normal, cambio de pestaña, ventana interna, pérdida de foco, ventana privada con la normal abierta, regreso, cierre y reapertura del navegador y del bridge.
6. Detener el bridge con Ctrl+C. La extensión debe mostrar “Bridge no disponible” sin errores no capturados ni acumulación de reportes.

Para diagnóstico local puede consultarse GET /v1/status con X-Deriva-Token y Origin exacto desde una herramienta HTTP local. Muestra solo el último host vigente, estado, secuencia y contador; no mantener ese resultado en logs. El host desaparece de esa respuesta al caducar (3 segundos) o llegar UNKNOWN. No muestra un historial. La interfaz de opciones nunca muestra el host.

`Start-BridgePilot.ps1` activa además `work/pilot/bridge-reports.log`, un registro temporal por reporte aceptado con timestamp, secuencia, estado y `hostPresent=yes/no`. Nunca escribe el host, token, URL ni título; se puede revisar después de cada prueba sin DevTools. Está ignorado por Git y se retira al cerrar el gate Q2.

## Protocolo del piloto

- GET /v1/ping: sin token; devuelve identificación de app/versión, nunca un host.
- POST /v1/session: token y Origin exacto; cuerpo `{"browser":"chrome"}`. Devuelve una sesión aleatoria del servidor e invalida la anterior.
- POST /v1/tab: campos exactos browser, session, seq, state y host. seq entero positivo creciente; state NORMAL/INTERNAL/UNKNOWN. Host obligatorio solo para NORMAL; en los demás debe ser null.
- GET /v1/status: autenticado y origen exacto. Diagnóstico temporal, captureEnabled=false.
- Rechazos: 400 cuerpo/esquema, 401 token, 403 Host/origen, 404 ruta, 405 método, 409 sesión/secuencia, 413 cuerpo >2 KB, 429 >20 peticiones/segundo.
- Validación: Host → límite de peticiones → ruta/query/ping → token → Origin → método/contenido/esquema → sesión/secuencia. No CORS abierto, no redirecciones y no log de payload.

El parser implementa solo objetos planos de strings/null/enteros no negativos. Rechaza duplicados, anidación, campos extra, trailing input, números ambiguos, UTF-8 malformado y escapes no admitidos. No es una biblioteca JSON general. Ninguna dependencia añadida al pom.

## Límites que no se deben ocultar

- Un reporte de extensión NO prueba qué HWND de Windows está en primer plano. Varias ventanas/perfiles pueden producir ambigüedad; por eso no se habilita captura.
- Se consulta la ventana normal enfocada y se excluyen ventanas/pestañas privadas antes de extraer la URL. Eso debe probarse en el navegador real; los tests de clasificación no bastan.
- Eventos y un timer de 1 segundo producen lecturas nuevas. Alarms cada 30 segundos solo despierta para consultar de nuevo. Si MV3 duerme o se retrasa, el servidor caduca el reporte a los 3 segundos; no hay heartbeat que renueve hosts antiguos.
- Las sesiones son efímeras. Reiniciar el worker o servidor exige nueva sesión; 409 descarta la anterior. No se encolan peticiones ni se guardan hosts en storage.local (solo puerto/token).
- Puertos del piloto y límites de recursos son para diagnóstico local. Antes de habilitar un servicio permanente, F2.3 debe revisar correlación nativa, renovación y resistencia a conexiones HTTP lentas; los dos hilos no garantizan protección frente a saturación local.

## Evidencia actual

2026-11-22: Maven verify offline PASÓ con 11 tests (1 bootstrap, 4 de estado, 6 de HTTP/parser); Node PASÓ con 5 tests de clasificación. El transporte HTTP de Java se probó con sockets reales.

2026-11-22 — Validación real en Chrome (E004): extensión cargada en perfil de prueba y bridge conectado. Línea base 5/5 NORMAL example.com; incógnito real 12/12 UNKNOWN sin host; regreso 11/12 NORMAL example.com; reinicio de Chrome con sesión nueva y 6/12 NORMAL example.com. Foco perdido/recuperado en la primera observación. BR-01 PASÓ; BR-02 PARCIAL: segundo perfil, caducidad exacta de 3 s y cierre ordenado con Ctrl+C quedan pendientes. CaptureEnabled=false en todas las muestras; no se habilitó captura.

Los intentos previos con perfiles aislados de Chrome/Edge/Brave quedan como historial: Chrome y Brave terminaron con código 2147483651; Edge terminó sin exponer el endpoint de depuración. No se cambió ningún perfil personal ni se desactivaron protecciones del navegador. No interpretar esos intentos como validación; la evidencia válida es E004.

## Decisión F2.3 (2026-11-22)

A13: los navegadores quedan **excluidos** y `browser_capture_enabled` sigue en false. La extensión y el bridge se conservan como piloto diagnóstico; no se guardan hosts ni tiempo de sitios. Motivo: Q2 sin correlación fiable HWND/perfil, segundo perfil no probado y caducidad exacta/cierre ordenado pendientes. No habilitar captura ni relajar las protecciones para “hacer pasar” una prueba. Solo una nueva decisión escrita, con esa evidencia, puede reabrir la integración.

## Resultado GATE-Q2 (2026-11-22)

PASS para **Chrome + un perfil** (A15/E011). El criterio fue frescura de reportes y UNKNOWN al perder foco, no correlación HWND; se acepta un límite de ~1 s. Segundo perfil repetido (UNKNOWN/no→NORMAL/yes), privado UNKNOWN/no, foco UNKNOWN/NORMAL, reinicio con secuencia nueva y una hora con `maxGapMs=1088`, `over3s=0`, 0 errores, 0 reinicios. Ctrl+C: primer intento falló y se cerró con Stop-Process; segundo detuvo el bridge; causa no investigada. F2.4 implementó la allowlist y la propagación del host en `PrivacyFilter`, pero la app aún no compone el proveedor ni tiene bucle/UI, así que **la captura real sigue desactivada**. El resto de navegadores/perfiles sigue excluido.

Referencias consultadas: [permisos de red](https://developer.chrome.com/docs/extensions/develop/concepts/network-requests), [ciclo de vida MV3](https://developer.chrome.com/docs/extensions/develop/concepts/service-workers/lifecycle/), [pruebas de extensiones](https://playwright.dev/docs/chrome-extensions). Las guías no sustituyen resultados en este equipo.
