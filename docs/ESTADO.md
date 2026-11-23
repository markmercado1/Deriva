# Estado actual — leer primero

Actualizado: 2026-11-23 (America/Lima).

## UI-CLARIDAD cerrada (2026-11-23)

Mejora de claridad de Interrupciones, **no** un cierre de F6.2 ni una implementación de Ajustes. **Anotación** muestra **Pendiente/Guardada** desde `answered_at`; **Estabilidad** muestra **En curso/Estable/No comprobada** y el detalle explica el motivo real de cierre. Rama `codex/claridad-anotaciones`; evidencia E031. F6.2 sigue PENDIENTE.

## Situación verificable

F0, F1.1, F1.2, F1.3, F2.1, F2.2, F2.3, GATE-Q2, F2.4, F2.4b, F3.1, F3.1b, F3.2, F4.1, F4.2, F4.3, F5.1a, F5.1b, F5.1c, F5.1d y F5.1 HECHO. F5.2a, F5.2b y F5.2 HECHO (E029); F6.1 HECHO con la medición sostenida E030. Guardar en Interrupciones ya persistía; E028 refuerza la confirmación visible. GATE-Q2 PASS para Chrome: segundo perfil repetido UNKNOWN/no→NORMAL/yes; una hora sin huecos >3 s; Ctrl+C no validado limpiamente. F2.4 permite Chrome + un perfil; F2.4b añade Brave con gate reducido (privada pasó; faltan segundo perfil, reinicio, 1 h y colisión de sesiones). F3.1 implementó la máquina de deriva pura. F3.1b implementó la captura por consola con resumen y stop file; base por defecto work/console-day/deriva.db. F3.2 persiste episodios, contextos y anotaciones, recupera RESTART y cierra EXIT. F4.1 implementa Interrupciones con JavaFX según docs/DISENO.md. F4.2 añade rango Desde/Hasta por solapamiento y verifica anotaciones persistidas y estados vacío/error. F4.3 añade Uso diario por aplicación y sitio con recorte de medianoche y sin doble conteo. F5.1a conecta la captura real con la UI: iniciar/detener/cerrar, estado visible y snapshot de persistido/abierto/pendiente sin cierre forzado. F5.1b añade borrado con confirmación, parada/join previos y fallo SQLite visible con parada controlada. F5.1c cubre la matriz de fallos/privacidad: fallo SQLite en la escritura del bucle, fallo de bridge, ventana privada, exclusión de app/navegador, idle/no disponible y ausencia de URL/título/token/host privado en DB/WAL/salida. F5.1d cierra el cierre completo y el bloqueo/reanudación: flush de abierto/pendiente, hilo de captura 0 y proceso terminado, bloqueo real de 45 s y suspensión S0 de 57 s como UNAVAILABLE, sin solapamientos. F5.1 HECHO con límites: S3/hibernación no disponibles, fallo nativo del sensor no inducido (SEN-01 PARCIAL). Resto de navegadores/perfiles excluido por A13/A15. Brave se habilitó en la captura de UI solo con bridge fresco (E026/A30), con gate reducido: faltan segundo perfil, reinicio, una hora y resolver la colisión de sesiones Chrome/Brave. La interrupción por créditos no perdió código.

Existe un bridge HTTP local, extensión MV3, sensor Windows real con filtro de privacidad y persistencia SQLite. `PrivacyFilter` permite Chrome o Brave con un perfil solo con `BrowserStateStore` fresco en NORMAL; el resto de navegadores/perfiles sigue excluido y no se usan Chrome y Brave a la vez contra el mismo bridge. Hay una UI JavaFX con Interrupciones (F4.1/F4.2), Uso diario (F4.3) y captura real iniciada/detenida desde la barra lateral (F5.1a). La captura usa un hilo dedicado y persiste segmentos/episodios; el borrado coordinado existe con confirmación y los fallos de bridge/almacenamiento se muestran en español sin detalle sensible. Hay pausa manual con cierre `PAUSE` y bandeja básica (Abrir, Pausar/Reanudar, Salir); la orquestación completa de producto sigue pendiente. La entrada de consola sigue disponible como diagnóstico. Los reportes del piloto solo viven en memoria. `Main` sigue siendo solo un mensaje de arranque; la interfaz se abre con `scripts/Start-Ui.ps1` y la captura se inicia desde la barra lateral. BridgePilot es una entrada explícita aparte.

## Última evidencia

