# Mensaje para continuar con otra IA

Copia el bloque siguiente en una nueva sesión y dale acceso a la carpeta del proyecto:

```text
Continúa el proyecto Deriva en C:\AppTImeControl.

Primero lee solo AGENTS.md, CONTINUAR.md y la tabla de docs/FASES.md. No cargues
todo el historial ni todo DERIVA_PLANO_v2.md: después abre solo la fila y los
archivos de la tarea que vayas a tocar, y los contratos del módulo correspondiente.

Estado actual: F0–F5.2 y F6.1 HECHO (2026-11-23; evidencia E021–E030). La app se
abre con scripts/Start-Ui.ps1; Main sigue siendo solo un mensaje de arranque.
Están probados: captura real desde la barra lateral, persistencia SQLite, borrado
coordinado, cierre sin hilo/proceso oculto, bloqueo real y suspensión S0, pausa
manual con cierre PAUSE, bandeja Abrir/Pausar-Reanudar/Salir, y medición sostenida
de 2 h 7 min con CPU media 0,15 %. Guardar en Interrupciones ya persistía; E028
añade confirmación visible. 124 tests Java + 5 JS, 0 fallos.

Siguiente paso: F6.2 — Entrega local reproducible (PENDIENTE de autorización).
Verificar instrucciones de arranque/test en README, limitaciones y pendientes.
No iniciar Ajustes ni ampliar alcance.

Pendientes abiertos que no bloquean F6.2 pero deben seguir documentados:
- P1: el idle de 120 s en navegador puede subestimar vídeo/ajedrez pasivo.
- P2: Ctrl+C de la consola no se validó limpiamente; la UI usa parada explícita.
- Brave: gate completo pendiente (segundo perfil, reinicio, una hora y choque de
  sesiones Chrome/Brave; el payload de Brave dice browser:"chrome").
- USE-02 PARCIAL: faltan Dota/YouTube en segundo plano, alternancia y cobertura
  sin extensión; el segmento abierto y el pendiente ya están cubiertos.
- SEN-01 PARCIAL: no se indujo un fallo nativo del sensor; el fallo de idle se
  convierte en UNAVAILABLE y el fallo de bridge está probado.
- S3/hibernación NO_DISPONIBLE en este equipo: solo S0; el hueco de congelación
  total se cubrió con simulación y tests de gap.
- Chrome + bridge real no se ejercitó en las pruebas de F5.1; requiere token,
  extensionId y puerto. Sin bridge, Chrome/Brave quedan excluidos.
- Los smokes de UI recorren controles y botones; no son clics humanos ni
  sustituyen una prueba manual de una persona.

Costo aproximado de F5.1: cuatro entregas (E021–E024). F5.2 y F6.1 se cerraron
después con E028–E030 y 124 tests Java + 5 JS. No hay contador de
tokens/créditos; no inventar una cifra. La bandeja se validó con un smoke que
invoca los callbacks del menú, no con un clic humano.

Trabaja solo en una tarea pequeña por entrega, siguiendo Clean Architecture y
los patrones documentados. Verifica archivos y diff antes de editar. No
reconstruyas lo existente, no amplíes el alcance y no modifiques los planos.

Antes de terminar, actualiza ESTADO, FASES, PRUEBAS, DECISIONES y ENTREGAS.
Dime qué funciona, qué probaste y qué falta. Distingue pruebas reales de
simulaciones. Si no tienes acceso a Windows o al navegador, dilo y no declares
pasada una prueba manual.
```

Si la carpeta cambia de equipo, sustituir la ruta del bloque. Los enlaces internos y las reglas son relativos al proyecto para permitir traslado. Nunca copiar tokens, bases reales ni registros sensibles al chat.
