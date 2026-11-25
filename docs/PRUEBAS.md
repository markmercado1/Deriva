# Pruebas y evidencia

Una prueba planificada no es una prueba ejecutada. PENDIENTE, PASÓ, FALLÓ y NO_DISPONIBLE deben distinguirse. NO_DISPONIBLE no cierra un criterio obligatorio de producto.

## Matriz de software (todo pendiente inicialmente)

| ID | Qué demostrar | Tipo | Fase | Estado |
|---|---|---|---|---|
| ENV-01 | Java/Maven compatibles y build reproducible | Entorno + arranque JVM | F1.1 | PASÓ (E002) |
| BR-01 | Extensión real, comunicación/token/origen y permisos | Manual + integración | F1.2 | PASÓ (E004) |
| BR-02 | Ventana privada, foco, reinicio, segundo perfil, caducidad | Manual real + simulada | F1.2/F2.3 | PARCIAL: segundo perfil/privado/foco/reinicio PASARON; Ctrl+C no validado limpiamente; caducidad exacta no medida (E011) |
| BR-05 | Worker MV3 1 h con pestaña normal; huecos <3 s | Integración real | GATE-Q2 | PASÓ (E011) |
| BR-06 | Chrome permitido solo con NORMAL/fresco; host como contexto; resto excluido | Unitaria + integración | F2.4 | PASÓ (E012) |
| INT-01 | ConsoleCapture: sensor→privacidad→segmentador→SQLite, resumen y privacidad | Integración real + unitaria | F3.1b | PASÓ (E014) |
| BR-07 | Brave allowlist reducido: NORMAL/fresco, privada y sesión única | Manual + unitaria | F2.4b | PARCIAL: privada UNKNOWN/no y captura real con sitios; segundo perfil, reinicio, 1 h y colisión Chrome/Brave pendientes (E015/E016) |
| SEN-01 | Proceso/ventana/idle, fallos del sensor | Manual + unitaria | F1.3 | PARCIAL: proceso/ventana/idle reales PASARON; fallo nativo no inducido (E006) |
| TR-01 | Debounce 2/3 s, fronteras, huecos y primera muestra | Unitaria con tiempo simulado | F2.1 | PASÓ (E007) |
| TR-02 | A→B→A, hosts, subdominios y proceso | Unitaria | F2.1 | PASÓ (E007) |
| DB-01 | Migración, transacciones, solapamiento y reapertura | Integración SQLite temporal | F2.2 | PASÓ (E008) |
| DR-01 | 9/10 cambios, ventana exacta, solapamiento sin duplicados | Unitaria | F3.1 | PASÓ (E013) |
| DR-02 | 179/180 s, segmento abierto, candidato pendiente | Unitaria | F3.1 | PASÓ (E013) |
| DR-03 | Cierre parcial NULL, reinicio, medianoche, >30 min | Unitaria + integración | F3.2 | PASÓ (E017) |
| UI-01 | Tabla, errores, anotaciones, filtros y hilo FX | Unitaria de casos de uso + UI real | F4.1/F4.2 | PASÓ (E019): rango, Guardar motivo/Justificada y persistencia al reabrir; vacío/error con bases temporales. El smoke recorre los controles y el botón, no fue un clic humano |
| UI-02 | Confirmación visible de Guardar y persistencia pese al refresco | UI real + SQLite | F4.2/F5.2 | PASÓ (E028): la anotación ya se persistía; ahora muestra “Guardando…”, banner “Guardado · motivo · Justificada · HH:mm” y conserva el aviso tras refrescar |
| CAP-01 | Iniciar/detener/cerrar captura real desde UI, estado visible y snapshot sin cierre forzado | UI real + unitaria | F5.1a | PASÓ (E021): sesión real corta, stop, cierre con captura activa y reapertura; abierto incluido, pendiente no sumado y deduplicado. Sin bridge en la prueba real |
| CAP-02 | Borrar registros con confirmación, parada/join y conservación de configuración/token | UI real + integración SQLite | F5.1b | PASÓ (E022): confirmación visible, borrado con captura activa, tablas de actividad vacías y configuración/token conservados; reapertura vacía |
| CAP-03 | Fallo de almacenamiento visible y parada controlada | Integración real con trigger SQLite | F5.1b | PASÓ (E022): trigger temporal, estado de error visible, sin segmento persistido ni OPEN y reapertura controlada |
| CAP-04 | Fallo SQLite durante la escritura del bucle, fallo de bridge, exclusión/privado y error visible | Integración + UI real | F5.1c | PASÓ (E023): fallo en bucle inducido por suspensión de 4 s; puerto de bridge ocupado; sin segmento ni OPEN; mensajes sin datos sensibles |
| CAP-05 | Cierre de ventana con captura activa, flush de abierto/pendiente y sin hilo/proceso oculto | UI real + integración | F5.1d | PASÓ (E024): hilo 0, proceso Java terminado, segmento de 6 s persistido, 0 OPEN; test de pendiente sin solapamiento |
| CAP-06 | Bloqueo/desbloqueo y suspensión/reanudación reales sin contar ni duplicar | UI real + sonda | F5.1d | PASÓ con límite S0 (E024): 45 s de bloqueo y 57 s de suspensión S0 como UNAVAILABLE; sin solapamientos; S3/hibernación no disponibles |
| USE-01 | Recorte por día/zona, totales por app/host, exclusiones y sin doble conteo navegador/sitio | Unitaria + integración | F4.3 | PASÓ (E020): recorte de medianoche, sitios dentro del navegador, IDLE/EXCLUDED/UNAVAILABLE y huecos fuera del total |
| USE-02 | Dota + YouTube de fondo, alternancia, instantánea persistida/pendiente/abierta y cobertura sin extensión | Unitaria + manual real | F4.3 | PARCIAL (E020): cobertura y jerarquía app/sitio con datos sintéticos; no se probó Dota/YouTube real ni el segmento abierto |
| PR-01 | Ausencia de datos sintéticos excluidos en DB/logs/episodios | Integración | F5.1 | PASÓ (E023): matriz con host normal, privado/UNKNOWN, navegador no autorizado, app propia, idle y no disponible; sin URL, título, token ni host privado en DB/WAL/logs/salida |
| LIFE-01 | Cierre, bloqueo/reanudación, fallo SQLite y borrado coordinado | Integración + manual | F5.1 | PASÓ (E022/E023/E024): borrado, fallos SQLite/bridge, cierre sin hilo/proceso oculto, bloqueo y suspensión S0. Límite: S3/hibernación no disponibles |
| PAUSE-01 | Pausar/reanudar cierra segmento y episodio con PAUSE, sin solape ni pérdida | Unitaria + integración + UI real | F5.2a | PASÓ (E029): estados activa→pausa→activa→detenida; segmentos separados por el hueco de pausa, 0 solapes, hilo 0; tests de cierre PAUSE y persistencia UNCONFIRMED |
| TRAY-01 | Bandeja Abrir, Pausar/Reanudar y Salir; cerrar oculta; Salir sin proceso oculto | UI real (smoke) | F5.2b | PASÓ (E029): “Tray: instalada”; pausa/reanudación desde la bandeja; ventana oculta con captura activa y reabierta; Salir dejó hilos 0 y salida 0 |
| AJU-01 | Cuatro umbrales configurables, persistencia, avisos y regla de sesión | Unitaria + integración SQLite + smoke JavaFX | AJUSTES-DETECCION | PASÓ (E034): rangos, modelos con umbral <10, `setting` real, efectivo/pendiente, aviso inactividad≤estabilidad, configuración inválida sin sobrescritura y pausa/reanudación vs. detener/iniciar |
| PERF-01 | Consumo real durante sesión sostenida | Medición manual | F6.1 | PASÓ (E030): 2 h 7 min 30 s, 256 muestras cada 30 s, CPU media 0,15 % (máx. 0,40 % de 16 CPUs lógicas), working set 249–416 MB, memoria privada ≤484 MB; captura activa con Brave/bridge |
| RUN-01 | Arranque y pruebas siguiendo README en entorno documentado | Reproducción | F6.2 | PENDIENTE |

La sección 12 del plano contiene los casos detallados; la sección 14 contiene los casos de navegador. Esta tabla es un índice, no reduce sus criterios.

## Registro de ejecución

DOC-001 — 2026-11-21: entrega documental de continuidad. PASÓ. Comprobación con PowerShell: Get-ChildItem/Get-FileHash/Test-Path y conteo de filas de FASES; diez archivos copiados y verificados por hash, once referencias presentes, catorce tareas F1–F6 pendientes y hashes de ambos planos preservados. Detalles del alcance en ENTREGAS. No es evidencia de pruebas de software.

Para cada ejecución futura añadir:

```text
ID y fecha:
Revisión/commit o archivos relevantes:
Directorio y comando exacto (si aplica):
Entorno/versiones (sin información sensible):
Resultado y conteo real si lo proporciona la herramienta:
Limitaciones / pasos manuales:
Siguiente acción si falla:
```

Usar bases temporales y datos sintéticos. Evitar sleeps largos, datos reales de navegación y resultados inventados. Guardar evidencia breve aquí; logs voluminosos y salidas de build no van al historial del repositorio. No volver a ejecutar toda la batería sin cambios o dudas que lo justifiquen.



DOC-002 — 2026-11-21: PASÓ. Validación documental por PowerShell: siete archivos instalados y verificados por hash, F4.3 y USE-01/02 presentes, frases de alcance contradictorias sustituidas; hash del plano original intacto. No se ejecutaron tests de software.


## ENV-01 / E002 — 2026-11-21 — F1.1

Directorio: C:\AppTImeControl. Sin commit; fuentes Main.java, BootstrapTest.java y pom.xml.

Comandos ejecutados:

```powershell
java -version
javac -version
mvn -version
git --version
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --batch-mode --no-transfer-progress clean verify
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress clean verify
java -jar .\target\deriva-0.1.0-SNAPSHOT.jar
```

Resultado: ambos clean verify dieron BUILD SUCCESS. La repetición offline se hizo tras cambiar el separador de la salida de consola a ASCII por un problema de representación del guion largo. Última ejecución: 1 test, 0 fallos, 0 errores, 0 omitidos; BootstrapTest 0.216 s. El JAR terminó con código 0 y mostró “Deriva - base del prototipo lista. Captura deshabilitada.”

El test arranca otra JVM, comprueba terminación en 15 s, mensaje y ausencia de archivos en su directorio temporal. No demuestra aún captura, privacidad completa, HTTP, SQLite ni UI. No se hicieron esas pruebas.

Entorno: Windows 11 amd64; Temurin Java/javac 21.0.7; Maven 3.9.9; Git 2.49.0. Navegadores detectados por archivo/versiones: Chrome 157.0.8103.79, Edge 158.0.4370.33, Brave 158.1.100.58. No fueron abiertos ni probados.

Incidencia del entorno: mvn imprime “Acceso denegado.” antes de su salida incluso con -version; causa no determinada. A pesar del aviso, los builds completaron con código 0, prueba pasada y JAR ejecutable. No se modificó Maven global para silenciarlo. Si una futura ejecución falla, investigar por separado; no confundir este aviso con prueba de éxito.

Plan original SHA-256: 8F0DB9F8C1FE49585C84480B3874B76624E4C486C793318E998E2E178C9AD3F2.
Plan v2 SHA-256: 7E4EE45F9325339FEC419A3561D63E7D9E712F494BAD3D2A603390DFC76FC81F.
Sin cambios en los planos. Git local inicializado sin remoto ni commits; archivos no rastreados, target y work ignorados.


## E003 — 2026-11-22 — F1.2 implementada; prueba de navegador pendiente

Directorio C:\AppTImeControl; sin commit. Java 21.0.7, Maven 3.9.9, Node v22.16.0.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
node --test extension/tests/report.test.mjs
node --check extension/chromium/background.js
node --check extension/chromium/options.js
```

PASÓ: 11 tests Java (1 bootstrap, 4 BrowserStateStore, 6 LocalBridgeServer/parser), 0 fallos/errores/omitidos. 5 tests JS, todos pasaron. Verificación sintáctica de background/options JS y AST PowerShell de Start-BridgePilot.ps1 sin errores. No se cambió pom.xml.

HTTP usa sockets reales: ping 200, token 401, origen/Host 403, ruta 404, método 405, sesión/replay 409, cuerpo 413, límite 429; sin CORS abierto. Estado: 3 s exactos de caducidad, UNKNOWN limpia, nueva sesión invalida reportes, hosts normalizados y URLs rechazadas. JSON acotado: escapes, duplicados, enteros inválidos y trailing input. JS: host sin ruta/query, ventana/tab privado, pérdida de foco, tab inactiva, ventana incorrecta, páginas internas y subdominios. Son pruebas automáticas; no certifican comportamiento real del navegador.

Además se ejecutó com.deriva.app.BridgePilot desde el JAR en otra JVM (puerto 47829 y token aleatorio solo en entorno). Invoke-RestMethod/Invoke-WebRequest comprobaron ping, session, tab=204 y status con host sintético y captureEnabled=false. El proceso propio fue detenido al terminar. stdout solo informó arranque, stderr vacío; no se imprimió token. No quedó servidor activo de esta prueba.

Intentos reales: Chrome y Brave, perfiles nuevos bajo work/, terminaron al arrancar con código 2147483651; Edge terminó sin canal CDP. No se pudo cargar extensión ni medir Origin real o navegación privada. Se consultó inventario de ventanas, sin operar sobre las pestañas personales. No se deshabilitó sandbox ni protección del navegador para forzar ejecución. BR-01/02 reales pendientes, no aprobados. Guía reproducible: docs/PILOTO_NAVEGADOR.md.

Planos SHA-256 esperados sin cambios: original 8F0DB9F8C1FE49585C84480B3874B76624E4C486C793318E998E2E178C9AD3F2; v2 7E4EE45F9325339FEC419A3561D63E7D9E712F494BAD3D2A603390DFC76FC81F.

2026-11-22: el usuario confirmó ID de extensión cnipocakflhegoediialcdngmopbieip y aportó captura de Chrome con la extensión activada. Start-BridgePilot.ps1 -ExtensionId cnipocakflhegoediialcdngmopbieip inició el servidor; Invoke-RestMethod http://127.0.0.1:47821/v1/ping devolvió app=deriva. No se considera aún evidencia de reporte desde la extensión. Token permanece local, no incluido en resultados.

2026-11-22: conexión real confirmada. Captura del usuario: Bridge conectado; captura deshabilitada. GET /v1/status autenticado: state=UNKNOWN, sequence=49, accepted=49, captureEnabled=False. No se imprimió token ni host. Confirma reportes desde Chrome y aceptación del origen exacto; falta verificar cambios entre sitios, foco/privado, reinicio y caducidad reales. No habilitar captura.

2026-11-22: observación en vivo de 55 s solicitada al usuario con example.com. Sitio esperado observado=True; host limpiado después=True; UNKNOWN después=True; muestras=108; fallos de consulta=0. Solo indicadores guardados, sin historial ni otros dominios. Resultado negativo no prueba defecto: puede requerir repetir con coordinación. No valida todavía incógnito/reinicio/perfiles.

2026-11-22 BR-03 prueba real coordinada de cambio de pestaña (55 s): example.com reconocido=True; lichess.org reconocido después=True; UNKNOWN sin host después de ambos=True; captura siempre deshabilitada=True; muestras=108; errores HTTP=0. Solo indicadores guardados, sin tokens ni otros dominios. No valida incógnito o reinicio.

2026-11-22 BR-04 observación coordinada de incógnito, 55 s: sitio normal reconocido=False; dominio privado de prueba observado=False; normal retomado después de UNKNOWN >=5 s=False; mayor intervalo UNKNOWN sin host=0s; captura deshabilitada=True; muestras=108; errores=0. Indicadores únicamente. Intervalo privado indicado por pasos al usuario, no validado por sensor HWND; correlación temporal, no garantía universal. Confirmar que el usuario siguió la secuencia antes de dar esta prueba manual por pasada.

## E004 — 2026-11-22 — F1.2 validada en Chrome real (BR-01/BR-02 parcial)

Directorio: C:\AppTImeControl. Sin commit; no se modificó código en esta validación. Entorno: Windows 11 amd64, Java 21.0.7, Maven 3.9.9, Node 22.16.0, Chrome real con extensión `cnipocakflhegoediialcdngmopbieip` cargada en perfil de prueba. Token leído desde `work/pilot/bridge-token.txt` por el script de observación; no se imprimió en salida ni documentación.

Comando de arranque:

```powershell
powershell -NoProfile -File .\scripts\Start-BridgePilot.ps1 -ExtensionId cnipocakflhegoediialcdngmopbieip -Port 47821
```

Consultas de estado: `Invoke-RestMethod http://127.0.0.1:47821/v1/status` con `X-Deriva-Token` y `Origin: chrome-extension://cnipocakflhegoediialcdngmopbieip`, cada 1 s; la salida guardada solo incluyó estado, secuencia, presencia de host y comparaciones booleanas con example.com/example.org.

Resultados reales:

- Línea base normal, 5/5 muestras: state=NORMAL, host=example.com, seq 543-550.
- Incógnito real (ventana oscura, extensión no visible), 12/12 muestras: state=UNKNOWN, host ausente, seq 605-616. No se expuso example.org.
- Regreso a ventana normal, 11/12 muestras: state=NORMAL, host=example.com, seq 660-671; la primera muestra fue la transición UNKNOWN.
- Reinicio de Chrome: sesión nueva, seq 65-77; 6/12 muestras NORMAL example.com tras reabrir, 6 UNKNOWN durante la reapertura/navegación. `accepted` acumulado 779. Confirma reconexión y renovación de sesión.
- Foco: en la primera observación coordinada, cambiar a otra aplicación produjo UNKNOWN ~17 s (t=94-111 s) y recuperación a NORMAL al volver. La misma ejecución tuvo una fase de incógnito mal ejecutada (example.org en ventana normal), por lo que no se usó como evidencia de privacidad.

Limitaciones: segundo perfil no probado; caducidad exacta de 3 s solo cubierta por tests automáticos, no medida en navegador real; cierre ordenado con Ctrl+C no validado (el bridge se detuvo forzosamente y el script informó código -1 por la terminación); correlación HWND/perfil sigue pendiente. CaptureEnabled=false en todas las muestras; no se habilitó captura. No se guardaron hosts, tokens ni historial.

Siguiente acción: F1.3 (sensor y pipeline simulado inicial) según FASES; mantener las limitaciones para F2.3.

## E005 — 2026-11-22 — F1.3 contratos de sensor y dobles deterministas

