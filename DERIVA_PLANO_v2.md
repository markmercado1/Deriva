# DERIVA — Plano de producto y arquitectura v2

Fecha: 21 de noviembre de 2026.
Derivado de DERIVA_PLANO.md. El original se conserva sin modificaciones.
Esta versión sustituye el alcance de construcción anterior: prototipo de seis días, con pruebas y dos vistas: Interrupciones y Uso diario. Uso diario se incorpora por petición posterior del usuario; aumenta el trabajo de la fase 4, sin convertir los seis días en una garantía.

## 0. Cómo trabajaremos

1. Leer este plano antes de implementar. Construir un módulo por vez, según la sección 12.
2. Las decisiones D1–D4 de la sección 3 quedan propuestas para ejecución por indicación del usuario. No esperar confirmaciones individuales; aplicarlas salvo objeción. No presentarlas como preferencias expresamente confirmadas.
3. Registrar decisiones menores con `// DECISIÓN:` y continuar. Para cambios posteriores sustanciales de alcance o contratos, presentar la propuesta concreta y su impacto en conjunto.
4. La lógica temporal y de detección será pura y testeable: sin JavaFX ni SQLite, con Clock inyectado y observaciones simulables.
5. No añadir dependencias fuera de la sección 4 sin justificar la necesidad. Fijar versiones compatibles al crear el proyecto.
6. Ningún módulo está terminado solo porque compila: ejecutar sus pruebas y verificar los criterios aplicables.
7. Cada entrega incluirá: **qué funciona, qué se probó, qué falta**. Separar pruebas automáticas, manuales y tareas todavía no ejecutadas.
8. La revisión de innovación y negocio ya está en REVISION_INNOVACION.md. No repetirla ni convertirla en requisito de desarrollo.
9. Este documento especifica trabajo futuro; su existencia no significa que la app o la extensión estén implementadas o probadas.

## 1. Producto y lenguaje

Deriva es una aplicación de Windows para observar cambios entre aplicaciones y sitios y revisar episodios de cambios frecuentes, denominados **derivas**. Permite saber cuánto tiempo transcurre hasta volver a una actividad estable según una regla explícita.

Propuesta: **“Observa tus cambios de actividad y revisa cuándo vuelves a una actividad estable. Local y sin cuenta.”**

La actividad de ventanas es una señal observable; no mide atención mental, productividad ni intención. Permanecer en una app no demuestra concentración. Cambiar entre herramientas no implica desperdiciar tiempo. No se atribuye causalidad al primer cambio.

| Concepto | Nombre y significado en v2 |
|---|---|
| Actividad estable | Permanecer 180 segundos continuos en el mismo contexto observable, sin interrupciones de observación. |
| Tiempo hasta volver a una actividad estable | Tiempo desde el primer cambio del episodio hasta el inicio del periodo estable, confirmado al completar los 180 segundos. |
| Primer cambio detectado | Destino del primer cambio incluido en la ventana que abrió el episodio; describe orden temporal. |
| Deriva | Episodio que alcanza el umbral fijo de cambios; no es un diagnóstico de distracción. |
| Justificada | Anotación del usuario; no borra ni altera los hechos registrados. |

No usar en la interfaz “foco real”, “minutos perdidos”, “costo de interrupción”, “disparador” ni “índice de atención”. Los índices y bloques largos quedan fuera del prototipo. Tampoco mostrar una suma como tiempo desperdiciado.

Restricciones: datos locales, sin cuenta, sin IA, sin servidores externos ni APIs de pago. Existe comunicación HTTP **local** con la extensión; no afirmar que la aplicación carece de red. El objetivo de seis días corresponde al prototipo recortado, no a un producto listo para distribución masiva.

## 2. Alcance del prototipo y uso

**Obligatorio:** captura Windows → segmentos → SQLite → detección de deriva → vistas Interrupciones y Uso diario por aplicación/sitio, con pruebas. Incluir una prueba real temprana de extensión Chromium y bridge. La integración de sitios solo se habilita si supera los criterios de privacidad y atribución de la sección 14.

Flujo mínimo:

1. El usuario abre Deriva manualmente; ve que el registro está activo y una explicación breve de los datos recogidos.
2. El sistema registra segmentos de aplicaciones permitidas; los títulos no se guardan.
3. Diez cambios confirmados en 120 segundos abren una deriva.
4. Interrupciones muestra episodios abiertos, finalizados o sin estabilidad confirmada, el primer cambio detectado y sus contextos.
5. El usuario puede anotar motivo y marcar Justificada desde la tabla.
6. Uso diario permite consultar cuánto tiempo registrado en primer plano corresponde a cada aplicación y sitio durante una fecha.
7. Salir detiene la captura, cierra correctamente los datos y termina el proceso.

**Si queda tiempo:** pausa manual y bandeja básica (Abrir, Pausar/Reanudar, Salir). No deben retrasar las pruebas del núcleo. Sin bandeja, cerrar la ventana sale de la app; nunca queda un capturador oculto.

