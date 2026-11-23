# DERIVA — Plano de producto y arquitectura

> Documento base para construir la app con Codex, módulo por módulo.
> **Regla principal:** este plano es la fuente de verdad. No cambies el esquema de base de datos, las definiciones ni los nombres de interfaces sin preguntar primero.

---

## 0. Cómo usar este documento (instrucciones para Codex)

1. Lee todo el documento antes de escribir código.
2. Construye **un módulo por vez**, en el orden de la sección 12.
3. No agregues dependencias fuera de las listadas en la sección 4.
4. No inventes funciones que no estén aquí. Si algo es ambiguo, elige la opción más simple, déjala anotada en un comentario `// DECISIÓN:` y sigue.
5. Toda la lógica de la capa `analysis` y `coach` debe ser **pura y testeable** (sin JavaFX, sin base de datos, con `Clock` inyectado).
6. Antes de dar un módulo por terminado, ejecuta sus tests y verifica sus criterios de aceptación (sección 12).
7. Antes de empezar, haz la **revisión de innovación** de la sección 3 y entrégala como un archivo aparte (`REVISION_INNOVACION.md`).

---

## 1. Resumen del producto

**Deriva** es una app de escritorio para Windows que mide la **fragmentación de la atención** de una persona. No dice "usaste Chrome 3 horas"; dice **cuánto te costó cada interrupción**.

- **Propuesta en una frase:** *"Deriva: mide cuánto te cuesta distraerte. Local, sin cuenta y sin clasificar tus apps."*
- **Usuario objetivo:** una persona que trabaja o estudia en su PC y quiere entender su propia atención (no es una herramienta de supervisión para jefes).
- **Restricciones de diseño:**
  - 100 % local. Ningún dato sale del equipo. Sin servidor, sin cuenta.
  - Gratis. Sin APIs de pago.
  - **Sin IA ni LLM.** Se usa estadística simple sobre los datos del propio usuario ("analítica adaptativa local"). Debe correr bien en un PC con pocos recursos.
  - Bajo consumo: muestreo de 1 s, escritura por lotes, sin consultas pesadas en el hilo de UI.
  - Plazo de desarrollo: 6 días.

---

## 2. Un día en la vida del usuario (guion de uso)

| Momento | Qué pasa | Módulo |
|---|---|---|
| Primera vez | Onboarding de 4 pasos: horario laboral, apps excluidas, privacidad de títulos, conectar el navegador (opcional) | Onboarding |
| 8:00 | Windows inicia Deriva en la bandeja. El usuario no hace nada | Tracking |
| 10:42 | 10 cambios de ventana en 2 min. Aparece un aviso: "Llevas 2 min saltando entre 6 apps. ¿Qué pasaba?" con 5 motivos | Coach |
| 12:30 | Abre **Hoy**: índice 68, 3 bloques de foco, las interrupciones le costaron 41 min | Hoy |
| 15:00 | Abre **Interrupciones**: WhatsApp Web fue el disparador en 5 de 7 derivas | Interrupciones |
| 18:00 | Aviso de cierre: "Hoy: 2 h 10 min de foco real, mejor bloque 48 min" | Resumen |
| Domingo | En **Insights** ve su mejor franja (9–11) y la tendencia semanal | Insights |

---

## 3. Posicionamiento e innovación (REVISIÓN REQUERIDA)

### 3.1 Honestidad sobre la innovación
Deriva **no inventa** el seguimiento automático del tiempo. Es una **innovación incremental**: pasa de *medir tiempo* a *medir el costo de la fragmentación*, sin clasificar apps y sin salir del equipo. Así debe presentarse. **No se afirma respaldo científico** para el "costo de interrupción": es una medida propia, definida en la sección 7.

### 3.2 Competidores conocidos (hallazgos preliminares, NO exhaustivos)

| Herramienta | Lo que se encontró | Fuente |
|---|---|---|
| **ActivityWatch** | Código abierto y gratis, datos locales, registra app activa, título de ventana y estado AFK. Multiplataforma. Su arquitectura incluye un componente `aw-notify` (notificaciones). Es un colector genérico y extensible con timelines y categorías. Tiene un componente para navegador (`aw-watcher-web`) que registra el tiempo por sitio web | https://github.com/ActivityWatch/activitywatch |
| **RescueTime** | En la nube, con muchas funciones de foco en el plan de pago. Su "Distraction Alert" se dispara cuando alternas con demasiada frecuencia entre actividades productivas e improductivas durante el día (depende de categorías). Resumen semanal con mejores horas de foco | https://help.rescuetime.com/article/413-the-rescuetime-assistant · https://help.rescuetime.com/article/323-alerts-in-your-assistant-feed |
| **Rize** | macOS y Windows. Sesiones de foco, insights de cambio de contexto, bloqueador de distracciones, pausas e insights con IA | https://rize.io/features/productivity |
| **DeskTime / Time Doctor** | Orientados a equipos remotos, cobro por usuario al mes, enfoque de supervisión | https://desktime.com/blog/es/los-12-mejores-programas-de-seguimiento-automatico-del-tiempo-2026/ |

### 3.3 Lo que Deriva afirma como diferencial (a verificar)

