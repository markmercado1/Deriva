# Deriva — espacio de trabajo

Proyecto local de Windows. El desarrollo está dividido en fases pequeñas y retomables.

**Inicio para una IA:** leer AGENTS.md y docs/ESTADO.md. **Mensaje para otra herramienta:** copiar CONTINUAR.md.

## Mapa de documentación

| Archivo | Responsabilidad |
|---|---|
| DERIVA_PLANO_v2.md | Requisitos y reglas funcionales vigentes |
| DERIVA_PLANO.md | Plano original; histórico, no modificar |
| AGENTS.md | Reglas de trabajo para agentes |
| CONTINUAR.md | Mensaje portable para iniciar otra sesión |
| docs/ESTADO.md | Punto actual y siguiente acción; actualizar cada entrega |
| docs/FASES.md | Tareas y criterios para cerrar cada fase |
| docs/ARQUITECTURA.md | Dependencias permitidas y patrones |
| docs/DECISIONES.md | Decisiones vigentes y asuntos por resolver |
| docs/PRUEBAS.md | Matriz de aceptación y evidencia de ejecución |
| docs/ENTREGAS.md | Historial corto de entregas |
| docs/Q2_GATE.md | Gate Q2 para decidir si se reabre el navegador |
| docs/DISENO.md | Paleta, tipografía y decisiones visuales de la UI |
| docs/INTEGRACION_MINIMA.md | Propuesta de captura real con salida por consola antes de F3.2 |

## Cómo avanzar

Cada sesión debe tomar una tarea pequeña, implementarla, verificarla y guardar el estado. Una fase puede necesitar varias sesiones. Se puede detener el trabajo entre tareas sin perder el plan.

La documentación es la memoria compartida. No es necesario entregar todo el historial del chat a otra IA ni releer la revisión comercial. Los pendientes se consultan en ESTADO y FASES, no se deducen de comentarios antiguos.

## Ejecución — base F1.1

Requisitos comprobados en este equipo: Windows 11 x64, Temurin JDK/javac 21.0.7, Maven 3.9.9 y Git 2.49.0. Deben estar en PATH. JAVA_HOME, si está definido, debe apuntar al JDK correcto. La primera compilación necesita Internet para Maven Central; la caché queda en work/m2, ignorada por Git.

Desde PowerShell:

```powershell
Set-Location -LiteralPath 'C:\AppTImeControl'
java -version
mvn -version
mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --batch-mode --no-transfer-progress clean verify
java -jar .\target\deriva-0.1.0-SNAPSHOT.jar
```

El JAR es únicamente el arranque de consola: informa que la captura está deshabilitada y termina. Todavía no tiene interfaz, sensor, extensión ni base de datos. No dejarlo ejecutándose para recopilar actividad: no registra nada.

Si trasladas el proyecto, cambia también la ruta absoluta de la caché Maven en el comando. No se incluye Maven Wrapper; usar la versión documentada de Maven. No se instalaron herramientas globales ni se modificó PATH.

### Estructura implementada

- pom.xml: Java release 21, UTF-8, plugins fijados y JUnit 5.11.4 de alcance test.
- src/main/java/com/deriva/app/Main.java: punto de entrada mínimo.
- src/test/java/com/deriva/app/BootstrapTest.java: arranque en otro proceso Java, salida correcta, terminación y directorio temporal sin archivos creados.
- target/: JAR y resultados de pruebas generados; ignorado por Git.

El resto de paquetes se creará cuando tenga implementación real. JavaFX/JNA/SQLite se incorporarán y fijarán al trabajar sus adaptadores, no como dependencias sin uso. Esto no cambia el stack acordado.

Git local inicializado, sin remoto y sin commits en F1.1. Los archivos existentes se conservaron; revisar git status antes de la siguiente tarea. No publicar datos de actividad ni la caché work/.

