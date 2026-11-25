# Registro de decisiones

No sustituye el plano. Referencia las reglas y registra razones para evitar que otra IA las reinvente. Nuevas decisiones se añaden con ID, fecha, estado, motivo, alcance y evidencia. Una decisión sustituida se conserva como historial.

| ID | Estado | Decisión | Referencia / motivo |
|---|---|---|---|
| D1 | Aplicable salvo objeción | Un episodio abierto; unir disparos solapados | Plano v2 §3 y §7; no contar minutos dos veces |
| D2 | Aplicable salvo objeción | Estabilidad por contexto evaluada cada tick | Plano v2 §3; no esperar segmento cerrado |
| D3 | Aplicable salvo objeción | Identidad = proceso + host | Plano v2 §3; etiquetas visibles no definen identidad |
| D4 | Aplicable salvo objeción | Excluir navegador si atribución/privacidad es incierta | Plano v2 §3 y §14; prueba real antes de habilitar |
| A1 | Base de trabajo solicitada | Clean Architecture ligera, puertos/adaptadores, DI manual, MVVM ligero | Usuario pide continuidad de arquitectura; concreta §5 sin introducir frameworks |
| A2 | Base de trabajo solicitada | Trabajo por tareas pequeñas y estado persistente | Usuario prevé interrupciones por créditos; cada entrega deja punto de guardado |
| A3 | Vigente por alcance v2 | Sin adaptación, estadísticas avanzadas ni análisis comercial nuevo | Petición explícita del usuario |

## Pendientes de definición, no bloquean preparar F1.1

| ID | Pregunta | Resolver en | Regla mientras no se resuelva |
|---|---|---|---|
| Q1 | Qué Chromium y perfil se usarán | F1.1/F1.2 | No asumir Brave por ejemplos del plano |
| Q2 | Cómo correlacionar sesión, secuencia y ventana activa | F1.2/F2.3 | Navegador excluido; no afirmar garantía de privado |
| Q3 | Renovación de evidencia con caducidad de 3 s y ciclo MV3 | F1.2/F2.3 | No aumentar caducidad para ocultar fallos |
| Q4 | Parser JSON seguro compatible con dependencias permitidas | F1.2 | No regex improvisada; proponer biblioteca concreta si hace falta |
| Q5 | Versiones y herramientas disponibles | F1.1 | No declarar comandos de build verificados |

No se necesita preguntar al usuario cada decisión rutinaria. Investigar lo comprobable en su equipo, documentar la elección mínima y continuar dentro del alcance. Si cambia una promesa o contrato sustancial, presentar el cambio concreto y el impacto juntos.

## A4 — 2026-11-21 — Uso diario básico

Estado: solicitado explícitamente por el usuario (“entonces agrega eso”), incorporado al alcance de F4.3. Mostrar tiempo diario por aplicación y sitio. Decisión de medición conservada del sensor: primer plano, no tiempo abierto ni reproducción de audio. Dota con YouTube de fondo suma a Dota mientras tenga foco; cambiar a YouTube reparte el tiempo. No se solicitó todavía implementar una métrica de audio simultáneo. No cambia D4: sitios solo con atribución segura. A3 sigue aplicando a estadísticas avanzadas, no a esta vista básica. Aumenta el trabajo de F4; seis días siguen siendo una previsión, no garantía.

## A5 — 2026-11-21 — Base mínima F1.1

Se usa el JDK 21.0.7 y Maven 3.9.9 ya instalados. Maven de un módulo, release 21, UTF-8, sin module-info ni framework nuevo. JUnit 5.11.4 solo para tests; plugins clean 3.4.1, resources 3.3.1, compiler 3.14.1, surefire 3.5.2, jar 3.4.2 fijados. Versiones seleccionadas compatibles, sin afirmar que son las más recientes. La compatibilidad efectiva se verifica mediante clean verify.

JavaFX/JNA/SQLite se añadirán al implementar sus adaptadores. El esqueleto no introduce clases de dominio ficticias ni tablas. Build usa caché local work/m2; no modifica la instalación global. Git inicializado sin remoto ni commits. Bootstrap de consola transitorio; UI sigue en F4.

Q5 resuelta para F1.1: Java/javac 21.0.7, Maven 3.9.9, Git 2.49.0. Chrome 157.0.8103.79, Edge 158.0.4370.33 y Brave 158.1.100.58 encontrados mediante metadatos de ejecutables. Q1: Chrome candidato inicial, selección de perfil y prueba pendientes en F1.2. Q2–Q4 siguen pendientes; no habilitar captura.

Referencia de configuración del compilador: https://maven.apache.org/components/plugins-archives/maven-compiler-plugin-3.14.1/usage.html . La evidencia de descarga/ejecución del build determina las versiones realmente resueltas.

## A6 — 2026-11-22 — Piloto F1.2, implementación y límites

Q4: se implementó un parser de protocolo acotado (FlatJson) por descenso de tokens, sin regex/subcadenas para interpretar JSON. Solo objetos planos de strings/null/enteros positivos; rechazo de duplicados, anidación, campos extra y trailing input. No añade dependencias ni pretende ser un parser JSON general.

Q2 parcialmente resuelta: POST /v1/session crea ID aleatorio del servidor, invalida reportes anteriores, y seq impide replay o reordenamiento dentro de una sesión. Nuevo worker/servidor negocia de nuevo. Falta correlación HWND/perfil: ningún reporte habilita captura; captureEnabled=false fijo en diagnóstico. El endpoint adicional es una concreción del contrato de sesión que el plano dejó pendiente para el piloto; no modifica la BD.

Q3: lectura nueva por eventos/timer 1 s mientras el worker corre; alarma 30 s solo despierta para nueva lectura. Caducidad monotónica estricta de 3 s, sin heartbeat que prolongue el host. UNKNOWN limpia inmediatamente el host. El comportamiento real MV3 está pendiente; no se garantiza cobertura continua.

Origen restringido al ID exacto configurado; token de 32 bytes, HTTP exclusivamente 127.0.0.1, 2 KB, 20 peticiones/s, sin CORS abierto. /v1/status autenticado es diagnóstico temporal en memoria, no historial. No logs de host/token/payload. El script Start-BridgePilot.ps1 genera token local dentro de work/pilot (ignorado por Git) y no lo imprime.

Q1 sigue parcialmente abierta: Chrome candidato, se intentó perfil aislado. Chrome/Brave abortaron con 2147483651, Edge no expuso CDP; causa no determinada. No se instalaron extensiones en perfiles personales ni se cambiaron sus ajustes. Prueba manual necesaria antes de cerrar BR-01/BR-02. No ampliar alcance a sensor/UI ni afirmar privacidad validada en navegador.

## A7 — 2026-11-22 — Cierre de F1.2 con validación real

