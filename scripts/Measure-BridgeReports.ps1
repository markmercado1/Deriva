param(
    [Parameter(Mandatory=$true)]
    [ValidatePattern('^[a-p]{32}$')]
    [string]$ExtensionId,
    [ValidateRange(1024,65535)]
    [int]$Port = 47821,
    [ValidateRange(0.05, 240)]
    [double]$DurationMinutes = 60,
    [ValidateRange(100, 2000)]
    [int]$IntervalMilliseconds = 250,
    [string]$TokenFile,
    [string]$OutputFile
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $TokenFile) { $TokenFile = Join-Path $projectRoot 'work\pilot\bridge-token.txt' }
if (-not $OutputFile) { $OutputFile = Join-Path $projectRoot 'work\q2-gate\report-gaps.json' }
if (!(Test-Path -LiteralPath $TokenFile)) { throw 'No existe el token local. Inicia el bridge primero.' }
$token = [IO.File]::ReadAllText($TokenFile).Trim()
if ($token -notmatch '^[0-9a-f]{64}$') { throw 'El token local tiene formato incorrecto.' }

$uri = "http://127.0.0.1:$Port/v1/status"
$headers = @{ 'X-Deriva-Token' = $token; 'Origin' = "chrome-extension://$ExtensionId" }
$outputDirectory = Split-Path $OutputFile -Parent
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

$watch = [Diagnostics.Stopwatch]::StartNew()
$lastAccepted = $null
$lastAcceptedAt = $null
$maxGapMs = 0
$over3s = 0
$over5s = 0
$over10s = 0
$reportsObserved = 0
$samples = 0
$statusErrors = 0
$bridgeRestarts = 0
$acceptedStart = $null
$acceptedEnd = $null

try {
    while ($watch.Elapsed.TotalMinutes -lt $DurationMinutes) {
        $samples++
        try {
            $status = Invoke-RestMethod -Uri $uri -Headers $headers -TimeoutSec 3
            $accepted = [long]$status.accepted
            if ($null -eq $acceptedStart) { $acceptedStart = $accepted }
            $acceptedEnd = $accepted

            if ($null -eq $lastAccepted) {
                $lastAccepted = $accepted
                $lastAcceptedAt = $watch.Elapsed.TotalSeconds
            } elseif ($accepted -lt $lastAccepted) {
                $bridgeRestarts++
                $lastAccepted = $accepted
                $lastAcceptedAt = $watch.Elapsed.TotalSeconds
            } elseif ($accepted -gt $lastAccepted) {
                $reportsObserved += ($accepted - $lastAccepted)
                $gapMs = [math]::Round((($watch.Elapsed.TotalSeconds - $lastAcceptedAt) * 1000), 0)
                if ($gapMs -gt $maxGapMs) { $maxGapMs = $gapMs }
                if ($gapMs -gt 3000) { $over3s++ }
                if ($gapMs -gt 5000) { $over5s++ }
                if ($gapMs -gt 10000) { $over10s++ }
                $lastAccepted = $accepted
                $lastAcceptedAt = $watch.Elapsed.TotalSeconds
            }
        } catch {
            $statusErrors++
        }
        Start-Sleep -Milliseconds $IntervalMilliseconds
    }
} finally {
    $tailGapMs = $null
    if ($null -ne $lastAcceptedAt) {
        $tailGapMs = [math]::Round((($watch.Elapsed.TotalSeconds - $lastAcceptedAt) * 1000), 0)
    }
    $summary = [pscustomobject]@{
        extensionId = $ExtensionId
        durationMinutes = $DurationMinutes
        pollIntervalMs = $IntervalMilliseconds
        resolutionMs = $IntervalMilliseconds
        samples = $samples
        statusErrors = $statusErrors
        bridgeRestarts = $bridgeRestarts
        acceptedStart = $acceptedStart
        acceptedEnd = $acceptedEnd
        reportsObserved = $reportsObserved
        maxGapMs = $maxGapMs
        over3s = $over3s
        over5s = $over5s
        over10s = $over10s
        tailGapMs = $tailGapMs
        tailOver3s = ($null -ne $tailGapMs -and $tailGapMs -gt 3000)
    }
    $summary | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath $OutputFile -Encoding UTF8
    $summary | ConvertTo-Json -Depth 3
}
