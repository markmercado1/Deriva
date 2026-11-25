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
- Progreso: minutos usados, entradas y estado **En curso ▲**, **Cerca del tope ◆**, **Excedido ●**, **Justificado por ti ✓** o **Sin responder ○**, siempre icono + texto. Si puede haber subconteo por exclusión ante duda, se explica en la fila. Línea fija: “Progreso con segmentos cerrados y el segmento abierto confirmado, cortado en la última muestra; no se cierra ningún segmento”.
- **Resumen de hoy** dentro de cada tarjeta: minutos justificados separados de los excedidos sin justificar y una línea por aviso registrado con umbral, métrica, hora local y el resultado posterior de A48; pendiente y sin resultado se muestran como tales. No hay pantalla ni escritura nueva.
- La vista se refresca al abrirse y cada 30 s; el temporizador se detiene al cambiar de vista y al cerrar. El cálculo y la persistencia ocurren fuera del hilo JavaFX.
- El máximo de actividad pasiva se edita en **Ajustes · Actividad pasiva**, con efectivo/pendiente igual que los umbrales de detección.

## Justificar excesos (F6B-6)

- Con una alerta al 100 % registrada, la fila ofrece **Justificar exceso** o **Justificar de nuevo** si el uso posterior ya superó la cobertura anterior.
- El diálogo pide un motivo opcional de 500 caracteres como máximo, con la nota «Evita pegar direcciones o datos personales.». Se guarda tal cual y no se envía a ningún sitio; guardar no cambia segmentos, episodios ni contextos.
- La justificación cubre el exceso acumulado al marcarla con la misma base que la alarma (segmentos cerrados y el abierto confirmado), no el resto del día. Se puede quitar y el cálculo vuelve al valor anterior.
- **Sin responder** lleva la línea «Sin responder es solo una anotación tuya; no cambia los registros de actividad.». **Justificado por ti** muestra el motivo y el exceso cubierto. No se usan «aprobado», «válido» ni «comprobado».

## Avisos diferidos (F6B-5)

- En **Ajustes · Avisos durante una deriva**, el máximo de espera es un campo en minutos con efectivo/pendiente: «En uso ahora: 300 s (5 min)» y «Guardado, pendiente de aplicar: …». Por defecto, 5 min; rango 1–30 min. Pausar/Reanudar conserva; Detener/Iniciar relee.
- Si hay deriva activa, la fila de Objetivos muestra «Aviso diferido: se enviará cuando termine la deriva o al llegar al máximo de espera.».
- Resultado posterior, siempre como observación y sin causalidad: «Al enviar el aviso no había deriva y no apareció ninguna en los 2 min siguientes.»; «Después del aviso, volvió a estabilidad en …»; «Después del aviso apareció una deriva durante la observación de 2 min.»; «Después del aviso, seguía habiendo deriva al terminar la observación de 2 min.»; «Después del aviso no se pudo observar el resultado: la captura se pausó o la aplicación se cerró.»; «Aviso no enviado: la aplicación se reinició antes de entregarlo.».
- No se usan «gracias al aviso», «el aviso causó» ni ninguna afirmación de efecto. El aviso no clasifica la app ni modifica segmentos o episodios.

## Restricciones

- Solo tema claro. Las pantallas existentes no se amplían fuera de lo previsto; F6B-3 añade la pantalla Objetivos y la sección Actividad pasiva. No crear otras pantallas ni secciones por anticipado.
- Sin glassmorphism, neón, desenfoques ni modo oscuro.
- Sin secciones nuevas por anticipado.
- No modificar los planos ni ampliar el alcance de F4.1/F4.2/F4.3.


Guardar se deshabilita si los valores coinciden con la anotación persistida (Guardado ✓); editar motivo o Justificada habilita Guardar cambios. Durante escritura y recarga se bloquean los controles del formulario, selección y filtros para evitar solicitudes simultáneas. Un error permite reintentar.

## Ventana inicial (UI-VENTANA)
Conservar el marco nativo de Windows, sus controles y el arrastre por la barra de título. Dimensionar la ventana completa (incluido el marco) hasta 1440x900, limitada al 94 % del área útil lógica del monitor principal; centrarla y limitar los mínimos a ese tamaño. Respetar escala de Windows y barra de tareas. La X conserva el comportamiento de bandeja documentado.

## Simplificación de Objetivos (A52, sustituye las indicaciones anteriores sobre estas burbujas)
Resumen de hoy es un desplegable por tarjeta, cerrado por defecto. Balance y líneas de avisos aparecen solo al desplegar; el refresco conserva su estado. Se eliminan las burbujas explicativas de Sin responder y subconteo por petición del usuario. Se mantienen estado, progreso, acciones, colores y reglas de cálculo.

## Navegación de Ajustes (A53)
Al entrar, cuatro tarjetas en cuadrícula 2x2: Detección, Actividad pasiva, Avisos durante una deriva y Valores y aplicación. Cada tarjeta abre su apartado con regreso a Todos los ajustes. Mantener campos al navegar, guardar/restaurar dentro de su contexto, paleta cálida y foco visible por teclado. Detección conserva valores, avisos y validación junto al formulario; el cuarto apartado consulta valores y explica aplicación.

## Uso por periodos (A54, sustituye selector diario)
Título y navegación Uso. Periodo Día/Semana/Mes/Año/Personalizado con Desde/Hasta visibles e inclusivos. Semana lunes-domingo; meses y años calendario de la fecha elegida. Hasta se calcula salvo en Personalizado. Totales y listas del intervalo mantienen paleta y semántica. Error en rango invertido. Refresco 30 s; abierto confirmado incluido si hoy está en el rango.

Seleccionar Día desde otro periodo vuelve a hoy local; Desde permite elegir posteriormente otro día (A55).

## Ayuda de Ajustes (A56, sustituye A53)
Tres tarjetas. Ayudas bajo Más información cerrado por defecto. Detección incorpora Valores y avisos y Cómo se aplica como desplegables. Validación y cambios sin guardar visibles junto a Guardar.