Estado: aplicable. La validación paso a paso en Chrome real confirmó comunicación, línea base normal, exclusión de incógnito, regreso y reinicio. Incógnito: 12/12 UNKNOWN sin host; no se expuso el dominio privado. Reinicio: sesión nueva y reconexión. F1.2 se cierra HECHO; BR-01 PASÓ y BR-02 queda PARCIAL. Segundo perfil, caducidad exacta de 3 s en navegador real y cierre ordenado con Ctrl+C no se validaron y quedan para F2.3. No se habilita captura ni se afirma correlación HWND/perfil. Evidencia E004 en PRUEBAS.

## A8 — 2026-11-22 — Contratos de sensor y dobles deterministas

Estado: aplicable. `ActivitySample` es un record puro en `sensor`; una muestra disponible debe identificar proceso, PID y ventana, y una muestra `available=false` no puede llevar identidad ni idle. El nombre de proceso se limita a nombre de archivo (sin rutas ni controles) para no filtrar rutas completas. `FakeSensor` y `FakeIdleDetector` viven en `src/test/java` como dobles de test, no en el JAR. No se añadió JNA en esta tarea. Evidencia E005 en PRUEBAS.

## A9 — 2026-11-22 — Sensor Windows y privacidad antes del tracking

Estado: aplicable. JNA/jna-platform 5.19.1 fijados. `WindowsWindowSensor` usa `GetForegroundWindow`, `GetWindowThreadProcessId` y `QueryFullProcessImageName`, reutiliza un buffer de ruta en el hilo de muestreo y convierte fallos nativos en `available=false` sin identidad. `WindowsIdleDetector` usa `GetLastInputInfo` con resta unsigned de ticks de 32 bits; si falla lanza y la orquestación futura debe mapearlo a `UNAVAILABLE`. `PrivacyFilter` excluye el PID propio y los navegadores (D4) antes de crear contexto; idle >120 s produce `IDLE`; `Observation.OBSERVED` es el único estado con identidad. `Observation`/`ObservationKind` viven en `privacy`; `ContextKey`, debounce y segmentación quedan para F2.1. El fallo nativo no se indujo manualmente; SEN-01 queda PARCIAL. Evidencia E006 en PRUEBAS.

## A10 — 2026-11-22 — Segmentación F2.1

Estado: aplicable. `Segmenter` es una máquina de estados pura, sin hilos, SQL, HTTP ni UI. Debounce de 3 s sobre tiempo observado; al confirmar, el candidato empieza en su primer tick y cierra el segmento anterior en ese mismo instante. IDLE/EXCLUDED/UNAVAILABLE se aplican inmediatamente y absorben cualquier candidato pendiente. Un hueco >3 s cierra en el último tick válido y deja un hueco sin rellenar; un reloj hacia atrás cierra en el último tick válido y pasa a UNAVAILABLE. `StableTick` describe el contexto confirmado con `pendingChange` mientras hay candidato; al inicio, sin contexto confirmado, describe el candidato provisional. `Segment` no observado no lleva identidad y su `id` es 0 hasta F2.2. `ContextKey` normaliza proceso/host y conserva subdominios. El bucle de muestreo/`TrackingService` y la persistencia no forman parte de esta tarea. Evidencia E007 en PRUEBAS.

## A11 — 2026-11-22 — Diseño visual pendiente de referencias

Estado: pendiente. La UI es JavaFX y entra en F4.1 (Interrupciones), F4.2 (anotaciones/filtros) y F4.3 (Uso diario). Antes de implementar F4.1, recordar al usuario y pedir referencias concretas (screenshots, paleta, tipografía, densidad, estilo) para evitar un diseño genérico. No crear `docs/DISENO.md` hasta recibir esas referencias.

## A12 — 2026-11-22 — SQLite V1 y repositorio de segmentos

Estado: aplicable. SQLite JDBC 3.54.1.0 fijado. V1 crea el esquema completo del plano, incluida `schema_version`; las tablas de drift quedan listas para F3 aunque sus repositorios aún no existan. Cada conexión activa WAL, `foreign_keys=ON` y `busy_timeout`. `saveAll` abre una transacción y revierte el lote completo ante error; `between` usa solapamiento semiabierto `start_ts < to AND end_ts > from`. Los segmentos no observados usan centinelas `__IDLE__`, `__EXCLUDED__`, `__UNAVAILABLE__` y no llevan identidad. El adaptador abre una conexión por operación; el escritor serializado, la cola acotada y la ruta `%APPDATA%\DerivaPrototype\deriva.db` se conectarán en la orquestación/F5. Evidencia E008 en PRUEBAS.

## A13 — 2026-11-22 — Navegadores excluidos por D4

Estado: vigente. Se revisó la evidencia real E004 (extensión cargada, comunicación, foco, incógnito, regreso y reinicio) y sus límites: Q2 sigue sin correlación fiable entre reporte, ventana normal/HWND y perfil; segundo perfil, caducidad exacta de 3 s y cierre ordenado no se validaron. D4 y la sección 14.3 obligan a mantener `browser_capture_enabled=false` cuando no se puede distinguir con fiabilidad una ventana privada o un segundo perfil del estado normal anterior. Decisión: todos los navegadores quedan excluidos; no se guardan hosts ni tiempo de sitios. `PrivacyFilter` excluye Chrome, Edge, Brave, Firefox, Opera y Vivaldi. El núcleo continúa con procesos permitidos. Solo se reabrirá con correlación HWND/perfil fiable, segundo perfil y pruebas pendientes completas, mediante una nueva decisión escrita. Evidencia E009 en PRUEBAS.

## A14 — 2026-11-22 — Log temporal de diagnóstico del bridge

Estado: temporal, solo para GATE-Q2. Se activa con `DERIVA_BRIDGE_REPORT_LOG`; `Start-BridgePilot.ps1` lo apunta a `work/pilot/bridge-reports.log`. Por cada reporte aceptado escribe timestamp ISO, `seq`, `state` y `hostPresent=yes/no`; nunca host, URL, título ni token. Hace flush por línea. Un fallo de escritura se informa por stderr y no tumba el bridge. No es producción: se retira o desactiva al cerrar el gate. Evidencia E010.

## A15 — 2026-11-22 — GATE-Q2: Chrome + un perfil (PASS)

Estado: vigente para Chrome + un perfil; sustituye A13 solo en ese alcance. **El criterio no fue correlación HWND/perfil.** Fue: (a) frescura de reportes, con una hora de 13.680 muestras, `maxGapMs=1088`, `over3s=0`, 0 errores y 0 reinicios, aceptando un límite de ~1 s de latencia entre reportes; y (b) la extensión reporta UNKNOWN/no al perder el foco (otro perfil, ventana privada u otra app). No se demostró correlación nativa HWND; se acepta el límite de ~1 s y la evidencia empírica de foco.