Directorio: C:\AppTImeControl. Sin commit. Java 21.0.7, Maven 3.9.9.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
```

Resultado real: BUILD SUCCESS; 19 tests ejecutados, 0 fallos, 0 errores, 0 omitidos. Nuevos: `ActivitySampleTest` 5 tests y `FakeSensorTest` 3 tests. Los 11 tests previos de bootstrap/bridge siguen pasando.

Qué cubre:

- `ActivitySample` rechaza timestamp nulo; exige proceso no vacío, PID positivo y ventana distinta de cero cuando `available=true`; normaliza espacios del nombre; rechaza rutas y caracteres de control.
- Una muestra `available=false` no puede llevar proceso, PID, ventana ni estado idle, evitando arrastrar identidad previa.
- Una observación idle con identidad disponible es válida.
- `FakeSensor` devuelve muestras en orden, informa cuántas quedan y falla al agotarse.
- `FakeIdleDetector` devuelve duraciones en orden y luego cero; rechaza duraciones negativas o nulas.

Limitaciones: no se añadió JNA ni sensor Windows real; no se observó proceso/ventana real; no hay pipeline de tracking ni filtro de privacidad todavía. SEN-01 queda PARCIAL, no PASÓ. Los dobles viven en `src/test/java` y no se incluyen en el JAR.

Siguiente acción: implementar `sensor.windows` con JNA y probar proceso/ventana reales; después privacidad antes del detector y primera secuencia de pipeline.

## E006 — 2026-11-22 — F1.3 cerrada: sensor Windows, privacidad y secuencia

Directorio: C:\AppTImeControl. Sin commit. Java 21.0.7, Maven 3.9.9, JNA/jna-platform 5.19.1.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress test-compile
java -cp "target\classes;target\test-classes;work\m2\net\java\dev\jna\jna\5.19.1\jna-5.19.1.jar;work\m2\net\java\dev\jna\jna-platform\5.19.1\jna-platform-5.19.1.jar" com.deriva.sensor.windows.WindowsSensorProbe
```

Resultado real: BUILD SUCCESS; 26 tests, 0 fallos/errores/omitidos. Nuevos: `PrivacyFilterTest` 7 tests. Los 19 anteriores siguen pasando.

Probe real (sin imprimir rutas ni identidad sensible):

- Antes de que el usuario tocara mouse/teclado: `available=true`, `process=WindowsTerminal.exe`, `pidPositive=true`, `windowNonZero=true`, `idleMillis=63359`.
- Después de la entrada del usuario: `available=true`, `process=SearchHost.exe`, `pidPositive=true`, `windowNonZero=true`, `idleMillis=0`.
- Repetición reproducible con `WindowsSensorProbe`: `available=true`, `process=WindowsTerminal.exe`, PID/ventana válidos, `idleMillis=31`.

Qué cubre:

- `WindowsWindowSensor` usa `GetForegroundWindow`, `GetWindowThreadProcessId` y `QueryFullProcessImageName`; extrae solo el nombre del ejecutable y convierte fallos en `available=false` sin identidad.
- `WindowsIdleDetector` usa `GetLastInputInfo` y resta unsigned de ticks de 32 bits; el probe confirmó que el idle baja a 0 tras entrada real.
- `PrivacyFilter` excluye el PID propio y procesos de navegador (D4); idle >120 s produce `IDLE`; no disponible produce `UNAVAILABLE`; ninguno de esos estados lleva identidad.
- La secuencia determinista con `FakeSensor`/`FakeIdleDetector` produce `EXCLUDED, OBSERVED, IDLE, UNAVAILABLE, EXCLUDED` antes de cualquier tracking.

Limitaciones: no se indujo un fallo nativo de `OpenProcess`/`QueryFullProcessImageName`; el probe manual depende de la ventana enfocada y no es un test automático; no hay bucle de muestreo, segmentación, UI ni SQLite; los navegadores siguen excluidos y la captura de sitios deshabilitada; correlación HWND/perfil pendiente para F2.3. SEN-01 queda PARCIAL por el fallo nativo no inducido.

Siguiente acción: F2.1, segmentación completa con `ContextKey`, debounce, fronteras, huecos y tests deterministas.

## E007 — 2026-11-22 — F2.1 segmentación completa

Directorio: C:\AppTImeControl. Sin commit. Java 21.0.7, Maven 3.9.9.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
```

Resultado real: BUILD SUCCESS; 44 tests, 0 fallos/errores/omitidos. Nuevos: `ContextKeyTest` 4, `SegmenterTest` 11 y `TrackingModelTest` 3 (18 nuevos). Los 26 anteriores siguen pasando.

Qué cubre:

- `ContextKey` normaliza proceso y host, quita un `www.` inicial, conserva subdominios y distingue proceso/host.
- `Segmenter` puro con debounce de 3 s: candidato de 2 s absorbido por el contexto anterior; candidato de 3 s confirmado una sola vez; inicio en el primer tick del candidato.
- Fronteras IDLE/EXCLUDED/UNAVAILABLE inmediatas, sin debounce; candidato pendiente absorbido al entrar en la frontera.
- Regreso desde una frontera exige 3 s observados; el segmento de frontera termina en el primer tick del candidato.
- A→B→A con B confirmado produce dos cierres de segmento; mismo contexto repetido no duplica.
- Hueco mayor a 3 s cierra el segmento en el último tick válido y deja un hueco sin actividad inventada.
- Reloj hacia atrás cierra en el último tick válido, no produce duración negativa y pasa a UNAVAILABLE.
- `Segment`/`StableTick` impiden identidad en estados no observados y contexto ausente en OBSERVED.

Limitaciones: `Segmenter` es puro y no incluye el bucle de muestreo ni `TrackingService`; no hay SQLite, persistencia ni UI; las pruebas usan observaciones sintéticas y no integran sensor+privacidad+segmentador en un proceso real; los huecos quedan como huecos, no se rellenan con UNAVAILABLE; `Segment.id` es 0 hasta que F2.2 lo asigne; la captura de sitios sigue deshabilitada.

Siguiente acción: F2.2, SQLite V1 y repositorios con base temporal.

## E008 — 2026-11-22 — F2.2 SQLite y repositorios

Directorio: C:\AppTImeControl. Sin commit. Java 21.0.7, Maven 3.9.9, SQLite JDBC 3.54.1.0.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
```

Resultado real: BUILD SUCCESS; 50 tests, 0 fallos/errores/omitidos. Nuevos: `SQLiteSegmentRepositoryTest` 5 y `PipelinePersistenceIntegrationTest` 1 (6 nuevos). Los 44 anteriores siguen pasando.

Qué cubre:

- Migración V1 completa: `schema_version=1`, tablas del plano, WAL y `foreign_keys=ON`.
- `saveAll` inserta en una transacción; un trigger de prueba fuerza un fallo y se comprueba el rollback total del lote.
- `between(from,to)` usa solapamiento semiabierto `[start,end)`: excluye segmentos que solo tocan el borde.
- Segmentos no observados se guardan con centinelas `__IDLE__`, `__EXCLUDED__` y `__UNAVAILABLE__`, sin host, etiqueta ni fuente.
- Reapertura de la misma base conserva los segmentos y sus IDs.
- Integración real `FakeSensor → PrivacyFilter → Segmenter → SQLiteSegmentRepository`: 3 segmentos (OBSERVED/IDLE/OBSERVED), IDs asignados por SQLite y lectura equivalente tras reabrir.

Limitaciones: la ruta de producción `%APPDATA%\DerivaPrototype\deriva.db` todavía no se compone en `app`; el escritor serializado y la cola acotada quedan para la orquestación/F5; las tablas de drift existen en V1 pero sus repositorios llegan en F3; no hay bucle de muestreo ni UI; las pruebas usan bases temporales y datos sintéticos. La captura de sitios sigue deshabilitada.

Siguiente acción: F2.3, decisión escrita de integración del navegador según evidencia D4.

## E009 — 2026-11-22 — F2.3 navegadores excluidos (decisión)

Directorio: C:\AppTImeControl. Sin commit. No se ejecutaron pruebas nuevas de navegador; esta evidencia es una revisión de decisión sobre E004, E006, E008, D4 y Q2.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
```

Resultado real: BUILD SUCCESS; 50 tests, 0 fallos/errores/omitidos. `PrivacyFilterTest` mantiene 7 tests y amplía la lista de navegadores a Chrome, Edge, Brave, Firefox, Opera y Vivaldi.

Revisión D4:

- E004 confirma extensión real, comunicación, foco, incógnito, regreso y reinicio.
- Sigue sin resolverse Q2: no hay correlación fiable entre el reporte de la extensión y la ventana normal/HWND/perfil de Windows.
- Segundo perfil, caducidad exacta de 3 s en navegador real y cierre ordenado con Ctrl+C no están validados.
- D4 exige excluir si la atribución o privacidad es incierta, y la sección 14.3 ordena `browser_capture_enabled=false` cuando no se distingue un segundo perfil o una ventana privada del estado normal anterior.

Decisión A13: mantener todos los navegadores excluidos. No se guardan hosts ni tiempo de sitios; el núcleo sigue funcionando con procesos permitidos. No se habilitó captura ni se relajó ninguna protección. La decisión solo se reabrirá con correlación HWND/perfil fiable, segundo perfil y pruebas pendientes completas, mediante una nueva decisión escrita.

Limitaciones: no hay prueba real de segundo perfil ni de caducidad exacta; el piloto sigue siendo diagnóstico y en memoria; no hay UI que muestre la ausencia de sitios. La captura de navegadores permanece deshabilitada.

Siguiente acción: F3.1, máquina de deriva con umbral, ventana y estabilidad por tick.

## E010 — 2026-11-22 — Log de diagnóstico del bridge (GATE-Q2)

Directorio: C:\AppTImeControl. Sin commit. Java 21.0.7, Maven 3.9.9.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
```

Resultado real: BUILD SUCCESS; 52 tests, 0 fallos/errores/omitidos. Nuevos: `BridgeReportLogTest` 1 y un test más en `LocalBridgeServerTest` (7 en total).

Qué cubre:

- El log opt-in escribe cabecera `timestamp/seq/state/hostPresent` y una marca `# start` por arranque.
- Por reporte aceptado registra timestamp ISO, secuencia, estado y `yes/no` según haya host.
- El test envía `NORMAL` con `docs.google.com` y `UNKNOWN` sin host; el archivo contiene `NORMAL yes` y `UNKNOWN no`, pero **no** contiene el host.
- No se registra token, URL, título ni payload.

Uso: `Start-BridgePilot.ps1` activa `DERIVA_BRIDGE_REPORT_LOG=work/pilot/bridge-reports.log`. El usuario puede abrir ese archivo después de cada prueba sin DevTools.

Limitaciones: solo registra reportes aceptados (no rechazos); es diagnóstico temporal, no producción; el archivo está en `work/` e ignorado por Git; un fallo de escritura se informa por stderr pero no detiene el bridge.

Siguiente acción: ejecutar checklist manual y prueba de una hora del GATE-Q2; registrar resultado en E011.

## E011 — 2026-11-22 — GATE-Q2 PASS (Chrome + un perfil)

Directorio: C:\AppTImeControl. Sin commit. Java 21.0.7, Maven 3.9.9, Chrome real con extensión en un perfil.

Prueba de una hora:

```powershell
powershell -NoProfile -File .\scripts\Measure-BridgeReports.ps1 -ExtensionId cnipocakflhegoediialcdngmopbieip -DurationMinutes 60 -IntervalMilliseconds 250
```

Resultado real: 13.680 muestras; 3.741 reportes observados; `accepted` 93 → 3.834; `maxGapMs=1088`; `over3s=0`; `over5s=0`; `over10s=0`; `statusErrors=0`; `bridgeRestarts=0`; `tailGapMs=531`; `tailOver3s=false`. Resolución 250 ms. No se registraron hosts; el log solo guardó timestamp, seq, state y hostPresent.

Pruebas manuales:

- Segundo perfil: repetido y confirmado; UNKNOWN/no con B realmente enfocado y NORMAL/yes en menos de 1 s al volver a A (log 19:56:34.449).
- Ventana privada: 10 s en UNKNOWN/no, sin host privado.
- Foco: UNKNOWN al salir de Chrome y NORMAL/yes al volver.
- Reinicio: secuencia nueva y reconexión; el único hueco >3 s de la sesión de pruebas fue 6,67 s durante el reinicio (522 → 1), no en estado estable.
- Ctrl+C: el primer intento falló y se cerró con Stop-Process; el segundo detuvo el bridge. La causa del primer fallo no se investigó. No se marca como cierre ordenado validado.

Decisión A15 (corregida): PASS para Chrome + un perfil. El criterio fue frescura de reportes y UNKNOWN al perder foco, **no** correlación HWND; se acepta un límite de ~1 s. Segundo perfil repetido y confirmado. Resto de navegadores/perfiles excluido; captura todavía no activa. Limitaciones: Ctrl+C no validado limpiamente; caducidad exacta de 3 s no medida en navegador real.

Siguiente acción: F2.4 con allowlist de Chrome y propagación del host.

## E012 — 2026-11-22 — F2.4 Chrome + un perfil en el filtro

Directorio: C:\AppTImeControl. Sin commit. Java 21.0.7, Maven 3.9.9.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
```

Resultado real: BUILD SUCCESS; 59 tests, 0 fallos/errores/omitidos. Nuevos: `BrowserStateStoreTest` 5 (antes 4), `PrivacyFilterTest` 12 (antes 7) y `BrowserPrivacyIntegrationTest` 1.

Qué cubre:

- `PrivacyFilter` permite `chrome.exe` como OBSERVED con host solo si `BrowserStateProvider` devuelve un host NORMAL fresco; sin host, queda EXCLUDED.
- `BrowserStateStore.freshNormalHost()` devuelve host solo en NORMAL y antes de 3 s; STALE/UNKNOWN/privado devuelven vacío.
- Edge, Brave, Firefox, Opera y Vivaldi siguen EXCLUDED aunque el proveedor tenga host.
- Chrome idle >120 s queda IDLE sin host; un host de navegador no se adjunta a una app no-navegador.
- Integración `BrowserStateStore → PrivacyFilter`: NORMAL fresco permite `chess.com`; a los 3 s pasa a EXCLUDED; UNKNOWN queda EXCLUDED.

Limitaciones: el constructor por defecto de `PrivacyFilter` sigue excluyendo todos los navegadores; la composición de la app aún no conecta `BrowserStateStore::freshNormalHost` ni existe bucle de muestreo/UI, por lo que la captura real sigue inactiva. El límite de ~1 s de A15 sigue vigente. No se habilitaron otros navegadores ni perfiles.

Siguiente acción: F3.1, máquina de deriva.

## E013 — 2026-11-22 — F3.1 máquina de deriva

Directorio: C:\AppTImeControl. Sin commit. Java 21.0.7, Maven 3.9.9.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
```

Resultado real: BUILD SUCCESS; 77 tests, 0 fallos/errores/omitidos. Nuevos: `ChangeCounterTest` 5, `DriftStateMachineTest` 8 y `DriftModelTest` 5 (18 nuevos).

Qué cubre:

- `ChangeCounter`: solo cambios entre OBSERVED confirmados y consecutivos; pendiente/boundary no cuentan; A→B→A cuenta dos.
- 9 cambios no abren; el 10º abre un episodio con `startTs` del cambio más antiguo, `detectedAt` del tick y `firstChangeContext` del primer cambio.
- Ventana `(t-120, t]`: un cambio exactamente 120 s antiguo no cuenta.
- Episodio único: cambios posteriores actualizan el mismo id, `switchCount` y contextos.
- Estabilidad: 179 s no cierra; 180 s cierra STABLE con `stabilityStartTs`, `closedAt` y `timeToStabilityMs`; candidato pendiente bloquea.
- Fronteras IDLE/EXCLUDED/UNAVAILABLE cierran UNCONFIRMED con motivo, `lastObservedTs` del último OBSERVED y sin `timeToStabilityMs`.
- Tras STABLE, un episodio nuevo exige 10 cambios nuevos; una frontera limpia la ventana pendiente.
- `DriftEpisode` valida OPEN/STABLE/UNCONFIRMED, contexto inicial incluido y umbral mínimo.

Limitaciones: detector puro; no hay persistencia de drift (F3.2), UI ni bucle de muestreo; los motivos PAUSE/LOCK/EXIT/GAP/RESTART aún no los produce `Segmenter`; `DriftUpdate` no se guarda. Pendientes P1 (idle de navegador) y P2 (causa Ctrl+C) anotados en FASES.

Siguiente acción: revisar propuesta de integración mínima; luego F3.2.

## E014 — 2026-11-22 — F3.1b captura mínima por consola

Directorio: C:\AppTImeControl. Sin commit. Java 21.0.7, Maven 3.9.9, SQLite JDBC 3.54.1.0, JNA 5.19.1.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
```

Resultado real: BUILD SUCCESS; 88 tests, 0 fallos/errores/omitidos. Nuevos: `ConsoleSummaryTest` 4, `SQLitePrivacyCheckTest` 3, `SegmenterTest` +3 y `BrowserPrivacyIntegrationTest` +1.

Smoke real: `ConsoleCapture` durante 12 s con base separada `work/console-day/smoke.db` y stop file. Arrancó, verificó privacidad, imprimió al menos un segmento, mostró el resumen con categorías separadas y terminó sin stderr; la base se creó. No se muestran hosts en esta evidencia.

Qué cubre:

- `ConsoleSummary`: OBSERVED por app, sitios como desglose, IDLE, EXCLUDED, UNAVAILABLE y sin registro; recorte al día.
- `Segmenter.close`: vuelca el segmento abierto o frontera y absorbe candidato pendiente.
- `ConsoleCapture`: guarda cada segmento al cerrarse; imprime drift OPENED/UPDATED/CLOSED sin persistirlo; stop file alternativo probado; resumen al salir.
- `SQLitePrivacyCheck`: rechaza columnas URL/título y hosts con `/`, `?`, `#` o `:`; verifica el esquema al arrancar.
- `BrowserPrivacyIntegrationTest`: estado UNKNOWN/privado no guarda host; el segmento queda EXCLUDED con `site_host` nulo.
- `Start-ConsoleCapture.ps1` resuelve classpath Maven para SQLite/JNA; la base por defecto es `work/console-day/deriva.db`.

Limitaciones: smoke corto; no se provocó un episodio de deriva (requiere 10 cambios); sin UI; las escrituras son sincrónicas y no hay cola/escritor de producción; solo Chrome + un perfil; P1 (idle de navegador) y P2 (causa Ctrl+C, hipótesis de consola interactiva) siguen abiertos. F4.3 y F5.1 no se marcan terminadas.

Siguiente acción: F3.2 persistencia de episodios.

## E015 — 2026-11-22 — F2.4b Brave allowlist reducida

Directorio: C:\AppTImeControl. Sin commit. Java 21.0.7, Maven 3.9.9.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
```

Resultado real: BUILD SUCCESS; 90 tests, 0 fallos/errores/omitidos. Nuevos: 2 tests de `PrivacyFilter` para Brave con host fresco y sin host.

Qué cubre:

- `brave.exe` se marca OBSERVED solo si `BrowserStateProvider` devuelve un host NORMAL fresco; sin host queda EXCLUDED.
- La comparación es case-insensitive (`BRAVE.EXE`).
- Edge/Brave sin host y el resto de navegadores siguen EXCLUDED.
- Ventana privada de Brave: reporte del usuario UNKNOWN/no, sin NORMAL yes. No se marca prueba de una hora ni segundo perfil.

Pendientes del gate Brave (no PASÓ completo):

- Segundo perfil de Brave.
- Reinicio de Brave.
- Una hora de estabilidad y huecos.
- Colisión de sesiones Chrome/Brave: el payload de la extensión de Brave dice `browser:"chrome"` y `BrowserStateStore` mantiene una sola sesión; no ejecutar ambos contra el mismo bridge.

Limitaciones: mismo ID/origen de extensión; no se validó el gate completo; `ConsoleCapture` puede usarse con Brave solo si Chrome no está reportando al mismo bridge. Comando: `Start-ConsoleCapture.ps1 -ExtensionId cnipocakflhegoediialcdngmopbieip -Port 47821`, base `work/console-day/deriva.db`.

Siguiente acción: F3.2 persistencia de episodios; completar el gate Brave en paralelo cuando se pueda.

## E016 — 2026-11-22 — Captura real de Brave y corrección del resumen

Directorio: C:\AppTImeControl. Sin commit. Java 21.0.7, Maven 3.9.9.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
```