Estado actual: **F0–F5.2 y F6.1 HECHO; AJUSTES-DETECCION HECHO; F6.2 pendiente** (2026-11-23). GATE-Q2 PASS para Chrome + un perfil; F2.4 implementó la allowlist, F2.4b añadió Brave con gate reducido, F3.1 la máquina de deriva, F3.1b la captura mínima por consola, F3.2 la persistencia de episodios, F4.1 la pantalla de Interrupciones, F4.2 el rango por solapamiento, F4.3 Uso diario, F5.1 la captura real con borrado y cierre, F5.2 la pausa manual y la bandeja, F6.1 la medición sostenida y AJUSTES-DETECCION los cuatro umbrales de detección. Uso diario incluye el abierto confirmado como provisional y muestra el pendiente sin sumarlo. Con bridge se permite Chrome o Brave con un perfil; sin bridge los navegadores quedan excluidos. Ctrl+C no validado limpiamente (P2). El editor de exclusiones, el panel de bridge y F6.2 siguen pendientes.

## Piloto F1.2 validado (2026-11-22)

El bridge y la extensión están implementados como piloto diagnóstico, sin sensor ni BD. La ejecución normal del JAR sigue siendo el bootstrap de F1.1. Para arrancar el servidor y cargar la extensión, seguir [PILOTO_NAVEGADOR](docs/PILOTO_NAVEGADOR.md).

Código nuevo: bridge/BrowserReport y BrowserStateStore (Java puro); bridge/http/LocalBridgeServer y FlatJson (adaptador); app/BridgePilot (composición); extension/chromium (MV3); scripts/Start-BridgePilot.ps1 (arranque explícito). No cambió el stack ni se añadieron dependencias. Las opciones guardan solo puerto y token; reportes sin persistencia.

Pruebas automáticas: 11 Java + 5 JS pasadas. Validación real en Chrome (E004): línea base 5/5 NORMAL example.com; incógnito 12/12 UNKNOWN sin host; regreso 11/12 NORMAL example.com; reinicio con sesión nueva y 6/12 NORMAL example.com. F1.2 HECHO; BR-02 queda PARCIAL por segundo perfil, caducidad exacta y cierre ordenado con Ctrl+C, pendientes para F2.3. La captura de actividad sigue NO habilitada. No confundir estado del servidor “conectado” con garantía de privacidad o captura activa.

## Sensor F1.3 validado (2026-11-22)

JNA 5.19.1; `sensor.windows` lee ventana, proceso e inactividad reales. `PrivacyFilter` elimina identidad antes del tracking y mantiene los navegadores excluidos por D4. Probe real: WindowsTerminal.exe y SearchHost.exe con PID/ventana válidos; idle 63359 ms antes de entrada y 0 ms después. 26 tests Java pasando (7 nuevos de privacidad). Siguiente: F2.1 segmentación.

## Segmentación F2.1 validada (2026-11-22)

`ContextKey`, `Segment`, `StableTick`, `TickResult` y `Segmenter` puro. Debounce de 3 s, fronteras IDLE/EXCLUDED/UNAVAILABLE, huecos >3 s, reloj hacia atrás, A→B→A y contextos por proceso/host/subdominio. 44 tests Java pasando (18 nuevos). Sin bucle de muestreo, SQLite ni UI todavía. Siguiente: F2.2 SQLite y repositorios.

## Persistencia F2.2 validada (2026-11-22)

SQLite JDBC 3.54.1.0; esquema V1 completo con WAL/foreign_keys, `SegmentRepository` y adaptador con transacción por lote, lectura por solapamiento semiabierto y reapertura. Test de integración `FakeSensor → PrivacyFilter → Segmenter → SQLiteSegmentRepository`. 50 tests Java pasando (6 nuevos). Ruta de producción, escritor serializado/cola y UI pendientes. Siguiente: F2.3 decisión de navegador.

## Decisión de navegador F2.3 (2026-11-22)