1. **Costo de cada interrupción en minutos** (tiempo hasta recuperar un foco estable), sumado por día.
2. **Ranking de disparadores**: qué app **o sitio web** (YouTube, chess.com...) suele ser el primer salto que rompe tu foco, **sin** clasificarlo como productivo o distractor, con detección de sitios **local**: por una **extensión de navegador propia** (dominio exacto, sin salir del equipo) y, como respaldo, por el título de la ventana.
3. **Pregunta en el momento** ("¿qué pasaba?") y marca de **"justificada"** para no penalizar el cambio legítimo entre apps (ej. editor ↔ documentación).
4. **100 % local, sin cuenta y sin IA**, con títulos de ventana **ocultos por defecto**.

### 3.4 Tarea para el revisor (Codex)

Con tus propias fuentes y capacidad de búsqueda (si no tienes acceso a internet, dilo explícitamente y limítate a lo que sepas, marcando el nivel de certeza):

1. Verifica si las afirmaciones de la tabla 3.2 son correctas y están vigentes. Corrige lo que esté mal.
2. Verifica específicamente: ¿qué hace `aw-notify` de ActivityWatch? ¿Alguna herramienta existente (ActivityWatch y sus extensiones, RescueTime, Rize, Timing, Toggl, otras) ya ofrece alguno de los 4 puntos de 3.3? Indica cuál y en qué medida.
3. Busca al menos 3 alternativas adicionales de escritorio (open source o gratuitas) que no estén en la tabla.
4. Entrega `REVISION_INNOVACION.md` con: (a) afirmaciones confirmadas, (b) afirmaciones refutadas o dudosas, (c) solapamientos encontrados, (d) una recomendación de **redacción** del posicionamiento que sea defendible.
5. **No modifiques el modelo de datos ni la arquitectura por esta revisión.** Si hay solapamientos, ajusta únicamente el texto del posicionamiento y propón (sin implementar) mejoras.
6. No inventes fuentes. Si no puedes confirmar algo, escribe "no verificado".

---

## 4. Stack tecnológico

| Aspecto | Decisión |
|---|---|
| Lenguaje | Java 21 |
| UI | JavaFX 21 (LTS) con FXML + CSS |
| Build | Maven |
| Base de datos | SQLite vía `org.xerial:sqlite-jdbc` (modo WAL) |
| Sensor de ventana | JNA + `jna-platform` (Windows) |
| Tests | JUnit 5 |
| Logging | `java.util.logging` o `slf4j-simple` (uno solo) |
| Empaquetado | `jpackage` (instalador `.exe`/`.msi`) |
| Prohibido | Frameworks de inyección (Spring, Guice), ORMs (Hibernate), librerías de IA, servicios en la nube |

Usa las **últimas versiones estables** de cada dependencia al crear el proyecto y verifica que sean compatibles entre sí.

---

## 5. Arquitectura

### 5.1 Paquetes

```
com.deriva
├── app        // Main, AppContext (DI manual), configuración, logging
├── sensor     // Lectura del sistema operativo
├── tracking   // Muestras → segmentos
├── storage    // SQLite, repositorios, migraciones
├── analysis   // Lógica pura: cambios, foco, índice, costo, línea base
├── coach      // Detección de deriva en tiempo real + avisos
├── privacy    // Exclusiones, ocultar títulos, exportar, borrar
├── bridge     // Servidor local (127.0.0.1) que recibe el sitio activo desde la extensión del navegador
└── ui         // Vistas JavaFX (MVVM ligero)
```

### 5.2 Flujo de datos

```
WindowSensor ─► TrackingService ─┬─► SegmentRepository (SQLite, por lotes)
 IdleDetector                    └─► DriftDetector (en memoria) ─► DriftEvent ─► NotificationService
                                                                              └► DriftRepository
SegmentRepository ─► analysis (bajo demanda) ─► DailyMetric ─► ViewModels ─► Vistas
BrowserExtension ─► BridgeServer ─► BrowserStateStore ─► ExtensionSiteResolver ─┐
                                  TitleSiteResolver (respaldo) ───────────────────┴─► ChainSiteResolver ─► TrackingService
```

### 5.3 Hilos (importante)

- **Hilo de muestreo:** `ScheduledExecutorService` de 1 hilo, tick cada 1 s.
- **Hilo de escritura:** `ExecutorService` de 1 hilo; vacía una cola cada 10 s o al cerrar la app.
- **Hilo de análisis:** executor aparte; los cálculos nunca corren en el hilo de JavaFX.
- **Servidor del bridge:** `HttpServer` con executor propio de 1–2 hilos; solo actualiza en memoria un mapa concurrente (`ConcurrentHashMap`) con el último host por navegador. No toca la base de datos.
- **UI:** todo acceso a controles pasa por `Platform.runLater`.
- Mezclar JavaFX con la bandeja del sistema (AWT): usar `Platform.setImplicitExit(false)` o la app se cierra al ocultar la ventana.

### 5.4 Módulos e interfaces (contratos)