**Después del prototipo:** adaptación con 7/14 días, línea base, índices, bloques largos, estadísticas avanzadas (excepto Uso diario básico, que sí se incluye), el panel Hoy avanzado, Insights, mapa de calor, ranking agregado, mejor franja, línea de tiempo, resumen diario, avisos flotantes, onboarding completo, exportación CSV, editor de reglas, inicio con Windows, Firefox y múltiples perfiles. El instalador es posterior; basta una ejecución local reproducible documentada.

## 3. Decisiones conservadoras propuestas

### D1. Unión de derivas solapadas

Mantener **como máximo un episodio abierto**. Todo disparo nuevo mientras ese episodio siga abierto actualiza el mismo episodio. No crear registros adicionales por cada ventana deslizante ni por cada aviso.

Al confirmar estabilidad, cerrar el episodio y vaciar la ventana de cambios. Un episodio posterior debe alcanzar el umbral con cambios nuevos. No sumar intervalos solapados. La implementación contará transiciones una vez por identidad temporal, aunque aparezcan en varias ventanas.

### D2. Recuperación dentro de una misma app

La estabilidad se decide por **contexto**, no solo por nombre de proceso. Cada tick evalúa la duración del contexto actual, aunque el segmento aún esté abierto. Una deriva se confirma como estabilizada tras 180 segundos continuos sin cambios válidos, sin candidatos de cambio pendientes y sin pérdida de observación.

Ejemplo: primer cambio a las 10:00; el último cambio lleva a un contexto a las 10:02; continúa ahí hasta las 10:05. A las 10:05 se confirma el episodio, con inicio de estabilidad a las 10:02 y duración hasta estabilidad de 2 minutos. No esperar a que el usuario cambie otra vez de ventana.

### D3. Sitios como contextos distintos

Identidad de contexto: `(process_name normalizado, host normalizado o null)`. La etiqueta visible no determina identidad. `docs.google.com` y `mail.google.com` son distintos; dos páginas del mismo host son el mismo contexto. Cambiar de Chrome a Edge cuenta como cambio aunque muestren el mismo host.

Para el prototipo el host se pasa a minúsculas y se elimina un `www.` inicial. No colapsar subdominios restantes. Dos hosts con la misma etiqueta siguen siendo distintos. No distinguir documentos o tareas dentro de una misma app o sitio: limitación explícita.

### D4. Ventanas privadas y situaciones inciertas

Aplicar exclusión **antes** de cualquier resolución o persistencia. No usar títulos como respaldo en el prototipo. Los títulos no permiten garantizar que una ventana sea normal o privada.

Por defecto, los navegadores quedan excluidos hasta validar la integración. Una ventana privada, un reporte ausente/caducado, una pérdida de foco, un reinicio o un perfil no soportado producen `__EXCLUDED__`, con host, etiqueta y título nulos. Nunca reutilizar el último sitio normal como si describiera una ventana privada.

La primera integración soportará un navegador Chromium elegido y un único perfil. **Si las APIs y la prueba real no permiten vincular de forma fiable el reporte con la ventana normal activa, conservar excluida toda la actividad del navegador.** La prueba de extensión se entrega igualmente con su resultado y limitación; no retrasar el núcleo ni rebajar la privacidad para aparentar integración completa.

Esto prioriza perder observaciones frente a guardar o atribuir datos dudosos. No prometer detección universal de navegación privada. La sección 14 define la prueba temprana que decide si se habilita el registro de sitios.

## 4. Stack

| Aspecto | Decisión |
|---|---|
| Lenguaje | Java 21 |
| UI | JavaFX 21, FXML y CSS |
| Build | Maven |
| Persistencia | SQLite mediante org.xerial:sqlite-jdbc, WAL |
| Sensor Windows | JNA y jna-platform |
| Tests | JUnit 5 |
| Logging | java.util.logging |
| Bridge | com.sun.net.httpserver.HttpServer del JDK |
| Extensión piloto | JavaScript, Manifest V3, un Chromium y un perfil |

Fijar versiones estables compatibles con Java 21/JavaFX 21; no actualizar versiones mayores automáticamente. No Spring, Guice, ORM, IA ni nube. El protocolo JSON es pequeño y de esquema fijo: validar estrictamente estructura, tipos, tamaño y escapes; no parsearlo mediante búsquedas por subcadenas o expresiones regulares improvisadas. Si la implementación segura requiere otra biblioteca, proponer esa única dependencia con su motivo.

## 5. Arquitectura y contratos del prototipo

Paquetes: `app`, `sensor`, `tracking`, `storage`, `analysis`, `coach`, `privacy`, `bridge`, `ui`.

