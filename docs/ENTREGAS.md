# Historial de entregas

## E000 — 2026-11-21 — Preparación de continuidad

- Alcance: instrucciones para agentes, mapa de arquitectura, fases, punto de guardado, decisiones, matriz de pruebas y mensaje para retomar en otra IA.
- Funciona: estructura documental; todavía no hay aplicación implementada.
- Probado: DOC-001 PASÓ: diez archivos copiados y comprobados por hash; once referencias de entrada presentes; catorce tareas de implementación pendientes; hashes SHA-256 de ambos planos intactos. Sin pruebas de software porque todavía no existe implementación.
- Falta: F1.1 (herramientas y esqueleto), seguida de F1.2 (piloto temprano del navegador).
- Planos: deben conservarse sin cambios; la documentación adicional no reemplaza DERIVA_PLANO_v2.md.

Las siguientes entradas deben ser cortas: tarea, cambios, evidencia, límites y siguiente paso. El estado vigente se mantiene solo en ESTADO.md.


## E001 — 2026-11-21 — Uso diario incorporado al plano

- Funciona: especificación de la vista Uso diario y tarea F4.3; semántica de primer plano y ejemplo Dota/YouTube documentados. No hay software implementado.
- Verificación documental: comprobar al instalar siete archivos, referencias a F4.3/USE-01/USE-02 y hash del original intacto. No se ejecutaron pruebas de aplicación.
- Falta: implementar F1.1 y posteriores; USE-01/02 quedan pendientes. No se añadió medición de audio en segundo plano.

- Resultado de verificación E001: DOC-002 PASÓ; siete archivos actualizados, original intacto.

## E002 — 2026-11-21 — F1.1 terminada

- Funciona: estructura Maven Java 21, arranque de consola, JAR ejecutable y repositorio Git local sin remoto/commits.
- Probado: herramientas/versiones, clean verify online y offline (1 test pasado), JAR código 0. ENV-01 en PRUEBAS. Ningún navegador fue abierto.
- Límites: arranque de consola solamente; sin captura, UI, BD o extensión. Aviso inicial de Maven “Acceso denegado.” registrado aunque build y ejecución terminan correctamente.
- Falta: F1.2, prueba temprana de extensión y bridge; no se inició.

## E003 — 2026-11-22 — Piloto F1.2 guardado

- Funciona: bridge loopback autenticado, sesiones/seq/caducidad, extensión MV3 preparada, opciones y script de arranque. Reportes en memoria, captura deshabilitada.
- Probado: 11 tests Java y 5 JS pasados; sintaxis JS/PowerShell y prueba separada del JAR por HTTP aprobadas. Evidencia detallada en PRUEBAS.
- Falta: cargar extensión en navegador real y validar Origin, foco, incógnito, reinicios y perfiles. Los navegadores aislados no arrancaron desde el entorno. F1.2 sigue EN_CURSO; no se declara finalizada ni se inicia F1.3.
- Retomar: docs/PILOTO_NAVEGADOR.md; no reescribir código ni repetir investigación. Captura sigue deshabilitada por D4.

## E004 — 2026-11-22 — F1.2 validada en Chrome real

- Funciona: extensión MV3 y bridge local con reportes reales; foco, incógnito y reinicio comprobados.
- Probado: línea base 5/5 NORMAL example.com; incógnito 12/12 UNKNOWN sin host; regreso 11/12 NORMAL example.com; reinicio con sesión nueva y 6/12 NORMAL example.com. Detalle en PRUEBAS E004. Sin cambios de código.
- Límites: segundo perfil, caducidad exacta de 3 s y cierre ordenado con Ctrl+C no validados; correlación HWND/perfil pendiente; captura deshabilitada.
- Siguiente: F1.3, sensor y pipeline simulado inicial.

## E005 — 2026-11-22 — F1.3 contratos de sensor

