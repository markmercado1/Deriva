# Diseño de interfaz — decisiones vigentes

Base visual: **Propuesta 3** (cálida, convencional, sin efectos decorativos), con la estructura de tabla + panel de detalle de la Propuesta 2. Solo tema claro.

## Paleta

| Uso | Color |
|---|---|
| Fondo | `#F5F2EC` |
| Superficies | `#FFFEFB` |
| Texto principal | `#302E2A` |
| Texto secundario | `#746F66` |
| Bordes | `#E2DDD3` |
| Acento terracota | `#B75436` |
| Selección | `#F8E9DE` |

Estados de estabilidad: salvia para **Estable**, ámbar para **En curso** y gris para **No comprobada**. Siempre icono + texto; nunca solo color. Sin puntuaciones ni juicios de productividad.

## Tipografía y densidad

- UI: **Segoe UI**.
- Horas, duraciones y contadores: **Consolas**.
- Densidad media; jerarquía clara, sin decoración.

## Interrupciones (F4.1/F4.2)

- Tabla con panel de detalle a la derecha. **No** detalle expandido dentro de las filas.
- Columnas: inicio con fecha y hora (`dd/MM HH:mm`), **Estabilidad**, **Anotación**, primer cambio detectado, cambios y tiempo hasta estabilidad.
- **Estabilidad** solo describe la comprobación automática: **En curso** (episodio abierto), **Estable** (el contexto se mantuvo el tiempo de estabilidad configurado, sin cambios pendientes) o **No comprobada** (cerrado por una frontera o motivo de cierre). No se usa «Sin confirmar» ni se presenta como concentración comprobada.
- **Anotación** es independiente: **Pendiente** o **Guardada**. «Guardada» se deriva del dato persistido `answered_at`, no de que haya un motivo elegido ni de la casilla Justificada.
- Filtros: rango **Desde/Hasta** inclusivo por día local, estabilidad e incluir justificadas. Las etiquetas del filtro deben coincidir con las de la columna.
- El rango usa solapamiento: un episodio que cruza medianoche aparece en ambos días consultados y una sola vez en un rango que cubre ambos.
- Detalle: contexto, tiempo, motivo de cierre real, estado de la anotación y Justificada. Para **No comprobada** se explica por qué no se comprobó la estabilidad usando el motivo de cierre persistido. Guardar actualiza la anotación, se conserva al reabrir y **no** modifica el estado automático ni reabre el episodio.
- Rango invertido, estados vacíos y errores visibles; no sustituir un error por una tabla vacía.
- **Modo de prueba** con el botón “Ver datos de ejemplo”: carga seis episodios sintéticos en memoria (en curso, estables y no comprobadas) para comprobar tabla, filtros y detalle sin esperar una captura real. Se avisa con “Modo prueba · episodios de ejemplo · no se guardan”; Guardar y la anotación quedan deshabilitados y no se escribe en SQLite. “Volver a datos reales” restaura la consulta normal.
- Apertura/migración, consultas, anotación y refresco fuera del hilo JavaFX; la UI se construye cuando la base está lista.

## Navegación y captura

Lateral compacta: **Interrupciones**, **Uso diario**, **Objetivos** y **Ajustes**. Desde AJUSTES-DETECCION, Ajustes está habilitado solo para los cuatro umbrales de detección; no incluye exclusiones, bridge ni empaquetado.

La barra lateral incluye un panel de captura con estado textual y botón **Iniciar/Detener captura**. Estados: detenida, iniciando, capturando, en pausa, deteniendo y error; nunca solo color. Con la captura activa aparece **Pausar captura**; en pausa el botón pasa a **Reanudar captura** y el estado dice `Captura en pausa.` en ámbar. El cierre de la ventana detiene la captura y espera su salida ordenada cuando no hay bandeja. Sin bridge, los navegadores quedan excluidos; con bridge configurado se permite Chrome o Brave con un perfil (uno a la vez). El gate completo de Brave sigue pendiente.

## Pausa y bandeja (F5.2)

- Pausar cierra el segmento y el episodio abierto con motivo `PAUSE`; el tiempo en pausa no suma a Uso diario. Reanudar abre una sesión nueva sin solapes.
- Con bandeja disponible, cerrar la ventana la oculta y avisa de que la captura continúa. El menú de la bandeja ofrece **Abrir Deriva**, **Pausar/Reanudar captura** y **Salir**.
- **Salir** detiene la captura, espera el `join`, vuelca lo abierto, retira el icono y termina el proceso; nunca queda un capturador oculto.
- Si la bandeja no está soportada, cerrar la ventana termina la aplicación y hace flush como antes. No se añaden pantallas nuevas.