```java
// sensor
public record ActivitySample(Instant timestamp, String processName, String windowTitle, long pid, boolean idle) {}
public interface WindowSensor { ActivitySample sample(); }          // WindowsWindowSensor (JNA)
public interface IdleDetector { Duration idleTime(); }              // GetLastInputInfo
public interface SiteResolver { Optional<String> resolve(String processName, String windowTitle); } // etiqueta de sitio ('YouTube') o vacío; el título solo se usa en memoria
// SiteResolver: ChainSiteResolver(ExtensionSiteResolver, TitleSiteResolver)

// bridge
public record BrowserReport(String browser, String host, boolean incognito, Instant timestamp) {}
public interface BrowserStateStore { void update(BrowserReport r); void heartbeat(String browser, Instant ts); Optional<String> currentHost(String browser); boolean isConnected(String browser); }
public interface BridgeServer { void start(int port, String token); void stop(); boolean isRunning(); }

// tracking
public record Segment(long id, String processName, String site, String windowTitle, Instant start, Instant end, boolean idle) {}  // site: etiqueta ('YouTube') o null
public interface SegmentListener { void onSegmentClosed(Segment s); }
public interface TrackingService { void start(); void pause(Duration d); void resume(); void stop(); void addListener(SegmentListener l); }

// storage
public interface SegmentRepository { void saveAll(List<Segment> s); List<Segment> between(Instant from, Instant to); void deleteAll(); }
public interface FocusBlockRepository { void replaceForDay(LocalDate d, List<FocusBlock> b); List<FocusBlock> between(Instant from, Instant to); }
public interface DriftRepository { long save(DriftEvent e); void update(DriftEvent e); List<DriftEvent> between(Instant from, Instant to); }
public interface DailyMetricRepository { void upsert(DailyMetric m); Optional<DailyMetric> get(LocalDate d); List<DailyMetric> range(LocalDate from, LocalDate to); }
public interface SettingsRepository { String get(String key, String def); void set(String key, String value); }

// analysis (puro)
public interface SwitchCounter { SwitchStats count(List<Segment> s, Config c); }
public interface FocusBlockDetector { List<FocusBlock> detect(List<Segment> s, Config c); }
public interface FragmentationIndex { int compute(DayStats d, Baseline b, Config c); }
public interface RecoveryCostCalculator { long costSeconds(DriftEvent e, List<Segment> after, Config c); }
public interface BestWindowCalculator { Optional<HourRange> bestWindow(List<DailyHourStats> history, Config c); }
public interface BaselineService { Baseline compute(List<DailyMetric> last14Days); }

// coach
public interface DriftDetector { Optional<DriftEvent> onSegment(Segment s); }   // ventana deslizante en memoria
public interface NotificationService { void showDrift(DriftEvent e); void showDaySummary(DailyMetric m); }
```

`Config` es un objeto inmutable cargado desde `SettingsRepository` (sección 9). Todos los componentes de `analysis` y `coach` reciben un `Clock` por constructor para poder testear con tiempo simulado.

---

## 6. Base de datos (SQLite)

Archivo: `%APPDATA%\Deriva\deriva.db`. Al abrir la conexión: `PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON;`.
Los timestamps se guardan como **epoch en milisegundos** (INTEGER). Las fechas como `YYYY-MM-DD` (TEXT).

```sql
CREATE TABLE schema_version (
  version INTEGER NOT NULL
);

CREATE TABLE segment (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  process_name  TEXT    NOT NULL,           -- '__IDLE__' si es inactividad, '__EXCLUDED__' si la app está excluida
  site          TEXT,                       -- etiqueta de sitio web ('YouTube'); NULL si no es navegador
  site_source   TEXT CHECK (site_source IN ('EXTENSION','TITLE')),  -- de dónde salió el sitio
  window_title  TEXT,                       -- NULL por defecto (privacidad)
  start_ts      INTEGER NOT NULL,
  end_ts        INTEGER NOT NULL,
  duration_s    INTEGER NOT NULL,
  idle          INTEGER NOT NULL DEFAULT 0 CHECK (idle IN (0,1))
);
CREATE INDEX idx_segment_start ON segment(start_ts);

CREATE TABLE focus_block (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  start_ts      INTEGER NOT NULL,
  end_ts        INTEGER NOT NULL,
  duration_s    INTEGER NOT NULL,
  main_process  TEXT    NOT NULL,
  switch_count  INTEGER NOT NULL
);
CREATE INDEX idx_focus_start ON focus_block(start_ts);

CREATE TABLE drift_event (
  id               INTEGER PRIMARY KEY AUTOINCREMENT,
  start_ts         INTEGER NOT NULL,
  end_ts           INTEGER NOT NULL,
  switch_count     INTEGER NOT NULL,
  trigger_process  TEXT    NOT NULL,
  trigger_site     TEXT,                    -- sitio que inició la deriva, si aplica
  apps_json        TEXT    NOT NULL,        -- lista JSON de procesos implicados
  reason           TEXT CHECK (reason IN ('SEARCHING_INFO','MESSAGES','BREAK','BOREDOM','OTHER')),
  justified        INTEGER NOT NULL DEFAULT 0 CHECK (justified IN (0,1)),
  recovery_cost_s  INTEGER NOT NULL DEFAULT 0,
  answered_at      INTEGER
);
CREATE INDEX idx_drift_start ON drift_event(start_ts);

CREATE TABLE daily_metric (
  date                TEXT PRIMARY KEY,     -- YYYY-MM-DD
  attention_index     INTEGER NOT NULL,     -- 0..100
  active_seconds      INTEGER NOT NULL,
  focus_seconds       INTEGER NOT NULL,
  block_count         INTEGER NOT NULL,
  switch_count        INTEGER NOT NULL,
  drift_count         INTEGER NOT NULL,
  interruption_cost_s INTEGER NOT NULL,
  computed_at         INTEGER NOT NULL
);

CREATE TABLE setting (
  key   TEXT PRIMARY KEY,
  value TEXT NOT NULL
);

CREATE TABLE excluded_app (
  process_name TEXT PRIMARY KEY
);

CREATE TABLE site_rule (
  id        INTEGER PRIMARY KEY AUTOINCREMENT,
  site      TEXT NOT NULL,          -- etiqueta mostrada: 'YouTube'
  kind      TEXT NOT NULL DEFAULT 'TITLE' CHECK (kind IN ('TITLE','HOST')),
  keyword   TEXT NOT NULL,          -- TITLE: texto a buscar en el título (sin distinguir mayúsculas) · HOST: dominio o sufijo ('docs.google.com')
  user_made INTEGER NOT NULL DEFAULT 0
); -- la migración V1 inserta la semilla de la sección 7.1b

CREATE TABLE excluded_site (
  host TEXT PRIMARY KEY             -- host o sufijo a excluir, ej. 'mibanco.com.pe'
); -- visitas a estos sitios se guardan como '__EXCLUDED__'
```