Resultado real: BUILD SUCCESS; 91 tests, 0 fallos/errores/omitidos (1 nuevo). La captura real de Brave produjo segmentos de `brave.exe@chess.com`, `youtube.com`, `windowsterminal.exe`, `winword.exe`, `explorer.exe`, y el resumen separó apps, sitios, IDLE, EXCLUDED, UNAVAILABLE y sin registro. No se incluyen hosts privados ni títulos.

Corrección: `Sin registro` mostraba `16:20:52` porque contaba desde medianoche como duración. Ahora `ConsoleSummary` mide huecos desde el primer segmento de la sesión. Test nuevo: `uncoveredStartsAtFirstSegmentNotMidnight`.

No hubo episodio de deriva: la mayoría de los cambios observados duraron <3 s y fueron absorbidos por el debounce; solo cuentan los contextos confirmados. No es un fallo.

Limitaciones: no valida segundo perfil, reinicio, una hora ni colisión Chrome/Brave; no hay UI; P1/P2 abiertos. F4.3/F5.1 no terminadas.

Siguiente acción: F3.2 persistencia de episodios.

## E017 — 2026-11-22 — F3.2 persistencia de episodios

Directorio: C:\AppTImeControl. Sin commit. Java 21.0.7, Maven 3.9.9.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
```

Resultado real: BUILD SUCCESS; 97 tests, 0 fallos/errores/omitidos. Nuevos: `SQLiteDriftRepositoryTest` 5 y `DriftStateMachineTest` +1 (`close`).

Qué cubre:

- `upsertDetection` inserta OPENED, actualiza UPDATED/CLOSED y escribe contextos; el id de sesión se mapea al id SQLite.
- Anotaciones `reason/justified/answered_at` se preservan al actualizar detección y al recuperar.
- `between` devuelve episodios por solapamiento, con contextos y anotaciones.
- `recoverOpenEpisodes` convierte OPEN en UNCONFIRMED/RESTART con `closed_at=last_observed_ts`, sin tiempo de estabilidad.
- `close(EXIT)` cierra un episodio abierto al salir; `ConsoleCapture` persiste OPEN/UPDATED/CLOSED y el cierre.
- Episodio >30 min y cruce de medianoche sin truncar; anotación inválida/id desconocido rechazados.

Limitaciones: sin UI/borrado/retención; no se provocó un drift real en el smoke de consola; los ids en memoria se mapean a ids SQLite por sesión; P1/P2 y gate Brave completo siguen abiertos. F4.3/F5.1 no terminadas.

Siguiente acción: F4.1, previa petición de referencias de diseño (A11).

## E018 — 2026-11-22 — F4.1 Interrupciones (JavaFX)

Directorio: C:\AppTImeControl. Sin commit. Java 21.0.7, Maven 3.9.9, JavaFX 21.0.13.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
```

Resultado real: BUILD SUCCESS; 102 tests, 0 fallos/errores/omitidos. Nuevos: `InterruptionsViewModelTest` con 5 casos (orden y conteo, filtros, formato de fila, anotación y fallbacks).

Captura real de la interfaz ejecutándose:

```powershell
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Demo -Screenshot C:\AppTImeControl\work\ui-preview\interrupciones.png
```

Resultado real: ventana JavaFX iniciada en modo demo, captura escrita en `work/ui-preview/interrupciones.png` (1440×900). La demo inserta 7 episodios sintéticos y 1 abierto. La captura muestra tabla + panel de detalle, filtros, navegación lateral y estados con forma y texto (ámbar abierta, salvia estable, gris hueco sin confirmar). JavaFX imprimió `Unsupported JavaFX configuration: classes were loaded from unnamed module`, aviso esperado al ejecutar desde classpath; se usa `Launcher` para evitar el error de módulos.

Qué cubre:

- Tabla ordenada por inicio descendente con inicio, estado, primer cambio, número de cambios y tiempo hasta estabilidad.
- Panel de detalle a la derecha; contextos como chips; motivo/Justificada; Guardar deshabilitado sin motivo.
- Filtros de fecha, estado e incluir justificadas; consultas y anotación en un ejecutor fuera del hilo JavaFX, con descarte de respuestas obsoletas.
- Estados vacío y error visibles; nunca se sustituye un error por una tabla vacía.
- Navegación lateral con Uso diario y Ajustes deshabilitados; no se crearon pantallas anticipadas.
- Paleta, tipografía y densidad de `docs/DISENO.md`; solo tema claro.

Limitaciones: la captura usa datos sintéticos de demo, no una sesión real; no se pulsó Guardar manualmente ni se forzó un error/vacío real (el backend de anotación sí está probado); no hay bucle de captura/orquestación de producto; no se implementaron Uso diario, Ajustes, borrado ni panel de conexión. F4.3/F5.1 no están terminadas.

Siguiente acción: F4.2/F4.3 según decisión del usuario, manteniendo el gate completo de Brave y P1/P2 pendientes.

## E019 — 2026-11-22 — F4.2 rango y comprobaciones de UI

Directorio: C:\AppTImeControl. Sin commit. Java 21.0.7, Maven 3.9.9, JavaFX 21.0.13.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
```

Resultado real: BUILD SUCCESS; 106 tests, 0 fallos/errores/omitidos (4 nuevos). Nuevos: 2 en `InterruptionsViewModelTest` (rango inclusivo y rango invertido), 1 en `InterruptionsServiceTest` (cruce de medianoche y sin duplicados) y 1 en `SQLiteDriftRepositoryTest` (anotación tras reabrir el repositorio).

### Rango por solapamiento

El rango `Desde`/`Hasta` es inclusivo por día local y consulta con `start_ts < fin_exclusivo` y `COALESCE(closed_at,last_observed_ts) > inicio`. La apertura/migración de la base, la consulta y la anotación corren fuera del hilo FX; la UI se construye cuando la base está lista. Un episodio que empieza a las 23:50 y cierra a las 00:05 aparece si se consulta el día anterior, el día siguiente o ambos; en un rango amplio aparece una sola fila. Verificado en `InterruptionsServiceTest.crossMidnightEpisodeAppearsInBothDaysAndOnlyOnceInWideRange`. La tabla muestra `dd/MM HH:mm` para que el cruce de medianoche no sea ambiguo.

### Guardar desde la interfaz y persistencia

Se usó una base temporal con datos sintéticos. El smoke recorre la tabla, selecciona el motivo y Justificada en los controles reales y dispara el botón Guardar; no llama directamente al repositorio. **No fue un clic humano.**

```powershell
$dir = 'C:\AppTImeControl\work\ui-preview\f4-2'
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Demo -Database "$dir\persist.db" -Smoke annotate -Screenshot "$dir\guardado.png"
```

Salida real:

```text
UI smoke: episodio=7, motivo=MESSAGES, justificada=true, guardarDeshabilitado=false
UI smoke: estado tras Guardar=Guardado
```

Después se cerró y se volvió a abrir la misma base, sin reseed:

```powershell
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Demo -Database "$dir\persist.db" -Screenshot "$dir\reabierto.png"
```

La captura `reabierto.png` muestra Motivo **Mensajes** y **Justificada** marcada. Consulta directa:

```powershell
sqlite3 "C:\AppTImeControl\work\ui-preview\f4-2\persist.db" "SELECT id, reason, justified, answered_at IS NOT NULL FROM drift_event WHERE id=7;"
```

Salida real:

```text
7|MESSAGES|1|1
```

### Estados vacío y error

Vacío, con base temporal nueva y sin datos:

```powershell
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database "$dir\empty.db" -Screenshot "$dir\vacio.png"
```

Resultado: 0 episodios, 0 en curso, placeholder “Sin datos registrados para este rango de fechas” y detalle sin selección. `SELECT COUNT(*) FROM drift_event` devolvió `0`.

Error, con una base temporal y un esquema deliberadamente incompleto (no se tocó la base real):

```powershell
sqlite3 "$dir\bad.db" "CREATE TABLE schema_version(version INTEGER NOT NULL); INSERT INTO schema_version VALUES(1); CREATE TABLE drift_event(id INTEGER PRIMARY KEY, start_ts INTEGER NOT NULL);"
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database "$dir\bad.db" -Screenshot "$dir\error.png"
```

Resultado: la ventana no se cerró; mostró “No se pudieron cargar las interrupciones.”, totales `—` y “Error al cargar”. La base real `work/console-day/deriva.db` conservó su hash SHA-256 `0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475`.

Rango invertido, con `Desde` posterior a `Hasta`:

```powershell
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Demo -Database "$dir\final-demo.db" -Smoke invalid-range -Screenshot "$dir\rango-invalido.png"
```

Salida real: `UI smoke: aviso de rango=Desde no puede ser posterior a Hasta.` La captura muestra la tabla vacía con “El rango no es válido” y el aviso junto a los filtros. El servicio rechaza el rango invertido en un test unitario; la ruta de UI lo evita antes de pedir datos.

### Evidencia visual

- `work/ui-preview/f4-2/rango.png`: 8 episodios, 1 abierto; incluye el episodio `21/11 23:50` que cruza medianoche.
- `work/ui-preview/f4-2/guardado.png`: motivo Mensajes, Justificada y “Guardado”.
- `work/ui-preview/f4-2/reabierto.png`: misma anotación tras cerrar y reabrir.
- `work/ui-preview/f4-2/vacio.png`: estado vacío.
- `work/ui-preview/f4-2/error.png`: estado de error.
- `work/ui-preview/f4-2/rango-invalido.png`: aviso de rango invertido.

Limitaciones: las capturas usan datos sintéticos; el smoke de UI es automatizado, no un clic humano; no se probó una sesión de captura real; el error se indujo con un esquema inválido en una base temporal; F4.3, Ajustes, borrado, panel de conexión, bucle de captura real, gate completo de Brave, P1 y P2 siguen pendientes. F4.3/F5.1 no están terminadas.

Siguiente acción: detenerse antes de F4.3 según la instrucción del usuario.

## E020 — 2026-11-22 — F4.3 Uso diario

Directorio: C:\AppTImeControl. Sin commit. Java 21.0.7, Maven 3.9.9, JavaFX 21.0.13.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
```

Resultado real: BUILD SUCCESS; 110 tests, 0 fallos/errores/omitidos (4 nuevos). Nuevos: 2 en `DailyUsageServiceTest` (recorte/cruce de medianoche/jerarquía y día pasado) y 2 en `DailyUsageViewModelTest` (sin datos frente a cero, orden/formato).

### Cálculo y límites

`DailyUsageService` usa `SegmentRepository.between` y recorta cada segmento a `[inicio del día local, día siguiente)`. Solo suma `OBSERVED` al total de primer plano y a las listas. Los sitios se agregan por `siteHost` y son un subconjunto del tiempo de su navegador; no se suman al total de aplicaciones. `IDLE`, `EXCLUDED`, `UNAVAILABLE` y los huecos no entran en el uso; la vista muestra cobertura separada. “Sin datos” significa que no hay segmentos; “0 s” significa que hay segmentos pero ningún primer plano observado.

La base de demo tiene 220 min OBSERVED en bruto, de los cuales 30 min cruzan medianoche; al recortar hoy quedan 210 min (3 h 30 min). Los 40 min no observados (15 IDLE + 10 EXCLUDED + 15 UNAVAILABLE) solo cuentan para cobertura. La captura muestra Chrome 2 h 10 min y sitios YouTube 1 h 5 min + Instagram 50 min dentro de ese navegador. El segmento abierto no está persistido: `ConsoleCapture` guarda solo segmentos completados; la UI lo indica y no se presenta como datos en vivo.

### Captura principal

```powershell
$dir = 'C:\AppTImeControl\work\ui-preview\f4-3'
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Demo -Database "$dir\demo.db" -View usage -Screenshot "$dir\uso-diario.png"
```

Resultado real: ventana JavaFX abierta en Uso diario, total **3 h 30 min**, cobertura **4 h 10 min de 19 h 24 min**, aplicaciones Chrome/VS Code/Terminal/Word y sitios YouTube/Instagram. La etiqueta de sitios incluidos y la nota de datos persistidos/segmento abierto están visibles.

### Sin datos, cero uso y error

```powershell
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database "$dir\empty.db" -View usage -Screenshot "$dir\sin-datos.png"
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database "$dir\zero.db" -View usage -Screenshot "$dir\cero-uso.png"
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database "$dir\bad-usage.db" -View usage -Screenshot "$dir\error-uso.png"
```

- `sin-datos.png`: “Sin datos”, “Sin registro para esta fecha” y listas con “Sin datos para esta fecha”.
- `cero-uso.png`: “0 s”, cobertura con 30 min de IDLE y “Sin uso en primer plano”; las listas no muestran filas.
- `error-uso.png`: totales `—`, mensaje “No se pudieron cargar los totales” y listas “No disponible por error de carga”.

La base real `work/console-day/deriva.db` conservó su hash SHA-256 `0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475`.

### Regresión de Interrupciones

```powershell
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Demo -Database "$dir\regression.db" -Smoke annotate -Screenshot "$dir\regresion-interrupciones.png"
```

Salida real: `UI smoke: estado tras Guardar=Guardado`; la navegación lateral muestra Interrupciones y Uso diario activos y Ajustes deshabilitado.

Limitaciones: datos sintéticos; no se ejecutó una sesión real de captura ni Dota/YouTube en segundo plano; el segmento abierto no se incluye; USE-02 queda PARCIAL; Brave, P1 y P2 siguen pendientes; F5 no se inicia.

Siguiente acción: detenerse antes de F5.

## E021 — 2026-11-22 — F5.1a captura real desde la UI

Directorio: C:\AppTImeControl. Sin commit. Java 21.0.7, Maven 3.9.9, JavaFX 21.0.13.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
```

Resultado real: BUILD SUCCESS; 114 tests, 0 fallos/errores/omitidos. Nuevos: `Segmenter.snapshot` (abierto/pendiente sin mutar), `DailyUsageService` (abierto incluido, pendiente no y deduplicación), `DailyUsageViewModel` (estado de instantánea) y `PrivacyFilter` (conjunto Chrome-only excluye Brave con host fresco).

### Sesión real corta y reapertura

```powershell
$dir = 'C:\AppTImeControl\work\ui-preview\f5-1'
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database "$dir\real.db" -Smoke capture -Screenshot "$dir\captura.png"
```

Salida real de la primera ejecución:

```text
UI smoke: captura solicitada
UI smoke: estado captura=Captura activa · sin bridge; navegadores excluidos.
UI smoke: uso durante captura=Instantánea 19:45:41 · abierto: Word.
UI smoke: estado tras detener=Captura detenida.
```

La captura en vivo (`captura-live.png` de una repetición) muestra el panel de captura, la instantánea y el estado abierto. La ejecución inicial observó Word como contexto abierto y lo incluyó provisionalmente. Después se cerró y reabrió la misma base:

```powershell
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database "$dir\real.db" -View usage -Screenshot "$dir\reabierto.png"
```

`reabierto.png` muestra **Word — 9 s** como dato persistido, con la captura detenida. Consulta real:

```powershell
sqlite3 "$dir\real.db" "SELECT kind, process_name, site_host, (end_ts-start_ts)/1000 FROM segment ORDER BY start_ts;"
```

Salida: `EXCLUDED|__EXCLUDED__||3` y `OBSERVED|winword.exe||9`; `SELECT COUNT(*) FROM drift_event WHERE status='OPEN'` devolvió `0`.

### Cierre con captura activa

```powershell
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database "$dir\close.db" -Smoke capture-close -Screenshot "$dir\cierre.png"
```

Salida real: `UI smoke: cierre con captura=Captura activa · sin bridge; navegadores excluidos.` El proceso terminó por la ruta de cierre de la ventana. La base quedó con un segmento `EXCLUDED` de 6 s y ningún episodio OPEN; el hilo de captura se detuvo y persistió el cierre antes de salir.

### Snapshot y no duplicación

- `Segmenter.snapshot` expone `open` y `pending` sin cerrar ni mutar el estado.
- `DailyUsageService` suma el abierto confirmado al uso y a la cobertura, pero no suma el pendiente; si el abierto se solapa con un segmento persistido, lo omite para no duplicar.
- `DailyUsageViewModel` muestra “abierto: …” y “pendiente: … (no suma hasta confirmarse)” mientras la captura está activa. La vista solo usa el snapshot si la fecha consultada es hoy; en fechas pasadas muestra datos persistidos.
- El refresco de Uso diario es cada 2 s mientras la vista está visible; no se llama a `Segmenter.close` para actualizar.

### Privacidad y alcance

- `CaptureSession` usa Chrome + un perfil solo si hay bridge configurado por `DERIVA_BRIDGE_TOKEN`, `DERIVA_EXTENSION_ID` y `DERIVA_BRIDGE_PORT`.
- Sin bridge, los navegadores quedan excluidos. Brave queda excluido incluso con host fresco; el test `PrivacyFilterTest.restrictedBrowserSetKeepsBraveExcludedWithFreshHost` lo comprueba.
- P1: el umbral de idle de 120 s se mantiene; puede subestimar vídeo/ajedrez pasivo. P2: la UI usa parada explícita y `join`; el Ctrl+C de la consola sigue sin validarse limpiamente.
- No se añadió pausa/bandeja, Ajustes completo, borrado ni F5.2. La base real `work/console-day/deriva.db` conservó su SHA-256 `0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475`.

Limitaciones: sesión de 10 s; sin bridge en la prueba real, por lo que Chrome/Brave no se capturaron; el abierto observado se vio en la primera ejecución y el abierto de exclusión en una repetición; el pendiente solo se verificó en test de UI/ViewModel, no en una sesión real; no se probaron borrado, fallo SQLite, bloqueo ni matriz de privacidad. F5.1 sigue EN_CURSO.

Siguiente acción: completar las comprobaciones restantes de F5.1, sin iniciar F5.2 ni Ajustes.

## E022 — 2026-11-22 — F5.1b borrado coordinado y fallo SQLite

Directorio: C:\AppTImeControl. Sin commit. Java 21.0.7, Maven 3.9.9, JavaFX 21.0.13.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
```

Resultado real: BUILD SUCCESS; 115 tests, 0 fallos/errores/omitidos. Nuevo: `SQLiteDataMaintenanceTest` (borra segmentos, episodios y contextos; conserva `setting`, excluidos y `schema_version`).

### Borrado con confirmación y captura activa

```powershell
$dir = 'C:\AppTImeControl\work\ui-preview\f5-1'
Copy-Item "$dir\real.db" "$dir\delete-test.db"
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database "$dir\delete-test.db" -Smoke delete -Screenshot "$dir\delete.png"
```

Salida real:

```text
UI smoke: confirmación=Se borrarán segmentos, episodios y anotaciones. Se conservan la configuración y el token. La captura se detendrá para coordinar el borrado.
UI smoke: estado tras borrar=Registros borrados. Captura detenida.
```