- Funciona: `ActivitySample`, puertos `WindowSensor`/`IdleDetector` y dobles `FakeSensor`/`FakeIdleDetector` con secuencia determinista.
- Probado: Maven verify offline BUILD SUCCESS, 19 tests, 0 fallos (8 nuevos). Evidencia E005 en PRUEBAS.
- Límites: sin JNA, sin sensor Windows real y sin pipeline de privacidad/tracking; SEN-01 PARCIAL.
- Siguiente: sensor Windows real con JNA y prueba de proceso/ventana.

## E006 — 2026-11-22 — F1.3 cerrada

- Funciona: sensor Windows real con JNA 5.19.1, detector de inactividad, `PrivacyFilter` y secuencia determinista antes del tracking.
- Probado: Maven verify offline BUILD SUCCESS, 26 tests, 0 fallos (7 nuevos de privacidad). Probes reales: WindowsTerminal.exe/SearchHost.exe con PID y ventana válidos; idle 63359 ms antes de entrada y 0 ms después. Evidencia E006 en PRUEBAS.
- Límites: fallo nativo no inducido; sin bucle de muestreo, segmentación, UI ni SQLite; navegadores excluidos y captura de sitios deshabilitada.
- Siguiente: F2.1 segmentación completa.

## E007 — 2026-11-22 — F2.1 segmentación completa

- Funciona: `ContextKey`, `Segment`, `StableTick`, `TickResult` y `Segmenter` puro con debounce, fronteras y huecos.
- Probado: Maven verify offline BUILD SUCCESS, 44 tests, 0 fallos (18 nuevos). Cubre candidato 2/3 s, IDLE/EXCLUDED/UNAVAILABLE, A→B→A, hueco >3 s, reloj hacia atrás y contextos por proceso/host. Evidencia E007 en PRUEBAS.
- Límites: sin bucle de muestreo/`TrackingService`, sin SQLite ni UI; pruebas sintéticas y `Segment.id` 0 hasta persistencia.
- Siguiente: F2.2 SQLite V1 y repositorios.

## E008 — 2026-11-22 — F2.2 SQLite y repositorios

- Funciona: SQLite JDBC 3.54.1.0, esquema V1 completo, `SegmentRepository` y adaptador con transacción, solapamiento y reapertura.
- Probado: Maven verify offline BUILD SUCCESS, 50 tests, 0 fallos (6 nuevos). Incluye rollback forzado, WAL/foreign_keys, centinelas y flujo FakeSensor→PrivacyFilter→Segmenter→SQLite. Evidencia E008 en PRUEBAS.
- Límites: ruta de producción, escritor serializado/cola, repositorios de drift y UI pendientes; sin bucle de muestreo.
- Siguiente: F2.3 decisión de integración del navegador.

## E009 — 2026-11-22 — F2.3 navegadores excluidos

- Funciona: decisión escrita A13; navegadores excluidos por D4 y `PrivacyFilter` reforzado para seis procesos.
- Probado: revisión de E004/E006/E008; Maven verify offline 50 tests, 0 fallos. No se ejecutaron pruebas nuevas de navegador. Evidencia E009 en PRUEBAS.
- Límites: sin correlación HWND/perfil, segundo perfil, caducidad exacta ni cierre ordenado validados; captura de sitios deshabilitada.
- Siguiente: F3.1 máquina de deriva.

## E010 — 2026-11-22 — Log de diagnóstico del bridge

- Funciona: log opt-in por reporte aceptado con timestamp, seq, state y hostPresent yes/no; sin host ni token.
- Probado: Maven verify offline 52 tests, 0 fallos. `BridgeReportLogTest` y test de servidor comprueban que `docs.google.com` no aparece. Evidencia E010 en PRUEBAS.
- Límites: solo reportes aceptados; temporal para GATE-Q2; archivo en `work/pilot/bridge-reports.log`.
- Siguiente: checklist manual y prueba de una hora del GATE-Q2.

## E011 — 2026-11-22 — GATE-Q2 PASS

