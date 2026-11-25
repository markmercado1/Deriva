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


## 2026-11-23 — UI-SAVE (E033)
Guardar sin cambios queda deshabilitado; controles bloqueados durante escritura/recarga, con reintento en error. 129 tests Java sin fallos y smoke JavaFX de clics repetidos/cambios/reversión/error. Sin cierre de F6.2.

## 2026-11-23 - AJUSTES-DETECCION (E034)
- Funciona: Ajustes con cuatro umbrales de detección (cambios, ventana, estabilidad e inactividad), persistencia en `setting`, valores efectivos vs. pendientes, restaurar predeterminados sin guardar, aviso inactividad≤estabilidad y fallback visible sin sobrescribir configuración inválida.
- Probado: Maven verify offline 158 tests Java, 0 fallos (29 nuevos); smokes `settings` y `settings-session` sobre bases temporales; persistencia y reapertura; configuración inválida intacta; regla Pausar/Reanudar conserva y Detener/Iniciar aplica; captura con 0 OPEN y hilos 0. Capturas en `work/ui-preview/ajustes/`.
- Límites: smokes de controles, no clics humanos; sin episodio real de deriva con umbral distinto; consola diagnóstica conserva defaults; el hallazgo de EXCLUSION al enfocar Deriva queda pendiente de revisión separada. No se implementaron exclusiones, bridge ni F6.2.
- Siguiente: F6.2 sigue pendiente; revisar el comportamiento de EXCLUSION por foco como tarea aparte.

## 2026-11-23 - BRAVE-EXCLUSIONES (E035)
- Funciona: la extensión ya no convierte en UNKNOWN un cambio de revisión durante la lectura ni una URL aún vacía de una pestaña que carga; relee la pestaña de forma acotada (hasta 3 intentos con 150 ms entre reintentos y una lectura final) sin reutilizar hosts. Incógnito, foco perdido, página interna y pestaña cerrada siguen excluyéndose de inmediato. El núcleo Java, la frescura de 3 s, la semántica de EXCLUDED y los umbrales no cambian.
- Probado: sonda real de solo lectura sobre el bridge (250 ms, sin hosts/URLs/títulos/tokens) mostró UNKNOWN frescos de ~0,5-0,8 s con Brave en primer plano, sin STALE ni reinicio de sesión, coincidiendo con EXCLUDED y con el cierre del episodio 3. Tras recargar la extensión, 38 s NORMAL continuos durante la navegación y episodio 4 abierto con 6 cambios. 14 tests JS y 158 tests Java, 0 fallos. Artefactos en `work/diagnostico-continuidad-brave/`; base protegida intacta.
- Límites: una máquina y un perfil/ventana de Brave; sin repetición real de incógnito, pérdida de foco, caducidad o segundo perfil. El gate completo de Brave y F6.2 siguen pendientes. La UI indica bridge configurado, no frescura del sitio.
- Siguiente: F6.2 pendiente; completar el gate reducido de Brave (segundo perfil, reinicio, 1 h, colisión Chrome/Brave) y decidir aparte el mensaje de estado del bridge.

## 2026-11-24 - F6B-1 modelo de datos de objetivos (E036)
- Funciona: esquema V2 con `goals` y `goal_alerts`, migración V1→V2 idempotente sobre `schema_version`, y repositorios SQLite con crear/leer/actualizar/borrar/listar activos. Topes de minutos y/o entradas validados por dominio y base; un objetivo por contexto (app o app+sitio), proceso insensible a mayúsculas; sin columnas de URL/título.
- Probado: Maven verify offline 177 tests Java, 0 fallos (19 nuevos); migración con datos V1 sin pérdida, doble inicialización, base nueva en V2, CRUD con reapertura, cascada objetivo→alertas, FK, límites de justificación y umbral, rechazos por `CHECK`, y `deleteActivity` borrando alertas sin tocar objetivos.
- Límites: bases temporales `@TempDir`; no se migró la base real (la hará el usuario con respaldo y SHA-256); V2 no la abre el código V1; `SQLitePrivacyCheck` no se amplía hasta F6B-7. Sin UI, sensor ni máquina de deriva.
- Siguiente: F6B-0 (actividad pasiva); F6.2 sigue pendiente después de F6B-7.