La captura se inició antes de borrar. El flujo hizo parada + `join`, borró las tablas de actividad y dejó la captura detenida; Uso diario mostró “Sin datos”. Consulta real:

```powershell
sqlite3 "$dir\delete-test.db" "SELECT (SELECT COUNT(*) FROM segment), (SELECT COUNT(*) FROM drift_event), (SELECT COUNT(*) FROM drift_context), (SELECT COUNT(*) FROM drift_event WHERE status='OPEN');"
```

Salida: `0|0|0|0`. La reapertura con `-View usage` mostró “Sin datos” (`delete-reopen.png`). La captura de confirmación `confirmacion.png` se obtuvo con `-Smoke delete-confirm` sobre una copia; no se confirmó el borrado en esa ejecución y la copia conservó sus 2 segmentos.

### Fallo SQLite controlado

Se creó una base temporal con el esquema y un trigger que aborta los INSERT en `segment`:

```powershell
Copy-Item "$dir\..\f4-3\empty.db" "$dir\failure.db"
sqlite3 "$dir\failure.db" "CREATE TRIGGER fail_segment_insert BEFORE INSERT ON segment BEGIN SELECT RAISE(ABORT, 'simulated storage failure'); END;"
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database "$dir\failure.db" -Smoke capture-failure -Screenshot "$dir\failure.png"
```

Salida real:

```text
UI smoke: estado antes de detener=Captura activa · sin bridge; navegadores excluidos.
UI smoke: estado tras fallo=No se pudo cerrar la captura: Could not save segments
```

La UI mostró el error y el botón “Reintentar captura”; la captura quedó detenida. La consulta devolvió `0|0|0` para segmentos, episodios y OPEN; la reapertura (`failure-reopen.png`) mostró “Sin datos”. No hay cola de escritura: el fallo termina el bucle y no se acumulan segmentos en memoria.

### USE-02 y segmento abierto

El **segmento abierto ya está cubierto**: E021 observó Word real como abierto, Uso diario lo incluyó provisionalmente y luego lo persistió al detener. Lo que queda de USE-02 no es el abierto: falta validar en real el candidato pendiente, Dota/YouTube en segundo plano, la alternancia y la cobertura sin extensión. El pendiente se probó aquí solo en tests de `Segmenter`/ViewModel, no en una sesión real.

### Límites

- La confirmación y el borrado se ejercitaron con smokes de UI, no con clics humanos.
- Sin bridge en estas pruebas: Chrome/Brave no se capturaron; Brave permanece excluido.
- P1 (idle 120 s) y P2 (Ctrl+C de consola) siguen pendientes; la UI no usa Ctrl+C.
- No se probaron bloqueo/desbloqueo, cierre sin proceso oculto, matriz completa de privacidad ni F5.2.
- Base real intacta: SHA-256 `0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475`; planos intactos.

F5.1 sigue EN_CURSO.

Siguiente acción: completar la matriz de fallos/privacidad, cierre sin proceso oculto y bloqueo/reapertura; no iniciar F5.2 ni Ajustes.

## E023 — 2026-11-22 — F5.1c matriz de fallos y privacidad

Directorio: C:\AppTImeControl. Sin commit. Java 21.0.7, Maven 3.9.9, JavaFX 21.0.13.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
```

Resultado real: BUILD SUCCESS; 117 tests, 0 fallos/errores/omitidos. Nuevos: `LocalBridgeServerTest.occupiedPortFailsInsteadOfSilentlySkippingTheBridge` y `BrowserPrivacyIntegrationTest.persistedPrivacyMatrixContainsNoSensitiveMarkers`.

### Fallo SQLite durante la escritura del bucle

Se copió una base temporal vacía, se añadió un trigger que aborta los INSERT en `segment` y se suspendió el proceso Java de la UI durante 4 s. Al reanudar, el hueco >3 s cierra el segmento abierto y el siguiente `saveAll` del bucle falla. El proceso Java se suspendió con `NtSuspendProcess`/`NtResumeProcess`; no se tocó la sesión ni la base real.

```powershell
$dir = 'C:\AppTImeControl\work\ui-preview\f5-1'
Copy-Item "$dir\..\f4-3\empty.db" "$dir\loop-failure.db" -Force
sqlite3 "$dir\loop-failure.db" "CREATE TRIGGER fail_segment_insert BEFORE INSERT ON segment BEGIN SELECT RAISE(ABORT, 'simulated loop write failure'); END;"
# La UI se abrió con -Smoke capture; el script suspendió el proceso java 4 s y lo reanudó.
powershell -NoProfile -ExecutionPolicy Bypass -File .\work\ui-preview\f5-1\run-loop-failure.ps1
```

Salida real:

```text
UI smoke: captura solicitada
UI smoke: estado captura=La captura se detuvo por un error: no se pudo guardar en la base local. · el cierre también falló.
UI smoke: uso durante captura=Sin datos para esta fecha. No hay segmentos guardados.
UI smoke: estado tras detener=La captura se detuvo por un error: no se pudo guardar en la base local. · el cierre también falló.
```

El error aparece **antes** de que el smoke pulse Detener, por lo que corresponde al bucle y no al cierre. La consulta devolvió `0|0|0` para segmentos, episodios y OPEN. La captura quedó detenida; la UI muestra “Reintentar captura”. Capturas `work/ui-preview/f5-1/loop-failure.png` y `loop-failure-live.png`.

### Fallo de bridge

Se ocupó el puerto 47831 con un `TcpListener` y se arrancó la UI con `DERIVA_BRIDGE_TOKEN`, `DERIVA_EXTENSION_ID` y `DERIVA_BRIDGE_PORT` válidos. El fallo es de enlace, no de token; el token no se imprime ni se persiste.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\work\ui-preview\f5-1\run-bridge-failure.ps1
```

Salida real:

```text
UI smoke: estado antes de detener=La captura se detuvo por un error: no se pudo abrir el bridge; el puerto está ocupado.
UI smoke: estado tras fallo=La captura se detuvo por un error: no se pudo abrir el bridge; el puerto está ocupado.
token in output: False
token in db: False
0|0|0
```

La reapertura con `-View usage` mostró “Sin datos” (`bridge-failure-reopen.png`). El mensaje visible es genérico y en español; no incluye puerto, token, host ni detalle de la excepción.

### Matriz de privacidad

`BrowserPrivacyIntegrationTest` recorre, en una sola base temporal: Chrome autorizado con host 
ormal.example`, Chrome con reporte UNKNOWN, host privado aceptado y luego caducado, Firefox no autorizado con host fresco, proceso propio, Chrome idle y muestra no disponible. Persiste cuatro segmentos (OBSERVED, EXCLUDED, IDLE, UNAVAILABLE), verifica `SQLitePrivacyCheck` y escanea los bytes de `matrix.db`, `-wal` y `-shm`: aparece 
ormal.example`; no aparecen `private-marker.example`, `https://`, `title=` ni un token sintético de 64 caracteres. Los segmentos no observados no llevan contexto.

También se escanearon las bases y salidas reales de E023 (`loop-failure.db`, `bridge-failure.db`, `.out.txt`, `.err.txt`) con codificación Latin-1: `Url=False`, `Title=False`, `PrivateHost=False`, `BridgeEnv=False` en todos. El log de diagnóstico del bridge ya estaba cubierto por `BridgeReportLogTest` (E010): escribe timestamp, seq, state y hostPresent, nunca el host.

### Cambios de código y límites

- `CaptureSession` conserva el primer fallo del bucle y añade “el cierre también falló” si la finalización falla; `BindException` y `StorageException` se traducen a mensajes genéricos en español. Las excepciones desconocidas muestran solo el nombre de la clase, sin mensaje.
- `UiSmoke.captureCycle` ya no dispara el botón si muestra “Reintentar captura”; antes, un error durante la captura reiniciaba la captura al intentar detenerla en el smoke. No cambia la acción humana de reintentar. Regresión del camino normal sobre base temporal: `-Smoke capture` terminó con “estado tras detener=Captura detenida”, un segmento OBSERVED y otro EXCLUDED persistidos y 0 OPEN.
- No se indujo un fallo nativo del sensor de ventana; SEN-01 sigue PARCIAL. El fallo de idle ya se convierte en UNAVAILABLE y está cubierto por `PrivacyFilterTest`.
- Los smokes son rutas de controles, no clics humanos; la suspensión de 4 s simula un hueco de reanudación, no un bloqueo de Windows real.
- Base real intacta: SHA-256 `0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475`. Planos intactos.

F5.1 sigue EN_CURSO: faltan cierre sin proceso oculto y bloqueo/reanudación; P1/P2, Brave y USE-02 siguen pendientes.

Siguiente acción: comprobar el cierre sin proceso/hilo de captura vivo y el bloqueo/reanudación; no iniciar F5.2 ni Ajustes.

## E024 — 2026-11-22 — F5.1d cierre completo y bloqueo/reanudación

Directorio: C:\AppTImeControl. Sin commit. Java 21.0.7, Maven 3.9.9, JavaFX 21.0.13.

```powershell
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify
```

Resultado real: BUILD SUCCESS; 121 tests, 0 fallos/errores/omitidos. Nuevos: `SegmenterTest.closeFlushesPendingCandidateWithoutOpenSegment`, `PipelinePersistenceIntegrationTest.closeFlushesPendingCandidateAndReopenKeepsBothWithoutOverlap`, `PipelinePersistenceIntegrationTest.suspensionGapIsExcludedFromTotalsWithoutDuplicates` y `PrivacyFilterTest.windowsLockSurfaceIsUnavailableNotObserved`.

### Cierre de ventana con captura activa

```powershell
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f5-1d\shutdown-final.db -Smoke shutdown-close -Screenshot .\work\ui-preview\f5-1d\shutdown-final.png
```

Salida real:

```text
UI smoke: captura solicitada
UI smoke: cierre de ventana con captura=Captura activa · sin bridge; navegadores excluidos.
UI screenshot: C:\AppTImeControl\work\ui-preview\f5-1d\shutdown-final.png
UI shutdown: hilos deriva-capture vivos=0
```

El proceso terminó con código 0 en 11,1 s totales (incluye Maven y arranque). La base quedó con un segmento `OBSERVED` de 6 s, 0 OPEN y `Get-Process java` sin procesos después del cierre. `CaptureSession.shutdown` ahora marca `ERROR` si el hilo sigue vivo tras el `join`; el smoke reporta el número de hilos `deriva-capture` vivos.

### Pendiente al cerrar

`Segmenter.close` ahora cierra el abierto en el inicio del candidato y persiste el pendiente como `OBSERVED` hasta el cierre, sin solapamiento. Los tests cubren un pendiente sin abierto y un pendiente tras un abierto: dos segmentos contiguos, 6 s en total, persistidos y recuperados tras reabrir. En el cierre real se persistió el último segmento de la sesión; la captura de `shutdown-final.png` muestra el estado activo antes de cerrar.

### Bloqueo/desbloqueo real

Primera pasada real: el sensor observó `LockApp.exe` durante 4 s y se contó como uso. Se corrigió `PrivacyFilter` para que `lockapp.exe` y `logonui.exe` sean `UNAVAILABLE` antes de idle. Segunda pasada real (60 s de captura, bloqueo ~45 s):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\work\ui-preview\f5-1d\run-session-scenario.ps1 -Scenario lock -HoldSeconds 60 -ProbeSeconds 80
```

Resumen real de la base `lock-real.db`:

```text
EXCLUDED|__EXCLUDED__|8 s|2 segmentos
OBSERVED|windowsterminal.exe|4 s|1 segmento
UNAVAILABLE|__UNAVAILABLE__|45 s|1 segmento
span=57 s, suma=57 s, overlaps=0, gaps=0, OPEN=0
```

La sonda `lock-probe.log` mostró `LockApp`/`inputDesktopOpen=0` y ausencia de ventana en primer plano durante el bloqueo. No aparece `lockapp.exe` en `segment`; los 45 s quedan como `UNAVAILABLE`. Al reabrir, Uso diario mostró 4 s de primer plano y 57 s registrados (`lock-usage.png`).

### Suspensión/reanudación real S0

La máquina solo ofrece suspensión S0 Modern Standby (`powercfg /a`; S1/S2/S3/hibernación no disponibles). Se ejecutó una captura de 90 s y el usuario suspendió/reanudó durante el intervalo:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\work\ui-preview\f5-1d\run-session-scenario.ps1 -Scenario sleep -HoldSeconds 90 -ProbeSeconds 110
```

Resumen real de `sleep-real.db`:

```text
EXCLUDED|__EXCLUDED__|11 s|2 segmentos
OBSERVED|windowsterminal.exe|19 s|1 segmento
UNAVAILABLE|__UNAVAILABLE__|57 s|1 segmento
span=87 s, suma=87 s, overlaps=0, gaps=0, OPEN=0
```

La sonda `sleep-probe.log` muestra el menú Inicio antes del intervalo, `inputOpen=0`/sin ventana en primer plano durante la suspensión y la reanudación posterior en `java`/WindowsTerminal. El registro de Windows confirma el modo de espera moderno: eventos Kernel-Power 506 (entrada, 20:57:01, motivo `Screen Off Request`) y 507 (salida, 20:57:46, motivo `Input Keyboard`) en `kernel-power.txt`. El intervalo no se contó como primer plano: al reabrir, Uso diario mostró 19 s de uso y 1 min 27 s registrados (`sleep-usage.png`). No hubo pérdida ni duplicado: la unión de segmentos cubre 87 s sin solapes ni huecos.

### Pruebas automáticas de suspensión y pendiente

- `SegmenterTest` y `PipelinePersistenceIntegrationTest` verifican que un gap de 7 s entre dos tramos deja un hueco no contado: 6 s observados, 6 s registrados y el resto como no registrado.
- El cierre con pendiente se prueba con reloj simulado y reapertura SQLite; no depende de un clic humano.

### Límites

- El cierre y las sesiones se ejercitaron con smokes de UI y una acción humana de bloqueo/suspensión; no hubo clic humano en los botones.
- S0 no congela necesariamente los procesos: el intervalo se marcó UNAVAILABLE, no un hueco de reloj. El hueco por congelación se cubrió con simulación (E023) y tests de gap; S3/hibernación no están disponibles en este equipo.
- No se indujo un fallo nativo del sensor; SEN-01 sigue PARCIAL.
- P1 (idle 120 s), P2 (Ctrl+C de consola), Brave y USE-02 siguen abiertos fuera de F5.1.
- Base real intacta: SHA-256 `0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475`. Planos intactos.

F5.1 queda HECHO con los límites anteriores; no se inicia F5.2 ni Ajustes.

## E025 — 2026-11-22 — F6.1 preparación (sin prueba sostenida)

- Suite ejecutada una vez: `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify` → **121 tests Java, 0 fallos, 0 errores, 0 omitidos**; 
ode --test .\extension\tests\report.test.mjs` → **5 tests, 0 fallos**.
- Medidor creado: `scripts/Measure-UiResources.ps1`. Identifica el proceso `java.exe` con `com.deriva.app.Launcher`; guarda por fila `Timestamp, ElapsedSeconds, Pid, LogicalCpus, CpuPercent, WorkingSetMB, PrivateMB, ThreadCount, HandleCount`. No guarda títulos, hosts, URLs, rutas de navegador ni línea de comandos; no abre la base.
- Validación de mecánica: proceso de prueba de 20 s, intervalo 1 s y `-DurationMinutes 0.05` → 3 muestras y fin por duración. La primera fila es línea base con CPU vacía.
- Validación contra la UI real: `Start-Ui.ps1 -Database work\f6\smoke-ui.db -Smoke shutdown-close` con base temporal; el medidor encontró un único PID, tomó 7 muestras y terminó con `fin: process-exited`; después no quedó `java.exe`. Artefactos del smoke eliminados.
- **No ejecutada:** la prueba sostenida de F6.1. No hay cifras de CPU/memoria de una sesión larga ni un CSV real todavía. Los valores del smoke (arranque de la UI) no se presentan como resultado de rendimiento.
- Límites: una sola máquina; resolución de 30 s por defecto; la copia de la base debe hacerse con la UI cerrada e incluir `-wal`/`-shm` si existen; el medidor no cubre procesos hijos ni el bridge si corre aparte. Protocolo en `docs/F6_MEDICION.md`.
- Siguiente: el usuario ejecuta la sesión sostenida con la base copiada y aporta el CSV; entonces se cierra F6.1 con método, equipo y duración.

## E026 — 2026-11-22 — Brave en la captura de UI y parámetros de bridge

- Funciona: `CaptureSession` autoriza `chrome.exe` y `brave.exe` cuando hay bridge configurado; sin bridge ambos quedan excluidos porque el proveedor de host está vacío. El estado visible pasa a “Chrome/Brave + un perfil”. `Start-Ui.ps1` acepta `-ExtensionId`, `-Port` (47821 por defecto) y `-BridgeTokenFile` (por defecto `work\pilot\bridge-token.txt`), valida el token sin imprimirlo, comprueba que el puerto esté libre y restaura las variables de entorno al salir.
- Probado: `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify` → **121 tests Java, 0 fallos**; `PrivacyFilterTest` ya cubre `brave.exe` con host fresco y su exclusión sin host. Prueba conjunta real con base temporal: UI con `-ExtensionId cnipocakflhegoediialcdngmopbieip -Port 47823 -Smoke shutdown-close` y el medidor a la vez → bridge activo, estado “Chrome/Brave + un perfil”, **7 muestras** del medidor, `UI shutdown: hilos deriva-capture vivos=0` y sin `java.exe` después. Artefactos eliminados; base real intacta.
- **No probado:** un reporte real de Brave con host a través de la UI. La prueba conjunta no tenía la extensión configurada para el puerto temporal, así que no valida sitios de Brave en la UI; eso requiere abrir Brave, confirmar el ID en `brave://extensions`, configurar puerto/token y verificar el host. El gate completo de Brave (segundo perfil, reinicio, 1 h, colisión) sigue pendiente.
- Límites: usar un solo navegador/perfil a la vez contra el bridge; el payload de Brave dice `browser:"chrome"`, así que Chrome y Brave simultáneos se pisan. No ejecutar `Start-BridgePilot.ps1` ni `Start-ConsoleCapture.ps1` con la UI en el mismo puerto. El medidor no usa red y no compite con el bridge.
- Siguiente: configurar la extensión de Brave y ejecutar la sesión real; la prueba sostenida de F6.1 sigue pendiente.

## E027 — 2026-11-23 — Modo de prueba de Interrupciones

- Funciona: botón **Ver datos de ejemplo** en Interrupciones. Carga 6 episodios sintéticos en memoria (1 abierta, 4 estables y 1 sin confirmar), con contextos, motivos y justificadas variadas; respeta los filtros de estado y fecha y rellena tabla, contadores y detalle. El botón alterna a **Volver a datos reales** y restaura la consulta normal.
- Privacidad/almacenamiento: no consulta ni escribe SQLite. Mientras el modo está activo muestra “Modo prueba · episodios de ejemplo · no se guardan”, deshabilita Motivo/Justificada/Guardar y no permite anotar. Los IDs sintéticos son `900001`–`900006`.
- Probado: `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify` → **122 tests Java, 0 fallos** (1 nuevo de ViewModel que comprueba 6 filas, 1 abierta, orden, filtros y que no se consulta el repositorio). Smoke `Start-Ui.ps1 -Database <temporal> -Smoke demo-episodes` → `modo ejemplo=Volver a datos reales, filas=6, guardarDeshabilitado=true`; la base temporal quedó con `drift_event=0` y `segment=0`. Captura en `work/ui-preview/f6-demo/demo-episodes.png`.
- Límites: son datos sintéticos, no una prueba del detector ni de la captura real; no hay refresco automático de la pantalla. El modo no persiste anotaciones y no sustituye la prueba con episodios reales.
- Siguiente: usar el modo para revisar visualmente la tabla/detalle; F6.1 sostenida sigue pendiente.