- Funciona: Chrome + un perfil autorizado por A15, con criterio de frescura de reportes y UNKNOWN al perder foco, no correlación HWND; límite ~1 s aceptado.
- Probado: una hora con 13.680 muestras, 3.741 reportes, `maxGapMs=1088`, `over3s=0`, 0 errores, 0 reinicios. Segundo perfil repetido (UNKNOWN/no→NORMAL/yes), privado/foco/reinicio reales. Evidencia E011 en PRUEBAS.
- Límites: Ctrl+C no validado limpiamente (primer intento falló y se cerró con Stop-Process; segundo detuvo; causa no investigada); caducidad exacta no medida; captura aún no implementada.
- Siguiente: F2.4 habilitar Chrome + un perfil, luego F3.1.

## E012 — 2026-11-22 — F2.4 Chrome + un perfil en el filtro

- Funciona: `PrivacyFilter` permite Chrome solo con `BrowserStateProvider` fresco NORMAL y propaga el host; UNKNOWN/STALE y otros navegadores quedan EXCLUDED.
- Probado: Maven verify offline 59 tests, 0 fallos (7 nuevos), incluida integración `BrowserStateStore → PrivacyFilter`. Evidencia E012 en PRUEBAS.
- Límites: constructor por defecto sigue excluyendo navegadores; sin composición/bucle/UI, la captura real sigue inactiva; límite ~1 s de A15.
- Siguiente: F3.1 máquina de deriva.

## E013 — 2026-11-22 — F3.1 máquina de deriva

- Funciona: `ChangeCounter` y `DriftStateMachine` puros con umbral 10/120 s, episodio único, estabilidad 180 s, pendiente bloqueante y cierres STABLE/UNCONFIRMED.
- Probado: Maven verify offline 77 tests, 0 fallos (18 nuevos). DR-01/DR-02 PASÓ. Evidencia E013 en PRUEBAS.
- Límites: sin persistencia de drift/UI/loop; PAUSE/LOCK/EXIT/GAP/RESTART aún no producidos por Segmenter.
- Siguiente: propuesta de integración mínima; luego F3.2.

## E014 — 2026-11-22 — F3.1b captura mínima por consola

- Funciona: `ConsoleCapture` con sensor→bridge/privacidad→segmentador→SQLite, resumen separado, drift impreso sin persistir, guardado por segmento y stop file.
- Probado: Maven verify offline 88 tests, 0 fallos (11 nuevos); smoke real de 12 s con base separada, stop file, resumen y privacidad verificada. Evidencia E014 en PRUEBAS.
- Límites: sin UI/cola/escritor; smoke corto sin episodio de deriva; P1 y P2 abiertos; F4.3/F5.1 no terminadas.
- Siguiente: F3.2 persistencia de episodios.

## E015 — 2026-11-22 — F2.4b Brave gate reducido

- Funciona: `brave.exe` permitido con las mismas reglas que Chrome (solo NORMAL/fresco); UNKNOWN/STALE y otros navegadores EXCLUDED.
- Probado: Maven verify offline 90 tests, 0 fallos (2 nuevos). Ventana privada de Brave UNKNOWN/no según el usuario. Evidencia E015 en PRUEBAS.
- Límites: gate reducido; faltan segundo perfil, reinicio, 1 h y la colisión de sesiones Chrome/Brave (`browser:"chrome"`); no ejecutar ambos a la vez.
- Siguiente: F3.2; completar gate Brave cuando se pueda.

## E016 — 2026-11-22 — Captura real de Brave y resumen corregido

- Funciona: captura real de Brave con sitios y resumen separado; `Sin registro` mide huecos desde el inicio de la sesión.
- Probado: Maven verify offline 91 tests, 0 fallos (1 nuevo). Brave capturó `chess.com`/`youtube.com`; no hubo drift porque los cambios <3 s se absorbieron. Evidencia E016 en PRUEBAS.
- Límites: no valida segundo perfil/reinicio/1 h/colisión de Brave; sin UI; P1/P2 abiertos.
- Siguiente: F3.2.

## E017 — 2026-11-22 — F3.2 persistencia de episodios

