# Fases y tareas

Estados: PENDIENTE, EN_CURSO, BLOQUEADA, HECHO. HECHO requiere evidencia enlazada en PRUEBAS. Una fase termina al cerrar todas sus tareas obligatorias. Una tarea bloqueada registra motivo y trabajo independiente disponible.

Los días de la sección 12 del plano son hitos de alcance. Cada fase puede repartirse entre sesiones y modelos. No avanzar de fase automáticamente para consumir los créditos disponibles.

| ID | Tarea | Estado | Criterio de cierre |
|---|---|---|---|
| F0 | Plan y continuidad | HECHO | Documentación de entrada, arquitectura, estado, pruebas y fases presentes; planos preservados (DOC-001) |
| F1.1 | Herramientas y esqueleto Maven | HECHO | ENV-01 PASÓ: Java 21/Maven verificados, clean verify online y offline, 1 test y JAR de consola; evidencia E002 |
| F1.2 | Piloto temprano de extensión y bridge | HECHO | Comunicación, foco e incógnito probados en Chrome real; reinicio renueva sesión. Segundo perfil y caducidad exacta quedan para F2.3 (E004) |
| F1.3 | Sensor y pipeline simulado inicial | HECHO | Proceso/ventana e idle reales observados; FakeSensor; primera secuencia determinista; privacidad antes del detector (E006) |
| F2.1 | Segmentación completa | HECHO | Debounce, fronteras, huecos, contexto por host y proceso probados (E007) |
| F2.2 | SQLite y repositorios | HECHO | V1, guardado, lectura por solapamiento, transacciones y reapertura probados (E008) |
| F2.3 | Decisión de integración navegador | HECHO | Evidencia D4 revisada; navegadores excluidos por falta de correlación HWND/perfil; decisión A13/E009 |
| GATE-Q2 | Gate para reabrir el navegador | HECHO | PASS: 1 h maxGap 1088 ms, over3s=0, 0 errores/reinicios; segundo perfil repetido UNKNOWN/no→NORMAL/yes; Ctrl+C no validado limpiamente (E011) |
| F2.4 | Habilitar Chrome + un perfil | HECHO | PrivacyFilter permite Chrome solo con NORMAL/fresco y propaga host; UNKNOWN/STALE y otros navegadores excluidos; tests e integración (E012) |
| F2.4b | Brave allowlist y gate reducido | HECHO | brave.exe solo NORMAL/fresco; privada UNKNOWN/no; pendientes segundo perfil, reinicio, 1 h y colisión de sesiones Chrome/Brave (A19/E015) |
| F3.1 | Máquina de deriva | HECHO | Umbral 10/120 s, ventana exacta, episodio único, estabilidad 180 s y UNCONFIRMED por frontera (E013) |
| F3.1b | Integración mínima por consola | HECHO | Sensor→privacidad→segmentador→SQLite; resumen app/sitio/IDLE/EXCLUDED/sin datos; drift impreso; guardado al cerrar; stop file probado; privacidad verificada (E014) |
| F3.2 | Persistencia de episodios | HECHO | OPEN/STABLE/UNCONFIRMED, contextos, anotaciones preservadas, recuperación RESTART y cierre EXIT; >30 min/medianoche (E017) |
| F4.1 | Interrupciones | HECHO | Tabla + panel de detalle, filtros, anotaciones, estados vacío/error y consultas fuera de FX; captura F4.1 (E018) |
| F4.2 | Anotaciones y filtros | HECHO | Motivo/Justificada persistidos y verificados con la ruta de UI al reabrir; rango de fechas por solapamiento, cruce de medianoche sin duplicados; sin puntuaciones (E019) |
| F4.3 | Uso diario por aplicación y sitio | HECHO | Totales de primer plano por fecha, recorte de medianoche, cobertura y sitios incluidos en su navegador sin doble conteo (E020). Limitación: no incluye el segmento abierto; USE-02 queda PARCIAL |
| F5.1a | Captura real desde la UI | HECHO | Iniciar/detener/cerrar con estado visible, snapshot persistido/abierto/pendiente sin cierre forzado, sesión real corta y reapertura (E021) |
| F5.1b | Borrado coordinado y fallo SQLite | HECHO | Borrar registros con confirmación, parada/join previos y conservación de configuración/token; fallo SQLite visible y parada controlada; reapertura tras ambos (E022) |
| F5.1c | Matriz de fallos y privacidad | HECHO | Fallo SQLite en la escritura del bucle, fallo de bridge, privado/exclusión/idle/no disponible y ausencia de datos sensibles en DB/WAL/logs/salida (E023) |
| F5.1d | Cierre completo y bloqueo/reanudación | HECHO | Cierre de ventana con captura activa sin hilo/proceso vivo y flush de abierto/pendiente; bloqueo real 45 s UNAVAILABLE; suspensión S0 real (eventos 506/507) de 57 s UNAVAILABLE; sin solapamientos ni duplicados (E024). S3/hibernación no disponibles en el equipo |
| F5.1 | Integración, privacidad y borrado | HECHO | F5.1a–F5.1d completados: captura, borrado, matriz de fallos/privacidad, cierre y bloqueo/reanudación con evidencia E021–E024. Límites: S0 únicamente, fallo nativo del sensor no inducido (SEN-01 PARCIAL), P1/P2/Brave/USE-02 fuera de F5.1 |
| F5.2 | Pausa/bandeja (OPCIONAL) | HECHO | Autorizada por el usuario el 2026-11-23; pausa y bandeja probadas (E029) |
| F5.2a | Pausa manual | HECHO | Pausar/reanudar cierra segmento y episodio con `PAUSE`, estado visible y reanudación sin pérdida ni solape (E029) |
| F5.2b | Bandeja básica | HECHO | Menú Abrir, Pausar/Reanudar y Salir; cerrar oculta; Salir detiene, hace flush y no deja proceso oculto (E029) |
| F6.1 | Prueba sostenida y rendimiento | HECHO | Medición real de 2 h 7 min 30 s con 256 muestras, CPU media 0,15 % (máx. 0,40 % de 16 CPUs), working set 249–416 MB y captura activa con Brave/bridge (E030). Límites: una máquina, resolución 30 s |
| F6.2 | Entrega local reproducible | PENDIENTE | Instrucciones verificadas de arranque/test, limitaciones y pendientes claros |