- `sensor`: ventana en primer plano, proceso, identificador de ventana, inactividad y disponibilidad de observación.
- `privacy`: transforma muestras en observaciones permitidas antes de enviarlas al resto del sistema.
- `tracking`: estabiliza candidatos de contexto y emite segmentos más observaciones temporales confirmadas.
- `analysis` / `coach`: cuenta cambios y mantiene una máquina de estados de deriva. Sin dependencias de UI o almacenamiento.
- `storage`: persiste segmentos, episodios y anotaciones.
- `ui`: consulta repositorios fuera del hilo JavaFX y presenta Interrupciones y Uso diario.

Flujo:

```text
WindowSensor + IdleDetector + BrowserStateStore
                   ↓
            PrivacyFilter
                   ↓
           TrackingService ──→ SegmentRepository
                   ↓ cada tick, incluido contexto abierto
             DriftDetector ──→ DriftRepository ──→ Interrupciones
```

Un executor de muestreo cada segundo; otro serializado para escritura por lotes cada 10 segundos; lecturas fuera del hilo JavaFX. El bridge solo actualiza estado en memoria. Cambios de controles mediante Platform.runLater. Guardar actualizaciones de episodio y anotaciones con operaciones separadas para no sobrescribir motivos al avanzar el detector.

Contratos orientativos vinculantes en significado; los tipos auxiliares se implementan junto a su módulo:

```java
record ContextKey(String processName, String siteHost) {}
enum ObservationKind { OBSERVED, IDLE, EXCLUDED, UNAVAILABLE }
enum EpisodeStatus { OPEN, STABLE, UNCONFIRMED }

// foregroundWindowId se usa solo en memoria para invalidar asociaciones del navegador.
record ActivitySample(Instant timestamp, String processName, long pid,
                      long foregroundWindowId, boolean idle, boolean available) {}
record SiteResolution(ObservationKind kind, String host, String label,
                      String source) {}
record Segment(long id, ObservationKind kind, ContextKey context,
               String siteLabel, String siteSource, Instant start, Instant end) {}

// StableTick describe el contexto vigente después del debounce.
// pendingChange impide confirmar estabilidad mientras hay un candidato pendiente.
record StableTick(Instant timestamp, ObservationKind kind, ContextKey context,
                  Instant contextSince, boolean pendingChange) {}

interface WindowSensor { ActivitySample sample(); }
interface IdleDetector { Duration idleTime(); }
interface SiteResolver { SiteResolution resolve(ActivitySample sample); }
interface TrackingService { void start(); void stop(); }
interface SegmentRepository {
    void saveAll(List<Segment> segments);
    List<Segment> between(Instant from, Instant to);
}
interface DriftDetector { List<DriftUpdate> onTick(StableTick tick); }
interface DriftRepository {
    void upsertDetection(DriftUpdate update);
    void annotate(long id, String reason, boolean justified, Instant answeredAt);
    List<DriftEvent> between(Instant from, Instant to);
}
```

DriftUpdate/DriftEvent contienen los campos de drift_event y sus contextos de la sección 6. Un identificador de episodio se asigna al abrirlo y permanece estable. Config es inmutable; Clock se inyecta en la lógica temporal. Pausa es una extensión opcional de TrackingService; no es requisito del primer módulo.

Cambios respecto del plano original: se elimina RecoveryCostCalculator y se representa directamente la duración hasta estabilidad; se transporta siteSource; se diferencia host de etiqueta; se evalúa el segmento abierto por tick; se eliminan del prototipo los contratos de DailyMetric, FocusBlock y Baseline.

## 6. Persistencia

Base del prototipo: `%APPDATA%\DerivaPrototype\deriva.db`, separada de una futura instalación estable. SQLite con WAL, foreign_keys y migración V1. No existe base implementada que migrar desde el plano original.

Timestamps: epoch en milisegundos; intervalos semiabiertos `[start, end)`. Calcular duraciones desde timestamps; no acumular redondeos de un segundo en cada tick.

Esquema mínimo:

```sql
CREATE TABLE schema_version (version INTEGER NOT NULL);

CREATE TABLE segment (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  kind TEXT NOT NULL CHECK (kind IN ('OBSERVED','IDLE','EXCLUDED','UNAVAILABLE')),
  process_name TEXT NOT NULL,
  site_host TEXT,
  site_label TEXT,
  site_source TEXT CHECK (site_source = 'EXTENSION'),
  start_ts INTEGER NOT NULL,
  end_ts INTEGER NOT NULL CHECK (end_ts >= start_ts)
);
CREATE INDEX idx_segment_start ON segment(start_ts);

CREATE TABLE drift_event (
  id INTEGER PRIMARY KEY,
  start_ts INTEGER NOT NULL,
  detected_at INTEGER NOT NULL,
  last_observed_ts INTEGER NOT NULL,
  stability_start_ts INTEGER,
  closed_at INTEGER,
  status TEXT NOT NULL CHECK (status IN ('OPEN','STABLE','UNCONFIRMED')),
  close_reason TEXT,
  switch_count INTEGER NOT NULL,
  first_change_process TEXT NOT NULL,
  first_change_host TEXT,
  time_to_stability_ms INTEGER,
  reason TEXT CHECK (reason IN ('SEARCHING_INFO','MESSAGES','BREAK','BOREDOM','OTHER')),
  justified INTEGER NOT NULL DEFAULT 0 CHECK (justified IN (0,1)),
  answered_at INTEGER,
  CHECK (time_to_stability_ms IS NULL OR time_to_stability_ms >= 0),
  CHECK (
    (status = 'STABLE' AND stability_start_ts IS NOT NULL
      AND time_to_stability_ms IS NOT NULL AND closed_at IS NOT NULL)
    OR (status = 'OPEN' AND stability_start_ts IS NULL
      AND time_to_stability_ms IS NULL AND closed_at IS NULL)
    OR (status = 'UNCONFIRMED' AND stability_start_ts IS NULL
      AND time_to_stability_ms IS NULL AND closed_at IS NOT NULL)
  )
);
CREATE INDEX idx_drift_start ON drift_event(start_ts);

CREATE TABLE drift_context (
  event_id INTEGER NOT NULL REFERENCES drift_event(id) ON DELETE CASCADE,
  process_name TEXT NOT NULL,
  site_host TEXT NOT NULL DEFAULT '',
  PRIMARY KEY (event_id, process_name, site_host)
);
CREATE TABLE setting (key TEXT PRIMARY KEY, value TEXT NOT NULL);
CREATE TABLE excluded_app (process_name TEXT PRIMARY KEY);
CREATE TABLE excluded_site (host TEXT PRIMARY KEY);
```

No crear tablas para métricas diarias o bloques de actividad estable en el prototipo. No guardar títulos: el esquema no tiene window_title. No hay apps_json: la relación drift_context evita necesitar JSON para persistencia.

En segmentos no observables usar únicamente `__IDLE__`, `__EXCLUDED__` o `__UNAVAILABLE__`; todos los campos de sitio deben ser nulos. El filtro se aplica también antes de crear first_change y drift_context. Los detalles privados nunca llegan a repositorios ni logs.

Retención: conservar 90 días de segmentos y episodios del prototipo; eliminar en una transacción, con borrado en cascada de contextos. No borrar episodios abiertos. La interfaz mínima permite borrar los datos con confirmación: detener captura, vaciar/cancelar operaciones pendientes, borrar segmentos y episodios y reiniciar el detector antes de reanudar. La configuración y el token se conservan; la acción se llama “Borrar registros”, no “restablecer toda la aplicación”. No se promete borrado forense del disco.

Al arrancar tras un cierre inesperado, los episodios OPEN se convierten en UNCONFIRMED con motivo RESTART y closed_at igual a last_observed_ts. No inventar actividad durante el tiempo que la app estuvo cerrada.

## 7. Reglas temporales

### 7.1 Captura y segmentación

- Muestrear cada 1 segundo. El primer tick inicia observación; no inferir actividad previa.
- Contexto permitido continuo produce un segmento. Cambios confirmados lo cierran e inician el siguiente.
- Debounce: un contexto candidato debe persistir al menos 3 segundos de tiempo observado. Al confirmarlo, su inicio es el primer tick del candidato; el cambio se emite una sola vez con ese timestamp. Si desaparece antes, su tiempo se absorbe en el contexto permitido anterior. Al inicio de sesión, sin contexto anterior, esos segundos se descartan.
- No añadir una segunda regla de “rebote de 10 segundos”: queda aplazada para evitar doble interpretación. A→B→A cuenta dos cambios si B superó el debounce; si B no lo superó, ninguno.
- Exclusión, inactividad y pérdida de observación se aplican inmediatamente y nunca se absorben mediante debounce.
- El proceso de Deriva se considera EXCLUDED: revisar su interfaz no añade tiempo a la aplicación anterior.
- Inactividad: al superar 120 segundos sin entrada, pasar a IDLE. Los 120 segundos de gracia anteriores permanecen en la observación; documentarlo, sin presentarlos como atención confirmada.
- Bloqueo de Windows, fallo del sensor, suspensión o hueco entre ticks mayor a 3 segundos interrumpen observación. El hueco no cuenta como actividad estable. El episodio se cierra sin confirmación hasta el último tick válido; limpiar candidatos y cambios.
- Si el reloj retrocede, interrumpir observación y no producir duraciones negativas. Usar tiempo monotónico para duraciones en ejecución y Clock para timestamps y simulación.

### 7.2 Cambios

Solo contar transiciones entre contextos OBSERVED consecutivos, distintos y confirmados. Una frontera IDLE/EXCLUDED/UNAVAILABLE rompe la continuidad; no contar cambios entre sus extremos. Las duraciones excluidas no se convierten en actividad ni en tiempo hasta estabilidad.

### 7.3 Apertura de deriva

Umbral fijo: al confirmar el décimo cambio cuyo timestamp esté dentro de `(t - 120 segundos, t]`, abrir un episodio si no hay uno abierto. `t` es el timestamp del cambio confirmado; detected_at es el tick en que se alcanzó el umbral tras debounce.

