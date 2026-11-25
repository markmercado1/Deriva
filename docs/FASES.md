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
| UI-CLARIDAD | Claridad anotación vs. estabilidad | HECHO | Columna Anotación desde `answered_at`, Estabilidad con En curso/Estable/No comprobada, explicación por motivo de cierre y filtros coherentes; guardar no altera el estado. No cierra F6.2 ni implementa Ajustes (E031) |
| EXCLUSION-FOCO | Revisión de cierre por foco en Deriva | PENDIENTE | Decidir si enfocar la propia ventana debe cerrar el episodio por EXCLUSION; no se cambia desde Ajustes ni se añaden registros diagnósticos sin aprobación |
| AJUSTES-DETECCION | Cuatro umbrales configurables de detección | HECHO | Ajustes edita cambios/ventana/estabilidad/inactividad, persiste en `setting`, distingue efectivo/pendiente, restaura sin guardar y avisa inactividad≤estabilidad. Pausar/Reanudar conserva; Detener/Iniciar reaplica. Configuración inválida usa predeterminados visibles sin sobrescribir. No cierra F6.2 ni añade exclusiones, bridge o empaquetado (E034) |
| BRAVE-EXCLUSIONES | UNKNOWN transitorio durante navegación normal | HECHO | Sonda real: UNKNOWN fresco aceptado con Brave en primer plano, sin STALE ni reinicio de sesión; el filtro lo convertía en EXCLUDED y la ventana de deriva se limpiaba. La extensión relee la pestaña de forma acotada ante cambio de revisión o URL no lista en carga, sin reutilizar hosts ni relajar incógnito/foco/perfiles. Verificación real posterior: 38 s NORMAL continuos y episodio abierto con 6 cambios (E035). No cierra F6.2 ni el gate completo de Brave |
| F6.1 | Prueba sostenida y rendimiento | HECHO | Medición real de 2 h 7 min 30 s con 256 muestras, CPU media 0,15 % (máx. 0,40 % de 16 CPUs), working set 249–416 MB y captura activa con Brave/bridge (E030). Límites: una máquina, resolución 30 s |
| F6B-1 | Objetivos: modelo de datos | HECHO | Esquema V2 con `goals` y `goal_alerts`, versionado por `schema_version` y migración V1→V2 idempotente; puertos y adaptadores SQLite con crear/leer/actualizar/borrar/listar activos; topes validados por dominio y base; sin URL/ruta/consulta/título (E036) |
| F6B-0 | Objetivos: actividad pasiva | HECHO | `PassiveActivity` puro promueve `IDLE` a `OBSERVED` solo si la muestra coincide con un objetivo activo `passive=true` y la inactividad no supera 10 min desde la última entrada; `UNAVAILABLE`/`EXCLUDED`, los huecos >3 s y el cambio de ventana mandan (también entre objetivos pasivos) hasta que vuelva la actividad. Regla de coincidencia app sin host frente a app+sitio exacta documentada en A43. Lee objetivos una vez al iniciar: los cambios se aplican al reiniciar la captura (limitación de F6B-0, sustituida por la recarga de F6B-3/A45) (E037) |
| F6B-2 | Objetivos: motor de cálculo | HECHO | `GoalProgressCalculator` puro: minutos/entradas/estado por objetivo con día local recortado, umbrales 80/100 %, entradas >=3 s, app con o sin host y aviso conservador de tiempo no medible; A43 sigue siendo solo la regla pasiva. 201 tests Java + 14 JS (E038) |
| F6B-3 | Objetivos: UI | HECHO | Pantalla **Objetivos** en la navegación lateral: crear, editar topes/pasivo/activo, activar/desactivar y borrar con confirmación; contexto elegido entre apps/sitios ya vistos o escrito a mano, sin poder cambiar el contexto al editar. Progreso del día con `GoalProgressCalculator` y estados En curso/Cerca del tope/Tope superado (icono+texto), aviso de subconteo y refresco cada 30 s solo en la vista. `PassiveGoalProvider` recarga la captura en curso sin reiniciar y conserva una promoción activa si su contexto sigue siendo pasivo. Máximo pasivo persistido en Ajustes con efectivo/pendiente (A45; E039) |
| F6B-4 | Objetivos: alarma básica | HECHO | `GoalAlertService` evalúa cada 30 s en el hilo `deriva-capture` solo con `OBSERVED`; añade el segmento en curso cortado en «ahora» vía `OpenSegmentProvider`; avisa por bandeja al 80/100 % por tope y día, deduplicado en `goal_alerts` tras reinicio; registra ambos umbrales y notifica solo el más alto si coinciden; un fallo de evaluación se registra y no detiene la captura; cambio de límite no repite un umbral ya registrado (A46; E040) |
| F6B-6 | Objetivos: justificar excesos | HECHO | Justificación por alerta al 100 % y día local; cubre el exceso acumulado con segmentos cerrados + abierto (A44), el uso posterior vuelve a contar, rejustificar reemplaza y quitar revierte. Tres estados icono+texto (Excedido/Justificado por ti/Sin responder) y nota de Sin responder; motivo ≤500 mostrado tal cual. V3 por `schema_version` sin tocar segmentos/episodios; motor puro con `GoalJustification`; escrituras en `deriva-goals`. 251 Java + 14 JS (A47; E041) |
| F6B-5 | Objetivos: alarma diferida durante deriva | HECHO | Aviso diferido con tope configurable (300 s por defecto, 60–1800 s, efectivo/pendiente) mientras hay deriva; consulta por puerto de solo lectura `DriftStateProvider`; V4 con `notified_at`, `observed_until`, `outcome_at` y `outcome_after_ms`; pendientes de una sesión anterior se descartan al arrancar (`DISCARDED`), pausa/reanudación de la misma sesión los entrega; observación de 2 min con `NO_DRIFT`, `STABLE`, `NEW_DRIFT`, `STILL_DRIFTING`, `NOT_OBSERVED`; mensaje combinado por objetivo; textos sin causalidad (A48; E042) |
| F6B-7 | Objetivos: resumen y gate | HECHO | «Resumen de hoy» por objetivo con minutos justificados/excedidos y avisos A48; auditoría de `goals`/`goal_alerts`; gate 65 min/130 muestras, CPU 0,20 % vs 0,15 % de E030, cierre con `deriva-capture` y `deriva-goals` vivos=0, 278 Java + 14 JS, hash de hechos idéntico y copia V1→V4 sin pérdida. Salvedad: hueco de 96 s por suspensión S0 (E043) |
| F6.2 | Entrega local reproducible | HECHO | README con guía verificada desde un clon limpio (requisitos, compilar/probar, abrir la app, extensión, datos/respaldo) y sección de limitaciones y pendientes. `maven-dependency-plugin` 3.7.0 fijado y ligado a `package` para generar `target/classpath.txt`; los scripts ya no llaman a Maven offline ni usan rutas absolutas (en un clon nuevo `Start-Ui.ps1` fallaba). ESTADO reducido y CONTINUAR al día (A58; E052) |