Segundo perfil: repetido y confirmado; UNKNOWN/no con B enfocado y NORMAL/yes <1 s al volver a A. Ctrl+C: el primer intento falló y se cerró con Stop-Process; el segundo detuvo el bridge; la causa del primer fallo no se investigó. No se marca como cierre ordenado validado, pero no bloquea el criterio de atribución por frescura/foco.

Alcance autorizado: Chrome, un único perfil, sin incógnito ni otros navegadores/perfiles. `browser_capture_enabled` sigue false hasta que F2.4 implemente la allowlist en `PrivacyFilter` y propague el host solo con `BrowserStateStore` fresco en NORMAL. Si las pruebas de F2.4 fallan, A15 se revisa y A13 sigue vigente. Evidencia E011.

## A16 — 2026-11-22 — F2.4 allowlist de Chrome

Estado: aplicable. `PrivacyFilter` mantiene el constructor por defecto con todos los navegadores excluidos. Con un `BrowserStateProvider` explícito, `chrome.exe` solo se marca OBSERVED si el proveedor devuelve un host NORMAL fresco; UNKNOWN/STALE lo dejan EXCLUDED. El host se propaga a `Observation` y `ContextKey`; idle, no disponible y proceso propio tienen prioridad y no llevan host. Edge/Brave/Firefox/Opera/Vivaldi siguen excluidos. `BrowserStateStore.freshNormalHost()` expone el host solo antes de 3 s. La app aún no compone el proveedor ni tiene bucle/UI, así que la captura real no está activa; el límite de ~1 s de A15 se mantiene. Evidencia E012.

## A17 — 2026-11-22 — F3.1 máquina de deriva

Estado: aplicable. `ChangeCounter` (analysis) cuenta solo transiciones confirmadas entre OBSERVED consecutivos; pendientes no cuentan y una frontera resetea continuidad. `DriftStateMachine` (coach) usa ventana `(t-120 s, t]`, umbral 10, un solo episodio abierto, `switchCount` y contextos actualizados por cambios, estabilidad de 180 s evaluada por tick y candidato pendiente bloqueante. STABLE fija `stabilityStartTs`, `closedAt` y `timeToStabilityMs = stabilityStartTs - startTs`. IDLE/EXCLUDED/UNAVAILABLE cierran UNCONFIRMED con motivo, `lastObservedTs` del último OBSERVED y sin tiempo de estabilidad. Al cerrar se limpia la ventana; un episodio nuevo exige 10 cambios nuevos. Sin persistencia de drift, UI ni bucle; eso es F3.2/orquestación. PAUSE/LOCK/EXIT/GAP/RESTART aún no los produce `Segmenter`. Evidencia E013.

## A18 — 2026-11-22 — F3.1b integración mínima por consola

Estado: temporal, rebanada vertical de diagnóstico; no sustituye F4.3 ni F5.1. `ConsoleCapture` compone sensor Windows, bridge, `PrivacyFilter` con `BrowserStateStore::freshNormalHost`, `Segmenter`, `SQLiteSegmentRepository` y `DriftStateMachine`. Base por defecto `work/console-day/deriva.db`; el script resuelve el classpath Maven para SQLite/JNA. Guarda cada segmento al cerrarse y usa `Segmenter.close` al salir. El resumen separa OBSERVED por app, sitios como desglose, IDLE, EXCLUDED, UNAVAILABLE y sin registro. Imprime drift sin persistirlo. Stop file como alternativa a Ctrl+C. `SQLitePrivacyCheck` verifica que no existan columnas URL/título ni hosts con ruta/query; UNKNOWN/privado no guarda host. Escrituras sincrónicas, sin cola/escritor de producción, sin UI; Chrome/Brave + un perfil. P1 (idle de navegador) y P2 (causa Ctrl+C, hipótesis de consola interactiva) abiertos. Evidencia E014.

## A19 — 2026-11-22 — Brave allowlist reducida

Estado: vigente de forma reducida. `brave.exe` se permite en `PrivacyFilter` con las mismas reglas que Chrome: solo si `BrowserStateProvider` devuelve un host NORMAL fresco; UNKNOWN/STALE, idle, no disponible, proceso propio y el resto de navegadores quedan EXCLUDED. El gate de Brave fue reducido: la ventana privada pasó (UNKNOWN/no, sin NORMAL yes), pero quedan pendientes segundo perfil, reinicio, prueba de una hora y el choque de sesiones Chrome/Brave. El payload de la extensión de Brave dice `browser:"chrome"` y `BrowserStateStore` mantiene una sola sesión; por tanto Chrome y Brave no deben reportar simultáneamente al mismo bridge. No se declara paridad completa con Chrome. Evidencia E015.

## A20 — 2026-11-22 — “Sin registro” desde el inicio de la sesión

Estado: aplicable solo a `ConsoleSummary` (F3.1b). El tiempo “sin registro” se mide desde el primer segmento de la sesión hasta el corte del resumen, no desde medianoche. Evita mostrar `16:20:52` como si fuera una hora del día cuando en realidad eran horas previas a iniciar la captura. Si no hay segmentos, se usa el rango solicitado. No cambia la métrica de Uso diario ni F4.3. Evidencia E016.

## A21 — 2026-11-22 — Persistencia de drift F3.2

Estado: aplicable. `DriftRepository`/`SQLiteDriftRepository` persisten OPENED/UPDATED/CLOSED, contextos y anotaciones. El id en memoria del episodio se mapea a un id SQLite por sesión; las actualizaciones no sobrescriben `reason/justified/answered_at`. Al arrancar, `recoverOpenEpisodes` convierte OPEN en UNCONFIRMED/RESTART con `closed_at=last_observed_ts`; al salir, `DriftStateMachine.close(EXIT)` cierra el episodio abierto. `between` consulta por solapamiento y devuelve contextos/anotaciones. No hay UI, borrado ni retención; no se persisten títulos ni URLs. Evidencia E017.

## A22 — 2026-11-22 — UI F4.1 Interrupciones

Estado: aplicable. La UI es JavaFX 21.0.13 con FXML/CSS, entrada `com.deriva.app.Launcher` (evita el arranque directo de `Application` desde classpath) y `DerivaApp` como composición. `docs/DISENO.md` fija la Propuesta 3 como base, tema claro, paleta cálida y tabla + panel de detalle (sin detalle expandido en filas). `InterruptionsService` consulta `DriftRepository` por rango y `InterruptionsViewModel` transforma a filas puras; el controlador ejecuta lectura y anotación en un ejecutor de un hilo y aplica resultados en el hilo FX, descartando respuestas obsoletas. Los estados se dibujan con formas (`Circle`) y texto, no solo color; no hay puntuaciones ni juicios. La demo (`DERIVA_UI_DEMO=1`, base `work/ui-preview/demo.db`) es solo para captura y no toca la base de consola; `seedIfMissing` evita borrar anotaciones al reabrir. F4.1/F4.2 no capturan ni borran; F4.3 añade Uso diario. Ajustes sigue fuera. Evidencia E018.

