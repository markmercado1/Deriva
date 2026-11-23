# F6.1 — Medición de CPU y memoria de la UI

Estado: **HECHO**. Medición sostenida aceptada en E030 con `work/f6/ui-resources.csv`.

## Resultado (2026-11-23)

- Archivo: `work/f6/ui-resources.csv`, SHA-256 `2DF7C43703890F7A43D6086E7ED6F3A27CC8542A8B7C6EF129FFC19175FA0EDD`.
- 256 muestras cada 30 s; 7.650,019 s (2 h 7 min 30 s); hueco máximo 30,025 s.
- CPU: media 0,15 %, máxima 0,40 %, p95 0,24 % del total de 16 CPUs lógicas.
- Working set: 394,7 → 249,0 MB (máx. 416,4 MB). Memoria privada: 439,6 → 484,0 MB (máx. 484,0 MB).
- Hilos 67–70; handles 750–760.
- Captura activa: 7.646 s registrados entre 22:04 y 00:12, con Brave/bridge (3.390 s con host), inactividad y exclusiones.
- Equipo: ASUS TUF Gaming F15 FX507ZC4, i5-12500H (12C/16T), 15,6 GB, Windows 11 Home 10.0.26200, Java 21.0.7.
- Límites: una máquina, resolución 30 s, solo el proceso de la UI; no se midió la sesión posterior de la mañana. Detalle en `docs/PRUEBAS.md` E030.

## Qué mide

`scripts/Measure-UiResources.ps1` muestrea el proceso Java de la UI cada 30 s
(configurable) y escribe un CSV:

```text
Timestamp,ElapsedSeconds,Pid,LogicalCpus,CpuPercent,WorkingSetMB,PrivateMB,ThreadCount,HandleCount
```

- `CpuPercent` es el consumo del proceso respecto a un intervalo real de pared,
  normalizado por las CPUs lógicas del equipo. Es un porcentaje del total del
  sistema, no de un núcleo.
- `WorkingSetMB` es la memoria residente; `PrivateMB`, la memoria privada.
- La primera fila es la línea base (CPU vacía).
- Solo se guardan PID y métricas. No se guardan títulos de ventana, nombres de
  documentos, hosts, URLs, rutas de navegador ni la línea de comandos.
- El medidor no abre la base de datos ni escribe en ella.

## Protocolo mínimo

1. Cerrar la UI y cualquier captura anterior. Comprobar que no queda `java.exe`.
2. Copiar la base real a `work/f6/` (ver comandos). No medir sobre la base real.
3. Elegir y anotar: fecha, modelo de CPU, RAM, versión de Windows, Java, duración
   prevista y si hay bridge/navegador habilitado.
4. Iniciar la UI con la base copiada y pulsar **Iniciar captura** en la barra
   lateral.
5. Arrancar el medidor en otra consola. Si se arranca antes que la UI, espera
   hasta 30 s a que aparezca el proceso.
6. Dejar la sesión durante la duración elegida. No cerrar la captura durante la
   medición salvo que se quiera registrar ese evento aparte.
7. Al terminar: **Detener captura**, cerrar la ventana, parar el medidor con
   `Ctrl+C` (el CSV ya tiene cada fila escrita) y comprobar que no queda
   `java.exe`.
8. Guardar el CSV y el hash de la base copiada. No hace falta adjuntar la base.

## Comandos