**Migraciones:** una clase `Migrator` compara `schema_version` y aplica scripts numerados (`V1__init.sql`, ...). Solo existe V1 al inicio.
**Retención:** al arrancar, borrar `segment` con más de `retention_days` (por defecto 90). Las tablas `daily_metric` y `drift_event` se conservan.

---

## 7. Definiciones y algoritmos (fuente de verdad)

Todos los valores numéricos son **propuestas configurables** (sección 9), no verdades científicas.

### 7.1 Segmentación (`tracking`)
- Cada 1 s se obtiene un `ActivitySample`.
- **Ignorar el proceso propio:** si `pid == ProcessHandle.current().pid()`, el muestreo se ignora (continúa el segmento anterior). Esto evita que el aviso de Deriva contamine los datos.
- **Idle:** si `idleTime() > idle_threshold_s` (120 s), el estado pasa a `__IDLE__`.
- **Segmento:** tiempo continuo en el mismo **contexto** (proceso y, en navegadores, sitio). Se cierra al cambiar de contexto o de estado idle.
- **Debounce:** un segmento de menos de `min_segment_s` (3 s) se descarta y su tiempo se asigna al segmento anterior.
- **App excluida:** se guarda como `__EXCLUDED__`, sin nombre ni título. Cuenta como un proceso distinto para los cambios, pero no revela qué app era.
- **Título:** se guarda solo si `hide_titles = false`. Con `hide_titles = true`, el título **nunca** se escribe en la base de datos.

### 7.1b Detección de sitios web (clave del producto)

Objetivo: que Deriva diga **"estuviste en YouTube / chess.com"** y no solo "estuviste en Brave".

**Cadena de resolución (en orden):**
1. **Extensión de navegador** (fuente principal, dominio exacto; sección 14), si está conectada para el navegador activo.
2. **Título de ventana** (respaldo), si la extensión no está conectada.
3. Sin sitio (`site = NULL`) si el proceso no es un navegador.

**Fuente A — Extensión (principal):**
- `site` = etiqueta del host actual reportado por la extensión. Se busca una regla `HOST` (coincidencia exacta o por sufijo: `host == regla || host.endsWith("." + regla)`; gana la regla más larga). Si no hay regla, la etiqueta es el **host sin `www.`** (ej. `ok.ru`). `site_source = 'EXTENSION'`.
- El host **no se colapsa** a dominio registrable: `docs.google.com`, `mail.google.com` y `drive.google.com` son contextos distintos.
- Páginas internas del navegador (`chrome://`, `about:`, nueva pestaña) → `site = "Navegador (interno)"`.
- "Conectada" = el bridge recibió un reporte o latido de ese navegador en los últimos `extension_stale_s` (150 s).
- Hosts que coincidan con `excluded_site` → segmento `__EXCLUDED__`.


**Fuente B — Título de ventana (respaldo):**

- Navegadores reconocidos (por nombre de proceso): `chrome.exe`, `msedge.exe`, `firefox.exe`, `brave.exe`, `opera.exe`, `vivaldi.exe`.
- `SiteResolver` recibe el título de la ventana y, **solo en memoria**:
  1. Quita el sufijo del navegador (" - Google Chrome", " - Brave", " - Microsoft Edge", " — Mozilla Firefox"), con o sin nombre de perfil.
  2. Quita el prefijo de notificaciones `^\(\d+\)\s*`.
  3. Busca en `site_rule` la regla cuya `keyword` aparezca en el título (sin distinguir mayúsculas; gana la keyword más larga). Si hay coincidencia → `site`; si no → `"Otro sitio"`.
- **Ventanas privadas** (título con "InPrivate", "Incognito", "Private Browsing" o "navegación privada"): con `private_windows_excluded = true` el segmento es `__EXCLUDED__`.
- Si `site_detection = false` o el proceso no es un navegador, `site = NULL`.
- El debounce de `min_segment_s` aplica también a cambios de sitio.
- **Semilla de reglas (V1, editable; `user_made = 0`; cada sitio tiene una regla `TITLE` y una `HOST`):** YouTube, chess.com, Lichess, WhatsApp Web (`whatsapp`), Gmail, Google Docs, Google Drive, GitHub, Stack Overflow, Netflix, Facebook, Instagram, TikTok, Reddit, Twitch, Discord, Telegram, ChatGPT, Claude, Spotify, Wikipedia, LinkedIn, Notion, Canva.
- **Limitación honesta:** es una heurística por título. Los sitios cuyo título no incluya su nombre caen en "Otro sitio". Por eso la extensión (sección 14) es la fuente principal y el título solo el respaldo.

