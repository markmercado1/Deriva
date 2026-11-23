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

Estados: salvia para estable, ámbar para abierta y gris para sin confirmar. Siempre icono + texto; nunca solo color. Sin puntuaciones ni juicios de productividad.

## Tipografía y densidad

- UI: **Segoe UI**.
- Horas, duraciones y contadores: **Consolas**.
- Densidad media; jerarquía clara, sin decoración.

## Interrupciones (F4.1/F4.2)

- Tabla con panel de detalle a la derecha. **No** detalle expandido dentro de las filas.
- Columnas: inicio con fecha y hora (`dd/MM HH:mm`), estado, primer cambio detectado, cambios y tiempo hasta estabilidad.
- Filtros: rango **Desde/Hasta** inclusivo por día local, estado e incluir justificadas.
- El rango usa solapamiento: un episodio que cruza medianoche aparece en ambos días consultados y una sola vez en un rango que cubre ambos.
- Detalle: contexto, tiempo, motivos y Justificada. Guardar actualiza la anotación y se conserva al reabrir.
- Rango invertido, estados vacíos y errores visibles; no sustituir un error por una tabla vacía.
- **Modo de prueba** con el botón “Ver datos de ejemplo”: carga seis episodios sintéticos en memoria (abierta, estables y sin confirmar) para comprobar tabla, filtros y detalle sin esperar una captura real. Se avisa con “Modo prueba · episodios de ejemplo · no se guardan”; Guardar y la anotación quedan deshabilitados y no se escribe en SQLite. “Volver a datos reales” restaura la consulta normal.
- Apertura/migración, consultas, anotación y refresco fuera del hilo JavaFX; la UI se construye cuando la base está lista.

## Navegación y captura

Lateral compacta: **Interrupciones**, **Uso diario** y **Ajustes**. En F5.1a Interrupciones y Uso diario están implementadas; Ajustes sigue deshabilitado y no se crea ninguna pantalla anticipada.

La barra lateral incluye un panel de captura con estado textual y botón **Iniciar/Detener captura**. Estados: detenida, iniciando, capturando, en pausa, deteniendo y error; nunca solo color. Con la captura activa aparece **Pausar captura**; en pausa el botón pasa a **Reanudar captura** y el estado dice `Captura en pausa.` en ámbar. El cierre de la ventana detiene la captura y espera su salida ordenada cuando no hay bandeja. Sin bridge, los navegadores quedan excluidos; con bridge configurado se permite Chrome o Brave con un perfil (uno a la vez). El gate completo de Brave sigue pendiente. Ajustes sigue deshabilitado.

## Pausa y bandeja (F5.2)

- Pausar cierra el segmento y el episodio abierto con motivo `PAUSE`; el tiempo en pausa no suma a Uso diario. Reanudar abre una sesión nueva sin solapes.
- Con bandeja disponible, cerrar la ventana la oculta y avisa de que la captura continúa. El menú de la bandeja ofrece **Abrir Deriva**, **Pausar/Reanudar captura** y **Salir**.
- **Salir** detiene la captura, espera el `join`, vuelca lo abierto, retira el icono y termina el proceso; nunca queda un capturador oculto.
- Si la bandeja no está soportada, cerrar la ventana termina la aplicación y hace flush como antes. No se añaden pantallas nuevas.

**Borrar registros** abre una confirmación propia que avisa de que se borran segmentos, episodios y anotaciones, se conservan configuración/token y la captura se detiene. El error de almacenamiento se muestra en el mismo panel de estado, con el botón **Reintentar captura**; no se oculta como una tabla vacía.

## Uso diario (F4.3)

- Selector de fecha (hoy por defecto) y zona horaria visible.
- Total en primer plano del día y cobertura registrada; inactividad, exclusiones y huecos no suman al uso.
- Dos listas ordenadas por duración descendente: **Aplicaciones** y **Sitios**, con barras sencillas adaptadas de la Propuesta 4.1.
- Etiqueta permanente: “Los sitios están incluidos en el tiempo de sus navegadores”. El sitio es un desglose del navegador, nunca una suma adicional.
- “Sin datos” (no hay segmentos) se distingue de “0 s” (hay segmentos, pero ningún primer plano observado).
- Con captura activa, la vista muestra una instantánea coherente: incluye el segmento abierto confirmado como provisional y muestra el candidato pendiente sin sumarlo hasta que se confirme. No se cierra ningún segmento para refrescar.
- Con captura detenida, solo se muestran segmentos persistidos. La vista distingue ambos casos y no presenta datos persistidos como actualización en vivo.
- Sin gráficos avanzados, puntuaciones ni una pantalla llamada “Hoy”.

## Restricciones

- Solo tema claro. La pausa y la bandeja son las únicas adiciones de F5.2; no crear pantallas ni secciones nuevas.
- Sin glassmorphism, neón, desenfoques ni modo oscuro.
- Sin secciones nuevas por anticipado.
- No modificar los planos ni ampliar el alcance de F4.1/F4.2/F4.3.