## A23 — 2026-11-22 — Rango de fechas F4.2 y smoke de UI

Estado: aplicable. El rango `Desde`/`Hasta` es inclusivo por día local: `desde` a las 00:00 y `hasta+1 día` a las 00:00, con `start_ts < fin` y `COALESCE(closed_at,last_observed_ts) > inicio`. Un episodio que cruza medianoche se incluye en ambos días consultados y no se duplica dentro de un mismo rango; la consulta SQL devuelve una fila por episodio. La tabla muestra `dd/MM HH:mm` para desambiguar días. Un rango invertido no consulta la base: muestra un aviso y limpia la tabla. La apertura/migración, la consulta y la anotación motivo/Justificada se mantienen fuera del hilo FX; la UI se construye después, y la anotación se conserva al reabrir. `seedIfMissing` solo siembra la demo cuando la base no existe. El smoke `DERIVA_UI_SMOKE=annotate` es diagnóstico: recorre los controles reales y dispara Guardar, pero no simula un clic humano; no sustituye una prueba manual de una persona. Las bases temporales viven en `work/ui-preview/f4-2/` y la base real no se toca. Evidencia E019.

## A24 — 2026-11-22 — Uso diario F4.3

Estado: aplicable. `DailyUsageService` reutiliza `SegmentRepository` y recorta cada segmento a `[inicio del día local, día siguiente)`. Solo los segmentos `OBSERVED` suman al total de primer plano, a las aplicaciones y a los sitios; `IDLE`, `EXCLUDED`, `UNAVAILABLE` y los huecos no suman. Los sitios se agregan por `siteHost` y son un subconjunto del tiempo de su navegador: se muestran en una lista aparte y nunca se añaden al total de aplicaciones. La cobertura usa el tiempo con registro (incluidos los segmentos no observados) frente al tiempo transcurrido del día (hasta ahora si es hoy, día completo si es pasado); el tiempo sin registro incluye huecos. “Sin datos” significa que no hay segmentos; “0 s” significa que hay segmentos pero ningún primer plano observado. La UI muestra fecha, zona, total, cobertura y dos listas con barras simples, con la etiqueta permanente de sitios incluidos. `ConsoleCapture` solo persiste segmentos completados: el segmento abierto no se incluye hasta cerrarse y la vista no se presenta como actualización en vivo. La demo siembra segmentos sintéticos, incluido uno que cruza medianoche, y no toca la base real. F4.3 no añade Ajustes ni inicia F5; USE-02 queda PARCIAL por la prueba real Dota/YouTube y el segmento abierto. Evidencia E020.

## A25 — 2026-11-22 — Captura real desde la UI y snapshot F5.1a

Estado: aplicable. `CaptureSession` implementa `CaptureControl`, corre sensor/segmentador/deriva/SQLite en un hilo dedicado y expone `CaptureStatus` y `CaptureSnapshot` volátiles. La UI solo inicia, detiene o cierra; las consultas y escrituras no corren en el hilo FX. Al cerrar la ventana se envía parada, se cierra el segmento y el episodio (EXIT) y se espera el hilo. La captura usa `PrivacyFilter` con `Set.of("chrome.exe")`: sin bridge los navegadores quedan excluidos; con `DERIVA_BRIDGE_TOKEN`/`DERIVA_EXTENSION_ID`/`DERIVA_BRIDGE_PORT` se permite Chrome + un perfil. Brave sigue excluido hasta cerrar su gate. `Segmenter.snapshot` devuelve el abierto confirmado y el candidato pendiente sin mutar ni cerrar. Uso diario suma el abierto a los totales como provisional, muestra el pendiente sin sumarlo y omite cualquier abierto que solape un segmento persistido; el refresco de la vista es cada 2 s y no fuerza cierres. `CaptureSnapshot` de un día pasado se ignora. P1 (idle 120 s) se mantiene; P2 se evita en la UI con parada explícita, pero el Ctrl+C de consola sigue sin validar. F5.1a no añade borrado, pausa/bandeja ni Ajustes; F5.1 sigue EN_CURSO. Evidencia E021.

## A26 — 2026-11-22 — Borrado coordinado y fallo SQLite F5.1b

Estado: aplicable. `SQLiteDataMaintenance.deleteActivity` borra solo `drift_context`, `drift_event` y `segment` en una transacción; conserva `setting` (incluido token), `excluded_app`, `excluded_site` y `schema_version`. La UI muestra una confirmación propia; al confirmar, `CaptureSession.deleteAllRecords` detiene la captura, espera el hilo (`join`), borra, deja el snapshot vacío y la captura detenida. La UI refresca Interrupciones y Uso diario; no se reanuda automáticamente y no puede reaparecer un segmento en memoria. Un fallo de SQLite durante `saveAll` termina el bucle de captura, se marca `ERROR` visible y se cierra lo posible; no hay cola de escritura, así que no se acumula memoria sin límite. El escenario se simuló con un trigger temporal sobre una base temporal. No se toca la base real. Brave sigue excluido; P1/P2, matriz completa de fallos/privacidad, cierre sin proceso oculto y bloqueo siguen pendientes. F5.1 sigue EN_CURSO. Evidencia E022.

## A27 — 2026-11-22 — Matriz de fallos y privacidad F5.1c

Estado: aplicable. La matriz se compone de pruebas automáticas y smokes de UI, no de clics humanos. `BrowserPrivacyIntegrationTest` recorre Chrome autorizado, UNKNOWN/privado, navegador no autorizado, proceso propio, idle y no disponible; persiste una base temporal y escanea DB, `-wal` y `-shm` para comprobar que solo se guarda el host normal y que no aparecen URL, título, token ni host privado. `LocalBridgeServerTest` comprueba que un puerto ocupado lanza `IOException` en vez de omitir el bridge silenciosamente.

El fallo de escritura del bucle se indujo sobre una base temporal con un trigger que aborta los INSERT en `segment`: se abrió la UI con `-Smoke capture`, se suspendió el proceso Java 4 s (`NtSuspendProcess`/`NtResumeProcess`) y, al reanudar, el hueco >3 s cerró el segmento y el `saveAll` del bucle falló. El primer fallo se conserva en `CaptureSession` y el fallo de cierre se añade como “el cierre también falló”; ambos dejan la captura en `ERROR` y, en el escenario probado, sin segmento ni OPEN. Un puerto de bridge ocupado produce un `BindException`, también visible como `ERROR`. `BindException` y `StorageException` se traducen a mensajes genéricos en español; las excepciones desconocidas muestran solo el nombre de la clase para no filtrar mensajes con datos sensibles. El token del bridge no se imprime ni se persiste en estos escenarios.