Dependencias: F1.1 → F1.2 → F1.3 → F2 → F3 → F4 → F5 → F6. GATE-Q2 PASS, F2.4 y F2.4b habilitan Chrome/Brave + un perfil en el filtro; F4.1, F4.2 y F4.3 están hechos (F4.3 solo con segmentos cerrados), F5.1a conecta la captura real con la UI, F5.1b añade borrado coordinado y fallo SQLite, F5.1c cubre la matriz de fallos/privacidad y F5.1d cierra el bloqueo/reanudación. **F5.1 HECHO** con límites documentados: el equipo solo ofrece suspensión S0, no S3/hibernación; el fallo nativo del sensor sigue sin inducir (SEN-01 PARCIAL). **F5.2 HECHO** el 2026-11-23 con pausa manual y bandeja básica (E029). P1/P2, Brave y USE-02 siguen abiertos fuera de F5.1/F5.2. Ajustes no está implementado. **F6.1 HECHO** con la medición sostenida E030; el siguiente bloque es F6.2, pendiente de autorización. El gate de Brave es reducido: faltan segundo perfil, reinicio, una hora y resolver la colisión de sesiones Chrome/Brave (`browser:"chrome"`). La captura ya tiene un bucle desde la UI, pero la orquestación completa de producto sigue pendiente; F4.1–F4.3 aportan la UI de lectura y F3.1b es consola diagnóstica. Si el navegador exige una acción externa, registrar el bloqueo del piloto y continuar la captura independiente; nunca postergar silenciosamente su primera prueba hasta F6.

## Avance F1.3

- [x] `ActivitySample`, `WindowSensor`, `IdleDetector`, `FakeSensor` y `FakeIdleDetector`; 8 tests nuevos (E005).
- [x] Sensor Windows real con JNA y prueba de proceso/ventana/idle (E006).
- [x] Privacidad antes del detector y primera secuencia determinista (E006).

## Avance F2.1

- [x] `ContextKey`, `Segment`, `StableTick`, `TickResult` y `Segmenter` puro (E007).
- [x] Debounce 2/3 s, primera muestra, fronteras IDLE/EXCLUDED/UNAVAILABLE.
- [x] Huecos >3 s, reloj hacia atrás, A→B→A y contextos por proceso/host/subdominio.

## Avance F2.2

- [x] SQLite JDBC 3.54.1.0, esquema V1 completo, WAL y foreign_keys.
- [x] `storage.port.SegmentRepository` y adaptador JDBC con transacción por lote.
- [x] Lectura por solapamiento semiabierto, centinelas no observados, rollback y reapertura.
- [x] Integración `FakeSensor → PrivacyFilter → Segmenter → SQLiteSegmentRepository`.
- [ ] Ruta de producción `%APPDATA%\DerivaPrototype\deriva.db`, escritor serializado y cola: quedan para la orquestación/F5.