GATE-Q2 **PASS** para **Chrome + un perfil** (A15/E011), basado en frescura de reportes y UNKNOWN al perder foco, no en correlación HWND; límite ~1 s aceptado. El resto de navegadores/perfiles sigue excluido. `PrivacyFilter` permite Chrome solo con un `BrowserStateProvider` fresco NORMAL; el constructor por defecto sigue excluyendo navegadores. La captura real no está activa todavía. 59 tests Java pasando.

## Gate Q2 — PASS (2026-11-22)

Segundo perfil repetido (UNKNOWN/no→NORMAL/yes), ventana privada, foco y reinicio reales; una hora con 13.680 muestras, `maxGapMs=1088`, `over3s=0`, 0 errores y 0 reinicios. Ctrl+C: primer intento falló y se cerró con Stop-Process; segundo detuvo el bridge; causa no investigada. Guía y resultado: [Q2_GATE](docs/Q2_GATE.md). Diagnóstico por reporte: `work/pilot/bridge-reports.log` (timestamp, seq, state, hostPresent; sin host).

## Chrome F2.4 en el filtro (2026-11-22)

`PrivacyFilter` permite `chrome.exe` y `brave.exe` solo si `BrowserStateStore` está fresco en NORMAL, propagando el host como contexto de sitio. UNKNOWN/STALE, idle, no disponible, proceso propio y todos los demás navegadores quedan EXCLUDED. 91 tests Java pasando (2 de Brave + 1 de resumen). La app aún no compone el proveedor ni tiene bucle/UI; la captura real existe solo en la consola diagnóstica. Siguiente: F3.1 máquina de deriva.

## Brave F2.4b — gate reducido (2026-11-22)

Brave quedó permitido con las mismas reglas que Chrome. La ventana privada pasó (UNKNOWN/no, sin NORMAL yes). Pendientes: segundo perfil, reinicio, una hora y la colisión de sesiones Chrome/Brave porque el payload de Brave dice `browser:"chrome"` y el bridge mantiene una sola sesión. **No ejecutes Chrome y Brave a la vez contra el mismo bridge.** Comando para Brave:

```powershell
powershell -NoProfile -File .\scripts\Start-ConsoleCapture.ps1 -ExtensionId cnipocakflhegoediialcdngmopbieip -Port 47821
```

La base es `work/console-day/deriva.db`.

## Deriva F3.1 validada (2026-11-22)

`ChangeCounter` y `DriftStateMachine` puros: 10 cambios en 120 s, episodio único, ventana exacta, estabilidad de 180 s por tick, candidato pendiente bloqueante y cierre UNCONFIRMED por IDLE/EXCLUDED/UNAVAILABLE. 77 tests Java pasando (18 nuevos). Sin persistencia de drift/UI/loop. Pendientes P1 (idle de navegador) y P2 (causa Ctrl+C) anotados. Siguiente: propuesta de integración mínima y luego F3.2.

## Consola F3.1b validada (2026-11-22)

`ConsoleCapture` conecta sensor→bridge/privacidad→segmentador→SQLite y `DriftStateMachine` en una sesión de consola. Guarda cada segmento al cerrarse, imprime drift sin persistirlo y al salir muestra un resumen separado por app, sitios, IDLE, EXCLUDED, UNAVAILABLE y sin registro desde el inicio de la sesión. Base por defecto: `work/console-day/deriva.db`. Stop file alternativo a Ctrl+C. `SQLitePrivacyCheck` verifica que no haya columnas URL/título ni hosts con ruta/query; UNKNOWN/privado no guarda host. 91 tests Java pasando, smoke real con stop file y captura real de Brave. F4.3/F5.1 NO están terminadas; esto es una rebanada de diagnóstico. Siguiente: F3.2.

```powershell
powershell -NoProfile -File .\scripts\Start-ConsoleCapture.ps1 -ExtensionId TU_EXTENSION_ID
```

## Persistencia de deriva F3.2 (2026-11-22)