`UiSmoke.captureCycle` no vuelve a pulsar el botón si muestra “Reintentar captura”; antes, un error durante la captura se interpretaba como una orden de detener y reiniciaba la captura en el smoke. La acción humana de reintentar no cambia. No se indujo un fallo nativo del sensor de ventana: SEN-01 sigue PARCIAL. La suspensión de 4 s simula un hueco de reanudación, no un bloqueo de Windows real; el bloqueo/desbloqueo y el cierre sin proceso oculto siguen pendientes. F5.1 sigue EN_CURSO. Evidencia E023.

## A28 — 2026-11-22 — Cierre completo, pendiente y bloqueo/reanudación F5.1d

Estado: aplicable. `Segmenter.close` persiste el candidato pendiente al terminar la sesión: si existe un abierto, lo cierra en el inicio del candidato y guarda el candidato como `OBSERVED` hasta el fin; si no hay abierto, guarda directamente el pendiente. Así no se pierde lo observado al cerrar y los intervalos quedan contiguos y sin solapamiento. Un candidato que empieza antes del abierto se descarta para no inventar un intervalo negativo; el abierto se cierra al final. La UI no necesita cerrar el segmento durante el refresco; el volcado ocurre solo al detener o cerrar.

`PrivacyFilter` trata `lockapp.exe` y `logonui.exe` como `UNAVAILABLE` antes de evaluar idle o navegador. La primera pasada real mostró 4 s de `LockApp.exe` contados como uso; tras el cambio, una segunda pasada de 45 s de bloqueo no guardó `lockapp.exe` y dejó todo el bloqueo como `UNAVAILABLE`. El sensor sigue devolviendo no disponible cuando no hay ventana en primer plano.

`CaptureSession.shutdown` marca `ERROR` si el hilo no termina tras el `join` de 10 s. `DerivaApp` usa un cierre único (`shutdownOnce`) para no duplicar parada/informe y escribe `UI shutdown: hilos deriva-capture vivos=N`; un cierre normal debe dar 0 y el proceso Java debe terminar. `UiSmoke` añade `shutdown-close` (cierra la ventana real) y `capture-hold` (duración por `DERIVA_UI_HOLD_SECONDS`) para las pruebas de bloqueo/suspensión.

La prueba real de suspensión solo pudo usar S0 Modern Standby: `powercfg /a` no ofrece S1/S2/S3 ni hibernación. En S0 el proceso puede seguir muestreando; el intervalo se registra como `UNAVAILABLE`, no como uso. El caso de congelación total (sin muestras) se cubre con la simulación de proceso y los tests de gap; no se declara una prueba S3 real. En el equipo de prueba, F5.1 queda HECHO con ese límite, SEN-01 PARCIAL, y P1/P2/Brave/USE-02 abiertos fuera de F5.1. Evidencia E024.

## A29 — 2026-11-22 — Medición de CPU y memoria F6.1

Estado: aplicable. La medición de F6.1 usa `scripts/Measure-UiResources.ps1`, que identifica el proceso `java.exe` cuya línea de comandos contiene `com.deriva.app.Launcher` y muestrea cada 30 s (configurable). Cada fila guarda timestamp, tiempo transcurrido, PID, CPUs lógicas, CPU %, working set, memoria privada, hilos y handles. `CpuPercent` es el delta de tiempo de CPU del proceso dividido por el intervalo real y por las CPUs lógicas; por tanto, es un porcentaje del total del sistema y puede superar 100 solo si el proceso usa varios núcleos. La primera fila es la línea base y deja la CPU vacía.

El CSV no incluye títulos de ventana, hosts, URLs, rutas de navegador, nombre de documentos ni la línea de comandos; la línea de comandos solo se usa en memoria para localizar el proceso. El medidor no abre la base ni escribe en ella. La base de prueba se copia con la UI cerrada, incluyendo `-wal`/`-shm` si existen, y se verifica el SHA-256 de la base original antes y después; nunca se mide sobre `work/console-day/deriva.db`. El medidor corre en primer plano y termina por duración, por cierre de la UI o con `Ctrl+C`, sin dejar tareas en segundo plano. Una sesión corta con `shutdown-close` valida el mecanismo, pero no se presenta como prueba de rendimiento. Evidencia E025.

## A30 — 2026-11-22 — Brave en la captura de UI y parámetros de bridge

Estado: aplicable. `CaptureSession` usa `CAPTURE_BROWSERS = {chrome.exe, brave.exe}` al construir `PrivacyFilter`. Sin bridge, el proveedor de host es `Optional::empty`, por lo que ambos navegadores quedan `EXCLUDED`; con bridge y estado `NORMAL` fresco, se observan con host. Esto sustituye solo la exclusión de Brave descrita en A25; el gate reducido de A19 (segundo perfil, reinicio, una hora y colisión de sesiones) sigue vigente y no se declara completo.

`Start-Ui.ps1` acepta `-ExtensionId`, `-Port` (47821 por defecto) y `-BridgeTokenFile` (por defecto `work\pilot\bridge-token.txt`). Lee el token sin imprimirlo, exige 64 hex, comprueba que el puerto no esté escuchando, define `DERIVA_BRIDGE_TOKEN`, `DERIVA_EXTENSION_ID` y `DERIVA_BRIDGE_PORT` para el proceso Java y restaura los valores previos al salir. La UI inicia el `LocalBridgeServer` dentro del proceso de captura; no hay que arrancar `Start-BridgePilot.ps1` ni `Start-ConsoleCapture.ps1` en paralelo, y hacerlo en el mismo puerto provoca un fallo de bind visible. El payload de Brave sigue declarando `browser:"chrome"`, así que solo se admite un navegador/perfil a la vez. El medidor de F6.1 no usa red ni el puerto del bridge: solo consulta métricas del proceso. Evidencia E026.

## A31 — 2026-11-23 — Modo de prueba de Interrupciones

Estado: aplicable. `InterruptionsViewModel.demoPage` genera seis `DriftEvent` sintéticos en memoria (IDs `900001`–`900006`): una abierta, cuatro estables y una sin confirmar, con contextos, motivos y justificadas variadas. No consulta `DriftRepository` ni escribe SQLite; el controlador usa esta página solo cuando el botón **Ver datos de ejemplo** está activo. En ese modo se muestra “Modo prueba · episodios de ejemplo · no se guardan”, se deshabilitan Motivo/Justificada/Guardar y `save()` no hace nada; **Volver a datos reales** restaura la consulta normal. Los filtros de estado e incluir justificadas se aplican igual que en datos reales; el rango usa el día “Desde” para fechar los ejemplos. No se crea una pantalla nueva ni se toca la base real; los IDs altos evitan confundirlos con episodios persistidos. No cambia el refresco de la pantalla: sigue sin actualización periódica. Evidencia E027.

## A32 — 2026-11-23 — Pausa manual y cierre con motivo PAUSE