start_ts es el timestamp del cambio más antiguo de esa ventana. first_change_process/host son su destino. switch_count incluye los cambios de esa ventana y, después, los nuevos del episodio. Los cambios anteriores al inicio del episodio no se añaden.

Mientras esté abierto, los cambios actualizan el episodio existente. No hay adaptación, notificaciones ni enfriamiento de avisos en el núcleo.

### 7.4 Actividad estable y cierre

Un contexto OBSERVED continuo durante 180 segundos confirma estabilidad. Comparar ContextKey completo. Un cambio confirmado reinicia el contador desde el inicio de su nuevo contexto. Un candidato pendiente bloquea la confirmación; si se descarta, el contador del contexto anterior conserva continuidad conforme al debounce.

Al confirmar:

```text
status = STABLE
stability_start_ts = inicio del contexto que completó 180 segundos
closed_at = tick de confirmación
time_to_stability_ms = stability_start_ts - start_ts
```

No sumar los 180 segundos de verificación a la duración. No aplicar un tope de 30 minutos que esconda tiempo realmente observado. D1 mantiene un solo episodio hasta que termina, aunque dure más. No usar el resultado como estimación de pérdida de productividad.

Si ocurre idle, exclusión, pausa opcional, bloqueo, salida o hueco antes de confirmar: status UNCONFIRMED, duración hasta estabilidad NULL, closed_at en la frontera observada y close_reason explícito. Mostrar “Sin estabilidad confirmada”; nunca tratar NULL como cero. La duración observada parcial puede mostrarse aparte con esa etiqueta.

Los episodios pueden cruzar medianoche: no se cortan ni duplican. Un filtro por fechas selecciona episodios cuyo intervalo observado se solape con el rango local seleccionado. Se muestran completos, con fecha y hora, sin atribuirlos íntegramente a cada día como un total diario.

### 7.5 Anotaciones

Motivo y Justificada se editan desde Interrupciones. Marcar Justificada conserva episodio, cambios y duración: solo añade interpretación personal. No hay puntuaciones ni penalizaciones. El filtro puede ocultar justificadas, pero por defecto se muestran todas.

### 7.6 Uso diario por aplicación y sitio

Métrica: **tiempo registrado en primer plano**, no tiempo abierto, reproducción de audio, atención ni productividad. Solo hay un contexto principal a la vez, incluso con varias pantallas o ventanas visibles. Una ventana no necesita estar minimizada para quedar en segundo plano: basta con que otra tenga el foco.

- Sumar exclusivamente segmentos OBSERVED autorizados, aplicando las mismas reglas de debounce e inactividad del tracking. No contar IDLE, EXCLUDED, UNAVAILABLE, pausas ni huecos.
- Por aplicación: agrupar por process_name; nombre amigable opcional (por ejemplo Dota 2), conservando el ejecutable como identidad. No agrupar procesos diferentes solo porque compartan etiqueta visible.
- Por sitio: agrupar por site_host autorizado y normalizado, sumando navegadores soportados. La etiqueta es presentación. Un host solo se muestra si D4 permite su captura; si no, explicar que los sitios no se están registrando, sin inventar ceros.
- Los sitios son un desglose del tiempo del navegador, no tiempo adicional. Por ejemplo Chrome 2 h incluye Instagram 1 h y YouTube 1 h; nunca sumar esas filas como 4 h.
- Día seleccionado: intervalo entre el inicio de ese día y el siguiente en una ZoneId explícita, inicialmente la zona del sistema mostrada en UI. Recortar cada segmento al intervalo antes de sumar, usando max(0, min(finSegmento, finDia) - max(inicioSegmento, inicioDia)). Esto evita duplicar segmentos que cruzan medianoche y respeta días de duración distinta por cambio horario.
- Consultar segmentos que se SOLAPEN con el día, incluidos los que empezaron antes. Sumar milisegundos y redondear solo al presentar.
- Para hoy, combinar una instantánea coherente de segmentos persistidos, pendientes y el contexto abierto confirmado hasta el último tick válido; no contar una porción dos veces ni extender hasta la hora actual si se perdió observación. Orquestación ofrece un corte consistente; el calculador puro recibe intervalos únicos. Actualizar cada 10 segundos con marca de última actualización; no forzar cierre de segmento al abrir la vista.
- Si no hubo registro, mostrar “Sin datos registrados”; no afirmar “No usaste aplicaciones”. Mostrar límites de cobertura: Deriva cerrado, exclusiones o integración deshabilitada.
- Caso Dota + música: durante una hora de juego en primer plano, con YouTube reproduciendo detrás, se suma aproximadamente una hora a Dota y ningún tiempo adicional a YouTube por esa reproducción. Si cambias cinco minutos al navegador y YouTube queda en primer plano, esos cinco minutos se atribuyen al navegador/YouTube, no simultáneamente al juego. Supone observación válida, sin idle ni exclusiones y transiciones que superan debounce.
- Escuchar música con navegador minimizado no activa captura de ese sitio ni genera una deriva por sí mismo. Medición de audio o uso simultáneo en segundo plano queda fuera de este prototipo; requeriría otra métrica separada.

