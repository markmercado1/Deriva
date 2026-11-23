# F3.1b — integración mínima con salida por consola

Estado: **implementado como F3.1b (E014)**. No sustituye F4.3 ni F5.1; es una rebanada vertical de diagnóstico.

Objetivo: ver el primer día de datos reales con una rebanada de consola, sin UI ni bandeja en esa rebanada. F3.2 añade la persistencia de drift; F4.1/F4.2/F4.3 añaden UI de lectura, anotación y Uso diario; F5.1a permite iniciar/detener la captura real desde la UI. La entrada de consola sigue como diagnóstico.

## Cómo ejecutarlo

```powershell
# Chrome o Brave, nunca ambos contra el mismo bridge
powershell -NoProfile -File .\scripts\Start-ConsoleCapture.ps1 -ExtensionId TU_EXTENSION_ID -Port 47821
```

El script resuelve el classpath Maven de SQLite/JNA, activa el bridge, usa `work/console-day/deriva.db` y ofrece un stop file:

```powershell
New-Item -ItemType File 'C:\AppTImeControl\work\console-day\STOP' -Force
```

## Alcance mínimo

Nuevo punto de entrada temporal `com.deriva.app.ConsoleCapture`:

1. Compone en un solo proceso: `WindowsWindowSensor`, `WindowsIdleDetector`, `BrowserStateStore` + `LocalBridgeServer`, `PrivacyFilter` con `store::freshNormalHost`, `Segmenter` y `SQLiteSegmentRepository`. Navegadores permitidos: Chrome y Brave, uno a la vez; ambos payloads usan `browser:"chrome"` y comparten una sola sesión del bridge.
2. Ejecuta un bucle de muestreo cada 1 s en un único hilo.
3. Por cada tick: muestra → idle → privacidad → segmentador. Guarda los segmentos cerrados y los anuncia en consola.
4. Al detener con Ctrl+C o stop file: cierra el segmento abierto, guarda, detiene el bridge y muestra un resumen del día.
5. Ruta de datos: `work/console-day/deriva.db` (primera prueba); `%APPDATA%\DerivaPrototype\deriva.db` queda para una entrega posterior.
6. Verificación de privacidad al arrancar: `SQLitePrivacyCheck` rechaza columnas URL/título y hosts con ruta/query; UNKNOWN/privado no guarda host.
7. Salida aproximada:

```text
10:00:01 OBSERVED notepad.exe
10:00:04 OBSERVED chrome.exe chess.com
10:05:00 segment OBSERVED chrome.exe chess.com 10:00:04 -> 10:05:00
10:06:00 IDLE
...
Resumen de hoy
  notepad.exe   00:42:10
  chrome.exe    01:15:30
    chess.com   00:55:10
    youtube.com 00:20:20
  Sin datos registrados fuera de cobertura; sitios no registrados se muestran como tales.
```

`Segmenter.close(Instant)` está implementado y probado. El resumen es un agregado temporal en `app` (`ConsoleSummary`), no el `DailyUsageCalculator` definitivo de F4.3. Cada segmento cerrado se guarda inmediatamente; el stop file se probó como alternativa a Ctrl+C.

## ¿Rompe el alcance del plan?

No cambia el producto ni la arquitectura: usa `app` para composición, los puertos existentes y los adaptadores ya probados. No añade navegadores, métricas nuevas ni UI.

Sí cambia el **orden**: adelanta una parte de la integración prevista para F5.1 y una versión mínima de la vista de Uso diario de F4.3. Se implementó como tarea explícita **F3.1b**; no marca F4.3/F5.1 como terminadas.

Lo que **no** incluye: UI, bandeja, instalador, retención/borrado, cola/escritor serializado, edición de anotaciones, detección de audio/video, múltiples navegadores simultáneos/perfiles. La persistencia de drift sí se añadió en F3.2. Chrome y Brave están permitidos por separado; no a la vez. Las escrituras pueden ser sincrónicas para una sesión de prueba; no es el endurecimiento de producción.

## Riesgos y pendientes

- **P1 — Umbral de idle para navegador:** 120 s global puede subcontar video/ajedrez sin input. La primera integración debe mostrar cobertura y no presentar el total como atención.
- **P2 — Causa del primer Ctrl+C:** el primer intento falló y se cerró con Stop-Process; el segundo detuvo el bridge. Hipótesis barata: el primer bridge corría como proceso de fondo sin consola interactiva; el segundo en ventana visible sí recibió Ctrl+C. No confirmado.
- El stop file de `ConsoleCapture` se probó y detuvo la captura; el Ctrl+C no se volvió a validar en esta entrega.
- El bridge y el bucle se detuvieron sin dejar procesos en el smoke test; falta una prueba sostenida.
- El script usa el classpath Maven para SQLite/JNA; el JAR simple no incluye esas dependencias.

## Estado

Implementado como F3.1b (E014) y probado con un smoke real de 12 s usando stop file y una base separada. La base por defecto `work/console-day/deriva.db` queda para la primera sesión real. F3.2 (E017) añade persistencia de drift y `ConsoleCapture` la usa. P1 (umbral de idle del navegador) sigue abierto y debe observarse con datos reales. F4.3/F5.1 no están terminadas.