Estado: aplicable. F5.2a añade `CaptureState.PAUSED` y `CaptureControl.pause()`. Pausar detiene el bucle de muestreo y el bloque de cierre usa `CloseReason.PAUSE` para el episodio de deriva abierto y `Segmenter.close` para el segmento abierto; no se guarda el tiempo en pausa como uso. Reanudar llama a `start()`, que crea un bucle y una sesión de captura nuevos. `stop()` desde `PAUSED` deja el estado en `STOPPED` sin lanzar un hilo. La UI muestra `Captura en pausa.`, mantiene **Detener captura** y ofrece **Reanudar captura**. El cierre por pausa se prueba de forma unitaria (`DriftStateMachine.close` con `PAUSE`) y con el hueco entre segmentos; un smoke corto no garantiza un episodio abierto, así que no se presenta como prueba de deriva real. Evidencia E029.

## A33 — 2026-11-23 — Bandeja básica y cierre de ventana

Estado: aplicable. F5.2b usa `java.awt.SystemTray` desde `com.deriva.app.AppTray`. El menú tiene **Abrir Deriva**, **Pausar/Reanudar captura** y **Salir**. Con bandeja instalada, `Platform.setImplicitExit(false)` y cerrar la ventana la oculta (con aviso); la captura sigue hasta que el usuario pause o salga. **Salir** ejecuta el mismo camino único de cierre: retira el icono, detiene la captura, espera el `join`, hace flush y termina el proceso; se usa `System.exit(0)` tras el cierre porque el bucle nativo de AWT puede mantener vivo el JVM. Si `SystemTray` no está disponible o falla, no se activa `setImplicitExit(false)` y cerrar la ventana termina y hace flush como en F5.1. La bandeja es específica de Windows y no se probó con un clic humano: el smoke invoca los mismos callbacks del menú. Evidencia E029.

## A34 — 2026-11-23 — Medición sostenida F6.1 aceptada

Estado: aplicable. La medición de F6.1 se acepta con `work/f6/ui-resources.csv` (SHA-256 `2DF7C43703890F7A43D6086E7ED6F3A27CC8542A8B7C6EF129FFC19175FA0EDD`): 256 muestras cada 30 s durante 2 h 7 min 30 s, con captura activa y bridge/Brave según los segmentos de la base de la sesión. Se registran CPU media 0,15 %, máxima 0,40 % y p95 0,24 % sobre 16 CPUs lógicas; working set 394,7→249,0 MB (máx. 416,4 MB); memoria privada ≤484,0 MB; hilos 67–70 y handles 750–760. El CSV no tiene huecos mayores de 30,025 s. Es una sola máquina y una resolución de 30 s; no se extrapola a otros equipos ni a sesiones de varias horas. La sesión posterior de la mañana (capturas en `docs/pruebas/`) no tiene CSV y no se usa para las cifras. La base protegida `work/console-day/deriva.db` no se tocó. Evidencia E030.

## A35 - 2026-11-23 - Anotación y estabilidad son datos distintos

Estado: aplicable. En Interrupciones, **Anotación** se deriva solo de `drift_event.answered_at`: `NULL` → **Pendiente**; no nulo → **Guardada**. No se usa `reason` ni `justified` para inferirlo. **Estabilidad** sustituye a «Estado» y usa **En curso** (`OPEN`), **Estable** (`STABLE`) y **No comprobada** (`UNCONFIRMED`); el filtro y el detalle usan esas mismas etiquetas. Para `UNCONFIRMED`, el detalle explica la ausencia de comprobación con el `close_reason` persistido. Guardar sigue ejecutando solo `UPDATE drift_event SET reason, justified, answered_at`; no toca `status`, `closed_at` ni `close_reason`, por lo que no reabre ni cierra el episodio. Sin cambios de esquema. Evidencia E031.

## A36 - 2026-11-23 - Ajustes sigue fuera de alcance

Estado: aplicable. El botón **Ajustes** continúa visible pero deshabilitado; no se crea una pantalla vacía. El plano v2 §8–9 pide una lista editable de procesos/hosts excluidos mediante configuración local documentada, valores de captura/umbrales, puerto/token del bridge y un panel de conexión. La base ya tiene las tablas `setting`, `excluded_app` y `excluded_site`, pero no hay repositorio, validación, pantalla ni política de reinicio implementados. Recomendación: ocultar o mantener deshabilitado Ajustes hasta una fase propia; si se implementa un mínimo, limitarlo a mostrar la ubicación y los valores efectivos de configuración, la lista de exclusiones y el aviso de reinicio, sin editor visual completo ni opciones inventadas. No se implementa en esta entrega.

## A37 — 2026-11-23 — Detalle desplazable
Por petición del usuario, envolver únicamente el detalle en ScrollPane: ajuste al ancho, scroll vertical según necesidad y sin scroll horizontal. Guardar permanece dentro del contenido. Sin cambiar barra lateral ni lógica.


## A38 — 2026-11-23 — Guardar solo cambios pendientes
Comparación pura en InterruptionRow entre motivo/Justificada y valores persistidos; answered_at nulo permite la primera anotación con motivo válido. El controller bloquea reentrada, invalida lecturas anteriores y mantiene bloqueo hasta recargar el resultado. No cambia esquema ni reglas de episodios.

## A39 — 2026-11-23 — Cuatro umbrales de detección configurables

Estado: aplicable. Ajustes implementa solo cuatro claves sobre la tabla `setting` existente: `drift_switches`, `drift_window_s`, `stable_s` e `idle_threshold_s`. Predeterminados conservados: **10 / 120 / 180 / 120**. Límites iniciales de producto (no científicos): cambios 4–30; ventana 30–600 s; estabilidad 30–900 s; inactividad 30–1800 s. La validación es pura en `DetectionSettings`; un valor persistido ausente usa el predeterminado sin aviso, y uno inválido usa el predeterminado del campo afectado, muestra qué valores están en uso y **no** sobrescribe lo guardado. La sesión congela sus valores al iniciar; **Pausar/Reanudar conserva** y **Detener/Iniciar relee**. Guardar no modifica el episodio en curso ni recalcula historial. `DriftEpisode`/`DriftEvent` exigen `switchCount > 0`, no 10, para admitir umbrales menores. La consola diagnóstica conserva los predeterminados y no lee la configuración. El aviso inactividad≤estabilidad es informativo y no bloquea. Sin editor de exclusiones, bridge ni cierre de F6.2. Evidencia E034.

## A40 — 2026-11-23 — UNKNOWN transitorio de la extensión no debe cortar la deriva

Estado: aplicable. La sonda real mostró que, con Brave normal en primer plano, la extensión enviaba un reporte **UNKNOWN fresco** y aceptado durante ~0,5–0,8 s al navegar. No era STALE (los reportes seguían a ~1/s), no era un rechazo ni un reinicio de sesión, y el usuario no usó incógnito ni cambió de perfil. El `PrivacyFilter` lo convertía correctamente en EXCLUDED y `DriftStateMachine` limpiaba la ventana. El origen en la extensión era doble: el contador de revisión invalidaba una lectura en curso y la URL podía estar vacía mientras la pestaña cargaba.