### 7.7 Contrato de cálculo y persistencia

Añadir un calculador puro DailyUsageCalculator en analysis: recibe intervalos autorizados no duplicados, fecha y zona; devuelve totales por proceso, por host y total observado. El caso de uso en app.usecase obtiene los datos por puertos y prepara la instantánea; el ViewModel solo presenta los resultados. No añadir tablas diarias, columnas ni nuevas dependencias: reutilizar segment. Las anotaciones Justificada no modifican el uso registrado.

## 8. Interfaz mínima

Una ventana JavaFX con dos pestañas: **Interrupciones** y **Uso diario**. Los siguientes controles de episodios corresponden a Interrupciones.

- Estado de captura visible, botón Salir y acceso a Borrar registros con confirmación.
- Tabla: fecha/hora de inicio, estado, cambios, primer cambio detectado, contextos observados, tiempo hasta volver a una actividad estable, motivo y Justificada.
- OPEN muestra “En curso”; UNCONFIRMED muestra “Sin estabilidad confirmada” y motivo de cierre; STABLE muestra la duración medida y la regla de 180 segundos.
- Estados vacíos y errores comprensibles. No reemplazar errores por tablas vacías silenciosas.
- Filtro por rango de fechas y por justificadas. Sin ranking de episodios, totales de episodios ni gráficos; los totales de uso están en Uso diario.
- Panel pequeño de conexión: navegador piloto, estado de integración, puerto, token con Copiar e instrucciones de instalación. Distinguir “bridge conectado” de “sitio válido para registro”.
- Lista mínima de procesos y hosts excluidos editable mediante configuración local documentada; no exigir un editor visual completo. Explicar dónde editarla y que requiere reiniciar.
- Actualización de datos fuera del hilo JavaFX, con refresco moderado; no ejecutar consultas cada tick.

Si se implementa bandeja, usar Platform.setImplicitExit(false) solo junto a un menú Salir funcional. Si no existe bandeja, cerrar la ventana termina el proceso y hace flush.

### Uso diario

Selector de fecha (hoy por defecto), zona horaria visible, total registrado en primer plano y dos listas ordenadas por duración descendente: Aplicaciones y Sitios. Mostrar nombre/host y duración en horas/minutos, con segundos para intervalos menores a un minuto. Etiqueta permanente: “Los sitios están incluidos en el tiempo de sus navegadores”. Sin gráficos avanzados.

Ejemplo de filas de días observados: “Dota 2 — 4 h”; “Instagram — 2 h”. Son valores calculados, no ejemplos que deban mostrarse como datos reales. Sitios requieren integración habilitada. La ayuda explica que una app abierta/minimizada o música en segundo plano no suma por ese solo hecho. Registrar cobertura y errores honestamente.

## 9. Configuración inicial

| Clave | Valor | Alcance |
|---|---|---|
| sample_period_ms | 1000 | Captura |
| min_segment_s | 3 | Debounce |
| idle_threshold_s | 120 | Umbral de inactividad |
| max_sample_gap_s | 3 | Interrupción de observación |
| drift_switches | 10 | Umbral fijo |
| drift_window_s | 120 | Ventana de cambios |
| stable_s | 180 | Confirmación de actividad estable |
| write_interval_s | 10 | Escritura por lotes |
| retention_days | 90 | Registros del prototipo |
| bridge_enabled | true | Loopback |
| bridge_port | 47821 | Puerto local |
| bridge_token | 32 bytes aleatorios en hex | Generado al primer arranque |
| browser_capture_enabled | false | Habilitar solo tras la prueba D4 |
| browser_report_max_age_s | 3 | Caducidad máxima de evidencia de sitio |

Guardar títulos está deshabilitado de forma fija. No incluir configuraciones de índices, adaptación, resumen o alertas que todavía no existen. Validar parámetros positivos y coherentes al cargar; un error debe ser visible.

## 10. Privacidad

Los registros permanecen en el equipo. La extensión se comunica únicamente con 127.0.0.1; no hay telemetría ni envíos externos.

Con integración habilitada, los hosts **sí se guardan** en site_host, first_change_host y drift_context. No guardar URL completa, ruta, consulta, título ni contenido. La extensión puede necesitar leer tab.url en memoria para extraer el host; no afirmar que nunca accede a ese valor.

Excluir apps o sitios significa omitir su identidad tanto en segmentos como en episodios, contextos y logs. Para hosts, coincidencia exacta o sufijo con frontera de punto. Aplicar la misma normalización en configuración y captura.

No escribir tokens, hosts, títulos ni cuerpos de peticiones en logs. Solo códigos de error y datos operativos necesarios. Las excepciones no deben imprimir payloads.

La extensión tampoco persiste el último host en storage.local: solo puerto, token y configuración del navegador piloto. Tras despertar o reiniciar consulta el estado real; nunca reenvía un sitio guardado previamente como si fuera actual.