`SQLiteDriftRepository` persiste OPENED/UPDATED/CLOSED, contextos y anotaciones. Las anotaciones no se sobrescriben al actualizar detección; los OPEN se recuperan como UNCONFIRMED/RESTART al arrancar y el episodio abierto se cierra como EXIT al salir. `ConsoleCapture` ya persiste los updates de drift. 97 tests Java pasando (6 nuevos). Sin borrado/retención en esa entrega; F4.1 añade después la UI de Interrupciones.

## Interrupciones F4.1/F4.2 (2026-11-22)

Pantalla JavaFX con tabla y panel de detalle, rango Desde/Hasta por solapamiento, filtros de estado/justificadas, anotaciones y estados vacío/error. Diseño en [DISENO](docs/DISENO.md). Lee la base persistida; no captura ni borra. Las consultas y el guardado corren fuera del hilo FX. 106 tests Java pasando (4 nuevos).

```powershell
# UI con datos reales de work/console-day/deriva.db
powershell -NoProfile -File .\scripts\Start-Ui.ps1

# UI con datos sintéticos de demostración (no toca la base real)
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Demo

# Base temporal y smoke de Guardar (diagnóstico, no clic humano)
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Demo -Database .\work\ui-preview\f4-2\persist.db -Smoke annotate

# Aviso de rango invertido (diagnóstico)
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Demo -Database .\work\ui-preview\f4-2\final-demo.db -Smoke invalid-range
```

El rango incluye episodios que cruzan medianoche sin duplicarlos. El inicio se muestra como `dd/MM HH:mm`. Uso diario está implementado en F4.3 y Ajustes tiene los cuatro umbrales de detección desde AJUSTES-DETECCION; el editor de exclusiones sigue pendiente. Capturas: `work/ui-preview/f4-2/rango.png`, `guardado.png`, `reabierto.png`, `vacio.png`, `error.png`, `rango-invalido.png`.

## Uso diario F4.3 (2026-11-22)

Totales de primer plano por fecha y por aplicación/sitio desde los segmentos persistidos. Recorta intervalos que cruzan medianoche, excluye IDLE/EXCLUDED/UNAVAILABLE y huecos, y muestra los sitios dentro de su navegador sin sumarlos dos veces. Distingue “Sin datos” de “0 s”. Con captura activa, el abierto confirmado se incluye como provisional y el pendiente se muestra sin sumarse; no se cierra el segmento para refrescar. 114 tests Java pasando (4 nuevos).

```powershell
# Uso diario con datos sintéticos
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Demo -Database .\work\ui-preview\f4-3\demo.db -View usage

# Uso diario con la base real persistida
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -View usage
```

Capturas: `work/ui-preview/f4-3/uso-diario.png`, `sin-datos.png`, `cero-uso.png`, `error-uso.png`. USE-02 queda PARCIAL por Dota/YouTube real y el segmento abierto; Brave, P1 y P2 siguen pendientes.

## Captura real y borrado F5.1a/F5.1b (2026-11-22)

La barra lateral tiene **Iniciar/Detener captura**, estado visible y **Borrar registros** con confirmación. La captura real corre en un hilo dedicado, persiste segmentos y episodios, y al cerrar la ventana detiene y espera la salida ordenada. Uso diario se refresca cada 2 s mientras está visible, sin forzar el cierre del segmento.

Borrar registros detiene la captura, espera el hilo, elimina segmentos/episodios/contextos y conserva configuración, token, exclusiones y esquema. Un fallo de SQLite detiene la captura con un error visible; no hay cola ilimitada. 115 tests Java pasando (1 nuevo).

```powershell
# Captura de aplicaciones; sin bridge los navegadores quedan excluidos
powershell -NoProfile -File .\scripts\Start-Ui.ps1

# Para Chrome + un perfil, configurar el bridge antes de arrancar
$env:DERIVA_EXTENSION_ID = 'TU_EXTENSION_ID'
$env:DERIVA_BRIDGE_TOKEN = 'TOKEN_DE_64_HEX'
$env:DERIVA_BRIDGE_PORT = '47821'
powershell -NoProfile -File .\scripts\Start-Ui.ps1

# Diagnóstico: sesión real de 10 s y cierre con captura activa
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f5-1\real.db -Smoke capture
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f5-1\close.db -Smoke capture-close

# Diagnóstico: borrado con confirmación y fallo SQLite (bases temporales)
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f5-1\delete-test.db -Smoke delete
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f5-1\failure.db -Smoke capture-failure
```