## 2026-11-24 - F6B-0 actividad pasiva (E037)
- Funciona: un objetivo activo `passive=true` mantiene su contexto como `OBSERVED` sin teclado ni mouse hasta 10 min desde la última entrada; después vuelve a `IDLE`. `UNAVAILABLE` (bloqueo/suspensión), los huecos >3 s y `EXCLUDED` mandan; cambiar de ventana corta la promoción hasta que vuelva la actividad. Objetivo de app solo coincide sin host; objetivo de sitio exige proceso+host exactos.
- Probado: Maven verify offline 189 tests Java, 0 fallos (12 nuevos); 14 tests JS, 0 fallos. `SegmenterTest` conserva 16 tests y `PrivacyFilterTest` 17, sin editar esos archivos.
- Límites: pruebas puras con tiempo simulado, sin sesión real de 10 min ni vídeo/ajedrez; los objetivos se leen una vez al iniciar la captura y sus cambios se aplican al reiniciar (F6B-3). El máximo de 10 min no se persiste todavía. P1 queda resuelto solo para contextos con objetivo pasivo. Sin UI ni cambio de esquema; no se abrió la base real.
- Siguiente: F6B-2 (motor de cálculo); F6.2 sigue pendiente después de F6B-7.

## 2026-11-24 - F6B-2 motor de cálculo de objetivos (E038)
- Funciona: `GoalProgressCalculator` puro calcula por objetivo minutos usados, entradas, estado (`IN_PROGRESS`/`NEAR`/`EXCEEDED`) y aviso de tiempo no medible. Recorta al día local con `analysis.DayWindow`; objetivo de app suma todo el proceso con o sin host, objetivo de sitio exige proceso+host; los límites son independientes y cada instante se cuenta una vez dentro de cada objetivo. Entradas nuevas a partir de 3 s fuera; solo `OBSERVED` suma.
- Probado: Maven verify offline 201 tests Java, 0 fallos (12 nuevos); 14 tests JS, 0 fallos. Cubre medianoche, umbrales 79/80/100 %, uno/dos topes, huecos de sitio, app+sitio, navegador con host, `IDLE`/`EXCLUDED`/`UNAVAILABLE`, solapes y segmentos fuera del día. Las 3 pruebas de `DailyUsageServiceTest` (E020) siguen intactas y verdes.
- Límites: sin UI, alarmas, base ni reloj; no incluye el segmento en curso (F6B-4 lo pasará cortado en «ahora»). `unmeasurable` es conservador porque `EXCLUDED` no lleva identidad. F6.2 sigue pendiente después de F6B-7.
- Siguiente: F6B-3 (UI de objetivos).

## 2026-11-24 - F6B-3 pantalla de objetivos y máximo pasivo (E039)
- Funciona: pantalla **Objetivos** para crear, editar topes/pasivo/activo, activar/desactivar y borrar con confirmación; contexto elegido entre apps/sitios vistos o escrito; contexto inmutable al editar. Progreso del día con estado En curso/Cerca del tope/Tope superado, icono+texto, aviso de subconteo y refresco cada 30 s solo en la vista. La captura en curso recarga los objetivos pasivos al cambiar la revisión y conserva una promoción activa si su contexto sigue pasivo. El máximo pasivo (10 min por defecto) se persiste y edita en Ajustes con efectivo/pendiente.
- Probado: Maven verify offline 220 tests Java, 0 fallos (19 nuevos); 14 tests JS, 0 fallos. Smoke `goals` crea «Ajedrez» 60 min/4 entradas, muestra progreso, borra con confirmación y deja hilos 0; capturas `work/ui-preview/f6b-3/{objetivos-creado,objetivos,ajustes}.png`. Las pantallas existentes siguen igual salvo el botón **Objetivos** y la sección «Actividad pasiva» en Ajustes.
- Límites: smoke de controles, no clic humano; sin sesión real de 10 min ni ajedrez real; el progreso no incluye el segmento en curso (F6B-4). Base temporal, no `work/console-day/deriva.db`. La consola diagnóstica conserva la lectura única y el máximo fijo. F6.2 sigue pendiente después de F6B-7.
- Siguiente: F6B-4 (alarma básica), que debe pasar el segmento abierto cortado en «ahora».