## 11. Windows y límites

Usar GetForegroundWindow, GetWindowThreadProcessId y QueryFullProcessImageName mediante JNA. Si no se puede identificar el proceso, registrar UNAVAILABLE; no convertir múltiples errores en una falsa actividad estable llamada “desconocido”. GetLastInputInfo aporta inactividad.

Conservar identificador de ventana solo en memoria; al cambiar, invalidar la asociación anterior del navegador. No retener handles nativos innecesarios. Probar bloqueo/desbloqueo, suspensión/reanudación, cierre y reapertura. Un fallo de SQLite debe avisar y detener la captura antes de acumular memoria sin límite o perder datos silenciosamente.

Objetivo de rendimiento: CPU media en reposo menor al 2 % en el equipo de prueba; medir y anotar hardware, duración y método. Registrar memoria observada; no declarar “bajo consumo” sin medición.

## 12. Plan de seis días y aceptación

El plan es una secuencia de entregas verificables, no seis jornadas garantizadas independientemente de los resultados. Si hay problemas, recortar opcionales y comunicar pendientes; no declarar aprobado un criterio que no se ejecutó.

| Día | Entrega | Verificación principal |
|---|---|---|
| 1 | Proyecto, sensor, primer pipeline de segmentos simulado. Prueba mínima real de bridge y extensión desde el comienzo. | Ventana/proceso correctos; extensión instalada en un Chromium, reporte normal, foco e incógnito. Registrar resultado temprano. |
| 2 | Segmentación y persistencia SQLite; resolver viabilidad de integración segura de sitios. | Debounce, fronteras de privacidad, reinicio, sitios distintos; decidir habilitar o mantener excluido el navegador. |
| 3 | Detector por tick y persistencia de episodios. | Umbral, episodio único, recuperación sin cerrar segmento, sitios como contextos. |
| 4 | Interrupciones, anotaciones y Uso diario básico por aplicación/sitio. | Estados de episodios, filtros y totales sin duplicados ni tiempo de fondo. F4.3 puede requerir otra sesión; recortar opcionales antes que pruebas. |
| 5 | Integración completa y pruebas de fallos y privacidad. | Cierre/reapertura, bloqueo, huecos, borrado, logs, ventanas privadas. Pausa/bandeja solo si el núcleo pasa. |
| 6 | Prueba sostenida, correcciones, recursos y entrega ejecutable local con instrucciones. | Ejecución reproducible, resultados de pruebas, limitaciones. No añadir estadísticas para llenar el último día. |

Criterios mínimos automatizados:

1. Tracking: candidato de 2 segundos absorbido; candidato de 3 segundos confirmado una vez; exclusión no absorbida; inicio de sesión sin contexto anterior; idle y huecos rompen continuidad.
2. Contextos: A→B→A confirmado cuenta dos; mismo host no cambia; dos subdominios cambian; etiqueta igual con hosts distintos cambia; navegador distinto cuenta como contexto distinto.
3. Derivas: 9 cambios no abren; 10 dentro de la ventana abren una vez; límites exactos de ventana; disparos solapados actualizan el mismo ID; un episodio posterior no reutiliza cambios anteriores.
4. Estabilidad: a los 179 segundos sigue abierto; a los 180 confirma sin cerrar segmento; candidato pendiente impide cierre; cambio de sitio reinicia estabilidad aunque el proceso sea el mismo.
5. Cierre parcial: idle, exclusión, salida, reinicio y hueco dan UNCONFIRMED con duración NULL; episodio mayor a 30 minutos no se trunca; cruce de medianoche no duplica datos.
6. Storage: V1, consultas por solapamiento, transacciones, anotaciones preservadas al actualizar detección, reapertura y borrado sin reaparición desde colas.
7. Privacidad: datos sintéticos sensibles ausentes de segmentos, episodios y logs; no almacenar títulos ni tokens en logs; reporte viejo no se usa; fallo de privacidad deja navegador excluido.

8. Uso diario: Dota en primer plano y YouTube detrás no duplican duración; alternancia reparte el tiempo; navegador y desglose de sitios no se suman; cruce de medianoche y zona horaria correctos; ignorar segmentos no observables; instantánea de persistidos/pendientes/abierto sin duplicados; integración deshabilitada no inventa sitios. Estas pruebas están planificadas, no ejecutadas.

Criterios manuales: sensor real, extensión real según 14, tabla utilizable, cierre sin proceso oculto, bloqueo y reanudación, consumo y persistencia tras una sesión sostenida. Las pruebas del navegador son obligatorias temprano aunque la captura de sitios finalmente quede deshabilitada.

Formato de entrega:

```text
Funciona: funcionalidades realmente implementadas y límites.
Probado: comandos/tests, resultado, pruebas manuales y equipo cuando corresponda.
Falta: requisitos pendientes, fallos conocidos y siguiente paso concreto.
```

## 13. Fuera de alcance