F5.1 se cerró después con E024: matriz de fallos/privacidad, cierre sin proceso oculto y bloqueo/suspensión S0. P1 (idle 120 s) y P2 (Ctrl+C de consola) siguen pendientes. Capturas: `work/ui-preview/f5-1/confirmacion.png`, `delete.png`, `delete-reopen.png`, `failure.png`, `failure-reopen.png`. Los smokes son diagnósticos, no clics humanos.

## Pausa, bandeja y rendimiento F5.2/F6.1 (2026-11-23)

La barra lateral añade **Pausar captura**; en pausa muestra `Captura en pausa.` y **Reanudar captura**. Pausar cierra el segmento/episodio abierto con motivo `PAUSE` y reanudar abre una sesión nueva sin solapes. Con bandeja disponible, cerrar la ventana la oculta y avisa; el menú ofrece **Abrir Deriva**, **Pausar/Reanudar captura** y **Salir**. Salir detiene, hace flush, retira el icono y termina sin proceso oculto. Si no hay bandeja, cerrar termina como antes.

```powershell
# Pausa y reanudación (diagnóstico, base temporal)
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f5-2\pause.db -Smoke pause

# Bandeja: pausa/reanudar, ocultar/reabrir y Salir (diagnóstico)
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f5-2\tray.db -Smoke tray

# Confirmación visible de Guardar sobre una copia
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\f5-2\annotate.db -Smoke annotate
```

Medición F6.1 aceptada: `work/f6/ui-resources.csv`, 256 muestras cada 30 s durante 2 h 7 min 30 s; CPU media 0,15 % (máx. 0,40 % de 16 CPUs lógicas), working set 249–416 MB, memoria privada ≤484 MB, hilos 67–70, con captura activa y Brave/bridge. Protocolo y límites en [F6_MEDICION](docs/F6_MEDICION.md); evidencia E030 en [PRUEBAS](docs/PRUEBAS.md). Capturas de UI de esta entrega: `work/ui-preview/f5-2/{guardado,pause-paused,pause,tray}.png`. Los smokes invocan los controles/callbacks, no son clics humanos.

## Ajustes de detección (2026-11-23)

Ajustes permite editar solo cuatro valores: cambios para abrir un episodio (`drift_switches`), ventana de cambios (`drift_window_s`), estabilidad (`stable_s`) e inactividad (`idle_threshold_s`). Predeterminados: **10 / 120 / 180 / 120**. Los rangos iniciales de producto son 4–30, 30–600 s, 30–900 s y 30–1800 s.

Guardar persiste en la tabla `setting`, pero no cambia la sesión en curso. **Pausar/Reanudar conserva** los valores de la sesión; **Detener/Iniciar** relee los guardados. La pantalla distingue **En uso ahora** de **Guardado, pendiente de aplicar**, avisa si inactividad ≤ estabilidad y, si la configuración guardada es inválida, usa el predeterminado afectado sin sobrescribirla. La consola diagnóstica `ConsoleCapture` conserva los predeterminados y no lee esta configuración. No incluye editor de exclusiones, bridge ni empaquetado. Evidencia E034.

```powershell
# Ver Ajustes con una base temporal
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\ajustes\defaults.db -View settings

# Smoke de guardado y reapertura
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\ajustes\settings-final.db -Smoke settings

# Smoke de la regla pausa/reanudación y detener/iniciar
powershell -NoProfile -File .\scripts\Start-Ui.ps1 -Database .\work\ui-preview\ajustes\session3.db -Smoke settings-session
```