Paquete **F6B (objetivos)**, alcance fijado por el usuario el 2026-11-24: un objetivo es un límite diario definido por el usuario sobre un contexto (una app, o app + sitio), con tope de minutos y/o tope de entradas; no clasifica nada como bueno o malo. Usa los segmentos como fuente y no los modifica; la justificación de un exceso reutiliza el espíritu de la anotación Justificada (E031); la alarma diferida consulta la máquina de deriva sin modificarla. Orden de ejecución: F6B-1, F6B-0, F6B-2, F6B-3, F6B-4, F6B-6, F6B-5 y F6B-7 están HECHOS. **F6.2 HECHO (E052).** El modelo de datos de F6B-1 lo entrega el usuario; no se asume nada fuera de él.

Pendiente de revisión de comportamiento, fuera de AJUSTES-DETECCION: enfocar la propia ventana de Deriva puede cerrar un episodio por **EXCLUSION**; no se cambia esa regla aquí ni se afirma que los umbrales la resuelvan.

Dependencias: F1.1 → F1.2 → F1.3 → F2 → F3 → F4 → F5 → F6. GATE-Q2 PASS, F2.4 y F2.4b habilitan Chrome/Brave + un perfil en el filtro; F4.1, F4.2 y F4.3 están hechos (F4.3 solo con segmentos cerrados), F5.1a conecta la captura real con la UI, F5.1b añade borrado coordinado y fallo SQLite, F5.1c cubre la matriz de fallos/privacidad y F5.1d cierra el bloqueo/reanudación. **F5.1 HECHO** con límites documentados: el equipo solo ofrece suspensión S0, no S3/hibernación; el fallo nativo del sensor sigue sin inducir (SEN-01 PARCIAL). **F5.2 HECHO** el 2026-11-23 con pausa manual y bandeja básica (E029). P1/P2, Brave y USE-02 siguen abiertos fuera de F5.1/F5.2. **AJUSTES-DETECCION HECHO** (E034): cuatro umbrales de detección, sin editor de exclusiones ni bridge. **F6.1 HECHO** con la medición sostenida E030; **F6B-7 HECHO** con el gate E043 y sus limitaciones; el siguiente bloque es F6.2, pendiente de autorización. El gate de Brave es reducido: faltan segundo perfil, reinicio, una hora y resolver la colisión de sesiones Chrome/Brave (`browser:"chrome"`). La captura ya tiene un bucle desde la UI, pero la orquestación completa de producto sigue pendiente; F4.1–F4.3 aportan la UI de lectura y F3.1b es consola diagnóstica. Si el navegador exige una acción externa, registrar el bloqueo del piloto y continuar la captura independiente; nunca postergar silenciosamente su primera prueba hasta F6.

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
- [x] Navegación lateral; Uso diario/Ajustes deshabilitados en F4.1, sin pantallas anticipadas; Ajustes se habilitó después (E034).
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
- [x] Navegación lateral funcional entre Interrupciones y Uso diario; después se habilitó Ajustes con los cuatro umbrales (AJUSTES-DETECCION, E034).
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