## Avance F2.3

- [x] Revisión de D4, Q2 y evidencia real E004.
- [x] Decisión escrita: mantener todos los navegadores excluidos (A13).
- [x] `PrivacyFilter` excluye Chrome, Edge, Brave, Firefox, Opera y Vivaldi; prueba E009.
- [x] Reabrir solo con evidencia del GATE-Q2: Chrome + un perfil autorizado (A15); resto excluido.

## Avance F3.1

- [x] `ChangeCounter` en `analysis`: solo cambios OBSERVED confirmados y consecutivos; fronteras y pendientes no cuentan.
- [x] `DriftStateMachine` en `coach`: umbral 10 cambios en `(t-120, t]`, episodio único, `switchCount`, contextos y `detectedAt`.
- [x] Estabilidad 180 s por tick, candidato pendiente bloquea, cierre STABLE con `timeToStabilityMs`.
- [x] Fronteras IDLE/EXCLUDED/UNAVAILABLE cierran UNCONFIRMED con motivo; no se reutilizan cambios al abrir otro episodio.
- [x] 18 tests nuevos (E013).

## Avance F2.4b

- [x] `brave.exe` permitido con `BrowserStateProvider` fresco NORMAL; UNKNOWN/STALE y otros navegadores EXCLUDED; 2 tests nuevos.
- [x] Ventana privada de Brave: UNKNOWN/no, sin NORMAL yes (reporte del usuario).
- [ ] Segundo perfil de Brave.
- [ ] Reinicio de Brave.
- [ ] Una hora de estabilidad de Brave.
- [ ] Colisión de sesiones Chrome/Brave: el payload de Brave dice `browser:"chrome"`; una sola sesión en el bridge.

## Avance F4.1

- [x] JavaFX 21 con FXML/CSS y paleta/estructura de docs/DISENO.md.
- [x] Tabla de Interrupciones + panel de detalle a la derecha; sin detalle expandido.
- [x] Filtros de fecha, estado e incluir justificadas; estados vacío/error.
- [x] Anotaciones motivo/Justificada contra `DriftRepository`; consultas y guardado fuera del hilo FX.
- [x] Navegación lateral; Uso diario/Ajustes deshabilitados, sin pantallas anticipadas.
- [x] Captura real de la UI y 5 tests nuevos de ViewModel (E018).
- [x] Modo de prueba en Interrupciones: botón “Ver datos de ejemplo” carga 6 episodios sintéticos en memoria (abierta, estables y sin confirmar), no escribe en SQLite y deshabilita Guardar; filtros y detalle funcionan (E027).

## Avance F4.2

- [x] Rango Desde/Hasta inclusivo por día local; consulta por solapamiento y sin duplicados (E019).
- [x] Episodio que cruza medianoche visible en ambos días y una sola vez en un rango amplio.
- [x] Inicio con fecha y hora (`dd/MM HH:mm`); el rango y el cruce de medianoche dejan de ser ambiguos.
- [x] Anotación motivo/Justificada ejercitada desde los controles y el botón Guardar; persiste al cerrar y reabrir (E019).
- [x] Estados vacío y error comprobados visualmente con bases temporales; la base real no se tocó (E019).
- [x] Rango invertido y estados vacío/error comprobados visualmente; apertura, consultas y anotación fuera del hilo FX.
- [x] 4 tests nuevos (rango, cruce de medianoche, anotación tras reabrir) y smoke de UI diagnóstico.

## Avance F4.3

- [x] Servicio de uso diario reutilizando `SegmentRepository`; recorte al día local y zona visible (E020).
- [x] Totales por aplicación y sitio, ordenados por duración; sitio incluido en el navegador, sin sumarlo dos veces.
- [x] Exclusión de IDLE/EXCLUDED/UNAVAILABLE y huecos del total de primer plano; cobertura separada.
- [x] “Sin datos” distinto de “0 s”; estados vacío/error con bases temporales.
- [x] Vista Uso diario con barras simples, fecha, zona, cobertura y la nota de sitios incluidos.
- [x] Navegación lateral funcional entre Interrupciones y Uso diario; Ajustes sigue deshabilitado.
- [x] No se incluye el segmento abierto: solo se calculan segmentos cerrados persistidos; la UI no se presenta como vista en vivo.
- [x] 4 tests nuevos y capturas de uso, sin datos, cero uso y error (E020); USE-02 queda PARCIAL por la prueba real Dota/YouTube y el segmento abierto.

