param(
    [switch]$Demo,
    [string]$Screenshot,
    [string]$Database,
    [ValidateSet('interruptions', 'usage', 'goals', 'settings')][string]$View,
    [ValidateSet('annotate', 'invalid-range', 'capture', 'capture-close', 'shutdown-close', 'capture-hold', 'delete', 'delete-confirm', 'capture-failure', 'demo-episodes', 'pause', 'tray', 'settings', 'settings-session', 'goals', 'goals-justify')][string]$Smoke,
    [ValidatePattern('^[a-p]{32}$')][string]$ExtensionId,
    [ValidateRange(1024,65535)][int]$Port = 47821,
    [string]$BridgeTokenFile
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$jar = Join-Path $projectRoot 'target\deriva-0.1.0-SNAPSHOT.jar'
if (!(Test-Path -LiteralPath $jar)) { throw 'Compila primero con Maven verify (ver README).' }

$uiDirectory = Join-Path $projectRoot 'work\ui-preview'
New-Item -ItemType Directory -Path $uiDirectory -Force | Out-Null
$classpathFile = Join-Path $uiDirectory 'classpath.txt'
$builtClasspath = Join-Path $projectRoot 'target\classpath.txt'
if (!(Test-Path -LiteralPath $builtClasspath)) { throw 'Falta target\classpath.txt. Compila primero con Maven verify (ver README).' }
Copy-Item -LiteralPath $builtClasspath -Destination $classpathFile -Force
$dependencies = (Get-Content -LiteralPath $classpathFile -Raw).Trim()
if ([string]::IsNullOrWhiteSpace($dependencies)) { throw 'El classpath de Maven esta vacio.' }
$classpath = "$projectRoot\target\classes;$dependencies"

$previousDemo = $env:DERIVA_UI_DEMO
$previousScreenshot = $env:DERIVA_UI_SCREENSHOT
$previousDatabase = $env:DERIVA_UI_DB
$previousView = $env:DERIVA_UI_VIEW
$previousSmoke = $env:DERIVA_UI_SMOKE
$previousBridgeToken = $env:DERIVA_BRIDGE_TOKEN
$previousExtensionId = $env:DERIVA_EXTENSION_ID
$previousBridgePort = $env:DERIVA_BRIDGE_PORT
try {
    if ($Demo) { $env:DERIVA_UI_DEMO = '1' } else { Remove-Item Env:DERIVA_UI_DEMO -ErrorAction SilentlyContinue }
    if ($Screenshot) { $env:DERIVA_UI_SCREENSHOT = $Screenshot } else { Remove-Item Env:DERIVA_UI_SCREENSHOT -ErrorAction SilentlyContinue }
    if ($Database) { $env:DERIVA_UI_DB = $Database } else { Remove-Item Env:DERIVA_UI_DB -ErrorAction SilentlyContinue }
    if ($View) { $env:DERIVA_UI_VIEW = $View } else { Remove-Item Env:DERIVA_UI_VIEW -ErrorAction SilentlyContinue }
    if ($Smoke) { $env:DERIVA_UI_SMOKE = $Smoke } else { Remove-Item Env:DERIVA_UI_SMOKE -ErrorAction SilentlyContinue }
    if ($ExtensionId) {
        if (-not $BridgeTokenFile) { $BridgeTokenFile = Join-Path $projectRoot 'work\pilot\bridge-token.txt' }
        if (!(Test-Path -LiteralPath $BridgeTokenFile)) {
            throw 'No existe el token local del bridge. Ejecuta Start-BridgePilot.ps1 una vez, detenlo y vuelve a intentar.'
        }
        $bridgeToken = (Get-Content -LiteralPath $BridgeTokenFile -Raw).Trim()
        if ($bridgeToken -notmatch '^[0-9a-f]{64}$') { throw 'El token local del bridge tiene formato incorrecto.' }
        if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) {
            throw "El puerto $Port ya esta en uso. Cierra Start-BridgePilot/ConsoleCapture antes de abrir la UI."
        }
        $env:DERIVA_BRIDGE_TOKEN = $bridgeToken
        $env:DERIVA_EXTENSION_ID = $ExtensionId
        $env:DERIVA_BRIDGE_PORT = "$Port"
    }
    if ($Demo) { Write-Host 'UI en modo demo: datos de ejemplo, sin captura real.' }
    elseif ($Database) { Write-Host "UI contra base indicada: $Database" }
    else { Write-Host 'UI real: usa work/console-day/deriva.db.' }
    if ($ExtensionId) {
        Write-Host "Bridge en 127.0.0.1:$Port; extension $ExtensionId; Chrome/Brave con un perfil."
    }
    if ($View) { Write-Host "Vista inicial: $View" }
    if ($Screenshot) { Write-Host "Captura de UI: $Screenshot" }
    if ($Smoke) { Write-Host "Smoke de UI: $Smoke (ruta de controles, no clic humano)." }
    & java -cp $classpath com.deriva.app.Launcher
    if ($LASTEXITCODE -ne 0) { throw "DerivaApp termino con codigo $LASTEXITCODE" }
} finally {
    if ($null -eq $previousDemo) { Remove-Item Env:DERIVA_UI_DEMO -ErrorAction SilentlyContinue }
    else { $env:DERIVA_UI_DEMO = $previousDemo }
    if ($null -eq $previousScreenshot) { Remove-Item Env:DERIVA_UI_SCREENSHOT -ErrorAction SilentlyContinue }
    else { $env:DERIVA_UI_SCREENSHOT = $previousScreenshot }
    if ($null -eq $previousDatabase) { Remove-Item Env:DERIVA_UI_DB -ErrorAction SilentlyContinue }
    else { $env:DERIVA_UI_DB = $previousDatabase }
    if ($null -eq $previousView) { Remove-Item Env:DERIVA_UI_VIEW -ErrorAction SilentlyContinue }
    else { $env:DERIVA_UI_VIEW = $previousView }
    if ($null -eq $previousSmoke) { Remove-Item Env:DERIVA_UI_SMOKE -ErrorAction SilentlyContinue }
    else { $env:DERIVA_UI_SMOKE = $previousSmoke }
    if ($ExtensionId) {
        if ($null -eq $previousBridgeToken) { Remove-Item Env:DERIVA_BRIDGE_TOKEN -ErrorAction SilentlyContinue }
        else { $env:DERIVA_BRIDGE_TOKEN = $previousBridgeToken }
        if ($null -eq $previousExtensionId) { Remove-Item Env:DERIVA_EXTENSION_ID -ErrorAction SilentlyContinue }
        else { $env:DERIVA_EXTENSION_ID = $previousExtensionId }
        if ($null -eq $previousBridgePort) { Remove-Item Env:DERIVA_BRIDGE_PORT -ErrorAction SilentlyContinue }
        else { $env:DERIVA_BRIDGE_PORT = $previousBridgePort }
    }
}