## Avance UI-CLARIDAD

- [x] Columna **Anotación** con **Pendiente/Guardada** derivada de `answered_at`, no del motivo ni de Justificada (E031).
- [x] **Estado** renombrado a **Estabilidad**: **En curso**, **Estable** y **No comprobada**; filtro y detalle usan las mismas etiquetas (E031).
- [x] Detalle de `UNCONFIRMED` explica por qué no se comprobó con el motivo real de cierre; `STABLE` y `OPEN` tienen textos propios (E031).
- [x] Guardar conserva la confirmación visible y solo actualiza `reason`, `justified` y `answered_at`; no cambia estado ni reabre (E031, test SQLite real).
- [x] Smoke de UI: guardado, reapertura y fallo inducido sin éxito falso; base sintética (E031).
- Límite: los smokes recorren controles, no son clics humanos. Mejora de claridad, **no** cierre de F6.2.

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





## UI-SCROLL — HECHO (E032)
Corrección autorizada: desplazar únicamente el detalle de Interrupciones. F6.2 sigue pendiente.

## UI-SAVE — HECHO (E033)
Guardar solo con cambios pendientes y bloquear solicitudes simultáneas. F6.2 pendiente.

## Avance BRAVE-EXCLUSIONES (E035)

- [x] Paso 0 con consultas de solo lectura: sin los cortes, 4 cambios confirmados dentro de 30 s a las 11:59:06; con los cortes, la ventana y la continuidad se reinician y no hay episodio.
- [x] Sonda opt-in de 250 ms sobre el bridge real: solo timestamp, estado, secuencia/contadores, `hostPresent` y categoría de foco; nunca host, URL, título ni token.
- [x] Causa demostrada: UNKNOWN fresco y aceptado (~0,5–0,8 s) con Brave normal en primer plano; no fue STALE, rechazo, reinicio de sesión, incógnito ni cambio de perfil. El filtro lo convirtió en EXCLUDED y la máquina de deriva limpió la ventana.
- [x] Corrección en la extensión: relectura acotada (hasta 3 intentos, 150 ms entre reintentos y una lectura final) ante cambio de revisión o URL no lista durante carga; cada intento lee de nuevo y nunca reutiliza un host; incógnito, foco perdido, página interna y pestaña cerrada no se reintentan.
- [x] Sin cambios en Java, `PrivacyFilter`, frescura de 3 s, semántica de EXCLUDED ni umbrales.
- [x] 14 tests JS (report/sampling/background) y 158 tests Java, 0 fallos; verificación real posterior con 38 s NORMAL continuos y episodio abierto con 6 cambios.
- Límites: una máquina y un perfil/ventana de Brave; no se repitieron incógnito, pérdida de foco real ni caducidad real en navegador. El gate completo de Brave y F6.2 siguen pendientes. La UI muestra que el bridge está configurado, no que reciba un sitio fresco; queda como mejora separada.

## Avance F6B-5 (E042)

- [x] `DriftStateProvider` de solo lectura; `CaptureSession` deriva `DriftSignal` de los `DriftUpdate` sin cambiar la máquina de deriva.
- [x] Aviso registrado con `notified_at = NULL` y entregado al terminar la deriva o al alcanzar `alert_deferral_max_s` (300 s por defecto, 60–1800 s, efectivo/pendiente).
- [x] V4 con `notified_at`, `observed_until`, `outcome_at` y `outcome_after_ms`; migración V3→V4 fija `notified_at = triggered_at` en las alertas existentes.
- [x] Reinicio: pendientes de una ejecución anterior se cierran con `DISCARDED`; pausa/reanudación de la misma sesión entrega el pendiente.
- [x] Observación de 2 min: `NO_DRIFT`, `STABLE` con tiempo, `NEW_DRIFT`, `STILL_DRIFTING`, `NOT_OBSERVED`; sin causalidad en los textos.
- [x] Un solo aviso por objetivo y evaluación con los umbrales de sus métricas combinados; se registran todos.
- [x] 21 tests nuevos, 272 Java + 14 JS, 0 fallos; smokes `settings` y `goals` con hilos de captura 0.
- Límites: deriva y reloj simulados, sin sesión real de 10 min; la migración de la base real la hará el usuario con respaldo; `SQLitePrivacyCheck` sigue pendiente para F6B-7.