### 7.2 Cambio de contexto
Transición entre dos segmentos consecutivos **no idle** de **contextos** distintos. **Contexto** = `site` si existe; si no, `process_name`. Saltar de YouTube a Google Docs dentro de Chrome cuenta como un cambio; moverse entre páginas del mismo sitio, no.
- **Rebote:** volver al mismo contexto en menos de `bounce_s` (10 s). Un rebote cuenta como **un** cambio, no como dos.
- `active_seconds` = suma de segmentos no idle. `switch_rate` = cambios por hora activa.

### 7.3 Bloque de foco
Periodo continuo de al menos `focus_min_s` (900 s = 15 min) donde:
- en cualquier ventana móvil de 5 min hay como máximo 3 cambios, y
- no hay una pausa idle mayor a 120 s, y
- no está dentro de una deriva.

Se guarda `main_process` = contexto (sitio o app) con más tiempo dentro del bloque. Esta tolerancia de 3 cambios por 5 min evita penalizar el uso legítimo de varias apps.

### 7.4 Deriva (`coach`)
- **Disparo:** `drift_switches` (10) cambios dentro de una ventana deslizante de `drift_window_s` (120 s).
- **Enfriamiento:** no avisar de nuevo durante `alert_cooldown_s` (900 s = 15 min). Los eventos se registran igual, pero el aviso solo se muestra si pasó el enfriamiento.
- **Umbral adaptativo:** con **7 días o más** de datos, `drift_switches = max(6, ceil(P90))`, donde P90 es el percentil 90 de cambios por ventana de 120 s calculado solo sobre ventanas activas de los últimos 14 días. Con menos de 7 días se usa el valor por defecto.
- **`trigger_process` / `trigger_site`:** el primer contexto distinto al que estaba activo cuando empezó la ventana que disparó la deriva (si es un navegador, se guarda también el sitio).
- **`apps_json`:** contextos distintos (apps y sitios) involucrados en la ventana.
- **Fin de la deriva:** cuando hay un segmento (o segmentos consecutivos del mismo proceso) de al menos `stable_s` (180 s) sin nuevos cambios.

### 7.5 Costo de interrupción (diferenciador)
`recovery_cost_s` = tiempo desde el inicio de la deriva (primer cambio de la ventana que la disparó) hasta el inicio del periodo estable de `stable_s`. **Tope:** `recovery_cap_s` (1800 s = 30 min). Si el día termina o hay idle largo antes de estabilizarse, se calcula hasta la última actividad, con el mismo tope.
`interruption_cost_s` del día = suma de `recovery_cost_s` de las derivas con `justified = 0`.

### 7.6 Disparadores (diferenciador)
Ranking por contexto de disparo (`trigger_site` si existe, si no `trigger_process`): número de derivas (no justificadas) iniciadas por esa app o sitio y minutos de costo acumulados, en el rango de fechas elegido. No requiere clasificar apps.

### 7.7 Índice de atención (0–100)
Pesos fijos por defecto **50 / 30 / 20** (constantes configurables `w_focus`, `w_switch`, `w_drift`; deben sumar 1).

```
focus_score  = 100 * clamp(focus_seconds / active_seconds, 0, 1)

r            = switch_rate_hoy / baseline_switch_rate
switch_score = 100 * clamp((2 - r) / 1.5, 0, 1)         // r<=0.5 -> 100 ; r>=2 -> 0

drifts_ph    = drift_count / (active_seconds / 3600)
drift_score  = 100 * clamp(1 - drifts_ph / 2, 0, 1)     // 2 derivas por hora activa -> 0

attention_index = round(w_focus*focus_score + w_switch*switch_score + w_drift*drift_score)
```

- Si `active_seconds < 1800` (menos de 30 min), el índice se muestra como "datos insuficientes" y no se guarda.
- **Línea base:** `baseline_switch_rate` = mediana de `switch_rate` diario de los últimos 14 días con al menos 1 h activa. Con menos de 7 días de datos se usa `default_baseline_switch_rate` (30 cambios/hora, una suposición inicial a validar con datos reales).

### 7.8 Mejor franja
Para cada hora del día (0–23): promedio de `focus_seconds` dentro de esa hora, entre los días que tuvieron al menos 20 min activos en esa hora. Se elige la ventana de 2 horas consecutivas con mayor promedio. **Se muestra solo si hay al menos 3 días válidos** en esa franja; si no, "datos insuficientes".

### 7.9 Resumen diario
`DailyMetric` se recalcula cada 60 s para el día actual mientras la vista Hoy está abierta, y una sola vez al cerrar cada día (en el arranque se completan los días pendientes).

---

## 8. Vistas y atributos

### V1. Onboarding (solo primera ejecución, 4 pasos)
1. Horario laboral: hora inicio, hora fin, días de la semana.
2. Apps excluidas: lista de procesos en ejecución para marcar.
3. Privacidad: interruptor "Guardar títulos de ventana" (**apagado** por defecto) y aviso "todo se guarda solo en tu equipo".
4. (Opcional) Conectar el navegador: pasos para cargar la extensión y token. Se puede omitir y hacer después desde Ajustes.