## E028 — 2026-11-23 — Guardar sí persiste; confirmación visual reforzada

- Diagnóstico del reporte “le doy a Guardar y no hace nada”: la anotación **sí se estaba guardando**. En la base real de la sesión (`work/f6/deriva-f6.db`, copia de trabajo) el episodio 1 tenía `reason=SEARCHING_INFO`, `justified=1` y `answered_at=2026-11-23 10:06:18`, mientras la captura y el bridge estaban activos. El problema era la confirmación: un texto pequeño bajo el botón que podía pasar desapercibido y se borraba al refrescar la tabla.
- Cambio de UI: `Guardar` pasa a `Guardando…` mientras escribe; al terminar muestra un banner visible `Guardado · <motivo> · [Justificada] · HH:mm` con estilo propio, lo conserva tras el refresco y muestra el error en rojo si falla. El banner se limpia al cambiar de episodio, motivo o casilla.
- Probado: `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify` → **124 tests Java, 0 fallos**. Smoke `Start-Ui.ps1 -Database <copia> -Smoke annotate` → `guardarDeshabilitado=false`, `estado tras Guardar=Guardado · Mensajes · Justificada · 10:15`, y la copia quedó con `MESSAGES|1` en el episodio 2. Captura: `work/ui-preview/f5-2/guardado.png` (banner verde visible).
- Límites: el smoke recorre los controles y pulsa `fire()`, no es un clic humano. La copia de prueba no es la base real bloqueada (`work/console-day/deriva.db` sigue con SHA-256 `0446AE5D…`).
- Siguiente: F5.2 (pausa y bandeja).

## E029 — 2026-11-23 — F5.2 pausa manual y bandeja básica

- **F5.2a (pausa):** `CaptureState.PAUSED`; `CaptureControl.pause()` detiene el bucle y el cierre usa `CloseReason.PAUSE` para el episodio abierto. `Segmenter.close` persiste el segmento abierto; reanudar con `start()` abre un bucle nuevo. La barra lateral muestra `Captura en pausa.` en ámbar, mantiene **Detener captura** y muestra **Reanudar captura**. Detener desde pausa deja el estado en `STOPPED`.
- **F5.2b (bandeja):** `AppTray` usa `java.awt.SystemTray` con menú **Abrir Deriva**, **Pausar/Reanudar captura** y **Salir**. Cerrar la ventana la oculta y avisa de que la captura sigue; **Salir** detiene, espera el `join`, hace flush, retira el icono y termina el proceso. Si la bandeja no está soportada, se conserva el comportamiento anterior: cerrar termina y hace flush. `Platform.setImplicitExit(false)` solo se activa con bandeja instalada.
- Probado: `mvn … verify` → **124 tests Java, 0 fallos** (2 nuevos: cierre `PAUSE` en la máquina y persistencia del episodio UNCONFIRMED con `PAUSE`). Smoke `-Smoke pause` (base temporal): `Captura activa → Captura en pausa. (botón Reanudar captura) → Captura activa → Captura detenida`; `hilos deriva-capture vivos=0`; la base quedó con dos segmentos separados por el hueco de pausa (10:15:26–10:15:30 y 10:15:33–10:15:36) y **0 solapes**. Capturas: `work/ui-preview/f5-2/pause-paused.png` y `pause.png`.
- Probado: smoke `-Smoke tray` (base temporal): `Tray: instalada`; pausa y reanudación desde los mismos callbacks del menú; cerrar la ventana dejó `ventana oculta=true` con la captura activa; reabrir dejó `ventana reabierta=true`; **Salir** terminó con `hilos deriva-capture vivos=0` y código de salida 0. Captura: `work/ui-preview/f5-2/tray.png`.
- Límites: el smoke invoca los mismos callbacks del menú, **no es un clic humano sobre el icono de la bandeja**. La bandeja es específica de Windows; si `SystemTray` no está disponible se usa el cierre normal. El cierre `PAUSE` se validó con test unitario y con el hueco de segmentos; el smoke corto no generó un episodio de deriva abierto. No se probó el aviso emergente de Windows con un clic humano ni la bandeja tras suspensión/bloqueo. Bases temporales; la base real no se tocó.
- Siguiente: F6.1 (registrar la medición sostenida) y después F6.2.

## E030 — 2026-11-23 — F6.1 medición sostenida real

- Fuente: `work/f6/ui-resources.csv` (SHA-256 `2DF7C43703890F7A43D6086E7ED6F3A27CC8542A8B7C6EF129FFC19175FA0EDD`), generado por `scripts/Measure-UiResources.ps1` con intervalo de 30 s.
- Duración y muestras: **256 filas**, de `2026-11-22T22:04:23` a `2026-11-23T00:11:53` (hora local), **7.650,019 s = 2 h 7 min 30 s**. Hueco máximo entre muestras: **30,025 s**; 0 filas fuera del intervalo de 30 s (±2 s).
- Consumo del proceso Java de la UI (PID 9084, 16 CPUs lógicas): CPU media **0,15 %** del total del sistema, máxima **0,40 %**, p95 **0,24 %**. Working set: 394,7 MB al inicio, 249,0 MB al final, máximo **416,4 MB**. Memoria privada: 439,6 MB al inicio, **484,0 MB máximo**. Hilos: **67–70**; handles: **750–760**.
- Captura activa durante la medición: la base de la sesión (`work/f6/deriva-f6.db`) tiene **7.646,4 s registrados** entre las 22:04 y las 00:12 (≈99,5 % de la ventana), con **3.390 s de Brave con host** (bridge activo), 2.101 s de inactividad, 1.239 s de exclusión y el resto en VLC, Terminal, Word y otros. No hubo episodios de deriva en esa ventana; no es un fallo.
- Equipo: ASUSTeK ASUS TUF Gaming F15 FX507ZC4 (Intel Core i5-12500H, 12 núcleos / 16 lógicos, 15,6 GB de RAM), Windows 11 Home 10.0.26200, Java 21.0.7, UI a 1440×900.
- Capturas reales del usuario de una sesión posterior (08:00–10:07 aprox.): `docs/pruebas/imagen1PruebaDeriva.png` (Interrupciones con 2 episodios) y `docs/pruebas/imagen2PruebaEnNavegadorYapps.png` (Uso diario con Brave/YouTube/Chess.com). Son evidencia de uso real, **no** de la medición de CPU/memoria.
- Límites: una sola máquina y resolución de 30 s; mide solo el proceso de la UI (el bridge corre dentro de ese proceso, pero no se miden procesos hijos); la sesión posterior de la mañana no tiene CSV; no se declara una prueba de varias horas ni de otro equipo. La comprobación de “captura activa” se hizo sobre una copia de lectura de la base de la sesión con la UI abierta, no sobre un backup SQLite online; los conteos son indicativos y el CSV es la evidencia principal. La base protegida `work/console-day/deriva.db` no se tocó.
- Resultado: **PERF-01 PASÓ; F6.1 HECHO**. Siguiente: F6.2 entrega local reproducible, pendiente de autorización.

## E031 - 2026-11-23 - Claridad entre anotación guardada y estabilidad no comprobada

- **Cambio de UI, no cierre de F6.2.** La columna «Anotación» usa `answered_at` persistido: `NULL` → **Pendiente**; con marca temporal → **Guardada**. No se deduce del motivo ni de Justificada. «Estado» pasa a **Estabilidad** con etiquetas **En curso / Estable / No comprobada**; el filtro usa las mismas etiquetas.
- **Automatizado:** `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify` → **127 tests Java, 0 fallos** (3 nuevos: 2 de `InterruptionsViewModelTest` y 1 de `SQLiteDriftRepositoryTest`; 124 previos). Los tests SQLite reales comprueban que anotar un episodio OPEN conserva `OPEN`, `closed_at` y `close_reason` nulos, y que anotar un STABLE no lo reabre. Sin mocks para SQLite: el test usa una base temporal real. `node --test .\extension\tests\report.test.mjs` → **5 tests JS, 0 fallos** (la extensión no cambió; se reejecutó como regresión).
- **Smoke de UI con datos sintéticos:** `powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Demo -Database .\work\ui-preview\ui-claridad\final.db -Smoke annotate -Screenshot .\work\ui-preview\ui-claridad\guardado.png`. Salida: `UI smoke: episodio=7, motivo=MESSAGES, justificada=true, guardarDeshabilitado=false`; `UI smoke: estado tras Guardar=Guardado · Mensajes · Justificada · 10:44`; `hilos deriva-capture vivos=0`. La captura muestra la columna **Anotación = Guardada** y el banner visible.
- **Persistencia tras reabrir:** se cerró y se volvió a abrir la misma base con `-Demo -Database ...\final.db -Screenshot ...\reabierto.png` (sin smoke). La captura muestra la columna **Guardada** y el detalle `Guardada · 23/11 10:44`; no se volvió a pulsar Guardar.
- **Fallo sin éxito falso:** sobre una copia temporal se creó un trigger SQLite que aborta `UPDATE drift_event` cuando `answered_at` no es nulo. El smoke mostró `UI smoke: estado tras Guardar=No se pudo guardar: Could not annotate drift event`, banner rojo y la fila permaneció **Pendiente** en la captura `fallo-pendiente.png`. El mensaje de causa sigue en inglés; la UI lo prefija en español.
- **Explicación de estabilidad:** sobre una copia temporal con un único episodio `UNCONFIRMED` (`CloseReason.IDLE`), la captura `no-comprobada.png` muestra **No comprobada**, `Tiempo hasta actividad estable —` y `No se comprobó la estabilidad: el cierre por inactividad.`
- **Capturas:** `work/ui-preview/ui-claridad/{guardado,reabierto,fallo-pendiente,no-comprobada}.png`; artefactos, bases y scripts de preparación solo en `work/` (ignorado por Git).
- **Límites:** los smokes recorren controles y botones; no son clics humanos. Todas las bases de estas capturas son sintéticas (`-Demo`), no una sesión real de captura. El fallo se indujo con un trigger, no con un disco lleno o un bloqueo real. La base protegida no se tocó: `work/console-day/deriva.db` SHA-256 `0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475`.
- **Resultado:** **UI-CLARIDAD HECHO** como mejora de claridad; F6.2 sigue **PENDIENTE** y Ajustes no se implementó.

### 2026-11-23 - Preparación de Git
Comandos: `git status --short`, `git diff --stat`, `git ls-remote https://github.com/markmercado1/Deriva.git`. Resultado: archivos aún sin seguimiento, sin diff rastreado y remoto accesible sin referencias. Revisión de .gitignore: work/, target/, bases y logs excluidos; se añade docs/pruebas/. No se ejecutan tests de aplicación: no cambia código. La subida se verifica después del commit.

## E032 — 2026-11-23 — Detalle desplazable
- `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress -DskipTests package`: BUILD SUCCESS; tests omitidos (solo contenedor FXML).
- `powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Demo -Screenshot C:\AppTImeControl\work\ui-preview\ui-scroll.png`: carga real JavaFX, captura inspeccionada y cierre con 0 hilos de captura.
- `$probeClasspath = 'C:\AppTImeControl\target\classes;' + (Get-Content work/ui-preview/classpath.txt -Raw).Trim(); java -cp $probeClasspath work/ui-preview/ScrollProbe.java`: PASS 1200x600; contenido supera viewport y bounds de Guardar completamente dentro al poner vvalue=1.
- Probe temporal en work/, sin base de datos; no es un clic humano ni prueba de captura. La validación XML inicial detectó un cierre de etiqueta faltante, corregido antes de la compilación final y los smokes.


## E033 — 2026-11-23 — Guardar sin duplicados
- Comando: `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify`. BUILD SUCCESS: 129 tests, 0 fallos/errores/omitidos; dos pruebas nuevas de comparación de anotaciones.
- Comando: `$probeClasspath = 'C:\AppTImeControl\target\classes;' + (Get-Content work/ui-preview/classpath.txt -Raw).Trim(); java -cp $probeClasspath work/ui-preview/SaveProbe.java`.
- Resultado: PASS; 30 clics antes de completar escritura producen una llamada; después de guardar no permite repetir; motivo y casilla habilitan, revertir deshabilita; fallo inducido conserva datos y permite reintento exitoso. Refresco durante escritura no desbloquea el botón.
- Límite: smoke JavaFX con repositorio simulado y sincronización mediante latch; no prueba navegador, SQLite real ni clic humano. Primera ejecución del probe falló por faltar Scene/CSS en el arnés; corregido y repetido con éxito. No se ejecutó JS porque no hay cambios de extensión.

## E034 - 2026-11-23 - AJUSTES-DETECCION: cuatro umbrales configurables

- **Alcance:** solo `drift_switches`, `drift_window_s`, `stable_s` e `idle_threshold_s` sobre la tabla `setting` existente. Predeterminados conservados: 10 / 120 / 180 / 120. Rangos iniciales de producto: 4-30; 30-600 s; 30-900 s; 30-1800 s. Sin editor de exclusiones, bridge, empaquetado ni cierre de F6.2.
- **Automatizado:** `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify` -> **158 tests Java, 0 fallos/errores/omitidos** (29 nuevos respecto a E033). Cubren límites y defaults (`DetectionSettingsTest`), umbral 4/ventana 30/estabilidad 30 y candidato pendiente (`DriftStateMachineConfigTest`), `switchCount` menor que 10 en los modelos, idle personalizado, política de sesión, servicio con fallback visible, repositorio SQLite real, integración servicio+SQLite, ViewModel y explicación de estabilidad sin 180 fijo.
- **Persistencia real:** smoke `-Smoke settings` sobre base temporal mostró `Guardar` deshabilitado con defaults, habilitado al editar 8/90/240/300, y tras guardar `En uso ahora: 8 cambios · 90 s · 240 s · 300 s`; `sqlite3 -readonly` devolvió las cuatro claves con esos valores. Al reabrir la misma base, los campos y `En uso ahora` mostraban los valores guardados. Capturas: `work/ui-preview/ajustes/settings-final2.png`, `settings-reopen.png`, `settings-session4.png`.
- **Aviso y defaults:** captura `settings-defaults.png` muestra 10/120/180/120 y el aviso de inactividad≤estabilidad; el aviso no bloquea Guardar.
- **Configuración inválida:** se copió una base temporal y se escribieron `drift_switches=3` e `idle_threshold_s=dos minutos`. La UI mostró `En uso ahora: 10 ... 120 ...` y un aviso que enumera ambos fallbacks; `sqlite3 -readonly` confirmó que la base seguía con `3` y `dos minutos`, sin sobrescritura silenciosa. Captura: `work/ui-preview/ajustes/settings-invalid.png`.
- **Regla de sesión:** smoke `-Smoke settings-session` con captura real sobre base temporal. Tras guardar 8/90/240/300 con captura activa: efectivo 10/120/180/120 y `pendienteVisible=true`; en pausa y tras reanudar siguió igual; tras Detener: efectivo 8/90/240/300 y `pendienteVisible=false`; tras Iniciar se mantuvo 8/90/240/300. `hilos deriva-capture vivos=0` y 0 episodios OPEN. Captura: `work/ui-preview/ajustes/settings-session4.png`.
- **Consola diagnóstica:** `ConsoleCapture` conserva `IDLE_THRESHOLD=120 s` y `new DriftStateMachine()` (defaults); no lee la configuración de la UI. Queda documentado en código y README.
- **Límites:** los smokes recorren controles y llaman `fire()`, no son clics humanos; las bases son temporales y no hubo un episodio real de deriva con umbral distinto (el detector se verificó con tiempo simulado). El smoke de sesión duró segundos y no valida horas de captura. No se añadió un código de causa de EXCLUSION; el hallazgo de que enfocar Deriva puede cerrar un episodio queda como revisión de comportamiento separada. F6.2 sigue PENDIENTE.
- **Base protegida:** `work/console-day/deriva.db` SHA-256 `0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475`, sin cambios.- **Regresión de extensión:** `node --test .\extension\tests\report.test.mjs` -> **5 tests JS, 0 fallos**; no se modificó la extensión.

## E035 - 2026-11-23 - EXCLUDED breves al navegar en Brave: UNKNOWN transitorio de la extensión