## 2026-11-24 - F6B-4 alarma básica por umbral (E040)
- Funciona: `GoalAlertService` evalúa los objetivos activos cada 30 s en el hilo de captura, solo con observación `OBSERVED` y captura `RUNNING`; añade el segmento abierto acotado por «ahora» vía `OpenSegmentProvider`, registra en `goal_alerts` los umbrales nuevos del día y avisa por la bandeja. Deduplica por objetivo, métrica, umbral y día local, también tras reiniciar; si 80 y 100 coinciden, registra ambos y notifica solo el más alto. Un fallo de evaluación se registra y no detiene la captura; subir o bajar el límite el mismo día no repite un umbral ya registrado (A46).
- Probado: Maven verify offline 235 tests Java, 0 fallos (15 nuevos); 14 tests JS, 0 fallos. Cubre 80/100 una vez, sin repetición, tope de 4 entradas, salto de minutos, cambio de límite, segmento abierto, cambio de día, reinicio, pausa/IDLE/bloqueo/suspensión, fallo de repositorio y fallo de notificador. Integración real con `java.exe` en primer plano: dos alertas 80/100 con `outcome`/`justification` nulos; cierre con hilos `deriva-capture` vivos=0. CPU corta: 0,21 % medio sin evaluación y 0,27 % con actividad observada y una alarma (45 s y 90 s, 5 s, 16 CPUs); E030 sostiene 0,15 % en 2 h 7 min. Bases temporales; base protegida intacta (`0446AE5D…`).
- Límites: globo de bandeja no capturado visualmente ni clic humano; se reutiliza `TrayIcon.displayMessage` de E029 y el texto se prueba con notificador falso. Sin sesión real de 10 min ni ajedrez; la pantalla Objetivos sigue sin el segmento en curso. Medición de CPU corta y de una máquina. F6.2 sigue pendiente después de F6B-7.
- Siguiente: F6B-6 (justificar excesos).

## 2026-11-24 - F6B-6 justificar excesos de objetivos (E041)
- Funciona: con una alerta al 100 % registrada, la pantalla Objetivos distingue **Excedido ●**, **Justificado por ti ✓** y **Sin responder ○**. **Justificar exceso** abre un motivo opcional ≤500 que se guarda tal cual; **Quitar justificación** revierte. La justificación cubre el exceso acumulado al marcar con la misma base que la alarma (cerrados + abierto confirmado), no el resto del día; el uso posterior vuelve a contar y **Justificar de nuevo** reemplaza la cobertura. Solo aplica al día local de `justified_at`; el motor `GoalProgressCalculator` sigue puro y recibe `GoalJustification` como dato.
- Probado: Maven verify offline 251 tests Java, 0 fallos (235 + 16 nuevos); 14 tests JS, 0 fallos. Cubre tres estados, revocar, uso posterior, rejustificar sin acumular, segmento abierto y cierre, día distinto, persistencia al reabrir, V2→V3 y V1→V3, y comparación SHA-256 de todas las columnas de `segment`, `drift_event` y `drift_context` antes/después de justificar y quitar (sin diferencias). Smoke `goals-justify` guarda «Partida con un amigo», muestra «Justificado por ti», «Cubierto: 10 min» y «Justificación guardada.», y al quitar vuelve a «Sin responder» con la nota; hilos `deriva-capture` vivos=0. Capturas `work/ui-preview/f6b-6/{exceeded,unanswered,justified,exhausted,justify-smoke-justificado}.png`.
- Límites: smokes de controles, no clics humanos; una máquina; sin sesión real de 10 min ni ajedrez; el segmento abierto se simula en tests; la migración de la base real la hará el usuario con respaldo (A41). Base protegida intacta (`0446AE5D…`). F6.2 sigue pendiente después de F6B-7.
- Siguiente: F6B-5 (alarma diferida durante deriva).