### V2. Hoy (pantalla principal)
- Tarjeta **Índice de atención** (0–100) y variación frente a ayer.
- Tarjeta **Costo de interrupciones** (minutos).
- Tarjetas: tiempo de foco real, nº de bloques, nº de cambios, nº de derivas.
- Gráfico de barras: **cambios por hora**.
- Lista **Dónde se fue tu atención**: top 4–5 contextos (sitios y apps) por tiempo activo del día. (La lista de bloques de foco pasa a la Línea de tiempo.)
- Estado del tracking (activo / pausado hasta HH:mm) y botón Pausar.

### V3. Línea de tiempo
- Selector de fecha.
- Barra horizontal del día: segmentos por contexto (app o sitio), bloques de foco resaltados (el tooltip muestra inicio, duración y contexto principal), derivas con marcador.
- Tooltip: app, título (si está permitido), duración.
- Filtro por app.

### V4. Interrupciones (vista estrella)
- Tabla de derivas: hora, nº de cambios, apps y sitios, **motivo**, **costo en minutos**, casilla **Justificada**.
- Ranking de **disparadores** (app o sitio, nº de derivas, minutos de costo).
- Filtros: rango de fechas, motivo.
- Total de costo del rango, excluyendo justificadas.

### V5. Insights
- Línea del índice de atención (7 y 30 días).
- **Mejor franja** horaria (o "datos insuficientes").
- Mapa de calor día de la semana × hora (segundos de foco).
- Comparativa: esta semana vs la anterior (índice medio, foco, costo de interrupciones).

### V6. Ajustes
- Detección: `drift_switches`, `drift_window_s`, `alert_cooldown_s`, `focus_min_s`.
- Resumen diario: hora de envío y activar/desactivar.
- Privacidad: apps excluidas, ocultar títulos, **Exportar CSV**, **Borrar todos los datos** (con confirmación).
- Sitios web: activar/desactivar la detección, lista de reglas (sitio + palabra clave) con añadir y eliminar, opción "Excluir ventanas privadas", **Conectar navegador** (estado, token con botón copiar, puerto, pasos) y lista de **sitios excluidos**.
- Sistema: iniciar con Windows, pausar.

### V7. Componentes fuera de ventana
- **Aviso de deriva (toast propio de JavaFX):** ventana pequeña sin decoración, siempre visible, esquina inferior derecha, con texto corto y 5 botones de motivo (*Buscaba información, Mensajes, Descanso, Aburrimiento, Otro*) más "Ignorar". Se oculta sola a los 12 s. **No puede ser un globo de la bandeja de AWT, porque ese no admite botones.**
  Debe evitar robar el foco; el muestreo ignora el proceso propio (7.1) por si lo hace.
- **Menú de bandeja:** Abrir Deriva · Pausar 30 min · Pausar hasta mañana · Salir.
- **Aviso de cierre del día:** foco total, mejor bloque, costo de interrupciones.

---

## 9. Configuración por defecto (tabla `setting`)

| Clave | Valor | Descripción |
|---|---|---|
| `hide_titles` | `true` | No guardar títulos de ventana |
| `site_detection` | `true` | Detectar sitios web por título de ventana |
| `private_windows_excluded` | `true` | Ventanas privadas se registran como `__EXCLUDED__` |
| `bridge_enabled` | `true` | Activa el servidor local del bridge |
| `bridge_port` | `47821` | Puerto en 127.0.0.1 |
| `bridge_token` | (generado) | Token aleatorio de 32 bytes en hex, creado en el primer arranque |
| `extension_stale_s` | `150` | Segundos sin reporte/latido para considerar la extensión desconectada |
| `idle_threshold_s` | `120` | Inactividad |
| `min_segment_s` | `3` | Debounce |
| `bounce_s` | `10` | Rebote |
| `focus_min_s` | `900` | Duración mínima de bloque de foco |
| `drift_switches` | `10` | Cambios para disparar deriva |
| `drift_window_s` | `120` | Ventana de deriva |
| `stable_s` | `180` | Tiempo para considerar foco recuperado |
| `recovery_cap_s` | `1800` | Tope del costo por deriva |
| `alert_cooldown_s` | `900` | Pausa entre avisos |
| `w_focus` / `w_switch` / `w_drift` | `0.5` / `0.3` / `0.2` | Pesos del índice |
| `default_baseline_switch_rate` | `30` | Cambios/hora (suposición inicial) |
| `summary_time` | `18:00` | Resumen diario |
| `summary_enabled` | `true` | |
| `retention_days` | `90` | Retención de segmentos |
| `autostart` | `false` | Iniciar con Windows |
| `onboarding_done` | `false` | |

---

## 10. Privacidad (promesa del producto)

- Ningún dato sale del equipo. No hay red en ninguna parte de la app.
- Títulos de ventana ocultos por defecto: con el interruptor apagado, el título **no se escribe** en disco.
- El título se usa solo en memoria para reconocer el sitio; en disco queda únicamente la etiqueta ("YouTube"), salvo que el usuario active guardar títulos.
- Extensión de navegador: solo envía el **host** de la pestaña activa a `127.0.0.1`, con token. Nunca URL completa, ruta, parámetros ni título. Los sitios sensibles se pueden excluir (`excluded_site`).
- Apps excluidas: se registran como `__EXCLUDED__`, sin nombre ni título.
- Exportación a CSV de segmentos y derivas por rango de fechas.
- Borrado total: elimina todas las tablas de datos y reinicia la configuración (con confirmación).

