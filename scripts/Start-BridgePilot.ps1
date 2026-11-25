param(
    [Parameter(Mandatory=$true)]
    [ValidatePattern('^[a-p]{32}$')]
    [string]$ExtensionId,
    [ValidateRange(1024,65535)]
    [int]$Port = 47821
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$jar = Join-Path $projectRoot 'target\deriva-0.1.0-SNAPSHOT.jar'
if (!(Test-Path -LiteralPath $jar)) { throw 'Compila primero con Maven verify (ver README).' }
$privateDirectory = Join-Path $projectRoot 'work\pilot'
New-Item -ItemType Directory -Path $privateDirectory -Force | Out-Null
$tokenFile = Join-Path $privateDirectory 'bridge-token.txt'
if (!(Test-Path -LiteralPath $tokenFile)) {
    $bytes = New-Object byte[] 32
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
    $newToken = ([BitConverter]::ToString($bytes)).Replace('-', '').ToLowerInvariant()
    [IO.File]::WriteAllText($tokenFile, $newToken)
}
$token = [IO.File]::ReadAllText($tokenFile).Trim()
if ($token -notmatch '^[0-9a-f]{64}$') { throw 'El archivo de token tiene formato incorrecto.' }
$reportLogFile = Join-Path $privateDirectory 'bridge-reports.log'
$previousToken = $env:DERIVA_BRIDGE_TOKEN
$previousReportLog = $env:DERIVA_BRIDGE_REPORT_LOG
try {
    $env:DERIVA_BRIDGE_TOKEN = $token
    $env:DERIVA_BRIDGE_REPORT_LOG = $reportLogFile
    Write-Host "Token local en $tokenFile (no lo compartas). Usa ese valor en las opciones de la extensión."
    Write-Host "Registro de diagnostico en $reportLogFile (hora, seq, state, hostSi/No; sin hosts)."
    Write-Host 'Ctrl+C detiene el piloto. Captura deshabilitada; reportes solo en memoria.'
    & java -cp $jar com.deriva.app.EmBridgePilot $ExtensionId $Port
    if ($LASTEXITCODE -ne 0) { throw "El piloto terminó con código $LASTEXITCODE" }
} finally {
    if ($null -eq $previousToken) { Remove-Item Env:DERIVA_BRIDGE_TOKEN -ErrorAction SilentlyContinue }
    else { $env:DERIVA_BRIDGE_TOKEN = $previousToken }
    if ($null -eq $previousReportLog) { Remove-Item Env:DERIVA_BRIDGE_REPORT_LOG -ErrorAction SilentlyContinue }
    else { $env:DERIVA_BRIDGE_REPORT_LOG = $previousReportLog }
}
