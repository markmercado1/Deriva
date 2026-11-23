# Gate Q2 — reabrir el navegador

Objetivo: decidir si se puede habilitar **un navegador y un perfil** sin violar D4. No sustituye la decisión A13 mientras no haya evidencia completa.

Regla de una sola sesión: si el gate falla, se registra el resultado (E011) y se continúa F3.1 con navegadores excluidos. No se repite hasta tener una hipótesis nueva.

## Lista corta de pruebas manuales

El usuario las ejecuta y reporta el resultado. No marcarlas como ejecutadas sin su confirmación.

1. **Segundo perfil**: con el perfil A en `example.com` y la extensión conectada, abre el perfil B de Chrome y mantenlo enfocado 10 s. Vuelve a A. ¿A mostró `UNKNOWN`/sin host mientras B tenía el foco, o conservó `example.com`?
2. **Ventana privada**: desde A, abre incógnito, ve a `example.org`, mantenlo enfocado 10 s y vuelve a A. ¿A mostró `UNKNOWN` y ningún host privado?
3. **Pérdida de foco**: en A con `example.com`, cambia a Notepad o a la terminal 10 s y vuelve. ¿Pasó a `UNKNOWN` y recuperó `example.com`?
4. **Reinicio**: cierra Chrome completo y vuelve a abrir el perfil A en `example.com`. ¿La extensión reconectó sola y volvió a reportar?
5. **Cierre ordenado**: detén el bridge con `Ctrl+C`. ¿La extensión mostró “Bridge no disponible” sin errores ni reportes acumulados?

No copiar hosts privados, tokens ni títulos a la documentación.

## Registro de diagnóstico

`Start-BridgePilot.ps1` activa el log en `work/pilot/bridge-reports.log`. Cada reporte **aceptado** añade una línea:

```text
timestamp<TAB>seq<TAB>state<TAB>hostPresent
```

`hostPresent` es `yes` o `no`; el host real nunca se escribe. No hay token, URL ni título. El archivo se puede abrir después de cada prueba sin DevTools. Solo se registran reportes aceptados; los rechazos no se escriben. Es temporal para este gate.

## Prueba de una hora

Condición: una pestaña normal en primer plano, sin tocar el worker MV3 ni el código de la extensión. Evitar suspensión/bloqueo de Windows durante la hora; si ocurre, la ejecución no cuenta.

```powershell
powershell -NoProfile -File .\scripts\Measure-BridgeReports.ps1 -ExtensionId IDENTIFICADOR_REAL -DurationMinutes 60
```

El script lee el token local y consulta `/v1/status` cada 250 ms. Solo usa el contador `accepted`; no guarda ni imprime hosts. Mide el hueco entre incrementos de reportes y cuenta cuántos superan 3, 5 y 10 s.

Criterio:

- PASS: las pruebas manuales pasan, `over3s = 0`, `maxGapMs < 3000`, sin errores de estado ni reinicios del bridge.
- FAIL: cualquier prueba manual falla o hay algún hueco >3 s. Registrar E011; no habilitar captura.

## Resultado — PASS (E011)

El criterio fue **frescura de reportes y UNKNOWN al perder foco**, no correlación HWND. Se acepta un límite de ~1 s.

- Segundo perfil: repetido y confirmado; UNKNOWN/no con B enfocado y NORMAL/yes <1 s al volver a A.
- Privado: UNKNOWN/no durante 10 s.
- Foco: UNKNOWN al salir, NORMAL al volver.
- Reinicio: secuencia nueva y reconexión.
- Ctrl+C: el primer intento falló y se cerró con Stop-Process; el segundo detuvo el bridge; causa del primer fallo no investigada. No se marca como cierre ordenado validado.
- Una hora: 13.680 muestras, 3.741 reportes, `maxGapMs=1088`, `over3s=0`, `over5s=0`, `over10s=0`, 0 errores, 0 reinicios, `tailGapMs=531`.

Decisión A15: PASS para **Chrome + un perfil**. F2.4 implementa la allowlist y la propagación del host. El resto de navegadores/perfiles sigue excluido. La captura no está activa todavía.

## Brave — gate reducido (A19/E015)

- Ventana privada: UNKNOWN/no, sin NORMAL yes (reporte del usuario).
- `brave.exe` permitido con las mismas reglas que Chrome: solo NORMAL/fresco; si no, EXCLUDED.
- Pendientes: segundo perfil, reinicio, una hora y colisión de sesiones Chrome/Brave.
- La extensión de Brave envía `browser:"chrome"` y `BrowserStateStore` mantiene una sola sesión. No ejecutar Chrome y Brave simultáneamente contra el mismo bridge; el segundo invalida la sesión del primero.