---

## 11. Notas técnicas de Windows

- Ventana activa: `User32.GetForegroundWindow` → `GetWindowThreadProcessId` → `Kernel32.OpenProcess` → `QueryFullProcessImageName`. Si falla (procesos elevados o protegidos), usar `process_name = "desconocido"` sin lanzar excepción.
- Inactividad: `User32.GetLastInputInfo`.
- Inicio con Windows: clave `HKCU\Software\Microsoft\Windows\CurrentVersion\Run` con `Advapi32Util`.
- `WindowSensor` e `IdleDetector` van detrás de interfaces para poder sustituirlos por un `FakeSensor` en tests y, más adelante, por versiones de Linux/Mac.
- Nombre amigable del proceso: tabla opcional de equivalencias (ej. `chrome.exe` → Chrome); sin ella se muestra el nombre del ejecutable.

---

## 12. Plan de construcción y criterios de aceptación

### Orden (6 días)
1. **Día 1:** `app` + `storage` + `sensor` + `tracking`.
2. **Día 2:** `analysis` completo con tests.
3. **Día 3:** `bridge` (servidor + extensión Chromium) por la mañana; `ui`: tema, componentes y vista Hoy por la tarde.
4. **Día 4:** `ui` — Línea de tiempo; `coach` + aviso de deriva + bandeja.
5. **Día 5:** vista Interrupciones, Ajustes (incluye Conectar navegador), Onboarding, `privacy`.
6. **Día 6:** Insights si hay tiempo, `jpackage`, pruebas manuales, colchón.

**Si falta tiempo, recortar en este orden:** Insights → resumen diario → extensión de Firefox → Línea de tiempo.
**Mínimo presentable:** `sensor`, `tracking`, `storage`, `analysis`, `bridge` con la extensión Chromium, `coach`, Hoy, Interrupciones, bandeja.

### Criterios de aceptación por módulo
- **tracking:** con un `FakeSensor` y `Clock` simulado, una secuencia de muestras genera los segmentos esperados; segmentos < 3 s se absorben; el proceso propio se ignora; idle > 120 s genera segmento `__IDLE__`.
- **sitios web:** `"Video - YouTube - Google Chrome"` → `YouTube` (sin guardar el título); `"(3) WhatsApp - Brave"` → `WhatsApp Web`; `"Partida - Chess.com - Microsoft Edge"` → `chess.com`; título sin coincidencia → `Otro sitio`; ventana InPrivate → `__EXCLUDED__`; pasar de YouTube a Google Docs en el mismo navegador cuenta como un cambio; un proceso que no es navegador deja `site = NULL`.
- **bridge:** ver los criterios de 14.7.
- **storage:** crea V1 en un archivo nuevo; `saveAll` y `between` son consistentes; con `hide_titles = true` ningún `window_title` queda en disco.
- **analysis:** tests para (a) conteo de cambios con rebotes, (b) detección de bloque de foco con tolerancia de 3 cambios/5 min, (c) índice con valores límite (0 y 100) y con `active_seconds < 1800`, (d) costo de recuperación con tope, (e) línea base con menos de 7 días, (f) mejor franja con menos de 3 días.
- **coach:** con una secuencia de 10 cambios en 120 s se dispara exactamente una deriva; un segundo disparo dentro del enfriamiento no muestra aviso pero sí se registra; al guardar un motivo se actualiza `drift_event.reason` y `answered_at`.
- **ui:** ninguna operación de base de datos en el hilo de JavaFX; las vistas muestran estados vacíos ("aún no hay datos") sin errores.
- **general:** consumo de CPU en reposo menor al 2 % y memoria razonable en un equipo modesto (medirlo y anotarlo).

---

## 13. Fuera de alcance (no implementar)

IA o LLM, sincronización en la nube, cuentas de usuario, comunidad, bloqueo de sitios o apps, lectura de URLs completas, rutas o historial del navegador (la extensión solo envía el host), y publicación de la extensión en tiendas, versión móvil, versión para equipos o jefes.


---

## 14. Extensión de navegador y módulo `bridge` (parte del producto)

### 14.1 Objetivo
Obtener el **dominio exacto** de la pestaña activa (YouTube, chess.com, ok.ru...) sin depender del título de la ventana. Sigue siendo 100 % local: la extensión solo habla con `127.0.0.1`.
- Navegadores: **Chromium** (Chrome, Edge, Brave, Opera, Vivaldi) con una sola extensión. **Firefox** con el mismo código y un manifiesto propio (prioridad baja).

### 14.2 Protocolo del bridge
Servidor: `com.sun.net.httpserver.HttpServer` (JDK, sin dependencias), solo en `127.0.0.1`, puerto `bridge_port` (47821).

| Endpoint | Cuerpo | Respuesta |
|---|---|---|
| `GET /v1/ping` | — | `200 {"app":"deriva","v":1}` (sin token) |
| `POST /v1/tab` | `{"browser":"brave","host":"youtube.com","incognito":false,"ts":1760000000000}` | `204` |
| `POST /v1/heartbeat` | `{"browser":"brave","ts":1760000000000}` | `204` |