- Funciona: `SQLiteDriftRepository` con OPENED/UPDATED/CLOSED, contextos, anotaciones preservadas, recuperación RESTART y cierre EXIT; `ConsoleCapture` persiste drift.
- Probado: Maven verify offline 97 tests, 0 fallos (6 nuevos); >30 min/medianoche, anotaciones y recuperación. Evidencia E017 en PRUEBAS.
- Límites: sin UI/borrado; no se provocó drift real en consola; ids en memoria mapeados a SQLite por sesión; P1/P2 y gate Brave completo pendientes.
- Siguiente: F4.1 con referencias de diseño (A11).

## E018 — 2026-11-22 — F4.1 Interrupciones

- Funciona: pantalla JavaFX de Interrupciones con tabla + panel de detalle, filtros de fecha/estado/justificadas, anotaciones, estados con forma/texto, estados vacío/error y navegación lateral con Uso diario/Ajustes deshabilitados.
- Probado: Maven verify offline 102 tests, 0 fallos (5 nuevos); captura real de la UI en modo demo en `work/ui-preview/interrupciones.png`; lectura/escritura fuera del hilo FX y descarte de respuestas obsoletas. Evidencia E018 en PRUEBAS.
- Límites: sin clic manual en Guardar ni error/vacío forzado; demo sintética, no captura real; sin bucle/orquestación de producto; Uso diario/Ajustes/borrado/panel de conexión pendientes; F4.3/F5.1 no terminadas.
- Siguiente: F4.2/F4.3 según decisión del usuario.

## E019 — 2026-11-22 — F4.2 rango y comprobaciones de UI

- Funciona: rango Desde/Hasta inclusivo por solapamiento, episodios que cruzan medianoche sin duplicados, inicio con fecha/hora, aviso de rango invertido y conservación de la anotación al reabrir.
- Probado: Maven verify offline 106 tests, 0 fallos (4 nuevos); smoke de UI con episodio 7 (Mensajes, Justificada, Guardar → “Guardado”) y reapertura con `MESSAGES|1`; estados vacío, error y rango invertido con bases temporales; base real intacta. Capturas en `work/ui-preview/f4-2/`. Evidencia E019 en PRUEBAS.
- Límites: el smoke de UI es automatizado, no un clic humano; datos sintéticos, no captura real; error inducido con esquema inválido temporal; F4.3, Ajustes, borrado, panel de conexión, bucle de captura, gate Brave, P1/P2 siguen pendientes.
- Siguiente: detenerse antes de F4.3 según la instrucción del usuario.

## E020 — 2026-11-22 — F4.3 Uso diario

- Funciona: Uso diario por aplicación y sitio desde segmentos persistidos; fecha y zona visibles, recorte de medianoche, sitios incluidos en su navegador sin doble conteo, IDLE/EXCLUDED/UNAVAILABLE y huecos fuera del total, cobertura separada y “sin datos” distinto de “0 s”. Navegación lateral entre Interrupciones y Uso diario; Ajustes deshabilitado.
- Probado: Maven verify offline 110 tests, 0 fallos (4 nuevos); captura principal con 3 h 30 min de primer plano, Chrome 2 h 10 min y sitios YouTube/Instagram dentro de Chrome; estados sin datos, cero uso y error; regresión de Guardar en Interrupciones. Capturas en `work/ui-preview/f4-3/`. Evidencia E020 en PRUEBAS.
- Límites: el segmento abierto no se persiste y no se incluye; no es una vista en vivo. No se probó Dota/YouTube real ni una sesión de captura; USE-02 queda PARCIAL. Brave, P1 y P2 siguen pendientes; F5 no se inicia y Ajustes no se añade.
- Siguiente: detenerse antes de F5.

## E021 — 2026-11-22 — F5.1a captura real desde la UI