## Avance F5.1a/F5.1b/F5.1c/F5.1d

- [x] `CaptureSession` inicia/detiene la captura real en un hilo dedicado; el estado y el botón son visibles en la barra lateral (E021).
- [x] Cierre de ventana con captura activa: señal de parada, cierre de segmento/EXIT y espera de salida ordenada.
- [x] Uso diario recibe un `CaptureSnapshot`: abre un segmento confirmado como provisional, muestra el pendiente sin sumarlo y no fuerza cierres.
- [x] Deduplicación entre persistido y abierto; consultas y refresco fuera del hilo FX.
- [x] Sesión real corta y reapertura con segmentos persistidos; `-Smoke capture`/`capture-close`/`shutdown-close`/`capture-hold` como diagnóstico de UI.
- [x] Chrome + un perfil con bridge configurado; sin bridge, navegadores excluidos. Brave se habilitó después en la captura de UI con bridge (E026/A30); su gate completo sigue pendiente.
- [x] P1/P2 revisados en lo que afecta a la integración: idle 120 s se mantiene; la UI no depende de Ctrl+C.
- [x] Borrado coordinado con confirmación, parada/join y conservación de configuración/token; fallo SQLite visible y parada controlada (E022).
- [x] Fallo SQLite durante la escritura del bucle, con trigger y hueco de 4 s; error visible, sin segmento ni OPEN (E023).
- [x] Fallo de bridge por puerto ocupado, error visible y token fuera de DB/salida (E023).
- [x] Ventana privada, exclusión de app/navegador no autorizado, idle y no disponible sin host persistido (E023).
- [x] Errores y DB/WAL/logs/salida sin URL, título, token ni host privado en los escenarios probados (E023/PR-01).
- [x] Cierre de ventana con captura activa: abierto y pendiente volcados, hilo de captura 0, proceso Java terminado y sin OPEN (E024).
- [x] `Segmenter.close` cierra el abierto en el inicio del candidato y persiste el pendiente como OBSERVED; sin solapamiento (E024).
- [x] `LockApp.exe`/`LogonUI.exe` se tratan como UNAVAILABLE antes de idle; el bloqueo real de 45 s no se cuenta como uso (E024).
- [x] Suspensión/reanudación real S0: 57 s UNAVAILABLE, sin solapamientos ni duplicados; `powercfg /a` solo ofrece S0 (E024).
- [x] Reapertura y Uso diario tras bloqueo/suspensión: 4 s y 19 s de primer plano, el intervalo no observado fuera del total (E024).
- **Límite:** bloqueo duro S3/hibernación NO_DISPONIBLE en este equipo; el hueco de congelación se cubrió con simulación y tests de gap.

## Avance F5.2

- [x] `CaptureState.PAUSED` y `CaptureControl.pause()`: el bucle se detiene y el episodio abierto se cierra con `CloseReason.PAUSE`; reanudar abre un bucle nuevo (E029).
- [x] La barra lateral muestra `Captura en pausa.`, permite **Detener captura** y **Reanudar captura**; detener desde pausa deja `STOPPED` (E029).
- [x] `AppTray` con menú **Abrir Deriva**, **Pausar/Reanudar captura** y **Salir**; icono generado en memoria, sin recursos nuevos (E029).
- [x] Cerrar la ventana oculta a la bandeja y avisa; **Salir** detiene, espera el `join`, hace flush, retira el icono y termina el proceso (E029).
- [x] Sin `SystemTray`, cerrar termina y hace flush como antes; `Platform.setImplicitExit(false)` solo con bandeja instalada (E029).
- [x] Test del cierre `PAUSE` en la máquina y de persistencia del episodio UNCONFIRMED con `PAUSE`; smokes de pausa/reanudación y bandeja; 124 tests Java (E029).
- Límite: el smoke invoca los mismos callbacks del menú, no es un clic humano sobre el icono.

## Avance F6.1

- [x] Medición sostenida real: 256 muestras cada 30 s, 2 h 7 min 30 s, sin huecos >30,025 s (E030).
- [x] CPU media 0,15 % (máx. 0,40 %) del total de 16 CPUs lógicas; working set 249–416 MB; memoria privada ≤484 MB; hilos 67–70 (E030).
- [x] Captura activa durante la medición: 7.646 s registrados de 7.680 s, con Brave/bridge (3.390 s con host), inactividad y exclusiones (E030).
- [x] Equipo y entorno registrados: ASUS TUF F15 FX507ZC4, i5-12500H, 15,6 GB, Windows 11 26200, Java 21.0.7 (E030).
- Límites: una máquina, resolución de 30 s, solo el proceso de la UI; la sesión posterior de la mañana no tiene CSV.