**Borrar registros** abre una confirmación propia que avisa de que se borran segmentos, episodios y anotaciones, se conservan configuración/token y la captura se detiene. El error de almacenamiento se muestra en el mismo panel de estado, con el botón **Reintentar captura**; no se oculta como una tabla vacía.

## Ajustes de detección (AJUSTES-DETECCION)

- Sección **Detección de cambios frecuentes** con cuatro campos: cambios para abrir un episodio, ventana de tiempo, tiempo en el mismo contexto para comprobar estabilidad y tiempo sin teclado ni ratón para considerar inactividad.
- Textos en lenguaje cotidiano y unidades visibles. Se usa «cambios entre aplicaciones o sitios», no «cambios de ventana». Se avisa de que solo cuentan cambios confirmados y de que estos ajustes no miden concentración ni productividad.
- **En uso ahora** muestra la configuración efectiva de la sesión; **Guardado, pendiente de aplicar** solo aparece si difiere. Guardar se deshabilita sin cambios o con valores inválidos. **Restaurar valores predeterminados** solo rellena el formulario.
- Aviso cuando inactividad ≤ estabilidad: la inactividad puede cerrar el episodio antes de comprobar la estabilidad. También se explica el riesgo inverso: un umbral de inactividad mayor puede contar como primer plano tiempo en que el usuario se alejó y permitir un cierre **Estable** sin uso real.
- Si la configuración guardada es inválida, se usa el predeterminado del campo afectado, se muestra qué valores están realmente en uso y **no** se sobrescribe lo guardado.
- Regla de aplicación: **Pausar/Reanudar conserva la configuración de la sesión; Detener/Iniciar relee la guardada.** Guardar no cambia el episodio en curso ni recalcula el historial. La consola diagnóstica conserva los predeterminados y no lee esta configuración.

## Uso diario (F4.3)

- Selector de fecha (hoy por defecto) y zona horaria visible.
- Total en primer plano del día y cobertura registrada; inactividad, exclusiones y huecos no suman al uso.
- Dos listas ordenadas por duración descendente: **Aplicaciones** y **Sitios**, con barras sencillas adaptadas de la Propuesta 4.1.
- Etiqueta permanente: “Los sitios están incluidos en el tiempo de sus navegadores”. El sitio es un desglose del navegador, nunca una suma adicional.
- “Sin datos” (no hay segmentos) se distingue de “0 s” (hay segmentos, pero ningún primer plano observado).
- Con captura activa, la vista muestra una instantánea coherente: incluye el segmento abierto confirmado como provisional y muestra el candidato pendiente sin sumarlo hasta que se confirme. No se cierra ningún segmento para refrescar.
- Con captura detenida, solo se muestran segmentos persistidos. La vista distingue ambos casos y no presenta datos persistidos como actualización en vivo.
- Sin gráficos avanzados, puntuaciones ni una pantalla llamada “Hoy”.

## Objetivos (F6B-3)

- Pantalla **Objetivos** con lista a la izquierda y formulario a la derecha. Crear, editar topes/pasivo/activo, activar/desactivar y borrar con confirmación; desactivar es la vía normal para dejar de vigilar.
- Contexto: elegir una app o un sitio ya visto en el registro, o escribir proceso y host opcional. Al editar un objetivo existente el contexto queda bloqueado; para cambiarlo se borra y se crea otro. Mensajes en español para topes ausentes/negativos, proceso con ruta, host con forma de URL y contexto duplicado.
- Progreso: minutos usados, entradas y estado **En curso ▲**, **Cerca del tope ◆** o **Tope superado ●**, siempre icono + texto. Si puede haber subconteo por exclusión ante duda, se explica en la fila. Línea fija: “Progreso según segmentos ya cerrados; el segmento actual se suma en una fase posterior”.
- La vista se refresca al abrirse y cada 30 s; el temporizador se detiene al cambiar de vista y al cerrar. El cálculo y la persistencia ocurren fuera del hilo JavaFX.
- El máximo de actividad pasiva se edita en **Ajustes · Actividad pasiva**, con efectivo/pendiente igual que los umbrales de detección.

## Restricciones

- Solo tema claro. Las pantallas existentes no se amplían fuera de lo previsto; F6B-3 añade la pantalla Objetivos y la sección Actividad pasiva. No crear otras pantallas ni secciones por anticipado.
- Sin glassmorphism, neón, desenfoques ni modo oscuro.
- Sin secciones nuevas por anticipado.
- No modificar los planos ni ampliar el alcance de F4.1/F4.2/F4.3.


Guardar se deshabilita si los valores coinciden con la anotación persistida (Guardado ✓); editar motivo o Justificada habilita Guardar cambios. Durante escritura y recarga se bloquean los controles del formulario, selección y filtros para evitar solicitudes simultáneas. Un error permite reintentar.
