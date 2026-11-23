# Deriva: instrucciones para cualquier agente

Estas reglas se aplican a todo el proyecto. Leer este archivo incluso si la herramienta no carga AGENTS.md automáticamente.

## Inicio de una sesión

0. Para cualquier trabajo de interfaz (F4+), leer `docs/DISENO.md` antes de tocar UI.
1. Leer docs/ESTADO.md: situación real y siguiente tarea.
2. Leer docs/ARQUITECTURA.md y la fila correspondiente en docs/FASES.md.
3. Consultar solo las secciones pertinentes de DERIVA_PLANO_v2.md y docs/DECISIONES.md. En la primera intervención sobre un módulo, leer sus contratos completos.
4. Inspeccionar archivos reales y cambios existentes antes de editar. Si hay Git, ejecutar git status --short y revisar el diff; no revertir trabajo ajeno. Si no hay Git, no asumir historial ni commits.
5. Elegir una tarea pequeña del primer bloque pendiente, marcarla EN_CURSO en docs/FASES.md y actualizar docs/ESTADO.md ANTES de implementarla.

## Autoridad y alcance

- Las instrucciones actuales del usuario tienen prioridad. DERIVA_PLANO_v2.md define el producto; docs/ARQUITECTURA.md concreta límites internos compatibles con él.
- DERIVA_PLANO.md es histórico e inmutable. No reabrir innovación/negocio ni recuperar funciones descartadas.
- D1–D4 son propuestas aplicables salvo objeción por instrucción del usuario; no afirmar aprobación explícita individual.
- Resolver decisiones menores, anotarlas y continuar. Para alterar sustancialmente contratos, esquema o alcance, presentar un cambio concreto y su impacto; no pedir confirmación por cada detalle rutinario.
- La petición actual de preparar continuidad no equivale a haber implementado el prototipo. No completar fases ficticiamente.

## Forma de trabajar

- Una tarea verificable por entrega; se pueden dividir las fases entre muchas sesiones. Los seis días son hitos previstos, no seis llamadas ni fechas límite automáticas.
- Evitar trabajo masivo, reescrituras generales y dependencias nuevas sin necesidad. No abrir varias fases de implementación a la vez. La prueba temprana de navegador es una tarea de F1, no una excepción para construir todo el bridge final.
- Seguir Clean Architecture ligera: reglas y tipos independientes de adaptadores; puertos en el núcleo; implementaciones externas; ensamblado manual en app.
- Mantener los paquetes del plano. No introducir un framework, arquitectura nueva o capas vacías para imponer preferencias personales.
- Tests deterministas para reglas temporales; integración real para SQLite y comprobación manual para Windows/navegador. Nunca presentar mocks como prueba del navegador real.
- No habilitar captura del navegador sin evidencia de D4. Datos sensibles fuera de documentos, fixtures, logs e historial.
- Hablar con el usuario en español. Código e identificadores en inglés; UI con el vocabulario del plano.

## Punto de guardado obligatorio

Después de cada tarea o antes de una operación larga, actualizar docs/ESTADO.md con archivos tocados, resultado de pruebas, trabajo parcial y próximo paso exacto. Así una interrupción de créditos pierde el mínimo contexto posible.

Al finalizar una entrega:

1. Actualizar docs/FASES.md sin marcar HECHO un criterio no probado.
2. Añadir evidencia a docs/PRUEBAS.md: comando exacto, fecha, resultado y límites. No inventar salidas o conteos.
3. Registrar decisiones nuevas en docs/DECISIONES.md; no reescribir la historia de decisiones anteriores.
4. Actualizar docs/ESTADO.md (breve, idealmente menos de 100 líneas) y añadir una entrada corta a docs/ENTREGAS.md.
5. Informar al usuario: Funciona / Probado / Falta. Señalar si la app todavía no arranca.

Si se interrumpe una sesión con EN_CURSO, la siguiente inspecciona el diff y las pruebas antes de continuar; no empieza de cero. No se garantiza que otra IA obedezca estos archivos: el punto de entrada está también en CONTINUAR.md para proporcionárselo explícitamente.