Corrección: `settleObservation` relee la pestaña de forma acotada (hasta 3 intentos con 150 ms entre reintentos y una lectura final) cuando cambia la revisión o cuando un UNKNOWN es reintentable por URL aún no lista en una pestaña `loading`. Cada intento hace una lectura nueva; no se reutiliza ningún host anterior. Incógnito, ventana no enfocada, pestaña inactiva/cerrada, página interna y URL inválida estable no se reintentan y siguen produciendo UNKNOWN/INTERNAL de inmediato. El plazo de frescura de 3 s, la semántica de EXCLUDED, los umbrales y el `PrivacyFilter` de Java no cambian. Si tras los reintentos la lectura sigue sin ser NORMAL, se envía UNKNOWN; la privacidad no se relaja. Evidencia E035.

## A41 — 2026-11-24 — Versionado del esquema y migración V1→V2

Estado: aplicable. El versionado sigue en la tabla existente `schema_version(version)` (una fila), no en `PRAGMA user_version`: ya está en todas las bases V1, se actualiza en la misma transacción que el DDL y no crea una segunda fuente de verdad. `initialize` aplica una escalera: crea el marcador si falta, lee la versión (`0` sin fila) y ejecuta V1 si `< 1` y V2 si `< 2`; al final normaliza el marcador a una sola fila y hace `commit`, con `rollback` ante error. Una base nueva queda directamente en V2; una V1 solo recibe V2. La migración es idempotente (guarda de versión y `IF NOT EXISTS` en el DDL nuevo) y no altera `segment`, `drift_event`, `drift_context`, `setting` ni `excluded_*`. **Límite explícito:** una base en V2 ya no la abre el código V1; `initialize` lanza `StorageException("Unsupported schema version: 2")` y el código anterior falla al abrir. Por eso la migración de la base real la hace el usuario con respaldo y SHA-256. El DDL de V1 no se modifica. Evidencia E036.

## A42 — 2026-11-24 — Borrado de objetivos y alertas

Estado: aplicable. `goal_alerts.goal_id` usa `ON DELETE CASCADE`: una alerta es un aviso derivado de un tope concreto y no tiene significado sin su objetivo; los hechos de actividad (`segment`, `drift_*`) no se tocan. El coste asumido es que la justificación de la alerta se pierde con el borrado físico; por eso la vía normal para dejar de vigilar es `active = 0`, que conserva objetivo, alertas y justificación. «Borrar registros» (`SQLiteDataMaintenance.deleteActivity`) borra también `goal_alerts` porque son historial de uso, pero **no** borra `goals` porque son configuración, igual que `setting` y las exclusiones. `SQLitePrivacyCheck` no se amplía todavía a las tablas nuevas; queda para F6B-7.

## A43 — 2026-11-24 — Actividad pasiva para objetivos

Estado: aplicable. `PassiveActivity` (paquete `goals`) es una regla pura que recibe el máximo y los objetivos activos inyectados; no conoce SQLite, UI ni reloj. Cuando `PrivacyFilter` devuelve `IDLE`, `CaptureSession` reevalúa la misma muestra con inactividad cero y, si el contexto coincide con un objetivo activo `passive=true` y la inactividad no supera el máximo, sustituye la observación por `OBSERVED`; así `Segmenter` no cambia y el segmento continúa. El máximo inicial es **10 min desde la última entrada**, incluido el umbral de inactividad (no son 10 min adicionales): a los 600 s cuenta y a los 601 s vuelve a `IDLE`. `UNAVAILABLE` (bloqueo, suspensión, sensor no disponible) y `EXCLUDED` nunca se promueven; un hueco de más de 3 s corta la promoción como suspensión, y cambiar de ventana también la corta, incluso hacia otro objetivo pasivo, hasta que vuelva la actividad real. Sin objetivos pasivos el resultado es idéntico al actual.

Coincidencia de contexto: un objetivo de app (`siteHost == null`) coincide solo con una muestra sin host; un objetivo de sitio coincide solo con el proceso normalizado y el host exactos. Un objetivo de app sobre un navegador no cubre sus sitios. Se conserva `ContextKey` y la privacidad: un navegador sin host fresco sigue `EXCLUDED` y no se promueve. El máximo queda como parámetro inyectado (10 min por defecto); su persistencia y la semántica efectivo/pendiente de E034 se posponen.

Limitación conocida: `CaptureSession` lee `GoalRepository.listActive()` una sola vez al iniciar la captura; crear, editar o borrar objetivos no cambia la sesión en curso y se aplica al reiniciar. **F6B-3** debe recargar la lista al crear, editar o borrar un objetivo y persistir el máximo con efectivo/pendiente. Sin cambios de esquema, `Segmenter`, `PrivacyFilter`, UI ni `SQLitePrivacyCheck`. Evidencia E037.

## A44 — 2026-11-24 — Motor de cálculo de objetivos

Estado: aplicable. `GoalProgressCalculator` (paquete `goals`) es puro: recibe el día local, los segmentos cerrados y los objetivos activos, y devuelve minutos usados, entradas, estado y aviso de tiempo no medible. No conoce SQLite, UI, alarmas, hilos ni reloj. Solo los segmentos `OBSERVED` suman; `IDLE`, `EXCLUDED`, `UNAVAILABLE` y los huecos no suman. El recorte al día local se extrajo a `analysis.DayWindow` y lo comparte `DailyUsageService`; las pruebas E020 no se modificaron.

**Coincidencia distinta de A43.** Para límites, un objetivo de app coincide con **todo** el tiempo observado de su proceso, con o sin host; un objetivo de sitio exige proceso y host exactos. Los objetivos son límites independientes: si coexisten una app y un sitio del mismo proceso, el tiempo del sitio suma a ambos, pero dentro de cada objetivo cada instante se cuenta una sola vez (unión de intervalos). A43 sigue siendo la regla de **promoción de IDLE a OBSERVED** de la actividad pasiva: allí un objetivo de app solo coincide con muestras sin host para no promover un sitio de navegador bajo un objetivo de app y para no relajar la privacidad. Aplicar A43 a los límites haría que un objetivo de app sobre un navegador ignorase todo su tiempo con host; son propósitos distintos y no se sustituyen.

**Entradas.** La primera presencia del día cuenta como 1. Una nueva entrada exige al menos 3 s fuera del contexto; 3 s exactos cuentan y menos de 3 s continúa la misma entrada. `IDLE`/`EXCLUDED`/`UNAVAILABLE` no suman minutos, pero un hueco suyo de 3 s o más separa entradas.

**Estado.** Se evalúan todos los topes configurados: `EXCEEDED` si alguno llega al 100 %, si no `NEAR` si alguno llega al 80 %, si no `IN_PROGRESS`. La comparación es exacta sobre milisegundos y entradas, sin redondeo de minutos.

