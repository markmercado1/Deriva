# Genera los archivos de distribución de Deriva en target\dist:
#   Deriva\Deriva.exe              aplicación con su propio runtime Java (jpackage, app-image)
#   Deriva-<v>-portable.zip        la carpeta anterior comprimida (sin instalación)
#   Deriva-<v>-Setup.exe           instalador por usuario (IExpress de Windows)
# Requiere haber ejecutado antes "mvn ... verify" (ver README).
param([string]$Version = '0.1.0')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$jarName = 'deriva-0.1.0-SNAPSHOT.jar'
$jar = Join-Path $projectRoot "target\$jarName"
$classpathFile = Join-Path $projectRoot 'target\classpath.txt'
if (!(Test-Path -LiteralPath $jar) -or !(Test-Path -LiteralPath $classpathFile)) {
    throw 'Falta el JAR o target\classpath.txt. Compila primero con Maven verify (ver README).'
}
if (!(Get-Command jpackage -ErrorAction SilentlyContinue)) { throw 'No se encontró jpackage (JDK 21).' }

$dist = Join-Path $projectRoot 'target\dist'
$staging = Join-Path $projectRoot 'target\package-input'
foreach ($dir in @($dist, $staging)) {
    if (Test-Path -LiteralPath $dir) { Remove-Item -LiteralPath $dir -Recurse -Force }
    New-Item -ItemType Directory -Path $dir | Out-Null
}

# 1. Entrada de jpackage: el JAR de la app y sus dependencias de ejecución.
Copy-Item -LiteralPath $jar -Destination $staging
foreach ($entry in (Get-Content -LiteralPath $classpathFile -Raw).Trim().Split(';')) {
    if ($entry) { Copy-Item -LiteralPath $entry -Destination $staging }
}

# La extensión de Chrome/Brave viaja dentro de la aplicación (carpeta "extension" junto a Deriva.exe).
$appContent = Join-Path $projectRoot 'target\app-content'
if (Test-Path -LiteralPath $appContent) { Remove-Item -LiteralPath $appContent -Recurse -Force }
New-Item -ItemType Directory -Path (Join-Path $appContent 'extension') -Force | Out-Null
Copy-Item -Path (Join-Path $projectRoot 'extension\chromium\*') -Destination (Join-Path $appContent 'extension') -Recurse

# 2. Aplicación con runtime propio (no hace falta Java instalado para usarla).
& jpackage --type app-image --name Deriva --app-version $Version `
    --vendor 'Emerson Mercado' --description 'Deriva - actividad local' `
    --app-content (Join-Path $appContent 'extension') `
    --icon (Join-Path $projectRoot 'packaging\deriva.ico') `
    --input $staging --main-jar $jarName --main-class com.deriva.app.EmLauncher `
    --dest $dist
if ($LASTEXITCODE -ne 0) { throw "jpackage terminó con código $LASTEXITCODE" }
$appImage = Join-Path $dist 'Deriva'
if (!(Test-Path -LiteralPath (Join-Path $appImage 'Deriva.exe'))) { throw 'jpackage no generó Deriva.exe.' }
if (!(Test-Path -LiteralPath (Join-Path $appImage 'extension\manifest.json'))) { throw 'Falta la extensión dentro de la aplicación.' }

# 3. Versión portable.
$portable = Join-Path $dist "Deriva-$Version-portable.zip"
Compress-Archive -Path $appImage -DestinationPath $portable -Force

# 4. Instalador IExpress: descomprime en %LOCALAPPDATA%\Programs\Deriva y crea accesos directos.
$setupFiles = Join-Path $projectRoot 'target\setup-files'
if (Test-Path -LiteralPath $setupFiles) { Remove-Item -LiteralPath $setupFiles -Recurse -Force }
New-Item -ItemType Directory -Path $setupFiles | Out-Null
Compress-Archive -Path (Join-Path $appImage '*') -DestinationPath (Join-Path $setupFiles 'Deriva-app.zip') -Force
foreach ($file in @('install.cmd', 'install.ps1', 'uninstall.ps1', 'deriva.ico')) {
    Copy-Item -LiteralPath (Join-Path $projectRoot "packaging\$file") -Destination $setupFiles
}
Set-Content -LiteralPath (Join-Path $setupFiles 'version.txt') -Value $Version -Encoding ASCII
$setupExe = Join-Path $dist "Deriva-$Version-Setup.exe"
$sed = Join-Path $projectRoot 'target\deriva-setup.sed'
$sedText = @"
[Version]
Class=IEXPRESS
SEDVersion=3
[Options]
PackagePurpose=InstallApp
ShowInstallProgramWindow=1
HideExtractAnimation=0
UseLongFileName=1
InsideCompressed=0
CAB_FixedSize=0
CAB_ResvCodeSigning=0
RebootMode=N
InstallPrompt=%InstallPrompt%
DisplayLicense=%DisplayLicense%
FinishMessage=%FinishMessage%
TargetName=%TargetName%
FriendlyName=%FriendlyName%
AppLaunched=%AppLaunched%
PostInstallCmd=%PostInstallCmd%
AdminQuietInstCmd=%AdminQuietInstCmd%
UserQuietInstCmd=%UserQuietInstCmd%
SourceFiles=SourceFiles
[Strings]
InstallPrompt=
DisplayLicense=
FinishMessage=
TargetName=$setupExe
FriendlyName=Instalador de Deriva $Version
AppLaunched=cmd /c install.cmd
PostInstallCmd=<None>
AdminQuietInstCmd=
UserQuietInstCmd=
FILE0="install.cmd"
FILE1="install.ps1"
FILE2="uninstall.ps1"
FILE3="version.txt"
FILE4="Deriva-app.zip"
FILE5="deriva.ico"
[SourceFiles]
SourceFiles0=$setupFiles\
[SourceFiles0]
%FILE0%=
%FILE1%=
%FILE2%=
%FILE3%=
%FILE4%=
%FILE5%=
"@
Set-Content -LiteralPath $sed -Value $sedText -Encoding ASCII
$iexpress = Start-Process -FilePath "$env:SystemRoot\System32\iexpress.exe" -ArgumentList '/N', '/Q', $sed -Wait -PassThru
if (!(Test-Path -LiteralPath $setupExe)) { throw "IExpress no generó el instalador (código $($iexpress.ExitCode))." }

Get-ChildItem -LiteralPath $dist -File | ForEach-Object {
    $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
    '{0}  {1,12:N0} bytes  SHA256 {2}' -f $_.Name, $_.Length, $hash
}