## 2026-11-24 - F6B-5 alarma diferida durante deriva (E042)
- Funciona: un aviso de objetivo que coincide con una deriva activa se difiere hasta que la deriva termina o se alcanza un tope configurable (`alert_deferral_max_s`, 300 s por defecto, 60–1800 s, efectivo/pendiente). Se consulta por el puerto de solo lectura `DriftStateProvider`; `DriftStateMachine` no cambia. V4 añade `notified_at`, `observed_until`, `outcome_at` y `outcome_after_ms`; la migración marca las alertas existentes con `notified_at = triggered_at` para que no se envíen como pendientes. Un pendiente de una ejecución anterior se descarta al arrancar (`DISCARDED`); pausar y reanudar en la misma sesión lo entrega. La observación posterior de 2 min guarda `NO_DRIFT`, `STABLE` con su tiempo, `NEW_DRIFT`, `STILL_DRIFTING`, `NOT_OBSERVED` o `DISCARDED`, con textos que no afirman causalidad. Dos topes del mismo objetivo en la misma evaluación producen un solo aviso combinado y registran todos los umbrales.
- Probado: Maven verify offline 272 tests Java, 0 fallos (251 previos + 21 nuevos); 14 tests JS, 0 fallos. Cubre diferir y entregar al terminar, tope, los cuatro resultados de observación, cierre/reinicio, pausa y bloqueo, mensaje combinado, V3→V4 con `notified_at = triggered_at` y arranque sin notificaciones. Smokes `settings` y `goals` sobre bases temporales con hilos `deriva-capture` vivos=0; capturas `work/ui-preview/f6b-5/{settings,goals,goals-creado}.png`.
- Límites: smokes de controles, no clics humanos; deriva y reloj simulados, sin sesión real de 10 min; el resultado es una observación, no prueba de que el aviso causara nada. La migración de la base real la hará el usuario con respaldo (A41). Base protegida intacta (`0446AE5D…`). `SQLitePrivacyCheck` no cubre aún las tablas nuevas (F6B-7). F6.2 sigue pendiente después de F6B-7.
- Siguiente: F6B-7 (resumen y gate).

## 2026-11-24 - F6B-7 resumen diario y gate final (E043)
- Funciona: **Resumen de hoy** en cada tarjeta de Objetivos con minutos justificados separados de los excedidos sin justificar y una línea «Aviso …» por alerta (umbral, métrica, hora y resultado A48). `SQLitePrivacyCheck` audita `goals` y `goal_alerts`; la justificación libre se conserva sin bloquear patrones y el formulario avisa «Evita pegar direcciones o datos personales.».
- Probado: gate real de 65 min, 130 muestras cada 30 s y 16 CPUs; CPU media 0,20 % / máxima 0,89 % frente a 0,15 % de E030 (causa probable: seis objetivos y alarmas cada 30 s); working set 155,5→279,8 MB (máx. 422,2), hilos 63→65 (máx. 70) y handles 531→708 (máx. 718), sin crecimiento sostenido. Salvedad: hueco de 96 s por suspensión S0. Cierre con `deriva-capture` y `deriva-goals` vivos=0. Huella de `segment`/`drift_event`/`drift_context` idéntica (`76684c90…`) antes/después de crear objetivo, alarma, justificar y quitar; copia V1→V4 con 14 segmentos y hash idéntico, original intacta (`0446AE5D…`). 278 tests Java + 14 JS, 0 fallos. Capturas `work/ui-preview/f6b-7/{summary.png,goals-final.png}`.
- Límites: el hueco de S0 impide llamar «sin cortes» a la muestra; smokes de controles, no clics humanos; bandeja no verificada con clic humano y dos topes juntos solo con notificador falso; P1 solo en objetivos pasivos; progreso con segmentos cerrados (hasta 30 s tarde); máximo pasivo fijo por sesión; subconteo de sitios por exclusión ante duda; migración real por el usuario con respaldo. F6.2 sigue PENDIENTE.
- Siguiente: F6.2 (entrega local reproducible), pendiente de autorización.