- Funciona: botón Iniciar/Detener captura en la barra lateral con estado visible; `CaptureSession` en hilo dedicado; cierre ordenado al detener o cerrar la ventana. Uso diario recibe snapshot de persistido + abierto confirmado, muestra pendiente sin sumarlo, deduplica y refresca sin cerrar el segmento. Brave excluido; Chrome solo con bridge configurado.
- Probado: Maven verify offline 114 tests, 0 fallos (4 nuevos); sesión real corta con Word (abierto observado y 9 s persistidos), stop, reapertura y cierre con captura activa sin OPEN. Capturas y consultas en `work/ui-preview/f5-1/`. Evidencia E021 en PRUEBAS.
- Límites: sin bridge en la prueba real, por lo que Chrome/Brave no se capturaron; el pendiente solo se validó en test, no en sesión real; sesión de 10 s; P1/P2 siguen. Borrado, matriz de fallos/privacidad, cierre sin proceso oculto y demás comprobaciones de F5.1 quedan pendientes. F5.1 EN_CURSO; no se añaden Ajustes ni F5.2.
- Siguiente: completar las comprobaciones restantes de F5.1.

## E022 — 2026-11-22 — F5.1b borrado coordinado y fallo SQLite

- Funciona: **Borrar registros** con confirmación; detiene y espera la captura, borra segmentos/episodios/contextos y conserva configuración/token/exclusiones/schema. Error SQLite visible y parada controlada, sin cola ilimitada.
- Probado: Maven verify offline 115 tests, 0 fallos (1 nuevo); borrado con captura activa (`0|0|0|0`), configuración/token preservados en test, reapertura vacía; fallo con trigger temporal, error visible, sin segmento ni OPEN y reapertura. Capturas en `work/ui-preview/f5-1/`. Evidencia E022 en PRUEBAS.
- Límites: smokes de UI, no clics humanos; sin bridge, Chrome/Brave no capturados y Brave excluido; P1/P2, matriz completa de fallos/privacidad, cierre sin proceso oculto y bloqueo siguen pendientes. F5.1 EN_CURSO; no se añaden Ajustes ni F5.2.
- Siguiente: completar las comprobaciones restantes de F5.1; no iniciar F5.2.

## E023 — 2026-11-22 — F5.1c matriz de fallos y privacidad

- Funciona: fallo SQLite en una escritura del bucle con parada controlada y error visible; fallo de bridge por puerto ocupado; ventana privada, navegador no autorizado, proceso propio, idle y no disponible sin host persistido; mensajes de error genéricos en español.
- Probado: Maven verify offline 117 tests, 0 fallos (2 nuevos); trigger + suspensión de 4 s del proceso Java induce el fallo del bucle (`0|0|0` y reapertura sin datos); puerto ocupado con token válido produce error visible y el token no aparece en salida ni DB/WAL; matriz sintética de privacidad sin URL, título, token ni host privado; base real intacta. Capturas y salidas en `work/ui-preview/f5-1/`. Evidencia E023 en PRUEBAS.
- Límites: smokes de UI, no clics humanos; la suspensión simula un hueco, no un bloqueo real; no se indujo fallo nativo del sensor (SEN-01 PARCIAL); P1/P2, Brave, USE-02, cierre sin proceso oculto y bloqueo/reanudación siguen pendientes. F5.1 EN_CURSO; no se añaden Ajustes ni F5.2.
- Siguiente: comprobar cierre sin proceso oculto y bloqueo/reanudación; no iniciar F5.2.

## E024 — 2026-11-22 — F5.1d cierre completo y bloqueo/reanudación

- Funciona: cierre de ventana con captura activa sin hilo ni proceso oculto; `Segmenter.close` persiste el abierto y el pendiente sin solapamiento; bloqueo/desbloqueo real y suspensión S0 no se cuentan como uso ni se duplican; `LockApp.exe`/`LogonUI.exe` son UNAVAILABLE.
- Probado: Maven verify offline 121 tests, 0 fallos (4 nuevos); cierre con 6 s persistidos, 0 OPEN, hilo 0 y sin `java.exe`; bloqueo real de 45 s como UNAVAILABLE (primera pasada detectó 4 s de LockApp y se corrigió); suspensión real S0 de 57 s como UNAVAILABLE (eventos Kernel-Power 506/507), span=suma sin solapes ni huecos; Uso diario 4 s y 19 s. Capturas y sondas en `work/ui-preview/f5-1d/`. Evidencia E024 en PRUEBAS.
- Límites: smokes de UI salvo la acción humana de bloqueo/suspensión; el equipo solo ofrece S0, no S3/hibernación (el hueco de congelación se cubrió con simulación y tests); fallo nativo del sensor no inducido (SEN-01 PARCIAL); P1/P2, Brave y USE-02 siguen abiertos fuera de F5.1. No se añaden Ajustes ni F5.2.
- Siguiente: F5.1 HECHO; no iniciar F5.2 ni Ajustes. Revisar los límites y pendientes antes de F6.1.

