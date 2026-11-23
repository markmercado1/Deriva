param(
    [string]$OutputFile,
    [ValidateRange(1, 3600)][int]$IntervalSeconds = 30,
    [ValidateRange(0, 1440)][double]$DurationMinutes = 0,
    [int]$ProcessId = 0,
    [ValidateRange(0, 300)][int]$WaitForProcessSeconds = 30
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent

if (-not $OutputFile) {
    $stamp = (Get-Date).ToString('yyyyMMdd-HHmmss')
    $OutputFile = Join-Path $projectRoot "work\f6\ui-resources-$stamp.csv"
}
$outputDirectory = Split-Path -Parent $OutputFile
if ($outputDirectory) { New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null }
if (Test-Path -LiteralPath $OutputFile) {
    throw "El CSV ya existe: $OutputFile. Usa otro nombre para no mezclar sesiones."
}

# Localiza solo el proceso Java de la UI. La linea de comandos se usa para
# identificar el proceso y no se guarda en el CSV.
function Find-UiProcessId {
    $found = @(Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" -ErrorAction Stop |
        Where-Object { $_.CommandLine -and $_.CommandLine -like '*com.deriva.app.Launcher*' })
    if ($found.Count -eq 0) { return $null }
    if ($found.Count -gt 1) {
        $ids = ($found | ForEach-Object { $_.ProcessId }) -join ', '
        throw "Hay varios procesos de la UI (PID $ids). Cierra los sobrantes o usa -ProcessId."
    }
    return [int]$found[0].ProcessId
}

$targetPid = if ($ProcessId -gt 0) { $ProcessId } else { $null }
if ($null -eq $targetPid) {
    $deadline = (Get-Date).AddSeconds($WaitForProcessSeconds)
    while ($true) {
        $targetPid = Find-UiProcessId
        if ($null -ne $targetPid) { break }
        if ((Get-Date) -ge $deadline) {
            throw 'No se encontro el proceso de la UI (java com.deriva.app.Launcher). Inicia Start-Ui.ps1 primero.'
        }
        Start-Sleep -Seconds 1
    }
}

$logicalCpus = [Environment]::ProcessorCount
$watch = [Diagnostics.Stopwatch]::StartNew()
$samples = 0
$ended = 'duration'
$previousCpuSeconds = $null
$previousElapsedSeconds = 0
$durationSeconds = if ($DurationMinutes -gt 0) { [double]($DurationMinutes * 60) } else { [double]::PositiveInfinity }

function Write-Row {
    param($Timestamp, $ElapsedSeconds, $PidValue, $CpuPercent, $WorkingSetMb, $PrivateMb, $ThreadCount, $HandleCount)
    [pscustomobject][ordered]@{
        Timestamp = $Timestamp
        ElapsedSeconds = $ElapsedSeconds
        Pid = $PidValue
        LogicalCpus = $logicalCpus
        CpuPercent = $CpuPercent
        WorkingSetMB = $WorkingSetMb
        PrivateMB = $PrivateMb
        ThreadCount = $ThreadCount
        HandleCount = $HandleCount
    } | Export-Csv -LiteralPath $OutputFile -NoTypeInformation -Encoding UTF8 -Append
}

try {
    $process = Get-Process -Id $targetPid -ErrorAction Stop
} catch {
    throw "El proceso $targetPid no existe."
}
try {
    $previousCpuSeconds = $process.TotalProcessorTime.TotalSeconds
    Write-Row (Get-Date).ToString('o') 0 $targetPid '' `
        ([math]::Round($process.WorkingSet64 / 1MB, 1)) `
        ([math]::Round($process.PrivateMemorySize64 / 1MB, 1)) `
        $process.Threads.Count $process.HandleCount
} finally {
    $process.Dispose()
}

try {
    $tick = 0
    while ($true) {
        $tick++
        $targetElapsed = $tick * $IntervalSeconds
        if ($targetElapsed -gt $durationSeconds) { $ended = 'duration'; break }
        $sleepMs = [int][math]::Ceiling(($targetElapsed - $watch.Elapsed.TotalSeconds) * 1000)
        if ($sleepMs -gt 0) { Start-Sleep -Milliseconds $sleepMs }
        try {
            $process = Get-Process -Id $targetPid -ErrorAction Stop
        } catch {
            $ended = 'process-exited'
            Write-Warning "El proceso $targetPid termino; se detiene la medicion."
            break
        }
        try {
            $elapsed = $watch.Elapsed.TotalSeconds
            $cpuNow = $process.TotalProcessorTime.TotalSeconds
            $wall = $elapsed - $previousElapsedSeconds
            $cpuPercent = if ($wall -gt 0) {
                [math]::Round(100 * ($cpuNow - $previousCpuSeconds) / $wall / $logicalCpus, 2)
            } else { '' }
            Write-Row (Get-Date).ToString('o') ([math]::Round($elapsed, 3)) $targetPid $cpuPercent `
                ([math]::Round($process.WorkingSet64 / 1MB, 1)) `
                ([math]::Round($process.PrivateMemorySize64 / 1MB, 1)) `
                $process.Threads.Count $process.HandleCount
            $samples++
            $previousCpuSeconds = $cpuNow
            $previousElapsedSeconds = $elapsed
        } finally {
            $process.Dispose()
        }
    }
} finally {
    $watch.Stop()
    Write-Host "Medicion: $OutputFile"
    Write-Host "Muestras: $samples; intervalo: ${IntervalSeconds}s; CPUs logicas: $logicalCpus; fin: $ended"
}