- **Alcance:** diagnosticar y corregir las EXCLUDED de ~1 s que cortaban la continuidad entre sitios en Brave normal. Sin cambiar `PrivacyFilter`, la frescura de 3 s, la semántica de EXCLUDED, los umbrales ni el esquema. No cierra F6.2 ni el gate completo de Brave.
- **Paso 0 (solo lectura, `work/f6/deriva-f6.db`):** configuración guardada 4/30/180/60. Sin los cortes, la secuencia del usuario tiene 5 cambios físicos de sitio; con debounce de 3 s los cambios confirmados caerían aproximadamente en 11:58:50, 11:58:55, 11:59:02, 11:59:06 y 11:59:11. A las 11:59:06 ya hay 4 cambios dentro de `(t-30 s, t]`, por lo que el episodio debería abrirse. Con los cortes reales, el EXCLUDED reinicia `ChangeCounter` y limpia la ventana; no hay 4 cambios acumulados y el evento no se crea (la base tenía solo los episodios 00:42 y 09:10 antes de la prueba; ninguno a las 11:59). En 11:58:37-12:02:40 se contaron 7 EXCLUDED de ~1 s, 1 de ~2 s, 1 de ~4 s y 1 de ~18 s; en toda la base hay 270 EXCLUDED, 96 de <=2,5 s.
- **Sonda real opt-in:** `powershell -NoProfile -ExecutionPolicy Bypass -File .\work\diagnostico-continuidad-brave\poll-bridge.ps1 -Seconds 1800 -OutFile .\work\diagnostico-continuidad-brave\poll-manual.csv`. Consulta `/v1/status` cada 250 ms y registra solo timestamp, `state`, `sequence`, `accepted`, `hostPresent` y categoría de foco (`BROWSER_AUTHORIZED`, `BROWSER_OTHER`, `OWN_APP`, `OTHER_APP`, `NONE`). Lee el token local sin imprimirlo; no registra host, URL, título, historial ni token. La sonda también se ejecutó sobre la app ya abierta, sin reiniciarla.
- **Causa demostrada (antes de corregir):** con `fg=BROWSER_AUTHORIZED` de forma continua durante 19:41:28-19:42:43, el bridge pasó de NORMAL a UNKNOWN durante 0,54-0,80 s en 19:41:58.651, 19:42:07.885-08.689, 19:42:33.023-33.561 y 19:42:42.819-43.613. Los reportes UNKNOWN eran frescos y aceptados: `sequence`/`accepted` avanzaban ~1/s (gap máximo 1,096 s), sin STALE, sin estado INTERNAL y sin reinicio de sesión. La base registró EXCLUDED en 19:41:37 (1,007 s), 19:42:08 (4,041 s), 19:42:33 (1,013 s) y 19:42:43 (16,153 s); el episodio 3 se abrió a las 19:41:46 y cerró a las 19:42:08 como UNCONFIRMED/EXCLUSION con 5 cambios. No hubo rechazo, caducidad, incógnito, cambio de perfil ni segundo reportero en la sonda.
- **Corrección:** `extension/chromium/sampling.js` añade `settleObservation`: ante un cambio de revisión durante la lectura o un UNKNOWN reintentable por URL aún no lista en pestaña `loading`, vuelve a leer la pestaña hasta 3 intentos con 150 ms entre reintentos y una lectura final. Cada intento es una lectura nueva; no reutiliza el host anterior. `report.js` expone `inspectTab` con la marca `retry`, y `background.js` envía solo `state`/`host` al bridge (nunca la marca interna). Incógnito, ventana sin foco, pestaña inactiva/cerrada, página interna y URL inválida estable no se reintentan. La marca `retry` no se persiste ni sale de la extensión.
- **Verificación real posterior:** se recargó la extensión descomprimida en Brave (misma ventana/perfil, sin reiniciar el navegador) y se repitió una vuelta de navegación. De 19:47:26.373 a 19:48:04.507 la sonda vio NORMAL de forma continua durante 38,13 s con `fg=BROWSER_AUTHORIZED`; no hubo UNKNOWN. La base muestra 19:47:35-19:48:00 como segmentos OBSERVED de Brave con host y sin EXCLUDED entre ellos; el episodio 4 se abrió a las 19:47:54 con 6 cambios. El único EXCLUDED cercano, 19:47:19-26, corresponde a la página interna de extensiones durante la recarga y es una exclusión legítima.
- **Automatizado:** `node --test extension/tests/report.test.mjs extension/tests/sampling.test.mjs extension/tests/background.test.mjs` -> **14 tests JS, 0 fallos**. Cubren: clasificación/privacidad existente, reintento solo para URL no lista en carga, no reintento de incógnito/foco/pestaña inactiva/interna, relectura ante cambio de revisión sin reutilizar host, UNKNOWN genuino inmediato, límite de reintentos y un test de `background.js` con `chrome`/`fetch` simulados que comprueba que un evento de pestaña durante la lectura produce NORMAL del host nuevo y un payload con solo `browser,state,host,seq,session`. `node --check` de `background.js`, `report.js` y `sampling.js` sin errores. `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify` -> **158 tests Java, 0 fallos/errores/omitidos**.
- **Artefactos:** `work/diagnostico-continuidad-brave/poll-manual.csv` SHA-256 `3B0AA024EDD8B9685846810402BFE67D6E2D4C35B441348FF60AB17EA0A9CD18`; `poll-fix.csv` SHA-256 `2E5864E6938AFCDE4FC7E7FEBF9D0EA935D21B0B1FA0DEB3B900952C029970AA`; `poll-bridge.ps1` (sonda). Están bajo `work/` y no se versionan. La base `work/f6/deriva-f6.db` fue extendida por la app real durante la secuencia (segmentos 979-1025 y episodios 3-4); no se escribió en ella desde la sonda. Base protegida `work/console-day/deriva.db` intacta, SHA-256 `0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475`.
- **Límites:** una máquina, un perfil/ventana de Brave y una secuencia corta; no se repitieron en navegador incógnito, pérdida real de foco, reporte caducado ni segundo perfil. Esas rutas siguen cubiertas por los tests de la extensión y por el `PrivacyFilter` Java sin cambios, no por una prueba real nueva. No se observó colisión Chrome/Brave (la secuencia no reinició sesión). El mensaje de la UI indica bridge configurado, no frescura del sitio; queda como mejora separada. F6.2 sigue PENDIENTE.

## E036 - 2026-11-24 - F6B-1: esquema V2 de objetivos y repositorios

- **Alcance:** solo base de datos y repositorios. Nuevo paquete `com.deriva.goals.model` (`Goal`, `GoalAlert`, `GoalMetric`), puertos `GoalRepository`/`GoalAlertRepository` y adaptadores `SQLiteGoalRepository`/`SQLiteGoalAlertRepository`. `SQLiteSchema` pasa a V2 con escalera de migración sobre `schema_version` (A41); `SQLiteDataMaintenance` borra `goal_alerts` pero conserva `goals` (A42). Sin UI, sensor, segmentación, máquina de deriva, `SQLitePrivacyCheck` ni `README.md`.
- **Esquema:** `goals(id, process_name COLLATE NOCASE, site_host, daily_minutes, daily_entries, passive, active, created_at, updated_at)` con `CHECK` de al menos un tope, topes positivos, proceso sin separadores y host sin `/`, `?`, `#`, `:` ni espacios; índices únicos parciales por contexto (app y app+sitio). `goal_alerts(id, goal_id ON DELETE CASCADE, triggered_at, threshold_percent 1–100, metric MINUTES/ENTRIES, outcome, justification <= 500)`.
- **Automatizado:** `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify` -> **177 tests Java, 0 fallos/errores/omitidos** (158 previos + 19 nuevos: 10 de objetivos, 6 de alertas, 3 de migración). BUILD SUCCESS.
- **Cobertura nueva:** migración V1 con datos (segmento, drift, contexto, setting, exclusiones) sin pérdida y `schema_version = 2`; `initialize` dos veces sin error ni filas duplicadas en `schema_version`; base nueva nace en V2 con ambas tablas; versión futura rechazada con `StorageException`; CRUD de objetivos y alertas con reapertura; `listActive` solo activos y ordenados; `listByGoal` y `listPending`; rechazo de objetivo sin tope y de host con forma de URL tanto en dominio como en la base; `process_name` insensible a mayúsculas en la base (`CODE.EXE` rechazado); borrado de objetivo en cascada a sus alertas y conservación de las de otro objetivo; FK rechaza alerta de objetivo inexistente; justificación de 501 caracteres rechazada por dominio y por `CHECK`; umbral 101 rechazado; `PRAGMA table_info` de ambas tablas sin columnas `url/uri/title/window_title/query/path/content`; `deleteActivity` deja `goal_alerts = 0` y `goals = 1`.
- **Límites:** bases temporales `@TempDir`, no la base real. **La migración de `work/console-day/deriva.db` no se ejecutó:** la hará el usuario con respaldo y SHA-256. Una base en V2 ya no la abre el código V1 (A41). `SQLitePrivacyCheck` sigue auditando solo `segment`; su ampliación queda para F6B-7. Sin prueba manual de UI ni de captura, porque esta fase no las toca. F6.2 sigue PENDIENTE.

## E037 - 2026-11-24 - F6B-0: actividad pasiva para objetivos

- **Alcance:** resolver P1 **solo** para contextos con un objetivo activo `passive=true`. Nuevo `com.deriva.goals.PassiveActivity` puro: recibe el máximo y los objetivos inyectados, y decide si una observación `IDLE` se promueve a `OBSERVED`. `CaptureSession` reevalúa la misma muestra con inactividad cero y aplica la regla entre `PrivacyFilter` y `Segmenter`; no se modificaron `Segmenter`, `PrivacyFilter`, sus tests ni el esquema. Sin UI.
- **Regla:** el máximo inicial es 10 min desde la última entrada (incluye el umbral de inactividad). Solo se promueve si la observación filtrada es `IDLE`, la misma muestra sin inactividad sería `OBSERVED`, el contexto coincide con un objetivo activo pasivo y la inactividad es ≤ 600 s. `UNAVAILABLE` (bloqueo, suspensión, sensor no disponible) y `EXCLUDED` mandan; cambiar a un contexto no pasivo corta la promoción. Coincidencia: objetivo de app (`siteHost == null`) solo con muestra sin host; objetivo de sitio con proceso y host exactos (A43).
- **Automatizado:** `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify` -> **189 tests Java, 0 fallos/errores/omitidos** (177 previos + 12 nuevos), BUILD SUCCESS. `node --test extension/tests/report.test.mjs extension/tests/sampling.test.mjs extension/tests/background.test.mjs` -> **14 tests JS, 0 fallos** (extensión sin cambios).
- **Cobertura nueva (`PassiveActivityTest`):** 5 min sin entrada cuentan como `OBSERVED`; 600 s cuenta y 601 s vuelve a `IDLE`; sin objetivos pasivos las cuatro clases de observación quedan idénticas; objetivo no pasivo y objetivo pasivo inactivo no aplican; cambiar de contexto pasivo a no pasivo corta; cambiar a otro contexto pasivo también corta hasta que vuelve la actividad; un hueco >3 s (suspensión) corta y exige actividad real; `lockapp.exe` y muestra no disponible siguen `UNAVAILABLE`; objetivo de app no coincide con muestra con host; objetivo de sitio exige proceso+host exactos; prueba pura filtro→política→segmentador mantiene el segmento abierto hasta 600 s y lo cierra como `IDLE` en 601 s.
- **Sin cambios de regresión:** `SegmenterTest` conserva **16 tests** y `PrivacyFilterTest` **17 tests**, ambos verdes; no se editaron `Segmenter.java`, `PrivacyFilter.java` ni sus archivos de prueba.
- **Límites:** pruebas deterministas con tiempo simulado y `@TempDir`; no se probó vídeo/ajedrez real ni una sesión de 10 min en Windows. `CaptureSession` lee los objetivos activos una sola vez al iniciar: crear, editar o borrar objetivos se aplica al reiniciar la captura (A43/F6B-3). El máximo es un parámetro inyectado de 10 min; no hay persistencia ni “efectivo vs. pendiente” todavía. No se abrió ni migró `work/console-day/deriva.db`. **P1 queda resuelto solo para contextos con objetivo pasivo**, no para toda la actividad pasiva. F6.2 sigue PENDIENTE.

## E038 - 2026-11-24 - F6B-2: motor de cálculo de objetivos

- **Alcance:** motor puro `com.deriva.goals.GoalProgressCalculator` con `GoalProgress`/`GoalState`; sin UI, alarmas, SQLite, hilos, reloj ni escritura. Entrada: día local, zona, segmentos cerrados y objetivos activos. Salida por objetivo: `usedMillis`, entradas, estado y `unmeasurable`. `analysis.DayWindow` concentra el recorte al día local y lo comparte `DailyUsageService`.
- **Reglas:** solo `OBSERVED` suma. Objetivo de app = mismo proceso con o sin host; objetivo de sitio = proceso + host exactos. Los objetivos son límites independientes: el tiempo del sitio suma también al objetivo de app del mismo proceso, pero dentro de cada objetivo cada instante se cuenta una vez (unión de intervalos solapados/desordenados). Estado: `EXCEEDED` si algún tope llega al 100 %, si no `NEAR` al 80 %, si no `IN_PROGRESS`. Entrada nueva si el hueco entre visitas es ≥3 s; <3 s continúa la misma. `unmeasurable` con cualquier `EXCLUDED` del día en objetivos de sitio y en apps cuyo proceso esté en la lista de navegadores de `PrivacyFilter`.
- **Automatizado:** `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify` -> **201 tests Java, 0 fallos/errores/omitidos** (189 previos + 12 nuevos), BUILD SUCCESS. `node --test extension/tests/report.test.mjs extension/tests/sampling.test.mjs extension/tests/background.test.mjs` -> **14 tests JS, 0 fallos** (extensión sin cambios).
- **Cobertura nueva (`GoalProgressCalculatorTest`):** cruce de medianoche; primera presencia = 1 entrada; huecos de sitio de 2 s y 4 s (solo suma lo observado, 2 entradas); exactamente 3 s y 4 s fuera abren entrada; `IDLE`/`EXCLUDED`/`UNAVAILABLE` no suman pero separan entradas; 79/80/100 % exactos; dos topes con el peor estado; app y sitio del mismo proceso independientes y app de navegador sumando tiempo con host; `unmeasurable` en Chrome/Brave/Edge y sitio, falso en app no navegador y sin `EXCLUDED`; solapes/desorden sin doble conteo; sin segmentos = 0; segmentos fuera del día ignorados.
- **E020 sin cambios:** `git diff -- src/test/java/com/deriva/app/usecase/DailyUsageServiceTest.java` vacío y sus **3 tests** siguen verdes; `DailyUsageService` solo cambió para usar `DayWindow`.
- **Límites:** pruebas deterministas con instantes fijos; no hay base, UI ni sesión real. No se incluye el segmento en curso: F6B-4 deberá pasarlo cortado en «ahora» como un segmento más (A44). `unmeasurable` es conservador porque `EXCLUDED` no lleva identidad. F6.2 sigue PENDIENTE.

## E039 - 2026-11-24 - F6B-3: pantalla de objetivos, recarga y máximo pasivo