## E025 — 2026-11-22 — F6.1 preparación

- Funciona: `scripts/Measure-UiResources.ps1` mide CPU y memoria del proceso Java de la UI cada 30 s (configurable) y escribe CSV; `docs/F6_MEDICION.md` fija copia de base, comandos, parada y límites. Sin títulos, hosts, URL ni línea de comandos en el CSV.
- Probado: Maven verify offline 121 tests Java y 5 tests JS, 0 fallos; medidor contra proceso de prueba (3 muestras, fin por duración) y contra la UI real con `shutdown-close` (7 muestras, proceso detectado, sin `java.exe` después). Artefactos del smoke eliminados; base real intacta.
- Límites: **la prueba sostenida no se ejecutó**; no hay cifras de rendimiento todavía. Una máquina, resolución de 30 s, copia de base con la UI cerrada. No se tocan planos ni `work/console-day/deriva.db`.
- Siguiente: el usuario ejecuta la sesión sostenida y aporta el CSV; después se cierra F6.1.

## E026 — 2026-11-22 — Brave en la captura de UI y parámetros de bridge

- Funciona: la UI captura Brave con sitios cuando hay bridge configurado; sin bridge, Chrome/Brave quedan excluidos. `Start-Ui.ps1` acepta `-ExtensionId`, `-Port` y `-BridgeTokenFile`, valida el token local sin imprimirlo y comprueba que el puerto esté libre.
- Probado: Maven verify offline 121 tests Java, 0 fallos; `PrivacyFilterTest` cubre Brave con/sin host; prueba conjunta UI+bridge en `47823`+medidor → 7 muestras, bridge activo, cierre limpio y sin `java.exe`. Base real intacta.
- Límites: **no se probó un reporte real de Brave con host en la UI**; el gate completo de Brave sigue pendiente. Un solo navegador/perfil a la vez contra el bridge; no ejecutar `Start-BridgePilot`/`Start-ConsoleCapture` en el mismo puerto.
- Siguiente: configurar la extensión de Brave y ejecutar la sesión real; F6.1 sostenida pendiente.

## E027 — 2026-11-23 — Modo de prueba de Interrupciones

- Funciona: botón **Ver datos de ejemplo** que carga 6 episodios sintéticos en memoria (abierta, estables y sin confirmar) para verificar tabla, filtros, contadores y detalle; alterna con **Volver a datos reales**.
- Probado: Maven verify offline 122 tests Java, 0 fallos (1 nuevo); smoke que pulsa el botón → 6 filas, Guardar deshabilitado, base temporal con `drift_event=0` y `segment=0`; captura `work/ui-preview/f6-demo/demo-episodes.png`.
- Límites: datos sintéticos, no detector ni captura real; no se guardan anotaciones; la pantalla sigue sin refresco automático. IDs `900001`–`900006`.
- Siguiente: usar el modo para revisión visual; F6.1 sostenida pendiente.

## E028 — 2026-11-23 — Guardar con confirmación visible

- Funciona: Guardar ya persistía la anotación; ahora muestra `Guardando…` y un banner `Guardado · motivo · [Justificada] · HH:mm` que sobrevive al refresco. El error se muestra en rojo. El banner se limpia al cambiar de episodio, motivo o casilla.
- Probado: Maven verify offline 124 tests Java, 0 fallos; smoke `annotate` sobre copia → banner visible y `MESSAGES|1` en la base copiada; captura `work/ui-preview/f5-2/guardado.png`. La base real conserva su hash.
- Límites: smoke de controles, no clic humano; diagnóstico de la base real de la sesión en copia.
- Siguiente: F5.2.