- 2026-11-23 E031: UI-CLARIDAD HECHO (no cierra F6.2). Columna **Anotación** desde `answered_at` (`Pendiente/Guardada`), **Estabilidad** (`En curso/Estable/No comprobada`), explicación por `close_reason` y filtros coherentes. Guardar solo actualiza `reason`/`justified`/`answered_at`. `mvn ... verify` offline: **127 tests Java, 0 fallos** (3 nuevos); tests SQLite reales confirman que anotar no cierra un `OPEN` ni reabre un `STABLE`. Smoke sintético: banner `Guardado · Mensajes · Justificada · 10:44`, reapertura con `Guardada · 23/11 10:44`; trigger SQLite → banner rojo y fila **Pendiente**; UNCONFIRMED → `No se comprobó la estabilidad: el cierre por inactividad.` Capturas `work/ui-preview/ui-claridad/`. Base protegida intacta (`0446AE5D…`). Límites: smokes de controles, no clics humanos; datos sintéticos.
- 2026-11-23 E030: F6.1 HECHO. `work/f6/ui-resources.csv` (SHA-256 `2DF7C437…`): 256 muestras cada 30 s, 2 h 7 min 30 s, sin huecos >30,025 s. CPU media 0,15 % (máx. 0,40 %, p95 0,24 % de 16 CPUs lógicas); working set 394,7→249,0 MB (máx. 416,4); privada ≤484,0 MB; hilos 67–70; handles 750–760. Captura activa: 7.646 s registrados en la ventana (99,5 %), Brave/bridge 3.390 s con host, inactividad 2.101 s y exclusión 1.239 s. Equipo: ASUS TUF F15, i5-12500H 12C/16T, 15,6 GB, Windows 11 26200, Java 21.0.7. Capturas reales del usuario en `docs/pruebas/`. Límites: una máquina, resolución 30 s, solo proceso UI; la sesión posterior de la mañana no tiene CSV. Base protegida intacta.
- 2026-11-23 E029: F5.2 HECHO. `CaptureState.PAUSED` + `pause()` cierran segmento/episodio con `PAUSE`; reanudar abre bucle nuevo. UI `Captura en pausa.` + **Reanudar captura**. `AppTray` con Abrir/Pausar-Reanudar/Salir; cerrar oculta y avisa; Salir detiene, `join`, flush, retira el icono y termina. Smokes `pause`/`tray` en bases temporales: estados correctos, 0 solapes, ventana oculta/reabierta, hilos 0 y salida 0; capturas `work/ui-preview/f5-2/{pause-paused,pause,tray}.png`. 124 tests, 0 fallos (2 nuevos de `PAUSE`). Límite: el smoke invoca los callbacks, no es un clic humano en la bandeja.
- 2026-11-23 E028: Guardar ya persistía; confirmación visible nueva. La base real de la sesión mostró `SEARCHING_INFO|1` con `answered_at=10:06:18`. Ahora `Guardando…` y banner `Guardado · motivo · Justificada · HH:mm` que sobrevive al refresco; error en rojo. Smoke `annotate` sobre copia → banner visible y `MESSAGES|1`; captura `work/ui-preview/f5-2/guardado.png`. 124 tests, 0 fallos. Base protegida intacta.
- 2026-11-22 E024: F5.1d. Cierre de ventana con captura activa: 6 s persistidos, 0 OPEN, `deriva-capture` vivos=0, proceso Java terminado y sin `java.exe` posterior. `Segmenter.close` persiste el pendiente tras cerrar el abierto en su inicio, sin solapamiento (tests). Bloqueo real: primera pasada contó `LockApp.exe` 4 s; corregido a UNAVAILABLE; segunda pasada 45 s UNAVAILABLE, 4 s OBSERVED y sin lockapp. Suspensión real S0: eventos Kernel-Power 506/507 (entrada/salida), 57 s UNAVAILABLE, 19 s OBSERVED, span=suma sin solapes/huecos. `powercfg /a` solo S0; S3/hibernación no disponibles. `mvn ... verify` offline: 121 tests, 0 fallos (4 nuevos). Uso diario 4 s y 19 s. Base real intacta (SHA-256 0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475). F5.1 HECHO; P1/P2/Brave/USE-02 siguen fuera.
- 2026-11-22 E023: F5.1c. Fallo SQLite en la escritura del bucle: trigger temporal + suspensión de 4 s del proceso Java; error visible antes de Detener, `0|0|0` y reapertura sin datos. Fallo de bridge con puerto ocupado: error visible en español, sin token en salida ni DB/WAL. Matriz sintética de privacidad (normal, UNKNOWN/privado, navegador no autorizado, app propia, idle, no disponible) sin URL/título/token/host privado. Regresión del smoke de captura normal tras el cambio: se detuvo y persistió sin OPEN. `mvn ... verify` offline: 117 tests, 0 fallos (2 nuevos). Capturas/salidas en `work/ui-preview/f5-1/`. Base real intacta (SHA-256 0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475). F5.1 sigue EN_CURSO.
- 2026-11-22 E022: F5.1b. Borrar registros con confirmación: la captura se detiene y se espera (`join`), se borran `segment`, `drift_event` y `drift_context`, y se conservan configuración/token/exclusiones/schema. Fallo SQLite simulado con trigger en base temporal: error visible, captura detenida, sin segmento persistido ni OPEN. Reapertura tras ambos: “Sin datos”. `mvn ... verify` offline: 115 tests, 0 fallos (1 nuevo). Capturas `work/ui-preview/f5-1/{confirmacion,delete,delete-reopen,failure,failure-reopen}.png`. Base real intacta (SHA-256 0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475). F5.1 sigue EN_CURSO.
- 2026-11-22 E021: F5.1a. Captura real iniciada/detenida desde la UI, estado visible, cierre con captura activa y reapertura. Uso diario incluye el abierto confirmado como provisional y muestra el pendiente sin sumarlo; no se cierra el segmento para refrescar y se deduplica contra persistidos. `mvn ... verify` offline: 114 tests, 0 fallos (4 nuevos). Sesión real de 10 s: abierto Word, luego 9 s persistidos tras detener y reabrir; cierre con captura activa persistió el segmento y no dejó OPEN. Capturas `work/ui-preview/f5-1/`. Base real intacta (SHA-256 0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475). Sin bridge en la prueba real; Chrome/Brave no se capturaron y Brave queda excluido.
- 2026-11-22 E020: F4.3. Uso diario desde segmentos persistidos: recorte al día local, sitios incluidos en el navegador sin doble conteo, IDLE/EXCLUDED/UNAVAILABLE y huecos fuera del total, cobertura separada y “sin datos” distinto de “0 s”. `mvn ... verify` offline: 110 tests, 0 fallos (4 nuevos). Capturas `work/ui-preview/f4-3/{uso-diario,sin-datos,cero-uso,error-uso,regresion-interrupciones}.png`. Base real intacta (SHA-256 0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475). Limitación: el segmento abierto no se persiste y no se incluye; no es una vista en vivo.
- 2026-11-22 E019: F4.2. Rango Desde/Hasta inclusivo por solapamiento; un episodio que cruza medianoche aparece en ambos días y una sola vez en el rango amplio. `mvn ... verify` offline: 106 tests, 0 fallos (4 nuevos). Smoke de UI: Guardar motivo Mensajes + Justificada en episodio 7, estado `Guardado`; tras cerrar y reabrir, la base conserva `MESSAGES|1`. Capturas `work/ui-preview/f4-2/{rango,guardado,reabierto,vacio,error,rango-invalido}.png`. Base real intacta (SHA-256 0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475). El smoke recorre controles y botón, no es un clic humano.
- 2026-11-22 E018: F4.1. JavaFX 21 + FXML/CSS; tabla de Interrupciones y panel de detalle, filtros de fecha/estado/justificadas, anotaciones, estados vacío/error y consultas/guardado fuera del hilo FX. `mvn ... verify` offline: 102 tests, 0 fallos (5 nuevos). Captura real en `work/ui-preview/interrupciones.png`; demo con 7 episodios y 1 abierto. Sin clic manual en Guardar; backend de anotación probado.
- 2026-11-22 E017: F3.2. `DriftRepository`/`SQLiteDriftRepository` con OPENED/UPDATED/CLOSED, contextos, anotaciones preservadas, `between` por solapamiento, recuperación OPEN→UNCONFIRMED/RESTART y cierre EXIT. `ConsoleCapture` persiste drift. `mvn ... verify` offline: 97 tests, 0 fallos (6 nuevos). Sin UI/borrado.
- 2026-11-22 E016: captura real de Brave (`brave.exe@chess.com`, youtube.com) con segmentos y resumen; `Sin registro` corregido para medir huecos desde el primer segmento, no desde medianoche. 91 tests, 0 fallos (1 nuevo). No hubo drift porque los cambios <3 s se absorbieron; no valida segundo perfil/reinicio/1 h/colisión.
- 2026-11-22 E015: F2.4b. `brave.exe` permitido en `PrivacyFilter` con las mismas reglas que Chrome (solo proveedor fresco NORMAL); UNKNOWN/STALE y otros navegadores EXCLUDED. Ventana privada de Brave UNKNOWN/no según el usuario. `mvn ... verify` offline: 90 tests, 0 fallos (2 nuevos). Pendientes Brave: segundo perfil, reinicio, 1 h y colisión de sesiones Chrome/Brave.
- 2026-11-22 E014: F3.1b. `ConsoleCapture` + `ConsoleSummary` + `SQLitePrivacyCheck`; resumen por app/sitio/IDLE/EXCLUDED/UNAVAILABLE/sin registro; drift impreso; guardado por segmento; stop file probado; privacidad de esquema/host/ventana privada verificada. `mvn ... verify` offline: 88 tests, 0 fallos (11 nuevos). Smoke real de 12 s con base separada.
- 2026-11-22 E013: F3.1. `ChangeCounter` y `DriftStateMachine`: 10 cambios en `(t-120,t]`, episodio único, estabilidad 180 s por tick, `switchCount`/contextos, STABLE y UNCONFIRMED con motivo. `mvn ... verify` offline: 77 tests, 0 fallos (18 nuevos). Sin persistencia de drift/UI.
- 2026-11-22 E012: F2.4. `PrivacyFilter` permite Chrome solo con `BrowserStateProvider` fresco NORMAL; UNKNOWN/STALE y otros navegadores quedan EXCLUDED; el host se propaga a `Observation`/`Segment`. `mvn ... verify` offline: 59 tests, 0 fallos (7 nuevos). Sin bucle/UI; captura real inactiva.
- 2026-11-22 E011 (cerrado): GATE-Q2 PASS en frescura/foco, no en correlación HWND. Una hora: 13.680 muestras, 3.741 reportes, `maxGapMs=1088`, `over3s=0`, límite ~1 s aceptado, 0 errores, 0 reinicios. Privado/foco/reinicio reales. Segundo perfil repetido: UNKNOWN/no con B enfocado y NORMAL/yes <1 s al volver a A. Ctrl+C: primer intento falló y se cerró con Stop-Process; segundo detuvo el bridge; causa no investigada.
- 2026-11-22 E010: log de diagnóstico opt-in del bridge en `work/pilot/bridge-reports.log`; por reporte aceptado escribe timestamp, seq, state y hostPresent yes/no, nunca host ni token. `mvn ... verify` offline: 52 tests, 0 fallos.
- 2026-11-22 E009: decisión D4 revisada con E004; sin correlación HWND/perfil ni segundo perfil, navegadores quedan excluidos (A13). `PrivacyFilter` cubre Chrome/Edge/Brave/Firefox/Opera/Vivaldi. `mvn ... verify` offline: 50 tests, 0 fallos. No se ejecutaron pruebas nuevas de navegador.
- 2026-11-22 E008: SQLite JDBC 3.54.1.0; esquema V1 completo, WAL/foreign_keys, `SQLiteSegmentRepository` y test de integración `FakeSensor → PrivacyFilter → Segmenter → repositorio`. `mvn ... verify` offline: BUILD SUCCESS, 50 tests, 0 fallos/errores/omitidos (6 nuevos).
- 2026-11-22 E007: `ContextKey`, `Segment`, `StableTick`, `TickResult` y `Segmenter` puro. `mvn ... verify` offline: BUILD SUCCESS, 44 tests, 0 fallos/errores/omitidos (18 nuevos). Debounce 2/3 s, fronteras IDLE/EXCLUDED/UNAVAILABLE, huecos >3 s, reloj hacia atrás, A→B→A y contextos por proceso/host/subdominio.
- 2026-11-22 E006: JNA 5.19.1; `WindowsWindowSensor`/`WindowsIdleDetector` reales; `PrivacyFilter` y secuencia determinista. Probes: antes de entrada, WindowsTerminal.exe con PID/ventana válidos e idleMillis=63359; tras entrada, SearchHost.exe con idleMillis=0; probe reproducible final WindowsTerminal.exe con idleMillis=31. `mvn ... verify` offline: BUILD SUCCESS, 26 tests, 0 fallos/errores/omitidos.
- 2026-11-22 E005: contratos `ActivitySample`, `WindowSensor` e `IdleDetector` y dobles `FakeSensor`/`FakeIdleDetector`. `mvn ... verify` offline: BUILD SUCCESS, 19 tests, 0 fallos/errores/omitidos (8 nuevos).
- 2026-11-22 E004: Chrome real con extensión y bridge. Línea base 5/5 NORMAL example.com (seq 543-550); incógnito 12/12 UNKNOWN sin host (seq 605-616); regreso 11/12 NORMAL example.com (seq 660-671); reinicio de Chrome con sesión nueva y 6/12 NORMAL example.com (seq 65-77).
- Foco: en la primera observación, cambiar a otra aplicación produjo UNKNOWN durante ~17 s y recuperación a NORMAL al volver.
- Tests automáticos: tras E031 hay 127 tests Java + 5 tests JS, 0 fallos.
- Límites: GATE-Q2 PASS (E011) por frescura/foco, no por HWND; una hora sin huecos >3 s y límite ~1 s aceptado. Segundo perfil confirmado; Ctrl+C no validado limpiamente; resto de navegadores/perfiles excluido. F2.4 implementado en el filtro; F5.1 terminada con límite S0 (sin S3/hibernación) y SEN-01 PARCIAL por el fallo nativo del sensor no inducido.
- Bridge detenido forzosamente al terminar; el script informó código -1 por la terminación del proceso, no por fallo de la aplicación.