- **Alcance:** nueva pantalla `Objetivos` (`objetivos.fxml`, `GoalsController`, `GoalsViewModel`, `GoalsPage`, `GoalRow`, `GoalContextOption`) y `GoalsService` sobre `GoalRepository`/`SegmentRepository`. Añade `GoalRepository.listAll()`, `SegmentRepository.observedContexts()`, `PassiveGoalProvider`/`PassiveGoalSnapshot`, `PassiveActivity.updateGoals(...)`, `PassiveMaxSnapshot`, el ajuste `passive_max_s` y la sección «Actividad pasiva» en Ajustes. `main.fxml` solo añade el botón **Objetivos**; las pantallas existentes conservan su estructura, salvo la sección nueva de Ajustes.
- **Comportamiento:** crear, editar topes/pasivo/activo, activar/desactivar y borrar con confirmación. El contexto se elige entre apps/sitios ya vistos o se escribe; al editar queda bloqueado. Validación en español: al menos un tope, valores positivos, proceso sin `/` ni `\`, host sin URL/ruta/consulta/puerto/espacios y contexto duplicado rechazado. Progreso con `GoalProgressCalculator`; estados **En curso ▲**, **Cerca del tope ◆**, **Tope superado ●** (icono+texto); aviso de subconteo si `unmeasurable`. Línea fija: «Progreso según segmentos ya cerrados; el segmento actual se suma en una fase posterior». Refresco al abrir y cada 30 s, detenido al cambiar de vista y al cerrar.
- **Recarga en captura:** `GoalsService` publica un `PassiveGoalSnapshot` con revisión; `CaptureSession` la consulta en cada tick y, al cambiar, actualiza `PassiveActivity` sin reiniciar. `updateGoals` conserva la promoción activa si el contexto sigue pasivo; eliminarlo o desactivarlo la corta. Editar un objetivo no relacionado no corta el contador.
- **Máximo pasivo:** `setting.passive_max_s`, 600 s por defecto, rango 60–3600 s, validado y sin sobrescribir valores inválidos; en Ajustes muestra efectivo/pendiente y sigue la regla de sesión de E034.
- **Automatizado:** `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify` -> **220 tests Java, 0 fallos/errores/omitidos** (201 previos + 19 nuevos), BUILD SUCCESS. `node --test extension/tests/report.test.mjs extension/tests/sampling.test.mjs extension/tests/background.test.mjs` -> **14 tests JS, 0 fallos**.
- **Cobertura nueva:** `GoalsViewModelTest` (9): crear «Ajedrez 60 min y 4 entradas», progreso 50/60 con 2 entradas y estado NEAR, edición que no cambia contexto, desactivar/reactivar, borrar, topes ausentes/no positivos/no enteros, rutas/URLs/duplicado y `unmeasurable`; `PassiveMaxSettingsViewModelTest` (4): predeterminado, pendiente, rango y restaurar; `SettingsServiceTest` (+3): máximo por defecto, inválido visible sin sobrescritura y guardado separado; `PassiveActivityTest` (+2): edición no relacionada conserva la promoción y quitar el contexto la corta; `SQLiteSegmentRepositoryTest` (+1): contextos observados distintos sin identidades de frontera. `SQLiteGoalRepositoryTest` cubre además `listAll` con inactivos.
- **Smoke de UI:** `powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f6b-3\objetivos.db -Smoke goals -Screenshot .\work\ui-preview\f6b-3\objetivos.png` -> crea `chess.exe` 60 min/4 entradas, imprime `Ajedrez`, `En curso` y `0 de 60 min · 0 de 4 entradas`, guarda `objetivos-creado.png`, abre confirmación, borra y deja `Sin objetivos`; hilos `deriva-capture` vivos=0. `powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f6b-3\ajustes.db -Smoke settings -Screenshot .\work\ui-preview\f6b-3\ajustes.png` -> la sección «Actividad pasiva» aparece con `En uso ahora: 600 s (10 min)` y el smoke de detección sigue guardando.
- **Límites:** base temporal en `work/ui-preview/f6b-3/`, no `work/console-day/deriva.db`; el smoke recorre controles, no es un clic humano. Sin sesión real de 10 min ni objetivo ajedrez real; no se incluye el segmento en curso (F6B-4). `ConsoleCapture` conserva la lectura única y el máximo de 10 min. No se abrió ni migró la base protegida. F6.2 sigue PENDIENTE.

## E040 - 2026-11-24 - F6B-4: alarma básica por umbral

- **Alcance:** `GoalAlertService` y `GoalAlertMonitor` en `app.usecase`; `GoalAlertPolicy` puro y `OpenSegmentProvider` en `goals`; `GoalAlertNotice`/`GoalAlertNotifier`; `AppTray.notifyGoal`; llamada desde el bucle `deriva-capture`. Sin cambios de esquema, `Segmenter`, `PrivacyFilter`, `GoalProgressCalculator` ni de la pantalla Objetivos. Decisiones en A46.
- **Comportamiento:** evaluación cada 30 s y en la primera muestra `OBSERVED`, solo con captura `RUNNING`; pausa, bloqueo, suspensión, `IDLE` y `EXCLUDED` no avisan. El segmento abierto se pasa acotado por «ahora» como un segmento más. Un fallo de evaluación se registra en `stderr` y se reintenta al siguiente ciclo sin detener la captura; un fallo de bandeja no impide el registro. Deduplicación en `goal_alerts` por objetivo, métrica, umbral y día local.
- **Automatizado:** `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify` -> **235 tests Java, 0 fallos/errores/omitidos** (220 previos + 15 nuevos), BUILD SUCCESS. `node --test extension/tests/report.test.mjs extension/tests/sampling.test.mjs extension/tests/background.test.mjs` -> **14 tests JS, 0 fallos** (extensión sin cambios).
- **Cobertura nueva (15):** `GoalAlertPolicyTest` (5): 79 % no avisa, 80 % una vez, 100 % después, sin repetición; tope de 4 entradas alcanza 80 y 100 en la misma entrada y notifica solo 100; salto de 7 a 11 min en un tope de 10 registra ambos y notifica 100; subir el tope de 10 a 20 min no repite el 80 ya registrado pero un 100 nuevo sí avisa; minutos y entradas son topes independientes. `GoalAlertServiceTest` (7): alerta 80 y luego 100 con texto «8 de 10 min»/«10 de 10 min», `outcome`/`justification` nulos; segunda evaluación sin repetir; reinicio el mismo día sin repetir; día siguiente vuelve a permitir; arranque por encima del 100 registra ambos y una sola notificación; 4 entradas registran 80 y 100 y una sola notificación; salto de minutos; segmento abierto acotado; subir el límite no repite el 80; fallo del notificador registra igualmente la alerta. `GoalAlertMonitorTest` (3): no evalúa en `PAUSED`, `IDLE`, `EXCLUDED` ni `UNAVAILABLE` (bloqueo/suspensión) y sí en `RUNNING`+`OBSERVED`; cadencia de 30 s; un repositorio que lanza `IllegalStateException` no escapa del bucle y se reintenta a los 30 s.
- **Integración real:** base temporal `work/f6b-4/observed.db` con objetivo `java.exe` de 60 min y 61 min observados; UI `capture-hold` de 150 s y ventana Java de prueba en primer plano. `GoalSmoke dump` muestra dos alertas del mismo objetivo con `threshold=80` y `threshold=100`, mismo `triggered_at`, `outcome=null` y `justification=null`; no hay URL, ruta, título ni consulta. El aviso se emite una vez con el umbral más alto (verificado con notificador falso; el globo real no se capturó visualmente).
- **Cierre limpio:** los smokes de captura con `cpu.db` y `observed.db` terminaron con `UI smoke: cierre de ventana con captura=Captura activa · sin bridge; navegadores excluidos.` y `UI shutdown: hilos deriva-capture vivos=0`; no se creó ningún hilo nuevo para la alarma.
- **CPU (corta, mismo método de E030):** `scripts/Measure-UiResources.ps1` con `-IntervalSeconds 5`, 16 CPUs lógicas, proceso de la UI, fórmula `100 * ΔCPU / Δwall / CPUs`. Sin actividad observada (Brave excluido): 9 muestras, 45,033 s, media 0,21 %, máx. 0,63 %. Con actividad observada de `java.exe` y una evaluación de alarma: 18 muestras, 90,023 s, media 0,27 %, máx. 1,27 %. E030 sigue siendo la referencia sostenida: 0,15 % medio en 2 h 7 min 30 s con muestras cada 30 s. La diferencia de 0,12 puntos porcentuales es de una muestra corta y una carga distinta; no se extrapola ni se declara equivalencia con E030. Artefactos `work/f6b-4/{ui-resources.csv,ui-observed.csv}` (ignorados).
- **Privacidad/base:** todas las bases de prueba en `work/f6b-4/` o `@TempDir`; `work/console-day/deriva.db` intacta, SHA-256 `0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475`. Sin captura de navegador nueva, sin nube, IA ni telemetría.
- **Límites:** el globo de la bandeja no se verificó con un clic humano ni captura visual; se reutiliza `TrayIcon.displayMessage` de E029 y el texto se comprueba con un notificador falso. No hubo una sesión real de 10 min ni objetivo ajedrez; la pantalla Objetivos sigue sin el segmento en curso. La medición de CPU es corta, de una máquina y con una ventana de prueba, no una prueba sostenida. F6.2 sigue PENDIENTE.

## E041 - 2026-11-24 - F6B-6: justificar excesos de objetivos

- **Alcance:** justificación de la alerta al 100 % desde la pantalla Objetivos. Añade `GoalJustification` puro, `GoalState.JUSTIFIED`, la sobrecarga de `GoalProgressCalculator`, las columnas V3 `goal_alerts.justified_at` y `goal_alerts.justified_excess`, los métodos `justify`/`removeJustification` de `GoalsService`, el diálogo de motivo y los estados Excedido/Justificado por ti/Sin responder. Sin cambios en `segment`, `drift_event`, `drift_context`, `Segmenter` ni `PrivacyFilter`.
- **Cobertura del exceso:** misma base que `GoalAlertService`: segmentos cerrados más el segmento abierto confirmado de `CaptureSnapshot`, sin cerrarlo. Al marcar se cubre el exceso acumulado; el uso posterior vuelve a contar y «Justificar de nuevo» reemplaza la cobertura, no la acumula. La justificación solo aplica al día local de `justifiedAt`.
- **Automatizado:** `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify` -> **251 tests Java, 0 fallos/errores/omitidos** (235 previos + 16 nuevos), BUILD SUCCESS. `node --test extension/tests/report.test.mjs extension/tests/sampling.test.mjs extension/tests/background.test.mjs` -> **14 tests JS, 0 fallos** (extensión sin cambios).
- **Cobertura nueva (16):** `GoalProgressCalculatorTest` (+4): la justificación cubre el exceso al marcar y el uso posterior vuelve a Excedido; una justificación de otro día se ignora; la más reciente reemplaza a la anterior sin sumar; las entradas también se justifican. `GoalsViewModelTest` (+5): los tres estados con icono/texto y la nota de Sin responder, motivo mostrado tal cual, cobertura de 10 min, revocar vuelve a Sin responder, uso posterior y «Justificar de nuevo», segmento abierto incluido, motivo >500 rechazado y textos sin causalidad/«comprobado». `GoalsJustificationServiceTest` (4): la alarma dispara el 100 % con el segmento abierto, la justificación lo cubre y al cerrarlo sigue Justificado; ignora la justificación de ayer y aplica la de hoy; rejustificar guarda 5 min (no 7) y persiste al reabrir; justificar y quitar dejan `segment`, `drift_event` y `drift_context` byte a byte idénticos. `SQLiteGoalAlertRepositoryTest` (+2): guarda, reabre y limpia la justificación; rechaza datos incompletos/negativos y justificar un 80 %. `SQLiteSchemaMigrationTest` (+1): V2→V3 sin pérdida; además V1→V3, base nueva en V3, idempotencia y versión futura 4 rechazada.
- **Identidad de actividad:** `GoalsJustificationServiceTest.justifyingAndRemovingLeaveActivityTablesByteIdentical` calcula SHA-256 sobre todas las columnas de `segment`, `drift_event` y `drift_context` en orden estable; el hash es idéntico antes de justificar, después de justificar y después de quitar. La justificación solo escribe `goal_alerts`.
- **Smoke de UI:** `powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f6b-6\justify-smoke.db -View goals -Smoke goals-justify -Screenshot .\work\ui-preview\f6b-6\justify-smoke.png` -> `justificado=?  Justificado por ti; motivo=Motivo: Partida con un amigo; cobertura=Cubierto: 10 min; estado=Justificación guardada.` y después `tras quitar=?  Sin responder; nota=Sin responder es solo una anotación tuya; no cambia los registros de actividad.; estado=Justificación quitada.`; `UI shutdown: hilos deriva-capture vivos=0`. Capturas de los estados con bases temporales: `exceeded.png` (Excedido ● sin botón), `unanswered.png` (Sin responder ○ + nota + Justificar exceso), `justified.png` (Justificado por ti ✓ + motivo + cobertura + Quitar), `exhausted.png` (Excedido ● + Justificar de nuevo + Quitar). El smoke `goals` existente sigue creando y borrando con `objetivos.png`.
- **Privacidad/base:** todas las pruebas usan `@TempDir` o `work/ui-preview/f6b-6/`; no se abrió ni migró `work/console-day/deriva.db`, SHA-256 `0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475`. Sin nube, IA, telemetría, URL, ruta, título ni consulta.
- **Límites:** los smokes recorren controles, no son clics humanos; una máquina y una sesión corta; el segmento abierto se simula en tests, no con una sesión real de 10 min; la migración de la base real sigue pendiente de que el usuario la haga con respaldo (A41). La pantalla puede mostrar Excedido hasta 30 s antes de que la alarma registre el 100 %. `SQLitePrivacyCheck` no cubre aún las tablas nuevas (F6B-7). F6.2 sigue PENDIENTE.

## E042 - 2026-11-24 - F6B-5: alarma diferida durante una deriva

- **Alcance:** puerto de solo lectura `DriftStateProvider`/`DriftSignal` actualizado por `CaptureSession` con los `DriftUpdate` existentes, sin modificar `DriftStateMachine`; `GoalAlertService` difiere avisos mientras hay deriva, los entrega al terminar o al tope, y registra una observación posterior; ajuste `alert_deferral_max_s` con efectivo/pendiente; V4 en `goal_alerts`; mensaje combinado por objetivo; textos de resultado en la pantalla Objetivos. Sin cambios en `Segmenter`, `PrivacyFilter`, segmentos ni episodios.
- **V4 (crítico):** columnas `notified_at`, `observed_until`, `outcome_at` y `outcome_after_ms`. La migración V3→V4, en la misma transacción, ejecuta `UPDATE goal_alerts SET notified_at = triggered_at WHERE notified_at IS NULL`, deja `outcome` y la observación nulos y no altera las demás tablas. Así las alertas de F6B-4/F6B-6 no se tratan como pendientes ni se envían al arrancar. Una base nueva nace en V4; una V4 no la abre el código anterior.
- **Ajuste:** `alert_deferral_max_s`, 300 s por defecto, rango 60–1800 s, validación y fallback visible sin sobrescribir lo guardado. Pausar/Reanudar conserva el valor de sesión; Detener/Iniciar relee el guardado. Sección **Avisos durante una deriva** en Ajustes.
- **Comportamiento:** un umbral se registra al cruzarse con `notified_at = NULL`. Se entrega cuando la deriva está inactiva o al llegar a `triggered_at + tope`. Un solo aviso por objetivo y evaluación agrupa los umbrales pendientes de sus métricas; se registran todos y el texto usa el umbral más alto por métrica, por ejemplo `Ajedrez: 100 % de entradas (4 de 4 entradas) y 80 % de minutos (8 de 10 min). Aviso del límite diario.`.
- **Arranque/pausa:** un pendiente de una ejecución anterior se cierra al arrancar con `outcome = DISCARDED` sin mostrarse. Pausar y reanudar dentro de la misma sesión lo entrega. Una observación abierta al cerrar, pausar, bloquear o suspender queda como `NOT_OBSERVED`; al arrancar también se cierran las observaciones huérfanas.
- **Observación de 2 min:** `NO_DRIFT` si no había deriva al enviar y no apareció ninguna; `STABLE` si había deriva y terminó, con `outcome_after_ms` desde el cierre; `NEW_DRIFT` si no había deriva y apareció una; `STILL_DRIFTING` si había deriva y seguía activa; `NOT_OBSERVED` si no se pudo observar. La resolución puede llegar hasta un ciclo de 30 s después de la ventana.
- **Textos exactos de UI:** «Al enviar el aviso no había deriva y no apareció ninguna en los 2 min siguientes.»; «Después del aviso, volvió a estabilidad en …»; «Después del aviso apareció una deriva durante la observación de 2 min.»; «Después del aviso, seguía habiendo deriva al terminar la observación de 2 min.»; «Después del aviso no se pudo observar el resultado: la captura se pausó o la aplicación se cerró.»; «Aviso no enviado: la aplicación se reinició antes de entregarlo.»; pendiente: «Aviso diferido: se enviará cuando termine la deriva o al llegar al máximo de espera.». Ninguno usa causalidad.
- **Automatizado:** `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify` -> **272 tests Java, 0 fallos/errores/omitidos** (251 previos + 21 nuevos), BUILD SUCCESS. `node --test extension/tests/report.test.mjs extension/tests/sampling.test.mjs extension/tests/background.test.mjs` -> **14 tests JS, 0 fallos** (extensión sin cambios).
- **Cobertura nueva (21):** `GoalAlertServiceTest` (+10): se difiere con deriva y se entrega al terminar; pausa/reanudación de la misma sesión entrega; el tope entrega aunque siga la deriva; `NO_DRIFT`, `STABLE` con tiempo, `STILL_DRIFTING` y `NEW_DRIFT`; reinicio descarta el pendiente con `DISCARDED`; interrumpir la observación guarda `NOT_OBSERVED`; bloqueo durante la espera no pierde el pendiente; dos topes del mismo objetivo generan un solo aviso combinado y tres alertas registradas. `SettingsServiceTest` (+3): 5 min por defecto, inválido visible sin sobrescritura y guardado separado con rangos. `AlertDeferralSettingsViewModelTest` (4): predeterminado, pendiente efectivo/guardado, rango y restaurar. `GoalsViewModelTest` (+1): pendiente y los textos exactos de los seis resultados, sin causalidad. `SQLiteGoalAlertRepositoryTest` (+1): guarda, reabre y resuelve entrega y observación. `SQLiteSchemaMigrationTest` (+2): V3→V4 conserva datos y fija `notified_at = triggered_at`; base V3 migrada no notifica al iniciar y no tiene observación pendiente.
- **Smoke de UI:** `powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f6b-5\settings.db -Smoke settings -Screenshot .\work\ui-preview\f6b-5\settings.png` -> carga Ajustes con el smoke existente, guarda los cuatro umbrales y termina con `UI shutdown: hilos deriva-capture vivos=0`; captura `settings.png`. `powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f6b-5\goals.db -Smoke goals -Screenshot .\work\ui-preview\f6b-5\goals.png` -> `objetivo creado=Ajedrez; estado=?  En curso; progreso=0 de 60 min · 0 de 4 entradas`, borra con confirmación y deja hilos 0; capturas `goals-creado.png` y `goals.png`. El smoke no imprime los campos nuevos de la sección de avisos; la verificación de sus textos es por ViewModel.
- **Privacidad/base:** todas las pruebas usan `@TempDir` o `work/ui-preview/f6b-5/`; no se abrió ni migró `work/console-day/deriva.db`, SHA-256 `0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475`. Sin nube, IA, telemetría, URL, ruta, título ni consulta.
- **Límites:** los smokes recorren controles, no son clics humanos; no hubo una deriva real de 10 min ni una sesión real con deriva; los casos se prueban con reloj y deriva simulados. La migración de la base real la hará el usuario con respaldo (A41). El resultado posterior es una observación y no prueba causal. `SQLitePrivacyCheck` no cubre aún las tablas nuevas (F6B-7). F6.2 sigue PENDIENTE.

## E043 - 2026-11-24 - F6B-7: resumen diario y gate final de F6B

- **Alcance:** resumen «Resumen de hoy» dentro de cada tarjeta de **Objetivos**, sin pantalla ni escritura nueva; línea «Aviso …» por alerta con umbral, métrica, hora local y resultado A48; minutos justificados separados de los excedidos sin justificar. `SQLitePrivacyCheck` amplía la auditoría a `goals` y `goal_alerts`; el formulario de justificación muestra «Evita pegar direcciones o datos personales.». Sin cambios de esquema, `Segmenter`, `PrivacyFilter`, segmentos ni episodios. Decisiones A49 y A50.
- **Resumen/UI:** `powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f6b-7\summary.db -View goals -Screenshot .\work\ui-preview\f6b-7\summary.png` sobre datos sintéticos en base temporal: Ajedrez 10 min/4 entradas con 12 min usados, «Justificado: 2 min · Excedido sin justificar: 0 min», avisos 80/100 % con resultados A48 y motivo «Partida con un amigo»; VS Code 30 min con 35 min usados, «Justificado: 0 min · Excedido sin justificar: 5 min», aviso 100 % y estado Sin responder. `powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f6b-7\goals-final.db -View goals -Smoke goals -Screenshot .\work\ui-preview\f6b-7\goals-final.png` crea y borra Ajedrez y termina con `hilos deriva-capture vivos=0; deriva-goals vivos=0`. Capturas `work/ui-preview/f6b-7/{summary.png,goals-final.png,goals-final-creado.png}`. Son recorridos de controles, no clics humanos.
- **Privacidad:** `SQLitePrivacyCheckTest` (7 tests): esquema actual con objetivos/alertas; columna prohibida en `goals` y en `goal_alerts`; `process_name` con URL y con ruta, y `site_host` con ruta/consulta, rechazados; justificación libre con URL/ruta aceptada y documentada como límite. `RunPrivacyCheck` sobre `work/f6b-7/gate.db` -> `SQLitePrivacyCheck OK`. Esquema: `goals(id, process_name, site_host, daily_minutes, daily_entries, passive, active, created_at, updated_at)` y `goal_alerts(id, goal_id, triggered_at, threshold_percent, metric, outcome, justification, justified_at, justified_excess, notified_at, observed_until, outcome_at, outcome_after_ms)`; ninguna columna de URL/ruta/título/consulta. Los datos de prueba y la corrida no contienen URL, ruta, consulta ni título.
- **Huella de hechos (paso controlado, sin captura):** `GoalsJustificationServiceTest.goalCreationAlarmAndJustificationKeepActivityTablesByteIdentical` calcula SHA-256 sobre todas las filas de `segment`, `drift_event` y `drift_context`, crea el objetivo, evalúa la alarma, justifica y quita la justificación; el hash es `76684c90cf393c5352ff66d4a00c12fd0adf0a24742b1b2ef6ebef0b43625fd1` antes y después de cada paso. No se compara a través de la corrida de 65 min, que sí escribe segmentos legítimos.
- **Migración de copia:** copia de `work/console-day/deriva.db` (V1, 14 segmentos, 0 episodios/contextos) a `work/f6b-7/gate.db`; `InitSchema` la abre y la deja en V4; `DbCheck` antes/después: `schema_version=1 -> 4`, `rows_segment=14`, `activity_sha256=715c1ba322859ca4a54bed5a9b1ba9461fc552a6154fdf5b2951ed458379d1c1` idéntico. La original conserva SHA-256 `0446AE5D39AC0A7841B6C345EBFA9A84DE842C3A30B28FF15D9EE37198B1E475` antes y después; no se migró la base real.
- **Corrida sostenida:** `powershell -NoProfile -ExecutionPolicy Bypass -File .\work\f6b-7\RunGate.ps1` sobre la copia migrada, con seis objetivos pasivos de 1 min (ChatGPT, Windows Terminal, Notepad, Explorer, VS Code y Java) y `passive_max_s=3600`; `capture-hold` ampliado solo como arnés a 3900 s. `Measure-UiResources.ps1` midió el PID de la UI cada 30 s, 16 CPUs lógicas, fórmula `100 * ΔCPU / Δwall / CPUs`. Resultado: 130 muestras, 3.900,006 s (65 min), fin `process-exited` al cerrar la UI; hueco máximo 96,056 s por suspensión S0 del sistema (eventos Kernel-Power 506/507), no por reinicio ni fallo de la app. Se registraron 8 avisos (80/100 % de minutos) en cuatro objetivos, todos con resultado `NOT_OBSERVED` por las fronteras de inactividad/suspensión; el usuario creó además un objetivo de Brave/YouTube, excluido sin bridge y sin avisos.
- **CPU y recursos:** CPU media 0,20 %, máxima 0,89 % (primera muestra, arranque), frente a 0,15 % de E030 en 2 h 7 min 30 s. Diferencia +0,05 puntos porcentuales; causa probable: seis objetivos y evaluación de alarmas cada 30 s con ocho avisos, carga que E030 no tenía. Working set 155,5 → 279,8 MB (máx. 422,2); privada 440,7 → 487,6 MB; hilos 63 → 65 (máx. 70); handles 531 → 708 (máx. 718). Por tercios: working set 338,7 / 393,4 / 311,1 MB; hilos 65,0 / 64,6 / 65,1; handles 658 / 702 / 710. No hay crecimiento sostenido de memoria ni de hilos; los handles suben desde el arranque en frío y se estabilizan.
- **Cierre limpio:** log de la UI `UI smoke: cierre de ventana con captura=Captura activa · sin bridge; navegadores excluidos.` y `UI shutdown: hilos deriva-capture vivos=0; deriva-goals vivos=0`. El PID 10336 de la corrida ya no existe; no queda ningún proceso de la corrida. El único `java.exe` restante es el PID 18972, una UI abierta antes de esta fase, visible y ajena a la medición.
- **Suite:** `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify` -> **278 tests Java, 0 fallos/errores/omitidos** (272 base + 6 nuevos: 4 de privacidad, 1 de huella de hechos y 1 de resumen), BUILD SUCCESS. `node --test extension/tests/report.test.mjs extension/tests/sampling.test.mjs extension/tests/background.test.mjs` -> **14 tests JS, 0 fallos**.
- **Artefactos:** `work/f6b-7/gate-resources.csv` (130 muestras), `gate-ui.out`, `gate-ui.err`, `gate.db`, `DbCheck.java`, `RunGate.ps1`, `AnalyzeGate.ps1`, `KeepGateWindow.ps1`, `SeedSummary.java`; `work/ui-preview/f6b-7/` con capturas. Todo bajo `work/` e ignorado por Git.
- **Límites:** una máquina y una corrida real con actividad del usuario; el hueco de 96 s por S0 impide llamar «sin cortes» a la muestra, aunque la duración supera 1 h. El resumen y los smokes son recorridos de controles, no clics humanos; la bandeja no se verificó con clic humano y el mensaje de dos topes juntos solo con notificador falso. P1 sigue resuelto solo en objetivos pasivos; el progreso usa segmentos cerrados y cuenta hasta 30 s tarde; el máximo pasivo es fijo por sesión; los sitios pueden subcontarse por exclusión ante duda; la migración de la base real la hará el usuario con respaldo. F6.2 sigue PENDIENTE.

## E044 — 2026-11-25 — UI-VENTANA: marco nativo accesible al arrancar

- Cambio: DerivaApp dimensiona la ventana exterior con Screen.getPrimary().getVisualBounds(), máximo 1440x900 y 94 % del área útil; centra y acota mínimos. Sin cambios de CSS, datos, captura ni bandeja.
- Compilación: `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress -DskipTests package` → BUILD SUCCESS, tests omitidos deliberadamente para este ajuste de geometría. La shell imprimió «Acceso denegado» antes de Maven, pero compilación y empaquetado finalizaron con código 0.
- Sonda temporal: `work/ui-window/WindowProbe.java`, carga DerivaApp real y comprueba Stage decorado/redimensionable, título, visibilidad y límites exteriores; cierra sola. Compilación: `$probeDeps = (Get-Content work/ui-preview/classpath.txt -Raw).Trim(); javac -encoding UTF-8 -cp "target/classes;$probeDeps" work/ui-window/WindowProbe.java`.
- Ejecución: `$probeDeps = (Get-Content work/ui-preview/classpath.txt -Raw).Trim(); $env:DERIVA_UI_DB='work/ui-window/probe.db'; $env:DERIVA_UI_SMOKE='window-probe'; java -cp "work/ui-window;target/classes;$probeDeps" WindowProbe` → código 0, `WINDOW x=48.0 y=24.8 w=1440.0 h=767.2 scale=1.25 style=DECORATED resizable=true`; área útil 1536x816; `PASS: decorated window entirely inside work area`; `UI shutdown: hilos deriva-capture vivos=0; deriva-goals vivos=0`.
- Límites: comprobación programática sobre ventana JavaFX real, sin clic/arrastre humano, sin otros monitores o escalas. Advertencia habitual de JavaFX por clases cargadas desde unnamed module. Solo base temporal, sin abrir base real ni cerrar instancias del usuario. F6.2 sigue pendiente.

## E045 — 2026-11-25 — UI-OBJETIVOS-RESUMEN
- Petición: Resumen de hoy plegable por tarjeta y retirar las burbujas de Sin responder y subconteo. TitledPane nativo, cerrado por defecto, flecha terracota y foco visible. Balance y avisos solo en el desplegable; conserva apertura por id al reconstruir tarjetas y elimina ids borrados. Sin cambios de cálculos, persistencia ni paleta.
- `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress -DskipTests package` → BUILD SUCCESS, tests de suite omitidos para cambio de presentación. Mensaje previo de shell «Acceso denegado» sin fallo de Maven.
- Sonda temporal con tarjeta sintética, sin base ni ventana visible: `$summaryDeps = (Get-Content work/ui-preview/classpath.txt -Raw).Trim(); javac -encoding UTF-8 -cp "target/classes;$summaryDeps" work/ui-window/GoalsSummaryProbe.java; if ($LASTEXITCODE -eq 0) { java -cp "work/ui-window;target/classes;$summaryDeps" GoalsSummaryProbe }` → código 0, `PASS: initially collapsed; balance and two alerts preserved; expands/collapses; refresh retains state; both bubbles absent; CSS loaded`.
- Límites: sonda JavaFX programática, no clic humano ni captura visual. Advertencia habitual unnamed module. No se abrieron bases reales. F6.2 pendiente.

## E046 — 2026-11-25 — UI-AJUSTES-MENU
- Ajustes abre un menú 2x2: Detección, Actividad pasiva, Avisos durante una deriva y Valores y aplicación. Botones con foco visible y acceso por teclado; regreso «Todos los ajustes». Formularios conservados en memoria. Guardar/restaurar detección solo en su apartado; sus valores y validación siguen junto al formulario y son consultables en el cuarto apartado. No cambia persistencia ni reglas.
- `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress -DskipTests package` → BUILD SUCCESS. Suite omitida para navegación de presentación. Mensaje de shell «Acceso denegado» previo a Maven, sin fallo de compilación.
- `$menuDeps = (Get-Content work/ui-preview/classpath.txt -Raw).Trim(); javac -encoding UTF-8 -cp "target/classes;$menuDeps" work/ui-window/SettingsMenuProbe.java; if ($LASTEXITCODE -eq 0) { java -cp "work/ui-window;target/classes;$menuDeps" SettingsMenuProbe }` → código 0: PASS FXML/CSS, cuatro tarjetas, apertura de cada panel, inicio sin paneles, regreso y campo sin guardar conservado. Repetida ejecución tras acotar altura de tarjetas a 180 px.
- Inspección visual de snapshot JavaFX 1120x740 `work/ui-window/settings-menu.png`: cuatro tarjetas completas, sin scroll innecesario, títulos y descripciones legibles. Primera captura reveló altura excesiva; corregida antes de entrega.
- Límites: sonda programática con FXML real y datos de formulario en memoria, sin clic humano ni nueva prueba de escritura SQLite; base real no abierta. Advertencia habitual unnamed module. F6.2 pendiente.

## E047 — 2026-11-25 — USO-RANGOS
- Uso reemplaza el título y navegación Uso diario. Selector Día/Semana/Mes/Año/Personalizado; Desde/Hasta inclusivos. Semana lunes-domingo, mes/año calendario según fecha seleccionada; Hasta editable en Personalizado. Rango invertido muestra error y limpia resultados anteriores.
- DailyUsageService.forRange hace una consulta por solapamiento para todo el rango, recorta con DayWindow y conserva desglose de sitios dentro del navegador. forDay delega. Snapshot abierto incluido si hoy pertenece al rango, conserva deduplicación y no suma pendiente. Cobertura termina en ahora para periodos actuales/futuros. Sin cambios de esquema ni captura.
- Refresco de Uso pasa de 2 a 30 s y no añade un refresco automático mientras hay consultas pendientes; consultas fuera de FX. No se han medido años con millones de segmentos; el coste sigue siendo proporcional a los segmentos del intervalo.
- `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress verify` → BUILD SUCCESS, 281 tests, 0 fallos/errores/omitidos. Log work/ui-window/usage-verify-full.log. Primera ejecución: expectativa de texto antiguo corregida y 8 errores de sockets del sandbox; repetición autorizada fuera del aislamiento correcta.
- Tres tests nuevos: rango con recorte inicial, abierto y cobertura hasta ahora; rango de 71 h por DST, futuro y orden inválido; febrero bisiesto y semana que cruza año. Suite existente conserva pruebas SQLite y deduplicación.
- Sonda: `$deps=(Get-Content work/ui-preview/classpath.txt -Raw).Trim(); javac -encoding UTF-8 -cp "target/classes;$deps" work/ui-window/UsageRangeProbe.java; java -cp "work/ui-window;target/classes;$deps" UsageRangeProbe` → PASS FXML: week/year/custom dates and controls. Snapshot work/ui-window/usage-range.png revisado a 1120x740. Sin datos reales en la sonda y sin clic humano; no se abrió base del usuario.

## E048 — 2026-11-25 — Día vuelve a hoy
- UsageController: al cambiar el periodo a DAY, establece LocalDate.now() y recalcula ambos extremos. Cambiar manualmente Desde sigue permitiendo consultar cualquier día.
- `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress -DskipTests package` → BUILD SUCCESS; sin repetir suite por cambio localizado de UI.
- `$deps=(Get-Content work/ui-preview/classpath.txt -Raw).Trim(); javac -encoding UTF-8 -cp "target/classes;$deps" work/ui-window/UsageRangeProbe.java; if ($LASTEXITCODE -eq 0) { java -cp "work/ui-window;target/classes;$deps" UsageRangeProbe }` → código 0, PASS: YEAR to DAY resets to local today; manual date remains selectable. Sonda FXML real programática, sin clic humano ni base real; advertencia habitual unnamed module.

## E049 — 2026-11-25 — AJUSTES-AYUDA
- Tres tarjetas. Explicaciones de Detección, Actividad pasiva y Avisos plegadas bajo Más información; Valores y avisos y Cómo se aplica pasan a desplegables dentro de Detección. Cerrados inicialmente. Errores y cambios sin guardar permanecen junto al botón Guardar.
- `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress -DskipTests package` → BUILD SUCCESS. Sonda: `$deps=(Get-Content work/ui-preview/classpath.txt -Raw).Trim(); javac -encoding UTF-8 -cp "target/classes;$deps" work/ui-window/SettingsMenuProbe.java; java -cp "work/ui-window;target/classes;$deps" SettingsMenuProbe` → código 0, comprobaciones de tres tarjetas, navegación, borrador conservado y TitledPane cerrados correctas. La salida conserva texto histórico «four cards», pero la aserción se actualizó a tres.
- Captura work/ui-window/settings-help.png revisada; explicaciones iniciales agrupadas en un único desplegable tras la revisión. Sonda repetida después. Sin base real, sin clic humano ni suite completa; cambio solo de presentación.

## E050 — 2026-11-25 — OBJETIVOS-HISTORIAL
- Esquema V5: `goals.archived_at`, índices únicos solo sobre objetivos no archivados, tabla `goal_history` alimentada por triggers de INSERT/UPDATE y revisión base (`baseline=1`) al migrar; borrar archiva en lugar de eliminar y `listPending` ignora archivados. Objetivos: selector de día, días pasados de solo lectura (sin editar, guardar ni justificar), configuración vigente ese día y líneas de revisiones; aviso de que lo borrado antes de V5 no se recupera.
- Tests nuevos: `SQLiteGoalRepositoryTest.keepsEveryConfigurationRevisionAndTheArchiveState`, `GoalsViewModelTest.pastDaysAreReadOnlyAndShowTheConfigurationUsedAndTheChanges`, `GoalsViewModelTest.historyKeepsAnArchivedGoalOnItsLastDayAndHidesItAfterwards`; `SQLiteSchemaMigrationTest` comprueba que las migraciones terminan en la versión 5, sin aserción propia sobre `goal_history` ni la revisión base.
- `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --batch-mode --no-transfer-progress -o verify` (2026-11-25, sesión de Claude Code) → 284 tests Java, 0 fallos, **8 errores** en `LocalBridgeServerTest` («Unable to establish loopback connection» al construir `HttpClient`), también sin sandbox. El resto (276) en verde, incluidos GoalsViewModelTest (18) y SQLiteGoalRepositoryTest (11). Igual que en E047, se atribuye al entorno sin sockets locales; falta repetir en una terminal normal.
- Límites: sin sonda FXML ni captura de la vista histórica, sin clic humano ni migración de la base real (la hará el usuario con respaldo). No cierra F6.2.

## E051 — 2026-11-25 — OBJETIVOS-RANGO
- `GoalsService.history(from, to, zone)` calcula cada día con la última revisión anterior al fin del día y congela el objetivo borrado en el borrado; `history(day)` reutiliza ese cálculo. ViewModel: `selectRange`, `setShowArchived`, filas de periodo con detalle por día; FXML con Desde/Hasta, Hoy y «Mostrar borrados».
- Tests nuevos en `GoalsViewModelTest`: `deletedGoalStaysVisibleReadOnlyAndCanBeHidden` (sustituye a `deletesTheGoalAndClearsTheList`, cuyo comportamiento cambió a petición del usuario), `periodSummarizesEachDayWithItsOwnConfigurationAndKeepsDeletedGoals`, `rejectsInvertedOrTooLongPeriodsAndKeepsTheSelection`. `-Dtest=Goals*Test,SQLiteGoal*Test` → 86 tests, 0 fallos (GoalsViewModelTest 20).
- `mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --batch-mode --no-transfer-progress -o verify` → 286 tests, 0 fallos, 8 errores de loopback en `LocalBridgeServerTest` (igual que en E050; código del bridge sin cambios respecto a `main`). 278 en verde.
- Sonda `work/ui-window/GoalsRangeProbe.java` (FXML real, base SQLite temporal, reloj fijo 24/11): hoy 1 tarjeta; periodo 20–24/11 con 2 tarjetas, el borrado con etiqueta «Borrado · solo lectura», sin botones y «Detalle por día»; al desmarcar «Mostrar borrados» queda 1 tarjeta y el resumen dice «1 borrado oculto»; Desde>Hasta muestra el error → `PASS`, código 0. Capturas `work/ui-window/goals-range.png` y `goals-range-hidden.png` revisadas; la nota de solo lectura se cortaba y se corrigió con `minHeight`.
- Límites: sin clic humano ni base real; el registro de cambios del periodo sigue listando los cambios de objetivos borrados aunque estén ocultos; rendimiento con un año de segmentos reales no medido.
- Confirmación del usuario (2026-11-25 12:54, cmd en su equipo): `mvn "-Dmaven.repo.local=C:\AppTImeControl\work\m2" --batch-mode --no-transfer-progress -o verify` → **286 tests, 0 fallos, 0 errores, BUILD SUCCESS**. Los 8 errores de `LocalBridgeServerTest` de E050/E051 eran del entorno de la sesión.

## E052 — 2026-11-25 — F6.2 entrega local reproducible
- Clon limpio 1 (`git clone --branch f6-2-entrega`, carpeta vacía del scratchpad, `work\m2` vacío, descarga desde Maven Central): `clean verify` → 286 tests, 278 en verde y 8 errores de loopback de `LocalBridgeServerTest` del entorno de la sesión; 45 s. Hallazgo: `Start-Ui.ps1 -Demo` falló con «No se pudo resolver el classpath de Maven» porque `maven-dependency-plugin` no se descarga en `verify` y el script lo invocaba `--offline`. Corregido (A58).
- Clon limpio 2 tras la corrección: `mvn "-Dmaven.repo.local=$PWD\work\m2" --batch-mode --no-transfer-progress clean verify '-Dtest=!LocalBridgeServerTest'` → 278 tests, 0 fallos, BUILD SUCCESS, `target\classpath.txt` generado; `node --test extension/tests/report.test.mjs extension/tests/sampling.test.mjs extension/tests/background.test.mjs` → 14 pass, 0 fail; `java -jar target\deriva-0.1.0-SNAPSHOT.jar` → mensaje de arranque, código 0; `powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Demo -View goals|usage -Screenshot …` → captura guardada y «UI shutdown: hilos deriva-capture vivos=0; deriva-goals vivos=0», código 0. Captura de Objetivos revisada (Desde/Hasta, Hoy, Mostrar borrados).
- Carpeta del proyecto con la caché existente: `verify --offline '-Dtest=!LocalBridgeServerTest'` → 278, BUILD SUCCESS, `target\classpath.txt` con 11 entradas.
- Base real: `work/f6/deriva-f6.db` ya estaba en V5 (2374 segmentos 22–25/11, 3 objetivos uno de ellos archivado, 12 avisos, 15 episodios), leída en solo lectura. Respaldo `work/respaldos/deriva-f6-2026-11-25-v5.db` con la API de backup de SQLite; `integrity_check` ok.
- Límites: los 8 tests del bridge no se ejecutaron con el `pom.xml` nuevo fuera del sandbox (la ejecución 286/286 del usuario fue con el pom anterior; el cambio solo añade un plugin en `package`). Sin clic humano; sin prueba en otro equipo ni con otra versión de JDK/Maven; la extensión real no se recargó en esta entrega.
- Confirmación del usuario (2026-11-25 13:07, cmd en su equipo, rama `f6-2-entrega` con el pom nuevo): `clean verify` → **286 tests, 0 fallos, 0 errores, BUILD SUCCESS**; «Wrote classpath file C:\AppTImeControl	arget\classpath.txt». Cubre el límite anterior de los 8 tests del bridge.

## E053 — 2026-11-25 — ENTREGA-FINAL (ejecutable, instalador, capturas y documentos)
- Terminal del usuario (pestaña del panel, fuera del sandbox): `mvn "-Dmaven.repo.local=C:\AppTImeControl\work\m2" --batch-mode --no-transfer-progress clean verify` → **290 tests, 0 fallos, 0 errores, BUILD SUCCESS** (incluye `LocalBridgeServerTest` 8/8 y `AppPathsTest` 4/4). `node --test extension/tests/...` → 14 pass.
- `scripts/Build-Package.ps1` → `Deriva-0.1.0-portable.zip` (79,7 MB) y `Deriva-0.1.0-Setup.exe` (78,6 MB); app-image de 175 MB. `Deriva.exe` con `LOCALAPPDATA` apuntando a una carpeta temporal: arranca, crea `<LOCALAPPDATA>\Deriva\deriva.db` en V5, captura guardada y código 0.
- Instalador: extracción con `/C /T:` → install.cmd, install.ps1, uninstall.ps1, version.txt, Deriva-app.zip. `install.ps1 -Quiet` con LOCALAPPDATA temporal → Deriva.exe instalado, accesos directos de escritorio y menú Inicio, clave HKCU Uninstall (DisplayVersion 0.1.0); la app instalada arranca y crea su base; `uninstall.ps1 -Quiet` quita programa, accesos y clave y conserva la base. Se comprobó antes que no existían accesos ni clave «Deriva» del usuario; el escritorio quedó como estaba.
- Capturas del manual: `work/entrega/ManualShots.java` siembra una base sintética (UiDemoData + 6 días de uso, objetivos con revisión de tope, avisos con resultado, justificación y un objetivo borrado) y recorre la app real con sus controles; 24 capturas revisadas. Sin datos reales del usuario.
- Documentos generados con Word (COM) desde `work/entrega/docs/*.ps1` y revisados como PDF renderizado con Windows.Data.Pdf: código fuente 197 págs., para ejecutar 6 págs., manual de uso 26 págs.
- Corrección encontrada al preparar el manual: el diálogo «Borrar registros» no mencionaba que también borra los avisos de objetivos (sí lo hacía el código); texto corregido.
- Límites: instalador sin firma digital ni clic humano en sus diálogos (probado en modo `-Quiet`); SmartScreen no se reprodujo; bridge.properties probado por test, no con una extensión real en la app empaquetada.
