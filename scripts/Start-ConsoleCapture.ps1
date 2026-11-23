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
$tokenFile = Join-Path $privateDirectory 'bridge-token.txt'
if (!(Test-Path -LiteralPath $tokenFile)) { throw 'No existe el token local. Inicia el bridge una vez para generarlo.' }
$token = [IO.File]::ReadAllText($tokenFile).Trim()
if ($token -notmatch '^[0-9a-f]{64}$') { throw 'El token local tiene formato incorrecto.' }

$consoleDirectory = Join-Path $projectRoot 'work\console-day'
New-Item -ItemType Directory -Path $consoleDirectory -Force | Out-Null
$database = Join-Path $consoleDirectory 'deriva.db'
$stopFile = Join-Path $consoleDirectory 'STOP'
$classpathFile = Join-Path $consoleDirectory 'classpath.txt'
& mvn '-Dmaven.repo.local=C:\AppTImeControl\work\m2' --offline --batch-mode --no-transfer-progress dependency:build-classpath "-Dmdep.outputFile=$classpathFile"
if ($LASTEXITCODE -ne 0) { throw 'No se pudo resolver el classpath de Maven.' }
$dependencies = (Get-Content -LiteralPath $classpathFile -Raw).Trim()
if ([string]::IsNullOrWhiteSpace($dependencies)) { throw 'El classpath de Maven esta vacio.' }
$classpath = "$projectRoot\target\classes;$dependencies"

$previousToken = $env:DERIVA_BRIDGE_TOKEN
$previousDb = $env:DERIVA_CONSOLE_DB
$previousStop = $env:DERIVA_CONSOLE_STOP_FILE
try {
    $env:DERIVA_BRIDGE_TOKEN = $token
    $env:DERIVA_CONSOLE_DB = $database
    $env:DERIVA_CONSOLE_STOP_FILE = $stopFile
    Write-Host "Token local en $tokenFile (no lo compartas)."
    Write-Host "Base de consola: $database"
    Write-Host "La extension debe usar el puerto $Port y el token local."
    Write-Host "Detener con Ctrl+C. Si Ctrl+C no responde:"
    Write-Host "  New-Item -ItemType File '$stopFile' -Force"
    Write-Host 'La captura guarda cada segmento al cerrarse; al salir muestra el resumen del dia.'
    & java -cp $classpath com.deriva.app.ConsoleCapture $ExtensionId $Port
    if ($LASTEXITCODE -ne 0) { throw "ConsoleCapture termino con codigo $LASTEXITCODE" }
} finally {
    if ($null -eq $previousToken) { Remove-Item Env:DERIVA_BRIDGE_TOKEN -ErrorAction SilentlyContinue }
    else { $env:DERIVA_BRIDGE_TOKEN = $previousToken }
    if ($null -eq $previousDb) { Remove-Item Env:DERIVA_CONSOLE_DB -ErrorAction SilentlyContinue }
    else { $env:DERIVA_CONSOLE_DB = $previousDb }
    if ($null -eq $previousStop) { Remove-Item Env:DERIVA_CONSOLE_STOP_FILE -ErrorAction SilentlyContinue }
    else { $env:DERIVA_CONSOLE_STOP_FILE = $previousStop }
}