```powershell
# 1. Copia segura (con la UI cerrada). Incluye WAL/SHM si existen.
$src = 'C:\AppTImeControl\work\console-day\deriva.db'
$dst = 'C:\AppTImeControl\work\f6\deriva-f6.db'
New-Item -ItemType Directory -Path (Split-Path $dst -Parent) -Force | Out-Null
$hashBefore = (Get-FileHash -Algorithm SHA256 -LiteralPath $src).Hash
Remove-Item -LiteralPath "$dst-wal","$dst-shm" -ErrorAction SilentlyContinue
Copy-Item -LiteralPath $src -Destination $dst -Force
foreach ($suffix in '-wal','-shm') {
    if (Test-Path -LiteralPath "$src$suffix") {
        Copy-Item -LiteralPath "$src$suffix" -Destination "$dst$suffix" -Force
    }
}
$hashAfter = (Get-FileHash -Algorithm SHA256 -LiteralPath $src).Hash
"Origen intacto: $($hashBefore -eq $hashAfter) ($hashAfter)"
Get-FileHash -Algorithm SHA256 -LiteralPath $dst

# 2. Medidor (consola B). Empieza a muestrear cada 30 s.
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Measure-UiResources.ps1 `
    -OutputFile .\work\f6\ui-resources.csv -IntervalSeconds 30 -DurationMinutes 60

# 3. UI real con la base copiada (consola A).
powershell -NoProfile -File .\scripts\Start-Ui.ps1 `
    -Database C:\AppTImeControl\work\f6\deriva-f6.db
# Después, pulsar "Iniciar captura" en la barra lateral.
```

`-DurationMinutes 0` (valor por defecto) mide hasta que se cierre la UI o se pulse
`Ctrl+C`. Para una prueba de 60 min, usar `-DurationMinutes 60`; el medidor
termina solo y no queda ningún proceso en segundo plano.

## Bridge para capturar sitios de Brave

La UI inicia el bridge dentro de su propio proceso. **No** hay que arrancar
`Start-BridgePilot.ps1` ni `Start-ConsoleCapture.ps1` a la vez; si ocupan el
puerto, la UI falla al enlazar.

```powershell
powershell -NoProfile -File .\scripts\Start-Ui.ps1 `
    -Database C:\AppTImeControl\work\f6\deriva-f6.db `
    -ExtensionId cnipocakflhegoediialcdngmopbieip `
    -Port 47821
```

- El ID documentado del perfil probado es `cnipocakflhegoediialcdngmopbieip`.
  Confírmalo en `brave://extensions` del perfil que vayas a usar; si Brave cargó
  la extensión desde otra ruta, el ID puede ser distinto y hay que pasar ese.
- `-BridgeTokenFile` usa por defecto `work\pilot\bridge-token.txt`. El token se
  lee sin imprimirlo; cópialo a las opciones de la extensión de Brave con
  `Get-Content .\work\pilot\bridge-token.txt -Raw | Set-Clipboard`.
- En las opciones de la extensión de Brave deben coincidir el puerto `47821` y
  ese token. Después, pulsar **Iniciar captura** en la barra lateral.
- Un solo navegador/perfil a la vez: el payload de Brave dice `browser:"chrome"`;
  si Chrome y Brave reportan al mismo bridge, se pisan. Para un día con Brave,
  deshabilita la extensión en Chrome o cierra Chrome.
- El medidor no abre puertos ni habla con el bridge: solo lee CPU/memoria del
  proceso Java, así que puede correr a la vez sin conflicto.

## Parada y comprobación

```powershell
# En la UI: Detener captura y cerrar la ventana.
# En la consola del medidor: Ctrl+C.
Get-Process java -ErrorAction SilentlyContinue | Select-Object Id, ProcessName
Get-Content .\work\f6\ui-resources.csv -TotalCount 3
```

Si `Get-Process java` devuelve algo después de cerrar la UI, no cerrar el equipo
ni forzar la base: anotar el PID y revisar antes de continuar.

## Límites

- Medición de un solo proceso Java; no incluye procesos hijos (la UI no los usa
  hoy) ni el coste del bridge si se ejecuta aparte.
- La resolución es de 30 s; picos más cortos pueden quedar suavizados.
- La copia de la base es un archivo, no un backup SQLite online: debe hacerse con
  la UI cerrada. Si existe WAL, se copia también para no perder commits.
- No sustituye una prueba de estabilidad de varias horas ni una prueba manual de
  una persona.
