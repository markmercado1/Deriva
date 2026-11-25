# Estado — 2026-11-25

**Última entrega — ENTREGA-FINAL HECHO (E053, A59):** rama `entrega-final`. `Deriva.exe` (jpackage), portable e instalador IExpress (`scripts/Build-Package.ps1`); la app empaquetada guarda datos en `%LOCALAPPDATA%\Deriva`. Documentos de entrega y capturas en `dist/Entrega Deriva` (generadores en `work/entrega`). 290/290 Java + 14 JS. Falta: fusionar `entrega-final` y que el usuario pruebe el instalador con doble clic.

## Situación

**Prototipo entregado: F0–F6.1, F6B, OBJETIVOS-HISTORIAL, OBJETIVOS-RANGO y F6.2 HECHO.** No hay fase siguiente fijada en el plano; el trabajo nuevo lo decide el usuario.

- `main` contiene F6B y el historial/rango de objetivos (PR #1, merge `ff8310c`). F6.2 está en la rama `f6-2-entrega`, pendiente de fusionar.
- Guía de uso verificada y limitaciones: inicio de `README.md`.
- Base real del usuario: `work/f6/deriva-f6.db`, esquema V5 (2374 segmentos 22–25/11, 3 objetivos, 12 avisos, 15 episodios). Respaldo: `work/respaldos/deriva-f6-2026-11-25-v5.db` (integrity_check ok). La base por defecto de `Start-Ui.ps1` sigue siendo `work/console-day/deriva.db` (V1, sin uso desde el 22/11).

## Última entrega — F6.2 (E052, A58)

- Archivos: `README.md` (guía y limitaciones), `pom.xml` (`maven-dependency-plugin` 3.7.0 → `target/classpath.txt` en `package`), `scripts/Start-Ui.ps1` y `scripts/Start-ConsoleCapture.ps1` (leen ese classpath; sin rutas absolutas), `CONTINUAR.md`, docs de seguimiento.
- Pruebas: clon limpio → 278 Java sin `LocalBridgeServerTest` (loopback bloqueado en la sesión) + 14 JS; app demo abre y cierra con hilos 0. El usuario obtuvo 286/286 en su terminal con el pom anterior.
- Confirmado por el usuario: 286/286 con el pom nuevo y `target/classpath.txt` generado (13:07). Falta solo abrir el PR y fusionar `f6-2-entrega`.

## Pendientes abiertos (no bloquean la entrega)

- Gate completo de Brave: segundo perfil, reinicio, una hora y colisión de sesiones (`browser:"chrome"`).
- P1: idle de 120 s en navegador; P2: Ctrl+C de la consola.
- USE-02 y SEN-01 parciales; S3/hibernación no disponible en este equipo.
- EXCLUSION-FOCO: enfocar Deriva puede cerrar un episodio por exclusión.
- Sin prueba con clic humano de bandeja/avisos; los smokes recorren controles por código.
- Objetivos: progreso hasta 30 s tarde, subconteo de sitios ante duda, máximo pasivo fijo por sesión; en un periodo, el registro de cambios lista también los de objetivos ocultos.

## Cómo retomar

Leer `AGENTS.md`, este archivo y la tabla de `docs/FASES.md`. Evidencia en `docs/PRUEBAS.md` (E001–E052), decisiones en `docs/DECISIONES.md` (hasta A58) e historial corto en `docs/ENTREGAS.md`. Las entradas anteriores de este archivo están en el historial de Git.