## Siguiente paso exacto

**UI-CLARIDAD cerrada** (E031) como mejora de claridad; **F6.2 sigue PENDIENTE de autorización explícita** y no se inició. Siguiente bloque real: **F6.2 — entrega local reproducible**. Para F6.2 hay que verificar instrucciones de arranque/test con el README, dejar claras las limitaciones y los pendientes (Brave completo, P1/P2, USE-02, SEN-01 PARCIAL, S3/hibernación no disponibles, Ajustes no implementado) y comprobar que no se depende de rutas o datos personales. **Ajustes:** el plano v2 §8–9 pide configuración/exclusiones y panel de conexión; las tablas existen pero no hay repositorio ni pantalla. Recomendación: mantenerlo deshabilitado/oculto hasta una fase propia (A36), sin construir una pantalla vacía. La captura real ya tiene pausa, bandeja y cierre sin proceso oculto; el smoke de bandeja invoca los callbacks, no un clic humano. Antes de F6.2, revisar que la base protegida `work/console-day/deriva.db` siga intacta (SHA-256 `0446AE5D…`, comprobada en E031).

## Punto de continuidad — F5.1 HECHO

**Estado:** F5.1a, F5.1b, F5.1c y F5.1d HECHO; **F5.1 HECHO** con los límites indicados. F5.2 se cerró después (E029); no iniciar Ajustes ni F6.2 sin autorización.

### Terminado

- **F5.1a:** captura real iniciada/detenida desde la barra lateral, estado visible, salida ordenada al cerrar, snapshot de persistido/abierto/pendiente sin forzar cierres y deduplicación. Evidencia E021.
- **F5.1b:** borrado con confirmación, parada/`join` antes de borrar, conservación de configuración/token/exclusiones/esquema y fallo SQLite visible con parada controlada. Evidencia E022.
- **F5.1c:** fallo SQLite en la escritura del bucle, fallo de bridge, escenarios de privacidad/exclusión y errores sin datos sensibles. Evidencia E023.
- **F5.1d:** cierre de ventana con flush de abierto/pendiente y sin hilo/proceso vivo; bloqueo/desbloqueo real y suspensión S0 sin contar ni duplicar. Evidencia E024.
- Interfaz: Interrupciones, Uso diario, captura y borrado; Ajustes sigue deshabilitado. F5.2 añade después la pausa y la bandeja (E029).

### Pruebas realizadas

- `mvn ... verify` offline: **121 tests Java + 5 JS, 0 fallos**.
- E021: sesión real corta de 10 s; Word observado como abierto y 9 s persistidos tras detener y reabrir; cierre con captura activa sin OPEN.
- E022: borrado con captura activa; `segment`, `drift_event` y `drift_context` a 0; reapertura sin datos; configuración/token conservados en test de integración.
- E022: fallo SQLite con trigger temporal; error visible, sin segmento persistido ni OPEN, captura detenida y reapertura sin datos.
- E023: trigger + suspensión de 4 s del proceso Java; fallo en el bucle con error visible antes de Detener, `0|0|0` y reapertura sin datos.
- E023: puerto de bridge ocupado; error visible en español, sin token en salida ni DB/WAL, `0|0|0` y reapertura sin datos.
- E023: matriz sintética de privacidad con host normal, UNKNOWN/privado, navegador no autorizado, app propia, idle y no disponible; DB/WAL sin URL, título, token ni host privado.
- E024: cierre de ventana con captura activa; 6 s persistidos, 0 OPEN, hilo 0 y sin `java.exe` posterior.
- E024: pendiente de cierre persistido como OBSERVED sin solapamiento; probado con reloj simulado y reapertura SQLite.
- E024: bloqueo real de 45 s como UNAVAILABLE (sin `LockApp` contado) y suspensión real S0 de 57 s como UNAVAILABLE; span=suma, sin solapes ni huecos; Uso diario 4 s y 19 s de primer plano.
- Capturas: `work/ui-preview/f5-1/{confirmacion,delete,delete-reopen,failure,failure-reopen,captura-live,reabierto,cierre,loop-failure,loop-failure-live,bridge-failure,bridge-failure-reopen}.png` y `work/ui-preview/f5-1d/{shutdown-final,lock-usage,sleep-usage}.png`.
- Base real intacta: `work/console-day/deriva.db` SHA-256 `0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475`. Planos intactos.
- Los smokes de UI son diagnósticos, no clics humanos.