Además de las funciones pospuestas en sección 2: IA/LLM, cuentas, nube, sincronización, supervisión de empleados, bloqueo de apps o sitios, historial de navegación, contenido de páginas, móvil, tiendas de extensiones y distribución comercial. No reabrir la revisión de innovación o negocio durante esta fase.

## 14. Extensión y bridge: prueba temprana

### 14.1 Objetivo y criterio de continuidad

Comprobar en el día 1 que una extensión Chromium puede comunicarse con Java y que sus señales distinguen de manera fiable la ventana normal activa de situaciones privadas o desconocidas. Terminar el día 2 con una decisión documentada: integración habilitable para el escenario soportado o navegador excluido.

Elegir un navegador realmente disponible en el equipo. Soporte inicial: uno y un perfil. No prometer compatibilidad automática con todos los Chromium. La selección del navegador se configura explícitamente; no depender de detectar la marca por user agent.

### 14.2 Protocolo mínimo

Servidor vinculado solo a 127.0.0.1. `GET /v1/ping` devuelve app y versión, sin datos de actividad. `POST /v1/tab` usa X-Deriva-Token y cuerpo máximo 2 KB:

```json
{"browser":"brave","host":"youtube.com","state":"NORMAL","seq":1}
```

state es NORMAL, INTERNAL o UNKNOWN. INTERNAL y UNKNOWN llevan host null. Una señal de incógnito se transforma en UNKNOWN **antes** de extraer/enviar host. Si no hay evidencia de ventana normal enfocada, enviar UNKNOWN. Los estados internos del navegador no se incluyen como contexto en el prototipo.

El servidor asigna receivedAt; no confiar en timestamps del cliente para frescura. seq es creciente dentro de la sesión de extensión; reiniciar sesión invalida todos los reportes anteriores. El diseño concreto de identificación de sesión y vinculación a ventana se decide en la prueba temprana, queda documentado y debe pasar D4 antes de habilitar captura.

Validar token en tiempo constante, Host local exacto, origen de extensión permitido, método, ruta, tipos, host sin ruta/credenciales/puerto y tamaño. Rechazar orígenes web, sin CORS abierto. Comprobar el Origin real que envía el navegador durante el piloto; no relajar la protección para resolver un fallo de integración. Respuestas: 400 formato, 401 token, 403 origen/Host, 413 tamaño y 429 exceso de 20 peticiones/segundo. Documentar orden de validación en los tests.

No existe heartbeat que renueve un host antiguo. Un ping comprueba conectividad; solo una lectura nueva de la pestaña enfocada puede renovar evidencia de sitio. Caducidad de 3 segundos; si el ciclo de vida MV3 impide renovarla, excluir durante esos huecos. No aumentar la caducidad para ocultar el problema.

### 14.3 Extensión y foco

Carpeta autocontenida `extension/chromium/`: manifest.json, background.js, options.html y options.js. Los archivos referenciados deben estar dentro de la carpeta que se carga descomprimida.

Permisos mínimos: tabs, storage, alarms y host_permissions para http://127.0.0.1/*. Sin history, webRequest, content scripts ni acceso a todos los sitios. No habilitar la extensión en incógnito.

Consultar estado nuevo en activación de pestaña, actualización de URL, cambio de foco, arranque y despertar del worker. No persistir hosts. Al perder foco invalidar estado. Solo una ventana confirmada normal y enfocada permite NORMAL. No asumir que la última pestaña de una ventana normal sigue siendo la activa cuando otra ventana toma foco.

Durante el piloto resolver y probar renovación de evidencia y correlación con cambios del foreground de Windows. Si no puede distinguir una ventana privada o un segundo perfil del estado normal anterior, D4 obliga a mantener browser_capture_enabled=false. Esta limitación debe ser visible y la app de escritorio sigue funcionando para procesos permitidos.

### 14.4 Pruebas de aceptación tempranas

- Instalar extensión real y verificar permisos; ping, token correcto y rechazo de token incorrecto.
- Abrir dos sitios en el mismo navegador; leer host correcto y distinguir subdominios. Ninguna URL completa, título o ruta en base/logs.
- Cambiar a otra app, otra ventana y de vuelta; ninguna atribución del último host durante estado incierto.
- Abrir ventana privada mientras la normal permanece abierta; cerrar la privada; repetir con distintas pestañas. No enviar ni guardar el host privado, ni atribuirle el host normal anterior.
- Reiniciar Deriva y navegador, suspender/reanudar worker y desconectar extensión. No reconstruir actividad desde caché.
- Segundo perfil y navegador no soportado: excluir, sin reutilizar el estado del piloto. Si esto no se puede garantizar, deshabilitar captura de navegadores globalmente.
- Cuerpo excesivo, origen web, Host incorrecto, reporte fuera de orden y caducidad producen rechazo o invalidación verificable.

Un test simulado del bridge no sustituye estas pruebas reales. Informar qué se ejecutó, qué falló y si la integración quedó habilitada o excluida.

