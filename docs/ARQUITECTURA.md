# Arquitectura y patrones estables

Esta guía concreta el plano v2 sin cambiar sus reglas de producto. Clean Architecture ligera dentro de un proyecto Maven, sin frameworks de inyección ni módulos artificiales.

## Dirección de dependencias

```text
ui (JavaFX) ───────────────→ casos de uso y modelos de presentación puros
sensor.windows (JNA) ─────→ puertos de captura
storage.sqlite (JDBC) ────→ puertos de repositorios
bridge.http (HTTP) ───────→ contratos de estado de navegador
                                  ↓
            tracking / privacy / analysis / coach: reglas y modelos puros

app: conoce adaptadores y núcleo, los construye y conecta manualmente.
El núcleo nunca importa app, JavaFX, JDBC, JNA ni HTTP.
```

Los paquetes de nivel superior siguen siendo com.deriva.app, sensor, tracking, storage, analysis, coach, privacy, bridge y ui. Dentro de cada módulo separar contrato y adaptador cuando exista una implementación real. No mover todo a nuevos paquetes domain/application/infrastructure por preferencia del agente.

## Ubicación prevista (crear conforme se necesite)

| Responsabilidad | Ubicación | Límites |
|---|---|---|
| Composición, ejecutores, arranque/cierre | app | Único lugar que construye toda la aplicación |
| ActivitySample, puertos WindowSensor/IdleDetector | sensor | Tipos Java puros; JNA solo en sensor.windows |
| ContextKey, Segment, StableTick y segmentación | tracking y tracking.model | Sin UI, SQL, HTTP o estado global |
| Exclusiones y resolución autorizada | privacy | Sanitiza antes de persistencia y detección |
| Conteo de cambios | analysis | Funciones/objetos deterministas |
| Episodios y máquina de estados | coach y coach.model | Sin hilos internos ni base de datos |
| Interfaces de repositorios | storage.port | Interfaces Java puras que usan modelos del núcleo |
| Repositorios y migraciones SQLite | storage.sqlite | JDBC y mapeos; no reglas de deriva |
| Contratos y estado de navegador | bridge | Sin exponer objetos HTTP al núcleo |
| Servidor local y validación de transporte | bridge.http | HTTP, validaciones del protocolo, sin escritura SQL |
| Casos de uso de lectura/anotación/borrado | app.usecase | Dependen de puertos, nunca de adaptadores concretos |
| ViewModels | ui.viewmodel | Java puro; sin JDBC/JNA ni controles JavaFX |
| Controllers, FXML/CSS y enlace visual | ui.fx y resources/com/deriva/ui | Adaptan datos y ejecutan cambios visuales en hilo FX |

Los subpaquetes previstos son organización interna de los paquetes existentes, no nuevos requisitos funcionales. Si un nombre ya está implementado y probado de forma equivalente, conservarlo y documentar la correspondencia.

## Patrones que se mantienen

1. **Puertos y adaptadores:** las reglas reciben interfaces; Windows, SQLite, HTTP y JavaFX son detalles sustituibles.
2. **Inyección manual por constructor:** app ensambla dependencias explícitas. Sin service locator, singletons mutables ni creación de repositorios dentro de controllers.
3. **Objetos de valor inmutables:** records y enums para contextos, segmentos, configuración y episodios. Validar invariantes al construir; no filtrar tipos de infraestructura hacia el dominio.
4. **Máquina de estados explícita:** debounce y deriva tienen estados y transiciones testeables. Cada tick produce cambios sin efectos externos; la orquestación decide persistirlos.
5. **Repository:** contratos pequeños; SQL, transacciones y mapeos solo en adaptadores. upsertDetection no sobrescribe anotaciones del usuario.
6. **MVVM ligero:** ViewModel prepara estados y acciones; controller hace enlace visual. Sin una biblioteca nueva para MVVM.
7. **Tiempo inyectado:** Clock para instantes y una abstracción pequeña de tiempo monotónico cuando se necesite. Tests avanzan tiempo simulado; no esperan 180 segundos reales con sleep.
8. **Privacidad antes de los efectos:** observación excluida nunca llega con identidad a detector, base o logs. D4 se aplica por defecto.

## Concurrencia y fallos

- Un hilo posee el estado mutable de captura/detector. No mutarlo desde JavaFX o HTTP.
- Escritor serializado y cola acotada; si se llena o falla SQLite, detener captura y mostrar error, sin pérdida silenciosa.
- Estado de navegador sincronizado y con instantáneas inmutables. Caducar reporte no significa renovar el host.
- Consultas y análisis fuera del hilo FX. Notificar a la UI mediante adaptación explícita.
- Cierre ordenado: detener nuevas muestras, resolver segmento/episodio parcial, vaciar escritura y cerrar recursos. Borrado coordina las colas para impedir reaparición de registros.
- Errores explícitos; no catch vacío, no resultado vacío para ocultar un fallo de almacenamiento.

## Verificación de arquitectura

En cada cambio revisar imports del núcleo y las dependencias entre paquetes. Cuando exista código, añadir comprobación reproducible ligera si aporta valor; no incorporar ArchUnit ni otro framework sin necesidad. Tests de dominio deben ejecutarse sin iniciar JavaFX, abrir SQLite o llamar Windows.

Antes de modificar esquema o contrato, comprobar consumidores y casos de prueba; registrar la decisión y su impacto. No duplicar las fórmulas y reglas del plano aquí: el plano sigue siendo su fuente única.

## Uso diario — ampliación autorizada

DailyUsageCalculator vive en analysis y es puro: recorta intervalos al día/zona y agrupa por proceso y host. app.usecase obtiene una instantánea coherente del registro persistido, pendiente y abierto mediante puertos; nunca consulta JDBC directamente. El adaptador SQLite consulta segmentos por solapamiento; tracking expone solo instantáneas inmutables. ui.viewmodel presenta totales/cobertura; ui.fx renderiza las listas. El tiempo del sitio es desglose del navegador. Sin nuevas tablas ni seguimiento de audio de fondo. Véase plano v2 §7.6–7.7.

## Ajustes de detección — ampliación autorizada

`DetectionSettings` es un record puro en `coach.model` con los cuatro valores, predeterminados, rangos y validación. `SettingsRepository` en `storage.port` abstrae la tabla `setting`; `SQLiteSettingsRepository` solo lee/escribe claves y valores. `SettingsService` en `app.usecase` interpreta las claves, aplica fallback visible y conserva la instantánea guardada. `CaptureSettingsPolicy` fija la regla de sesión; `CaptureSession` congela la configuración al iniciar, la conserva al pausar/reanudar y la relee al detener/iniciar. `SettingsViewModel` valida y presenta; `SettingsController` escribe fuera del hilo FX. `DriftStateMachine` y `PrivacyFilter` reciben los valores ya validados. La consola diagnóstica no lee esta configuración. Sin cambio de esquema.