### Comandos verificados

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
powershell -NoProfile -File .\scripts\Start-Ui.ps1
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f5-1\real.db -Smoke capture
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f5-1\delete-test.db -Smoke delete
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f5-1\confirm.db -Smoke delete-confirm
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f5-1\failure.db -Smoke capture-failure
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f5-1\delete-test.db -View usage
powershell -NoProfile -ExecutionPolicy Bypass -File .\work\ui-preview\f5-1\run-loop-failure.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\work\ui-preview\f5-1\run-bridge-failure.ps1
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f5-1d\shutdown-final.db -Smoke shutdown-close -Screenshot .\work\ui-preview\f5-1d\shutdown-final.png
powershell -NoProfile -ExecutionPolicy Bypass -File .\work\ui-preview\f5-1d\run-session-scenario.ps1 -Scenario lock -HoldSeconds 60 -ProbeSeconds 80
powershell -NoProfile -ExecutionPolicy Bypass -File .\work\ui-preview\f5-1d\run-session-scenario.ps1 -Scenario sleep -HoldSeconds 90 -ProbeSeconds 110
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f5-2\pause.db -Smoke pause -Screenshot .\work\ui-preview\f5-2\pause.png
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f5-2\tray.db -Smoke tray -Screenshot .\work\ui-preview\f5-2\tray.png
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Measure-UiResources.ps1 -OutputFile .\work\f6\ui-resources.csv -IntervalSeconds 30
```

### Archivos relevantes

- Captura: `src/main/java/com/deriva/app/CaptureSession.java`, `app/usecase/{CaptureControl,CaptureSnapshot,CaptureState,CaptureStatus}.java`, `tracking/Segmenter.java`.
- Pausa/bandeja: `src/main/java/com/deriva/app/AppTray.java`, `CaptureSession.pause()`, `app/usecase/CaptureState.PAUSED`, `ui/fx/MainController.java`, `main.fxml` (botón Pausar/Reanudar), `interrupciones.css` (`capture-paused`, `pause-button`), `UiSmoke.pauseResume`/`trayCycle`.
- Borrado/almacenamiento: `src/main/java/com/deriva/storage/sqlite/SQLiteDataMaintenance.java`, `CaptureSession.java`.
- Privacidad/bloqueo: `src/main/java/com/deriva/privacy/PrivacyFilter.java` (`LockApp`/`LogonUI` como UNAVAILABLE).
- UI: `src/main/java/com/deriva/ui/fx/{MainController,UsageController,InterruptionsController}.java`, `src/main/resources/com/deriva/ui/{main.fxml,uso-diario.fxml,interrupciones.fxml,interrupciones.css}`.
- Diagnóstico: `src/main/java/com/deriva/app/{UiSmoke,DerivaApp}.java`, `scripts/Start-Ui.ps1`, `work/ui-preview/f5-1d/{session-probe,run-session-scenario}.ps1`.
- Tests: `SQLiteDataMaintenanceTest`, `SegmenterTest`, `PipelinePersistenceIntegrationTest`, `DailyUsageServiceTest`, `DailyUsageViewModelTest`, `PrivacyFilterTest`, `LocalBridgeServerTest`, `BrowserPrivacyIntegrationTest`.
- Evidencia/documentación: `docs/PRUEBAS.md` E021–E030, `docs/FASES.md` F5.1a–F5.2b/F6.1, `docs/DECISIONES.md` A25–A34, `docs/DISENO.md`, `docs/F6_MEDICION.md`, `docs/ENTREGAS.md`.
- Capturas de esta entrega: `work/ui-preview/f5-2/{guardado,pause-paused,pause,tray}.png`; capturas reales del usuario: `docs/pruebas/imagen1PruebaDeriva.png`, `docs/pruebas/imagen2PruebaEnNavegadorYapps.png`.

### Criterios de F5.1 verificados

- [x] **Matriz de fallos/privacidad:** fallo SQLite en la escritura del bucle, fallo de bridge, ventana privada, exclusión de app/navegador, idle/no disponible y errores sin datos sensibles (E023). El fallo nativo del sensor no se indujo; SEN-01 sigue PARCIAL.
- [x] **Privacidad:** DB/WAL/logs/salida sin URL, título, token ni hosts privados en los escenarios anteriores (E023/PR-01).
- [x] **Bloqueo/reanudación:** bloqueo real de 45 s y suspensión real S0 de 57 s como UNAVAILABLE, sin pérdida ni duplicados; reapertura correcta (E024). S3/hibernación no disponibles.
- [x] **Cierre sin proceso oculto:** cierre de ventana con captura activa; abierto/pendiente persistidos, hilo 0, proceso Java terminado y sin OPEN (E024).
- **Límite de entorno:** S3/hibernación no disponibles; el hueco de congelación total se cubrió con simulación y tests de gap.

### Pendientes abiertos (no ampliar alcance)

- **Brave:** permitido en la captura de UI solo con bridge fresco (E026/A30); su gate completo (segundo perfil, reinicio, 1 h, colisión) sigue pendiente. No usar Chrome y Brave a la vez contra el mismo bridge.
- **Chrome:** captura con bridge no se ejercitó en las pruebas reales de F5.1a–d; requiere token/extensionId/port. E023 solo probó el fallo de puerto ocupado, no una sesión con extensión.
- **P1:** idle 120 s puede subestimar vídeo/ajedrez pasivo.
- **P2:** Ctrl+C de consola no validado; la UI usa parada explícita.
- **USE-02:** el segmento abierto se probó con Word real y el pendiente/cierre en tests; faltan Dota/YouTube en segundo plano, alternancia y cobertura sin extensión.
- **No implementados:** Ajustes completos, pausa/bandeja y F5.2.

## Archivos de F1.2

src/main/java/com/deriva/bridge/{BrowserReport,BrowserStateStore}.java; bridge/http/{FlatJson,LocalBridgeServer}.java; app/BridgePilot.java. Tests en src/test/java/com/deriva/bridge y bridge/http. extension/chromium/{manifest.json,background.js,report.js,options.html,options.js,package.json}; extension/tests/report.test.mjs. scripts/Start-BridgePilot.ps1 y docs/PILOTO_NAVEGADOR.md. README, DECISIONES, PRUEBAS, ENTREGAS y FASES actualizados.

## Archivos de F1.3

src/main/java/com/deriva/sensor/{ActivitySample,WindowSensor,IdleDetector}.java; src/main/java/com/deriva/sensor/windows/{WindowsWindowSensor,WindowsIdleDetector}.java; src/main/java/com/deriva/privacy/{Observation,ObservationKind,PrivacyFilter}.java; src/test/java/com/deriva/sensor/{FakeSensor,FakeIdleDetector,ActivitySampleTest,FakeSensorTest}.java; src/test/java/com/deriva/sensor/windows/WindowsSensorProbe.java; src/test/java/com/deriva/privacy/PrivacyFilterTest.java; pom.xml (JNA 5.19.1).

## Archivos de F2.1

src/main/java/com/deriva/tracking/model/{ContextKey,Segment,StableTick,TickResult}.java; src/main/java/com/deriva/tracking/Segmenter.java; src/test/java/com/deriva/tracking/{ContextKeyTest,SegmenterTest,TrackingModelTest}.java.

## Archivos de F2.2

src/main/java/com/deriva/storage/{StorageException.java,port/SegmentRepository.java,sqlite/SQLiteSchema.java,sqlite/SQLiteSegmentRepository.java}; src/test/java/com/deriva/storage/sqlite/SQLiteSegmentRepositoryTest.java; src/test/java/com/deriva/integration/PipelinePersistenceIntegrationTest.java; pom.xml (SQLite JDBC 3.54.1.0). V1 incluye tablas de drift; sus repositorios llegan en F3.

## Archivos de F2.3

docs/DECISIONES.md (A13), docs/FASES.md, docs/PRUEBAS.md, docs/ESTADO.md, docs/PILOTO_NAVEGADOR.md y README; src/test/java/com/deriva/privacy/PrivacyFilterTest.java (lista de navegadores). Sin cambios de producción.

## Archivos de F2.4

src/main/java/com/deriva/privacy/{BrowserStateProvider,PrivacyFilter}.java; src/main/java/com/deriva/bridge/BrowserStateStore.java; src/test/java/com/deriva/privacy/PrivacyFilterTest.java; src/test/java/com/deriva/bridge/BrowserStateStoreTest.java; src/test/java/com/deriva/integration/BrowserPrivacyIntegrationTest.java.

## Archivos de F2.4b

src/main/java/com/deriva/privacy/PrivacyFilter.java (allowlist Chrome/Brave); src/test/java/com/deriva/privacy/PrivacyFilterTest.java (Brave con/sin host); docs/DECISIONES.md (A19), docs/FASES.md, docs/PRUEBAS.md, docs/ENTREGAS.md, docs/Q2_GATE.md, docs/INTEGRACION_MINIMA.md y README.

## Archivos de F3.1

src/main/java/com/deriva/analysis/{Change,ChangeCounter}.java; src/main/java/com/deriva/coach/{DriftDetector,DriftStateMachine}.java; src/main/java/com/deriva/coach/model/{CloseReason,DriftEpisode,DriftStatus,DriftUpdate}.java; src/test/java/com/deriva/analysis/ChangeCounterTest.java; src/test/java/com/deriva/coach/{DriftModelTest,DriftStateMachineTest}.java.

## Archivos de F3.1b

src/main/java/com/deriva/app/{ConsoleCapture,ConsoleSummary}.java; src/main/java/com/deriva/storage/sqlite/SQLitePrivacyCheck.java; src/main/java/com/deriva/tracking/Segmenter.java (close); scripts/Start-ConsoleCapture.ps1; src/test/java/com/deriva/app/ConsoleSummaryTest.java; src/test/java/com/deriva/storage/sqlite/SQLitePrivacyCheckTest.java; src/test/java/com/deriva/tracking/SegmenterTest.java; src/test/java/com/deriva/integration/BrowserPrivacyIntegrationTest.java; docs/INTEGRACION_MINIMA.md.

## Archivos de F3.2

src/main/java/com/deriva/coach/model/DriftEvent.java; src/main/java/com/deriva/storage/port/DriftRepository.java; src/main/java/com/deriva/storage/sqlite/SQLiteDriftRepository.java; src/main/java/com/deriva/coach/DriftStateMachine.java (close EXIT); src/main/java/com/deriva/app/ConsoleCapture.java (persistencia drift); src/test/java/com/deriva/storage/sqlite/SQLiteDriftRepositoryTest.java; src/test/java/com/deriva/coach/DriftStateMachineTest.java.

## Archivos de F4.1

docs/DISENO.md; src/main/java/com/deriva/app/{DerivaApp,Launcher,UiDemoData}.java; src/main/java/com/deriva/app/usecase/InterruptionsService.java; src/main/java/com/deriva/ui/viewmodel/{InterruptionRow,InterruptionsPage,InterruptionsViewModel,ReasonOption,StatusFilter}.java; src/main/java/com/deriva/ui/fx/InterruptionsController.java; src/main/resources/com/deriva/ui/{interrupciones.fxml,interrupciones.css}; src/test/java/com/deriva/ui/viewmodel/InterruptionsViewModelTest.java; scripts/Start-Ui.ps1; pom.xml (JavaFX 21.0.13). Captura: work/ui-preview/interrupciones.png.

## Archivos de F4.2

src/main/java/com/deriva/app/usecase/InterruptionsService.java (forRange); src/main/java/com/deriva/ui/viewmodel/InterruptionsViewModel.java (rango, dd/MM HH:mm); src/main/java/com/deriva/ui/fx/InterruptionsController.java (Desde/Hasta, rango invertido, estado Guardado); src/main/resources/com/deriva/ui/{interrupciones.fxml,interrupciones.css}; src/main/java/com/deriva/app/{DerivaApp,UiDemoData,UiSmoke}.java; scripts/Start-Ui.ps1 (-Database/-Smoke); src/test/java/com/deriva/app/usecase/InterruptionsServiceTest.java; src/test/java/com/deriva/ui/viewmodel/InterruptionsViewModelTest.java; src/test/java/com/deriva/storage/sqlite/SQLiteDriftRepositoryTest.java. Evidencia: work/ui-preview/f4-2/.

## Archivos de F4.3

src/main/java/com/deriva/app/usecase/{DailyUsage,DailyUsageService}.java; src/main/java/com/deriva/ui/viewmodel/{DailyUsageViewModel,UsagePage,UsageRow,DisplayFormats}.java; src/main/java/com/deriva/ui/fx/{MainController,UsageController,InterruptionsController}.java; src/main/resources/com/deriva/ui/{main.fxml,uso-diario.fxml,interrupciones.fxml,interrupciones.css}; src/main/java/com/deriva/app/{DerivaApp,UiDemoData}.java; scripts/Start-Ui.ps1 (-View); src/test/java/com/deriva/app/usecase/DailyUsageServiceTest.java; src/test/java/com/deriva/ui/viewmodel/DailyUsageViewModelTest.java. Evidencia: work/ui-preview/f4-3/.

## Archivos de F5.1a

src/main/java/com/deriva/app/{CaptureSession,UiSmoke,DerivaApp}.java; src/main/java/com/deriva/app/usecase/{CaptureControl,CaptureSnapshot,CaptureState,CaptureStatus}.java; src/main/java/com/deriva/privacy/PrivacyFilter.java (conjunto de navegadores); src/main/java/com/deriva/tracking/Segmenter.java (snapshot); src/main/java/com/deriva/app/usecase/{DailyUsage,DailyUsageService}.java (abierto/pendiente); src/main/java/com/deriva/ui/viewmodel/DailyUsageViewModel.java; src/main/java/com/deriva/ui/fx/{MainController,UsageController}.java; src/main/resources/com/deriva/ui/{main.fxml,uso-diario.fxml,interrupciones.css}; scripts/Start-Ui.ps1 (-Smoke capture/capture-close); tests SegmenterTest, DailyUsageServiceTest, DailyUsageViewModelTest, PrivacyFilterTest. Evidencia: work/ui-preview/f5-1/.

## Archivos de F5.1b

src/main/java/com/deriva/storage/sqlite/SQLiteDataMaintenance.java; src/main/java/com/deriva/app/usecase/CaptureControl.java (borrado); src/main/java/com/deriva/app/CaptureSession.java (parada/join + borrado + error); src/main/java/com/deriva/ui/fx/MainController.java (confirmación y borrado asíncrono); src/main/resources/com/deriva/ui/{main.fxml,interrupciones.css} (overlay/confirmación); src/main/java/com/deriva/app/UiSmoke.java (delete/delete-confirm/capture-failure); scripts/Start-Ui.ps1; src/test/java/com/deriva/storage/sqlite/SQLiteDataMaintenanceTest.java. Evidencia: work/ui-preview/f5-1/{confirmacion,delete,delete-reopen,failure,failure-reopen}.png.

## Archivos de F5.2

`src/main/java/com/deriva/app/AppTray.java`; `CaptureSession.java` (pause, PAUSE en el cierre y stop desde pausa); `app/usecase/{CaptureControl,CaptureState,CaptureStatus}.java`; `ui/fx/{MainController,InterruptionsController}.java` (botón de pausa, banner de Guardado); `ui/viewmodel/InterruptionsViewModel.java` (sin cambios de lógica); `src/main/resources/com/deriva/ui/{main.fxml,interrupciones.fxml,interrupciones.css}`; `app/{DerivaApp,UiSmoke}.java` (tray, cierre y smokes `pause`/`tray`); `scripts/Start-Ui.ps1` (nuevos smokes); tests `DriftStateMachineTest` (cierre PAUSE) y `DailyUsageViewModelTest` (doble de `CaptureControl`). Evidencia: `work/ui-preview/f5-2/{guardado,pause-paused,pause,tray}.png` y E028–E029.

## Gate Q2 cerrado — PASS

docs/Q2_GATE.md, scripts/Measure-BridgeReports.ps1 y el log `work/pilot/bridge-reports.log` (opt-in por `DERIVA_BRIDGE_REPORT_LOG`, solo reportes aceptados, sin host ni token). Resultado E011: Chrome + un perfil autorizado (A15), basado en frescura/foco y no en HWND; límite ~1 s aceptado. Segundo perfil repetido: UNKNOWN/no→NORMAL/yes <1 s. Ctrl+C no validado limpiamente (primer intento falló, Stop-Process; segundo detuvo; causa no investigada). F2.4 HECHO; en ese momento la captura real seguía inactiva por falta de bucle/orquestación, que llegó con F5.1a.

## Decisiones y riesgos pendientes

A6 documenta protocolo de sesiones y JSON acotado sin dependencias. A8 documenta contratos de sensor; A9 fija JNA 5.19.1 y PrivacyFilter; A10 documenta la segmentación pura; A12 documenta SQLite V1 y el repositorio; A13 mantiene excluidos los navegadores no autorizados; A15 autoriza Chrome + un perfil con criterio de frescura/foco y límite ~1 s; A16 documenta la allowlist de F2.4; A17 documenta la máquina de deriva; A18 documenta F3.1b consola; A19 añade Brave con gate reducido; A20 corrige “sin registro”; A21 documenta la persistencia de drift; A22 documenta la UI F4.1; A23 documenta el rango F4.2 y su smoke diagnóstico; A24 documenta Uso diario F4.3 y el segmento abierto no incluido; A25 documenta la captura F5.1a y el snapshot abierto/pendiente; A26 documenta el borrado coordinado y el fallo SQLite F5.1b; A27 documenta la matriz de fallos/privacidad F5.1c y los mensajes de error genéricos; A28 documenta el flush del pendiente, el bloqueo como UNAVAILABLE y el cierre único con informe de hilo; A30 habilita Brave en la captura de UI con bridge y añade `-ExtensionId`/`-Port` a `Start-Ui.ps1`. Segundo perfil Chrome confirmado; Brave pendiente de segundo perfil/reinicio/1 h y colisión de sesiones. Ctrl+C no validado limpiamente (P2 con hipótesis de consola interactiva). Q2: PASS para Chrome + un perfil; resto de perfiles/navegadores sin autorización. Q3: renovación y reinicio reales confirmados; una hora sin huecos >3 s. Q4: parser acotado implementado y probado. Q1: Chrome real con extensión validado; segundo perfil repetido y confirmado. El camino de fallo nativo está implementado como UNAVAILABLE pero no se indujo manualmente; SEN-01 queda parcial por ese motivo. La captura real ya tiene un bucle desde la UI; la orquestación completa de producto sigue pendiente, pero las comprobaciones obligatorias de F5.1 están cerradas. Diseño visual: docs/DISENO.md; F4.1/F4.2/F4.3 y F5.1a–F5.1d implementados; **F5.1 HECHO** con límite S0 y SEN-01 PARCIAL. El servidor de diagnóstico no es todavía servicio permanente endurecido contra conexiones lentas. A32 documenta la pausa y el cierre `PAUSE`; A33 la bandeja y el cierre de ventana; A34 acepta la medición sostenida de F6.1. F5.2 y F6.1 están HECHO; F6.2 sigue pendiente de autorización. El smoke de bandeja invoca los callbacks del menú, no un clic humano.

## Entorno y continuidad

Java/javac 21.0.7, Maven 3.9.9, Node 22.16.0, Git 2.49.0. Caché work/m2; perfiles de intentos aislados en work/{chrome-pilot,chrome-pilot-node,edge-pilot,brave-pilot}. Artefactos/cachés ignorados. Git sin remoto ni commits; revisar cambios reales antes de editar. En este entorno git status simple informó que no estaba en work tree; no cambiar configuración global para resolverlo. Maven conserva aviso inicial de acceso denegado aunque build finaliza correctamente (E002/E003).

Los permisos de escritura pueden caducar entre turnos. Al retomar no reconstruir el piloto: comprobar estado y ejecutar solo las pruebas pendientes o afectadas por cambios.

## Punto de guardado: extensión instalada y bridge iniciado

2026-11-22: el usuario mostró Chrome con la extensión cargada y proporcionó ID cnipocakflhegoediialcdngmopbieip. Se inició Start-BridgePilot.ps1 con ese ID en el puerto 47821; GET /v1/ping respondió correctamente. Token generado en work/pilot/bridge-token.txt (no compartir ni imprimir). Sesión de ejecución 58362 de esta conversación, puede dejar de existir en otra sesión: comprobar ping antes de reiniciar. Pendiente introducir puerto/token en las opciones de la extensión y verificar reportes reales. La instalación está confirmada por captura del usuario; la comunicación extensión→bridge todavía no.

2026-11-22: conexión real confirmada. Captura del usuario: Bridge conectado; captura deshabilitada. GET /v1/status autenticado: state=UNKNOWN, sequence=49, accepted=49, captureEnabled=False. No se imprimió token ni host. Confirma reportes desde Chrome y aceptación del origen exacto; falta verificar cambios entre sitios, foco/privado, reinicio y caducidad reales. No habilitar captura.

2026-11-22: observación en vivo de 55 s solicitada al usuario con example.com. Sitio esperado observado=True; host limpiado después=True; UNKNOWN después=True; muestras=108; fallos de consulta=0. Solo indicadores guardados, sin historial ni otros dominios. Resultado negativo no prueba defecto: puede requerir repetir con coordinación. No valida todavía incógnito/reinicio/perfiles.

2026-11-22 BR-03 prueba real coordinada de cambio de pestaña (55 s): example.com reconocido=True; lichess.org reconocido después=True; UNKNOWN sin host después de ambos=True; captura siempre deshabilitada=True; muestras=108; errores HTTP=0. Solo indicadores guardados, sin tokens ni otros dominios. No valida incógnito o reinicio.

2026-11-22 BR-04 observación coordinada de incógnito, 55 s: sitio normal reconocido=False; dominio privado de prueba observado=False; normal retomado después de UNKNOWN >=5 s=False; mayor intervalo UNKNOWN sin host=0s; captura deshabilitada=True; muestras=108; errores=0. Indicadores únicamente. Intervalo privado indicado por pasos al usuario, no validado por sensor HWND; correlación temporal, no garantía universal. Confirmar que el usuario siguió la secuencia antes de dar esta prueba manual por pasada.

## Punto de guardado: F1.2 validada (E004)

2026-11-22: prueba paso a paso en Chrome real. Línea base 5/5 NORMAL example.com; incógnito real 12/12 UNKNOWN sin host; regreso 11/12 NORMAL example.com; reinicio de Chrome con sesión nueva y 6/12 NORMAL example.com. Foco perdido/recuperado en la primera observación. Bridge detenido al terminar; token local no impreso. Sin cambios de código. Segundo perfil, caducidad exacta y cierre ordenado con Ctrl+C quedan pendientes; captura deshabilitada.

## Punto de guardado: F1.3 contratos de sensor (E005)

2026-11-22: creados `ActivitySample`, `WindowSensor`, `IdleDetector`, `FakeSensor` y `FakeIdleDetector`; 8 tests nuevos. `mvn ... verify` offline: BUILD SUCCESS, 19 tests, 0 fallos. Los dobles viven en `src/test`; el sensor Windows real y el pipeline no están implementados. Captura de actividad sigue deshabilitada.

## Punto de guardado: F1.3 cerrada (E006)

2026-11-22: JNA 5.19.1; sensor real de ventana/proceso e idle; `PrivacyFilter` antes del tracking y secuencia determinista. Probes reales: WindowsTerminal.exe/SearchHost.exe con PID y ventana válidos; idle 63359 ms antes de entrada y 0 ms después. `mvn ... verify` offline: 26 tests, 0 fallos. F1.3 HECHO; SEN-01 parcial porque no se indujo un fallo nativo. Siguiente: F2.1 segmentación. Captura de sitios deshabilitada.

## Punto de guardado: F2.1 cerrada (E007)

2026-11-22: `ContextKey`, `Segment`, `StableTick`, `TickResult` y `Segmenter` puro; 18 tests nuevos. `mvn ... verify` offline: 44 tests, 0 fallos. Cubre debounce 2/3 s, primera muestra, fronteras IDLE/EXCLUDED/UNAVAILABLE, huecos >3 s, reloj hacia atrás, A→B→A y contextos por proceso/host/subdominio. Sin bucle de muestreo, SQLite ni UI. Siguiente: F2.2 SQLite y repositorios.

## Punto de guardado: F2.2 cerrada (E008)

2026-11-22: SQLite JDBC 3.54.1.0; esquema V1 completo con WAL/foreign_keys; `SQLiteSegmentRepository` con transacción por lote y lectura por solapamiento; 5 tests de repositorio y 1 de integración. `mvn ... verify` offline: 50 tests, 0 fallos. Ruta de producción, escritor serializado y UI siguen pendientes. Siguiente: F2.3 decisión de navegador.

## Punto de guardado: F2.3 cerrada (E009)

2026-11-22: decisión A13: navegadores excluidos. La evidencia E004 confirma comunicación, foco, incógnito y reinicio, pero no hay correlación fiable HWND/perfil, segundo perfil ni caducidad exacta; D4 impide habilitar captura. `PrivacyFilter` excluye Chrome/Edge/Brave/Firefox/Opera/Vivaldi y el test se amplió. `mvn ... verify` offline: 50 tests, 0 fallos. No se ejecutaron pruebas nuevas de navegador. Siguiente: F3.1 máquina de deriva.

## Punto de guardado: GATE-Q2 preparado

2026-11-22: antes de F3.1 se pausa para un gate Q2 de una sola sesión. Creados `docs/Q2_GATE.md`, `scripts/Measure-BridgeReports.ps1` (mide huecos de `accepted` cada 250 ms, sin hosts ni tokens) y el log `work/pilot/bridge-reports.log` (timestamp, seq, state, hostPresent; sin host). Pendiente: checklist manual confirmada por el usuario y una hora con pestaña normal en primer plano. Si falla: registrar E011 y continuar F3.1 con A13. Si pasa: habilitar un navegador/perfil y luego F3.1.

## Punto de guardado: GATE-Q2 cerrado (E011)

2026-11-22: el criterio del gate fue frescura de reportes y UNKNOWN al perder foco, **no** correlación HWND; se acepta un límite de ~1 s. Una hora: 13.680 muestras, 3.741 reportes, `maxGapMs=1088`, `over3s=0`, 0 errores, 0 reinicios. Privado/foco/reinicio reales. Segundo perfil repetido: UNKNOWN/no con B enfocado y NORMAL/yes <1 s al volver a A. Ctrl+C: primer intento falló y se cerró con Stop-Process; segundo detuvo el bridge; causa no investigada. F2.4 en curso; captura no activa.

## Punto de guardado: F2.4 cerrada (E012)

2026-11-22: `PrivacyFilter` permite Chrome solo con `BrowserStateProvider` fresco NORMAL; UNKNOWN/STALE y otros navegadores quedan EXCLUDED; el host se propaga a `Observation`/`Segment`. `BrowserStateStore.freshNormalHost()` añadido. `mvn ... verify` offline: 59 tests, 0 fallos (7 nuevos), incluida integración store→filter. Sin bucle/UI; captura real inactiva. Siguiente: F3.1 máquina de deriva.

## Punto de guardado: F3.1 cerrada (E013)

2026-11-22: `ChangeCounter` y `DriftStateMachine` puros. 10 cambios en `(t-120,t]` abren un episodio único; cambios posteriores actualizan `switchCount` y contextos; 180 s de contexto confirman STABLE con `timeToStabilityMs`; candidato pendiente bloquea; IDLE/EXCLUDED/UNAVAILABLE cierran UNCONFIRMED con motivo. `mvn ... verify` offline: 77 tests, 0 fallos (18 nuevos). Sin persistencia de drift/UI. Pendientes P1 (idle de navegador) y P2 (causa Ctrl+C) en FASES. Siguiente: revisar propuesta de integración mínima y luego F3.2.

## Punto de guardado: F3.1b cerrada (E014)

2026-11-22: `ConsoleCapture` con `work/console-day/deriva.db` por defecto, resumen separado por app/sitio/IDLE/EXCLUDED/UNAVAILABLE/sin registro, drift impreso sin persistir, guardado por segmento y `Segmenter.close`. Stop file probado en smoke real de 12 s con base separada; `SQLitePrivacyCheck` verificó esquema sin URL/título y hosts sin ruta/query; UNKNOWN/privado no guarda host. 88 tests, 0 fallos. Script `Start-ConsoleCapture.ps1` resuelve classpath SQLite/JNA. No marca F4.3 ni F5.1. P2: hipótesis de consola interactiva, no confirmada. Siguiente: F3.2.

## Punto de guardado: F2.4b cerrada (E015)

2026-11-22: `PrivacyFilter` permite `brave.exe` con las mismas reglas que Chrome: solo con `BrowserStateProvider` fresco NORMAL; UNKNOWN/STALE y otros navegadores EXCLUDED. Ventana privada de Brave UNKNOWN/no según el usuario. 90 tests, 0 fallos (2 nuevos). Gate de Brave reducido: pendientes segundo perfil, reinicio, una hora y colisión de sesiones Chrome/Brave porque el payload dice `browser:"chrome"`. Comando para Brave: `Start-ConsoleCapture.ps1 -ExtensionId cnipocakflhegoediialcdngmopbieip -Port 47821`, base `work/console-day/deriva.db`; no ejecutar Chrome y Brave a la vez contra el mismo bridge. Siguiente: F3.2.

## Punto de guardado: resumen F3.1b corregido (E016)

2026-11-22: captura real de Brave con sitios (`chess.com`, `youtube.com`), segmentos y resumen. `Sin registro` ahora se mide desde el primer segmento de la sesión; antes mostraba 16:20:52 como si fuera una hora del día. 91 tests, 0 fallos. No hubo drift porque los cambios <3 s se absorben; no valida segundo perfil/reinicio/1 h/colisión de Brave. Siguiente: F3.2.

## Punto de guardado: F3.2 cerrada (E017)

2026-11-22: `DriftRepository`/`SQLiteDriftRepository` con OPENED/UPDATED/CLOSED, contextos, anotaciones preservadas, `between` por solapamiento, recuperación OPEN→UNCONFIRMED/RESTART y cierre EXIT. `ConsoleCapture` persiste los updates de drift. 97 tests, 0 fallos (6 nuevos). Sin UI/borrado; P1/P2 y gate Brave completo siguen pendientes. Siguiente: pedir referencias de diseño y F4.1.

## Punto de guardado: F4.1 cerrada (E018)

2026-11-22: UI JavaFX de Interrupciones según `docs/DISENO.md`: tabla + panel de detalle, filtros de fecha/estado/justificadas, anotaciones, estados vacío/error, navegación lateral con Uso diario/Ajustes deshabilitados, consultas y guardado fuera del hilo FX. 102 tests, 0 fallos (5 nuevos). Captura real de la UI en modo demo: `work/ui-preview/interrupciones.png`. No se pulsó Guardar manualmente ni se forzó error/vacío real en esa entrega; las comprobaciones pendientes se completaron en E019. No hay bucle/orquestación de captura; Uso diario, Ajustes, borrado y panel de conexión siguen pendientes. Siguiente: F4.2/F4.3 según decisión del usuario, manteniendo gate Brave y P1/P2.

## Punto de guardado: F4.2 cerrada (E019)

2026-11-22: rango Desde/Hasta inclusivo por solapamiento; un episodio que cruza medianoche aparece en ambos días y una sola vez en el rango amplio. Inicio con fecha/hora. La anotación se ejercitó desde los controles de la UI (episodio 7, Mensajes, Justificada, Guardar) y se verificó tras cerrar y reabrir (`MESSAGES|1`). Estados vacío y error vistos con bases temporales; la base real conservó su hash. 106 tests, 0 fallos. El smoke de UI recorre controles, no es un clic humano. Quedan fuera de esta entrega: F4.3 Uso diario, Ajustes, borrado, panel de conexión, bucle de captura real, gate completo de Brave, P1 y P2.

## Punto de guardado: F4.3 cerrada (E020)

2026-11-22: Uso diario calculado desde segmentos persistidos, con recorte al día local, sitios dentro del tiempo de su navegador, IDLE/EXCLUDED/UNAVAILABLE y huecos fuera del total, cobertura separada y “sin datos” distinto de “0 s”. Navegación lateral funcional; Ajustes deshabilitado. 110 tests, 0 fallos. Capturas en `work/ui-preview/f4-3/`. Limitación: el segmento abierto no se persiste y no se incluye; la vista no es en vivo. USE-02 queda PARCIAL por Dota/YouTube real y el segmento abierto. F5 no se inicia; Brave, P1 y P2 siguen pendientes.

## Punto de guardado: F5.1a cerrada (E021)

2026-11-22: captura real iniciada/detenida desde la barra lateral, con estado visible y botón Iniciar/Detener. `CaptureSession` corre en hilo dedicado y cierra segmento/episodio al detener o cerrar la ventana. Uso diario recibe snapshot: abierto confirmado incluido como provisional, pendiente mostrado sin sumar, deduplicación con persistidos y refresco cada 2 s sin forzar cierre. Sesión real corta (Word 9 s persistidos; abierto Word observado), reapertura correcta y cierre con captura activa sin OPEN. 114 tests, 0 fallos. Sin bridge en la prueba real: Chrome/Brave excluidos; Brave queda excluido por gate. P1/P2 siguen; F5.1 EN_CURSO (borrado, matriz de fallos y demás pendientes). Capturas en `work/ui-preview/f5-1/`.

## Punto de guardado: F5.1b cerrada (E022)

2026-11-22: Borrar registros con confirmación propia. La operación detiene la captura, espera el hilo, borra segmentos/episodios/contextos y conserva configuración/token/exclusiones/schema; Uso diario e Interrupciones se refrescan y no reaparecen datos. Fallo SQLite simulado con trigger en base temporal: estado de error visible, captura detenida, sin segmento persistido ni OPEN, sin cola ilimitada. Reapertura correcta tras ambos escenarios. 115 tests, 0 fallos. Sin bridge: Chrome/Brave no capturados; Brave excluido. P1/P2, matriz de fallos/privacidad, cierre sin proceso oculto y bloqueo siguen pendientes; F5.1 EN_CURSO. Evidencia en `work/ui-preview/f5-1/`.

## Punto de guardado: F5.1c cerrada (E023)

2026-11-22: matriz de fallos y privacidad. Fallo SQLite en la escritura del bucle inducido con trigger + suspensión de 4 s del proceso Java: error visible antes de Detener, `0|0|0` y reapertura sin datos. Puerto de bridge ocupado: error visible en español y token ausente de salida y DB/WAL. Matriz sintética (host normal, UNKNOWN/privado, navegador no autorizado, app propia, idle, no disponible) sin URL/título/token/host privado. `CaptureSession` conserva el primer fallo y traduce `BindException`/`StorageException` a mensajes genéricos; `UiSmoke.captureCycle` no reintenta al ver “Reintentar captura”. 117 tests, 0 fallos (2 nuevos). Faltan cierre sin proceso oculto y bloqueo/reanudación; F5.1 EN_CURSO. Evidencia en `docs/PRUEBAS.md` E023 y `work/ui-preview/f5-1/`.

## Punto de guardado: F5.1d cerrada; F5.1 HECHO (E024)

2026-11-22: cierre completo y bloqueo/reanudación. Cierre de ventana con captura activa: 6 s persistidos, 0 OPEN, `deriva-capture` vivos=0 y sin `java.exe` después del proceso. `Segmenter.close` cierra el abierto en el inicio del candidato y persiste el pendiente como OBSERVED, sin solapamiento; probado con reloj simulado y reapertura SQLite. La primera pasada real de bloqueo contó `LockApp.exe` 4 s; `PrivacyFilter` ahora lo trata como UNAVAILABLE junto con `LogonUI.exe`; la segunda pasada dejó 45 s de bloqueo como UNAVAILABLE, 4 s OBSERVED y sin lockapp. La suspensión real solo pudo ser S0: 57 s UNAVAILABLE, 19 s OBSERVED, span=suma sin solapes ni huecos; `powercfg /a` no ofrece S3/hibernación. 121 tests, 0 fallos (4 nuevos). Uso diario mostró 4 s y 19 s de primer plano. Base real intacta. F5.1 HECHO con límites S0/SEN-01; P1/P2/Brave/USE-02 siguen fuera. Evidencia en `docs/PRUEBAS.md` E024 y `work/ui-preview/f5-1d/`.

## Punto de guardado: F6.1 preparación EN_CURSO (E025)

2026-11-22: sin tocar la base real ni los planos. `mvn ... verify` offline: 121 tests Java, 0 fallos; `node --test .\extension\tests\report.test.mjs`: 5 tests, 0 fallos. Creados `scripts/Measure-UiResources.ps1` (muestrea cada 30 s por defecto: timestamp, PID, CPU %, working set, memoria privada, hilos y handles; sin títulos, hosts, URL ni línea de comandos) y `docs/F6_MEDICION.md` (protocolo, copia de base y parada). Validación corta: medidor contra un proceso de prueba (3 muestras, fin por duración) y contra la UI real con `-Smoke shutdown-close` y base temporal (7 muestras, `fin: process-exited`, sin `java.exe` después). Artefactos del smoke eliminados. **La prueba sostenida de F6.1 no se ha ejecutado**; el CSV real lo aportará el usuario. Siguiente: ejecutar la sesión sostenida con la base copiada y registrar equipo, duración, muestras, CPU y memoria. Evidencia E025 en PRUEBAS.

## Punto de guardado: Brave habilitado en la captura de UI (E026)

2026-11-22: `CaptureSession` usa `CAPTURE_BROWSERS = {chrome.exe, brave.exe}`; sin bridge el proveedor de host es vacío y ambos quedan excluidos; con bridge fresco NORMAL se observan con sitio. El estado visible dice “Chrome/Brave + un perfil”. `Start-Ui.ps1` añade `-ExtensionId`, `-Port` y `-BridgeTokenFile`: lee el token local sin imprimirlo, valida formato, comprueba que el puerto esté libre, define `DERIVA_BRIDGE_TOKEN`/`DERIVA_EXTENSION_ID`/`DERIVA_BRIDGE_PORT` y restaura el entorno al salir. Prueba conjunta: UI con bridge en `47823`, base temporal y medidor → 7 muestras, bridge activo, sin `java.exe` al cerrar. `mvn ... verify` offline: 121 tests, 0 fallos. No se probó un reporte real de Brave con host a través de la UI; el gate completo de Brave sigue pendiente y no se deben usar Chrome y Brave a la vez contra el mismo bridge. Evidencia E026 en PRUEBAS; decisión A30.

## Punto de guardado: modo de prueba de Interrupciones (E027)

2026-11-23: botón **“Ver datos de ejemplo”** en Interrupciones. Genera en memoria 6 episodios sintéticos (1 abierta, 4 estables, 1 sin confirmar) con contextos, motivos y justificadas variadas; respeta los filtros de estado/fecha y rellena el detalle. No consulta ni escribe SQLite: en la base temporal de la prueba, `drift_event=0` y `segment=0`. Mientras está activo muestra “Modo prueba · episodios de ejemplo · no se guardan”, deshabilita Motivo/Justificada/Guardar y el botón pasa a “Volver a datos reales”. `mvn ... verify` offline: 122 tests, 0 fallos (1 nuevo). Smoke `-Smoke demo-episodes`: 6 filas, Guardar deshabilitado, cierre con hilos de captura 0; captura en `work/ui-preview/f6-demo/demo-episodes.png`. Evidencia E027 en PRUEBAS; decisión A31.

## Punto de guardado: F5.2 y F6.1 cerrados (E028–E030)

2026-11-23: F5.2 pausa/bandeja y F6.1 medición sostenida cerrados. **Guardar** ya persistía; E028 añade `Guardando…` y banner `Guardado · motivo · Justificada · HH:mm` que sobrevive al refresco (`work/ui-preview/f5-2/guardado.png`). **Pausa:** `PAUSED`, cierre `PAUSE`, reanudar abre bucle nuevo; la UI muestra `Captura en pausa.` y **Reanudar captura** (`pause-paused.png`, `pause.png`). **Bandeja:** `AppTray` con Abrir/Pausar-Reanudar/Salir; cerrar oculta y avisa; Salir detiene, `join`, flush, retira icono y termina (`tray.png`). Smokes en bases temporales: 0 solapes, `hilos deriva-capture vivos=0`, salida 0. **F6.1:** `work/f6/ui-resources.csv` (256 muestras, 2 h 7 min 30 s, CPU media 0,15 %, working set 249–416 MB) aceptado; PERF-01 PASÓ. 124 tests Java + 5 JS, 0 fallos. Base protegida `work/console-day/deriva.db` intacta. Siguiente: F6.2 pendiente de autorización; no iniciar Ajustes.

## Respaldo Git — 2026-11-23
Por solicitud del usuario se prepara el primer commit en main y la subida a https://github.com/markmercado1/Deriva.git. Se excluyen work/, target/, bases, logs, secretos locales y docs/pruebas/ (capturas de actividad real). No cambia el estado de F6.2 ni se modifica la app.

## Punto de guardado: UI-CLARIDAD cerrada (E031)

2026-11-23: rama `codex/claridad-anotaciones` (desde `main` 1745811). Archivos: `ui/viewmodel/{InterruptionRow,InterruptionsViewModel,StatusFilter,DisplayFormats}.java`, `ui/fx/InterruptionsController.java`, `resources/com/deriva/ui/{interrupciones.fxml,interrupciones.css}`, tests `InterruptionsViewModelTest` y `SQLiteDriftRepositoryTest`. **Anotación** = `answered_at` persistido; **Estabilidad** = En curso/Estable/No comprobada; detalle con motivo real de cierre. Guardar no toca estado. `mvn ... verify` offline: 127 tests, 0 fallos. Smokes sintéticos `annotate`/reapertura/fallo por trigger y captura UNCONFIRMED en `work/ui-preview/ui-claridad/`; no son clics humanos ni captura real. Base protegida intacta. F6.2 PENDIENTE. Siguiente paso exacto: revisar el commit y decidir con el usuario si se autoriza F6.2; antes, resolver si Ajustes se oculta o se mantiene deshabilitado (A36).

## UI-SCROLL — HECHO (2026-11-23, E032)
Detalle envuelto en ScrollPane, sin cambios de lógica ni barra lateral. Archivo funcional: interrupciones.fxml. Compilación y carga FXML correctas; probe JavaFX a 1200x600 confirma overflow y Guardar completamente visible al desplazar al final. Sin pruebas nuevas de captura ni SQLite. Próximo paso: reiniciar la app desde Salir para cargar el recurso actualizado; F6.2 pendiente.

## UI-SAVE — HECHO (2026-11-23, E033)
Guardar compara motivo/Justificada con la fila persistida y answered_at. Sin cambios: Guardado ✓ deshabilitado; al editar: Guardar cambios. Bloqueo durante escritura y recarga; error permite reintentar. Archivos: InterruptionsController.java, InterruptionRow.java y InterruptionsViewModelTest.java. Maven verify offline: 129 tests, 0 fallos. Smoke JavaFX con repositorio simulado: 30 clics generan una escritura; cambios/reversión/error/reintento correctos. No se tocaron bases reales. Próximo paso: salir de la instancia abierta y relanzar el build actualizado; F6.2 pendiente.