## Avance F3.2

- [x] `DriftRepository` y `SQLiteDriftRepository`: OPENED/UPDATED/CLOSED, contextos y anotaciones.
- [x] Anotaciones no se sobrescriben al actualizar detección.
- [x] Recuperación de OPEN a UNCONFIRMED/RESTART con `closed_at=last_observed_ts`.
- [x] Cierre EXIT al salir; `DriftStateMachine.close`.
- [x] `between` por solapamiento, varios episodios, >30 min y cruce de medianoche.
- [x] 6 tests nuevos (E017).

## Avance F3.1b

- [x] `ConsoleCapture` compone sensor Windows + bridge + PrivacyFilter + Segmenter + SQLite + DriftStateMachine.
- [x] Guarda cada segmento al cerrarse; `Segmenter.close` vuelca el segmento abierto al salir.
- [x] Resumen separado: OBSERVED por app, sitios como desglose, IDLE, EXCLUDED, UNAVAILABLE y sin registro.
- [x] Imprime OPENED/UPDATED/CLOSED de deriva sin persistirlos.
- [x] Stop file alternativo probado; script con classpath Maven para SQLite/JNA.
- [x] Verificación de esquema/host: sin URL, título ni host privado; `work/console-day/deriva.db` por defecto (smoke en base separada).
- [x] `Sin registro` se mide desde el primer segmento de la sesión, no desde medianoche (E016).

## Gate Q2 (antes de F3.1)

- [x] Log de diagnóstico por reporte aceptado, sin host (E010).
- [x] Segundo perfil repetido: UNKNOWN/no con B enfocado y NORMAL/yes <1 s al volver a A.
- [x] Ventana privada: UNKNOWN/no durante 10 s.
- [x] Foco: UNKNOWN al salir y NORMAL al volver.
- [x] Reinicio: secuencia nueva y reconexión.
- [ ] Ctrl+C: primer intento falló y se cerró con Stop-Process; segundo detuvo el bridge; causa del primer fallo no investigada.
- [x] Una hora con pestaña normal: `maxGapMs=1088`, `over3s=0`, `over5s=0`, `over10s=0`, 0 errores, 0 reinicios; límite ~1 s aceptado.
- [x] Decisión: PASS para Chrome + un perfil (A15), basado en frescura/foco, no en correlación HWND.
- Regla de una sola sesión: cumplida; no se repite sin una hipótesis nueva.

## Avance F2.4

- [x] `PrivacyFilter` permite Chrome solo cuando `BrowserStateStore` está fresco en NORMAL; si no, EXCLUDED.
- [x] El host del estado NORMAL se propaga como contexto de sitio.
- [x] Tests de Chrome permitido, UNKNOWN/STALE excluido y resto de navegadores excluidos (E012).
- [x] No se habilitaron otros navegadores ni perfiles.

## Pendientes anotados

- P1 — Umbral de idle para navegador: hoy el filtro usa 120 s global; con sitios habilitados, un video o ajedrez sin input puede subcontarse. Decidir si se mantiene, se separa por contexto o se añade otra métrica; no forma parte de F3.1.
- P2 — Causa del primer Ctrl+C: el primer intento falló y se cerró con Stop-Process; el segundo detuvo el bridge. Hipótesis barata: el primer bridge corría como proceso de fondo sin consola interactiva; el segundo en ventana visible sí recibió Ctrl+C. No confirmado. `ConsoleCapture` ofrece stop file y se probó como alternativa.

## Antes de marcar una tarea HECHO

- Criterio de aceptación demostrado, no solo código escrito.
- Test o prueba manual identificada en PRUEBAS con resultado real.
- Ningún incumplimiento de arquitectura sin decisión documentada.
- ESTADO actualizado con siguiente tarea y trabajo parcial.
- No marcar F5.2 obligatoria ni reincorporar estadísticas avanzadas, adaptación, Hoy avanzado o instalador. Uso diario básico F4.3 sí está autorizado.

Al actualizar, sustituir “Estado inicial” por “Estado” y mantener los IDs. Añadir una columna de evidencia o referencias breves al cierre de cada tarea.