## 2026-11-25 — UI-VENTANA (E044)
Arranque centrado con tamaño adaptado al área útil de Windows para mantener accesible la barra nativa. Compilación correcta y arranque real con escala 125 %, ventana completa en pantalla y cierre sin hilos. Pendiente comprobación humana de botones/arrastre y pantallas adicionales. F6.2 pendiente.

## 2026-11-25 — UI-OBJETIVOS-RESUMEN (E045)
Resumen de hoy plegado por defecto con expansión conservada al refrescar; retiradas ambas burbujas solicitadas. Build correcto y sonda JavaFX de plegado, contenido y refresco aprobada. Falta revisión visual del usuario; bases intactas y F6.2 pendiente.

## 2026-11-25 — UI-AJUSTES-MENU (E046)
Entrada con cuatro tarjetas, navegación y regreso, formularios conservados. Build correcto, sonda de cuatro rutas y borrador aprobada, captura visual revisada. Falta revisión del usuario en app abierta. F6.2 pendiente.

## 2026-11-25 — USO-RANGOS (E047)
Día, semana, mes, año y rango personalizado en Uso. Consulta y totales por intervalo, validación de fechas y snapshot conservado. 281 tests aprobados, sonda FXML y captura revisada. Límite: sin benchmark de años de datos reales. F6.2 pendiente.

## 2026-11-25 — USO-DIA-HOY (E048)
Corregido Año → Día: vuelve a hoy local. Build y sonda FXML aprobados; selección manual posterior conservada.

## 2026-11-25 — AJUSTES-AYUDA (E049)
Ayudas plegadas y tres tarjetas; información integrada en Detección. Build y sonda FXML correctos; base real intacta. Próximo: revisión del usuario tras reabrir.

## 2026-11-25 — OBJETIVOS-HISTORIAL (E050)
V5 con revisiones y archivado al borrar; vista histórica de Objetivos de solo lectura por día. 276/284 tests en verde; los 8 de `LocalBridgeServerTest` fallan por loopback en este entorno. Falta revisión visual del usuario y migrar la base real con respaldo. F6.2 pendiente.

## 2026-11-25 — OBJETIVOS-RANGO (E051)
Desde/Hasta en Objetivos con resumen por objetivo y detalle por día; borrados visibles en solo lectura con «Mostrar borrados». 278/286 tests en verde (8 de loopback del entorno); sonda FXML aprobada. Falta revisión del usuario en la app. F6.2 pendiente.

## 2026-11-25 — F6.2 entrega local reproducible (E052)
README con guía verificada desde un clon limpio y limitaciones; corregido el arranque en clones nuevos (classpath generado por `verify`, scripts sin rutas absolutas). 278 Java sin bridge + 14 JS en el clon; app demo abre y cierra sin hilos. Respaldo de la base real hecho. Usuario: 286/286 con el pom nuevo. Falta fusionar.

## 2026-11-25 — ENTREGA-FINAL (E053)
Deriva.exe (jpackage), versión portable e instalador IExpress; datos de la app empaquetada en %LOCALAPPDATA%\Deriva; tres documentos de entrega (código fuente, para ejecutar, manual de uso) con capturas reales sobre datos sintéticos. 290/290 Java + 14 JS en la terminal del usuario. Carpeta `dist/Entrega Deriva` (no versionada).

## 2026-11-25 — ENTREGA-FINAL-2 (E054)
Prefijo `Em`, apartado «Extensión del navegador», instalador con asistente (corregido un fallo real al extraer carpetas del zip), tres documentos Word/PDF en blanco y negro, carpeta `entrega/` con instalador, portable, extensión y LEEME, rama `entrega-final` publicada. 298/298 Java en la terminal del usuario; instalar → abrir → desinstalar probado con `LOCALAPPDATA` temporal. Falta fusionar en `main` y probar el instalador con clic humano.