## E029 — 2026-11-23 — F5.2 pausa manual y bandeja

- Funciona: pausa detiene la captura y cierra el segmento/episodio con `PAUSE`; reanudar abre un bucle nuevo. La barra lateral muestra el estado y **Reanudar captura**. `AppTray` ofrece Abrir, Pausar/Reanudar y Salir; cerrar oculta y avisa; Salir detiene, hace flush, retira el icono y termina sin proceso oculto.
- Probado: Maven verify offline 124 tests Java, 0 fallos (2 nuevos de `PAUSE`); smokes `pause` y `tray` sobre bases temporales → estados correctos, 0 solapes, `hilos deriva-capture vivos=0`, salida 0, ventana oculta/reabierta; capturas `work/ui-preview/f5-2/{pause-paused,pause,tray}.png`.
- Límites: Windows únicamente; el smoke invoca los callbacks del menú, no es un clic humano; sin `SystemTray` se conserva el cierre normal. No se probó el aviso emergente ni la bandeja tras suspensión/bloqueo.
- Siguiente: registrar F6.1 y decidir F6.2.

## E030 — 2026-11-23 — F6.1 medición sostenida

- Funciona: la medición real existe y se acepta: 2 h 7 min 30 s, 256 muestras cada 30 s, CPU media 0,15 % (máx. 0,40 % de 16 CPUs lógicas), working set 249–416 MB, privada ≤484 MB, hilos 67–70. Captura activa con Brave/bridge (3.390 s con host) y cobertura del 99,5 % de la ventana.
- Probado: `work/f6/ui-resources.csv` (SHA-256 `2DF7C437…`) sin huecos >30,025 s; base de la sesión con 7.646 s registrados; capturas reales del usuario en `docs/pruebas/` (Interrupciones y Uso diario). Equipo: ASUS TUF F15, i5-12500H, 15,6 GB, Windows 11 26200, Java 21.0.7.
- Límites: una máquina, resolución 30 s, solo el proceso de la UI; la sesión posterior de la mañana no tiene CSV; no sustituye una prueba de varias horas ni de otro equipo. La base protegida no se tocó.
- Siguiente: F6.2 entrega local reproducible, pendiente de autorización. Ajustes, Brave completo, P1/P2 y USE-02 siguen fuera.

## E031 — 2026-11-23 — Anotación guardada vs. estabilidad no comprobada

- Funciona: la columna **Anotación** muestra **Pendiente/Guardada** según `answered_at` persistido; **Estabilidad** usa **En curso/Estable/No comprobada**; el detalle explica por qué no se comprobó la estabilidad con el motivo real de cierre. Guardar mantiene la confirmación visible y no altera el estado automático ni reabre el episodio.
- Probado: Maven verify offline **127 tests Java, 0 fallos**; smoke `annotate` sobre base sintética → banner `Guardado · Mensajes · Justificada · 10:44`; reapertura de la misma base → `Guardada · 23/11 10:44` sin volver a guardar; trigger SQLite → banner rojo y fila **Pendiente**; episodio UNCONFIRMED → `No se comprobó la estabilidad: el cierre por inactividad.` Capturas en `work/ui-preview/ui-claridad/`.
- Límites: smokes de controles, no clics humanos; bases sintéticas, no captura real; fallo inducido con trigger. F6.2 no se inició ni cerró; Ajustes no se implementó.
- Siguiente: revisar la recomendación de Ajustes (A36) y decidir el alcance de F6.2 con autorización explícita.

### 2026-11-23 — Base inicial de Git
Preparación del respaldo del código y documentación en GitHub por solicitud del usuario. Capturas reales locales excluidas; sin implementación ni cierre de F6.2.

## 2026-11-23 — UI-SCROLL (E032)
Detalle de Interrupciones desplazable; compilación, carga JavaFX y accesibilidad geométrica de Guardar tras scroll a 1200x600 comprobadas. F6.2 pendiente.