Los `POST` llevan la cabecera `X-Deriva-Token`. Códigos de error: `400` JSON inválido · `401` token inválido · `403` origen o host no permitido · `413` cuerpo mayor a 2 KB · `429` más de 20 peticiones por segundo.

Validaciones obligatorias:
- La cabecera `Host` debe ser `127.0.0.1:<puerto>` (evita DNS rebinding).
- `Origin` debe empezar por `chrome-extension://` o `moz-extension://`; se rechaza cualquier `http(s)://`.
- Token comparado en tiempo constante; sin CORS abierto.
- `browser` ∈ {chrome, edge, brave, opera, vivaldi, firefox, chromium}. Deriva lo asocia con el proceso activo: `chrome.exe`→chrome, `msedge.exe`→edge, `brave.exe`→brave, `opera.exe`→opera, `vivaldi.exe`→vivaldi, `firefox.exe`→firefox. Un reporte de otro navegador distinto al activo se ignora.
- El token son 32 bytes aleatorios en hex, generados en el primer arranque, guardados en `setting.bridge_token`; se muestra y copia desde Ajustes y se puede regenerar.

### 14.3 Extensión (carpeta `extension/` del repositorio)
```
extension/
├── src/background.js          // lógica compartida
├── src/options.html + options.js   // el usuario pega puerto y token
├── chromium/manifest.json     // Manifest V3 (service worker)
└── firefox/manifest.json      // Manifest V3 (background.scripts) + browser_specific_settings.gecko.id
```
- Permisos: `tabs`, `storage`, `alarms`; `host_permissions`: `http://127.0.0.1/*`. **Prohibido** pedir `<all_urls>`, `history`, `webRequest` o content scripts.
- Usar `const api = globalThis.browser ?? globalThis.chrome;`.
- Eventos: `tabs.onActivated`, `tabs.onUpdated` (cuando cambia la URL de la pestaña activa), `windows.onFocusChanged`.
- En cada evento: pestaña activa de la ventana enfocada → `new URL(tab.url).hostname` sin `www.`; si el protocolo no es `http(s)`, enviar `host = "(interno)"`. Enviar `POST /v1/tab` solo si el host cambió o cambió el foco de ventana.
- Latido: `alarms` cada 60 s → `POST /v1/heartbeat`.
- Si el POST falla (Deriva cerrado), no hay cola ni reintentos acumulados: se intenta de nuevo con el siguiente evento.
- Detección del navegador: Edge `Edg/` en el user agent; Opera `OPR/`; Vivaldi `Vivaldi`; Brave `navigator.brave?.isBrave()`; Firefox con `browser.runtime.getBrowserInfo`; si no, `chrome`.
- El service worker de MV3 se duerme: no guardar estado crítico en variables; leer el último host de `storage.local` al despertar.
- La extensión no se habilita en ventanas privadas (no pedir ese permiso). Esas ventanas las cubre el respaldo por título (7.1b), que las marca `__EXCLUDED__`.

### 14.4 Instalación (limitaciones reales)
- **Chrome, Edge, Brave, Opera:** modo desarrollador → "Cargar descomprimida" apuntando a `extension/chromium`. Chrome suele mostrar un aviso de extensiones en modo desarrollador al iniciar. Publicar en Chrome Web Store tiene una cuota única de registro, así que queda fuera de alcance.
- **Firefox:** las extensiones temporales (`about:debugging`) se pierden al reiniciar. Para uso permanente: firmarla como "no listada" en addons.mozilla.org (gratis) o usar Firefox Developer Edition.
- Deriva incluye en **Ajustes → Conectar navegador**: estado (conectada / no conectada), token con botón Copiar, puerto y 4 pasos. El Onboarding lo ofrece como paso 4 opcional.

### 14.5 Integración con el resto del sistema
- `ChainSiteResolver`: 1) `ExtensionSiteResolver` (usa `BrowserStateStore`), 2) `TitleSiteResolver`, 3) vacío.
- Las etiquetas de la semilla son iguales en ambas fuentes ("YouTube"); para sitios desconocidos el host (extensión) y "Otro sitio" (título) difieren, y ese salto cuenta como cambio. Es aceptable.
- El estado `isConnected` se expone a la vista Ajustes y a un indicador discreto en la barra lateral.

### 14.6 Privacidad
- Solo viaja el **host** por loopback. Nunca URL completa, ruta, parámetros, título ni contenido.
- El host no se escribe en logs. En disco solo queda `segment.site`.
- Sitios sensibles: el usuario los añade a `excluded_site` (ej. su banco) y se guardan como `__EXCLUDED__`.

### 14.7 Criterios de aceptación del bridge
- Con `curl`: `POST` sin token → 401; con token y `Origin: http://evil.com` → 403; con `Host` distinto → 403; cuerpo mayor a 2 KB → 413; `ping` → 200.
- Con la extensión cargada en Brave: abrir YouTube → `site = "YouTube"`, `site_source = "EXTENSION"`; abrir ok.ru → `site = "ok.ru"`; pasar de uno a otro cuenta un cambio; `docs.google.com` y `mail.google.com` son contextos distintos.
- Con Deriva cerrado, la extensión no muestra errores ni acumula peticiones.
- Al desconectar la extensión, en máximo 150 s Ajustes muestra "No conectada" y se usa el título.
- Una búsqueda de `http` en la base de datos y en los logs no encuentra URLs completas.
- `chrome://extensions` muestra únicamente los permisos de 14.3.
