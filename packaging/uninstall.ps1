# Desinstalador de Deriva (lo registra install.ps1 en "Aplicaciones instaladas").
# Quita el programa y los accesos directos; conserva los datos en %LOCALAPPDATA%\Deriva.
param([switch]$Quiet)  # -Quiet: sin diálogos (pruebas automáticas)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
$title = 'Desinstalar Deriva'
$target = Join-Path $env:LOCALAPPDATA 'Programs\Deriva'
$data = Join-Path $env:LOCALAPPDATA 'Deriva'

if (Get-Process -Name 'Deriva' -ErrorAction SilentlyContinue) {
    [System.Windows.Forms.MessageBox]::Show(
        'Deriva está abierta. Ciérrala desde la bandeja (Salir) y vuelve a intentarlo.',
        $title, 'OK', 'Warning') | Out-Null
    exit 1
}
$answer = if ($Quiet) { 'Yes' } else { [System.Windows.Forms.MessageBox]::Show(
    "Se quitará Deriva de este equipo.`n`nTus registros en $data se conservan; bórralos a mano si ya no los necesitas.`n`n¿Continuar?",
    $title, 'YesNo', 'Question') }
if ($answer -ne 'Yes') { exit 2 }

foreach ($path in @(
        (Join-Path ([Environment]::GetFolderPath('Programs')) 'Deriva.lnk'),
        (Join-Path ([Environment]::GetFolderPath('Desktop')) 'Deriva.lnk'))) {
    if (Test-Path -LiteralPath $path) { Remove-Item -LiteralPath $path -Force }
}
Remove-Item -Path 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\Deriva' -Recurse -Force -ErrorAction SilentlyContinue
# The script lives inside $target; Windows allows deleting it while it runs.
if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Recurse -Force }
if ($Quiet) { 'Deriva se desinstaló.' } else { [System.Windows.Forms.MessageBox]::Show('Deriva se desinstaló.', $title, 'OK', 'Information') | Out-Null }