## Avance F6B-7 (E043)

- [x] «Resumen de hoy» en cada tarjeta de Objetivos: minutos justificados separados de los excedidos sin justificar y una línea «Aviso …» por alerta con umbral, métrica, hora y resultado A48.
- [x] `SQLitePrivacyCheck` audita `segment`, `goals` y `goal_alerts`; sin columnas prohibidas y sin identidades con forma de URL/ruta/consulta en `goals`. La justificación es texto libre y no se bloquea por patrones (A49).
- [x] Gate: 65 min, 130 muestras cada 30 s, CPU media 0,20 % / máxima 0,89 % (E030: 0,15 %), sin crecimiento sostenido de memoria/hilos; salvedad de un hueco de 96 s por suspensión S0.
- [x] Cierre limpio: `deriva-capture` y `deriva-goals` vivos=0; solo queda la UI 18972 abierta antes de la fase, visible y ajena a la corrida.
- [x] Suite: 278 tests Java + 14 JS, 0 fallos. Huella de `segment`/`drift_event`/`drift_context` idéntica antes/después de crear objetivo, alarma, justificar y quitar; copia V1→V4 con 14 segmentos y hash idéntico, original intacta.
- Límites para F6.2: subconteo de sitios por exclusión ante duda; P1 solo en objetivos pasivos; aviso de bandeja no verificado con clic humano y mensaje de dos topes solo con notificador falso; progreso con segmentos cerrados (hasta 30 s tarde); máximo pasivo fijo por sesión; migración real por el usuario con respaldo.

## UI-VENTANA — HECHO (2026-11-25, E044)
Petición del usuario: barra nativa accesible al arrancar (minimizar/maximizar/cerrar y arrastrar). Ajustar tamaño y posición al área útil del monitor; conservar colores y comportamiento de bandeja. F6.2 permanece pendiente.

Compilación y arranque JavaFX real verificados: ventana DECORATED/redimensionable dentro del área útil con escala 125 %. Sin prueba de clic/arrastre humano (E044).

## UI-OBJETIVOS-RESUMEN — HECHO (2026-11-25)
Resumen de hoy plegable por objetivo y eliminación de las dos burbujas señaladas por el usuario. Mantener paleta y lógica de objetivos. F6.2 pendiente.
Evidencia E045: compilación y sonda JavaFX correctas; sin clic humano.

## UI-AJUSTES-MENU — HECHO (2026-11-25)
Cuatro tarjetas de acceso a apartados de Ajustes, regreso claro y conservación del formulario. Mantener guardado y validaciones. F6.2 pendiente.
E046: build, navegación programática, conservación de borrador y snapshot revisado.

## USO-RANGOS — HECHO (2026-11-25)
Autorizado: Día/Semana/Mes/Año/Personalizado, fechas inclusivas y totales del intervalo sin doble conteo. F6.2 pendiente.
E047: 281 tests aprobados, sonda FXML y captura revisada; rendimiento masivo sin medir.

## USO-DIA-HOY — HECHO (E048) (2026-11-25)
Al cambiar a Día, seleccionar hoy local; conservar selección manual posterior.

## AJUSTES-AYUDA — HECHO (E049) (2026-11-25)
Tres tarjetas; ayudas plegadas y valores/aplicación dentro de Detección.

## OBJETIVOS-HISTORIAL — HECHO (E050) (2026-11-25)
Selector diario, historial de versiones y archivado al borrar, lectura histórica sin modificar el pasado. Migración V5; no reconstruir datos antiguos perdidos. Pruebas SQLite/migración/UI necesarias.

## OBJETIVOS-RANGO — HECHO (E051) (2026-11-25)
Petición del usuario: consultar Objetivos con Desde/Hasta y mostrar los objetivos borrados en solo lectura con una opción para ocultarlos. Un día = vista actual (hoy editable, pasado solo lectura); varios días = resumen por objetivo y detalle por día, solo lectura. Sin cambios de esquema. F6.2 pendiente.

## ENTREGA-FINAL — HECHO (E053) (2026-11-25)
Petición del usuario: preparar la entrega a partir de tres plantillas (código fuente, para ejecutar, manual de uso): ejecutable `Deriva.exe` con jpackage, instalador, capturas reales de la app sobre datos sintéticos y los tres documentos Word. La app empaquetada guarda sus datos en `%LOCALAPPDATA%\Deriva`. Sin datos reales del usuario en capturas ni documentos.
