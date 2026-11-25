# Instalador por usuario de Deriva. Lo ejecuta Deriva-<version>-Setup.exe (IExpress)
# desde su carpeta temporal; no necesita permisos de administrador.
param([switch]$Quiet)  # -Quiet: sin diálogos (pruebas automáticas)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$version = (Get-Content -LiteralPath (Join-Path $here 'version.txt') -Raw).Trim()
$target = Join-Path $env:LOCALAPPDATA 'Programs\Deriva'
$title = "Instalador de Deriva $version"

function Show-Message([string]$text, [string]$icon = 'Information', [string]$buttons = 'OK') {
    if ($Quiet) { Write-Host $text; return $(if ($buttons -eq 'YesNo') { 'Yes' } else { 'OK' }) }
    return [System.Windows.Forms.MessageBox]::Show($text, $title, $buttons, $icon)
}

try {
    if (Get-Process -Name 'Deriva' -ErrorAction SilentlyContinue) {
        Show-Message ("Deriva está abierta. Ciérrala desde el icono de la bandeja (Salir) " +
            "y vuelve a ejecutar el instalador.") 'Warning' | Out-Null
        exit 1
    }
    $answer = Show-Message ("Se instalará Deriva $version en:`n$target`n`n" +
        "Se crearán accesos directos en el menú Inicio y en el escritorio. " +
        "Tus datos se guardan aparte, en $env:LOCALAPPDATA\Deriva, y no se tocan.`n`n¿Continuar?") `
        'Question' 'YesNo'
    if ($answer -ne 'Yes') { exit 2 }

    if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Recurse -Force }
    New-Item -ItemType Directory -Path $target -Force | Out-Null
    Expand-Archive -LiteralPath (Join-Path $here 'Deriva-app.zip') -DestinationPath $target -Force
    Copy-Item -LiteralPath (Join-Path $here 'uninstall.ps1') -Destination $target -Force

    $exe = Join-Path $target 'Deriva.exe'
    if (!(Test-Path -LiteralPath $exe)) { throw "No se encontró Deriva.exe tras descomprimir." }

    $shell = New-Object -ComObject WScript.Shell
    $links = @(
        (Join-Path ([Environment]::GetFolderPath('Programs')) 'Deriva.lnk'),
        (Join-Path ([Environment]::GetFolderPath('Desktop')) 'Deriva.lnk')
    )
    foreach ($path in $links) {
        $link = $shell.CreateShortcut($path)
        $link.TargetPath = $exe
        $link.WorkingDirectory = $target
        $link.IconLocation = "$exe,0"
        $link.Description = 'Deriva — actividad local'
        $link.Save()
    }

    $key = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\Deriva'
    New-Item -Path $key -Force | Out-Null
    $uninstall = "powershell.exe -NoProfile -ExecutionPolicy Bypass -File `"$target\uninstall.ps1`""
    $values = @{
        DisplayName = 'Deriva'; DisplayVersion = $version; Publisher = 'Deriva';
        InstallLocation = $target; DisplayIcon = "$exe,0"; UninstallString = $uninstall;
        NoModify = 1; NoRepair = 1
    }
    foreach ($name in $values.Keys) {
        $type = if ($values[$name] -is [int]) { 'DWord' } else { 'String' }
        New-ItemProperty -Path $key -Name $name -Value $values[$name] -PropertyType $type -Force | Out-Null
    }

    $open = Show-Message "Deriva $version se instaló correctamente.`n`n¿Abrir Deriva ahora?" 'Information' 'YesNo'
    if ($open -eq 'Yes' -and -not $Quiet) { Start-Process -FilePath $exe -WorkingDirectory $target }
    exit 0
} catch {
    Show-Message ("No se pudo completar la instalación: " + $_.Exception.Message) 'Error' | Out-Null
    exit 1
}