**Tiempo no medible.** Si el día contiene algún segmento `EXCLUDED`, se marca `unmeasurable` en objetivos de sitio y en objetivos de app cuyo proceso esté en la lista compartida de navegadores de `PrivacyFilter` (Chrome/Brave y el resto de navegadores ya enumerados). Otras apps no se marcan. Es conservador porque `EXCLUDED` no lleva identidad y puede incluir la propia ventana de Deriva; `IDLE` y `UNAVAILABLE` no activan el aviso.

**F6B-4.** El motor no incluye todavía el segmento abierto. Cuando llegue la alarma básica, F6B-4 debe pasar el segmento en curso **cortado en «ahora»** como un segmento más de la lista; la API no cambia. Sin persistencia, UI, alarmas ni migración en esta fase. Evidencia E038.

## A45 — 2026-11-24 — Pantalla de objetivos, recarga y máximo pasivo

Estado: aplicable. La pantalla **Objetivos** vive en la navegación lateral entre Uso diario y Ajustes y usa `objetivos.fxml` con lista y formulario lateral. `GoalsService` concentra el caso de uso sobre puertos (`GoalRepository`, `SegmentRepository`) y `Clock`/`ZoneId`; `GoalsViewModel` es puro y `GoalsController` ejecuta consultas y escrituras en un ejecutor propio con `CompletableFuture` + `Platform.runLater`. El progreso usa `GoalProgressCalculator` con segmentos ya cerrados del día; la línea informa de que el segmento actual no se suma. **F6B-4** sigue siendo responsable de pasar el segmento abierto cortado en «ahora».

Recarga en captura: `CaptureSession` consulta `PassiveGoalProvider.snapshot()` en cada tick; si cambia la revisión, llama a `PassiveActivity.updateGoals(...)`. `GoalsService` publica un `PassiveGoalSnapshot` inmutable y sube la revisión tras crear, editar, activar, desactivar o borrar. La actualización **conserva la promoción en curso** si el contexto promovido sigue siendo pasivo; si se elimina o desactiva, la corta. Editar un objetivo no relacionado no corta el contador desde la última entrada. Así no hay dependencia de la UI ni de SQLite en el hilo de captura. La consola diagnóstica conserva su lectura única y su máximo de 10 min; esta regla es solo para la captura de la UI.

Edición: el contexto de un objetivo existente no se puede cambiar; el formulario bloquea proceso/host/combo y solo edita topes, pasivo y activo. Para otro contexto se borra y se crea uno nuevo. Esto evita colisiones con los índices únicos y alertas asociadas. El borrado físico pide confirmación; desactivar es la vía normal y conserva el objetivo.

Máximo pasivo: se persiste en `setting` con la clave `passive_max_s`, predeterminado 600 s y rango 60–3600 s. La pantalla es **Ajustes**, sección «Actividad pasiva», con el patrón efectivo/pendiente de E034: «En uso ahora» frente a «Guardado, pendiente de aplicar». Una captura en curso conserva su máximo; Detener/Iniciar relee el guardado y Pausar/Reanudar conserva el de sesión. Un valor guardado inválido cae al predeterminado con aviso visible sin sobrescribirlo.

Refresco: la vista Objetivos se actualiza al abrirse y cada 30 s, y el temporizador se detiene al cambiar de vista o al cerrar la aplicación. La edición de contexto no altera segmentos ni episodios. Sin cambios de esquema, `Segmenter`, `PrivacyFilter` ni `SQLitePrivacyCheck`. Evidencia E039.

## A46 — 2026-11-24 — Alarma básica por umbral de objetivos

Estado: aplicable. `GoalAlertService` (paquete `app.usecase`) evalúa los objetivos activos con `GoalProgressCalculator` y `GoalAlertPolicy` (puro), registra en `goal_alerts` los umbrales nuevos del día y avisa por la bandeja. La evaluación corre en el hilo `deriva-capture` cada 30 s y en la primera muestra `OBSERVED`; no crea hilos. `GoalAlertMonitor` solo deja evaluar con captura `RUNNING` y observación `OBSERVED`: pausa manual, bloqueo, suspensión, `IDLE` y `EXCLUDED` no avisan. Un fallo de la evaluación (SQL, `StorageException` o cualquier `RuntimeException`) se registra en `stderr` con el tipo de excepción y se reintenta en el siguiente ciclo de 30 s sin interrumpir la captura. Un fallo al mostrar el aviso también se registra, pero no se reintenta: el umbral ya quedó registrado.

Segmento en curso: el puerto `OpenSegmentProvider` devuelve el segmento confirmado abierto, acotado por «ahora»; `CaptureSession` lo implementa con `segmenter.snapshot(now).open()`. El servicio lo añade como un segmento más a la lista que recibe el calculador, sin cambiar su API ni acoplarse al `Segmenter`. La pantalla Objetivos sigue mostrando solo segmentos cerrados; mostrar el abierto queda fuera de esta fase.

Deduplicación: una alerta se identifica por objetivo, métrica, umbral y día local; se consulta en `goal_alerts`, no en memoria, así que un reinicio el mismo día no repite avisos y un día nuevo vuelve a permitirlos. La comparación es la entera exacta de F6B-2: para minutos, `usedMillis * 100 >= limitMillis * threshold`; para entradas, `entries * 100 >= limit * threshold`. Si el 80 % y el 100 % se alcanzan en la misma evaluación (por ejemplo, tope de 4 entradas), se registran ambos y se envía **un solo** aviso con el umbral más alto. Al arrancar con topes ya superados se aplica la misma regla, sin un aviso por cada umbral. Cada métrica es un tope independiente; un objetivo con minutos y entradas puede generar un aviso por métrica.

Cambio de límite el mismo día: los umbrales registrados no se borran al editar el objetivo. Subir el tope no repite un 80 % ya registrado aunque el nuevo tope vuelva a alcanzarlo; un 100 % nuevo sí puede avisar. Bajar el tope tampoco repite los umbrales ya registrados. Es el comportamiento deliberado de «un aviso por umbral, por tope y por día»; el coste es que el aviso se refiere al tope vigente en el momento de registrarse, no al nuevo.

Registro y texto: se guarda primero `GoalAlert.draft` con momento, umbral y métrica; `outcome` y `justification` quedan nulos. Después se notifica una vez por plan con `AppTray.notifyGoal` (globo INFO, mismo mecanismo de E029). El texto es neutro y solo usa contexto, lo usado y el límite: «code.exe: 8 de 10 min. Aviso al 80 % del límite diario.» o «…: 4 de 4 entradas. Aviso al 100 % …». Sin bandeja, el aviso se registra igual y no se muestra. Sin URL, ruta, título, consulta ni clasificación de apps. Evidencia E040.