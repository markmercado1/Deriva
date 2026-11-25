# Asistente de instalación de Deriva (por usuario, sin permisos de administrador).
# Lo ejecuta Deriva-<versión>-Setup.exe (IExpress) desde su carpeta temporal.
#   -Quiet               instala sin ventanas en la carpeta por defecto (pruebas automáticas)
#   -CaptureDir <ruta>   guarda una imagen de cada paso del asistente sin instalar nada (documentación)
param([switch]$Quiet, [string]$CaptureDir)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.Windows.Forms.Application]::EnableVisualStyles()

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$version = (Get-Content -LiteralPath (Join-Path $here 'version.txt') -Raw).Trim()
$payload = Join-Path $here 'Deriva-app.zip'
$defaultTarget = Join-Path $env:LOCALAPPDATA 'Programs\Deriva'
$dataFolder = Join-Path $env:LOCALAPPDATA 'Deriva'
$title = "Instalación de Deriva $version"

function Install-Deriva([string]$target, [bool]$desktop, [bool]$startMenu, [scriptblock]$progress) {
    if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target -Recurse -Force }
    New-Item -ItemType Directory -Path $target -Force | Out-Null
    $zip = [System.IO.Compression.ZipFile]::OpenRead($payload)
    try {
        $entries = @($zip.Entries)
        for ($i = 0; $i -lt $entries.Count; $i++) {
            $entry = $entries[$i]
            $destination = Join-Path $target $entry.FullName
            if ($entry.FullName.EndsWith('/')) { New-Item -ItemType Directory -Path $destination -Force | Out-Null; continue }
            New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
            [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $destination, $true)
            if ($progress -and ($i % 20 -eq 0 -or $i -eq $entries.Count - 1)) {
                & $progress ([int](($i + 1) * 90 / $entries.Count)) $entry.FullName
            }
        }
    } finally { $zip.Dispose() }
    Copy-Item -LiteralPath (Join-Path $here 'uninstall.ps1') -Destination $target -Force
    $exe = Join-Path $target 'Deriva.exe'
    if (!(Test-Path -LiteralPath $exe)) { throw 'No se encontró Deriva.exe tras copiar los archivos.' }

    $shell = New-Object -ComObject WScript.Shell
    $links = @()
    if ($startMenu) { $links += (Join-Path ([Environment]::GetFolderPath('Programs')) 'Deriva.lnk') }
    if ($desktop) { $links += (Join-Path ([Environment]::GetFolderPath('Desktop')) 'Deriva.lnk') }
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
    $values = @{
        DisplayName = 'Deriva'; DisplayVersion = $version; Publisher = 'Emerson Mercado';
        InstallLocation = $target; DisplayIcon = "$exe,0"; NoModify = 1; NoRepair = 1
        UninstallString = "powershell.exe -NoProfile -ExecutionPolicy Bypass -File `"$target\uninstall.ps1`""
    }
    foreach ($name in $values.Keys) {
        $type = if ($values[$name] -is [int]) { 'DWord' } else { 'String' }
        New-ItemProperty -Path $key -Name $name -Value $values[$name] -PropertyType $type -Force | Out-Null
    }
    if ($progress) { & $progress 100 'Listo' }
    return $exe
}

if ($Quiet) {
    $exe = Install-Deriva $defaultTarget $true $true $null
    Write-Host "Deriva $version instalada en $(Split-Path -Parent $exe)"
    exit 0
}

# ---------------------------------------------------------------- asistente
$font = New-Object System.Drawing.Font('Segoe UI', 9)
$form = New-Object System.Windows.Forms.Form
$form.Text = $title
$form.ClientSize = New-Object System.Drawing.Size(560, 380)
$form.FormBorderStyle = 'FixedDialog'
$form.MaximizeBox = $false
$form.MinimizeBox = $false
$form.StartPosition = 'CenterScreen'
$form.Font = $font
$iconFile = Join-Path $here 'deriva.ico'
if (Test-Path -LiteralPath $iconFile) { $form.Icon = New-Object System.Drawing.Icon($iconFile) }

$header = New-Object System.Windows.Forms.Panel
$header.SetBounds(0, 0, 560, 62); $header.BackColor = [System.Drawing.Color]::White
$headerTitle = New-Object System.Windows.Forms.Label
$headerTitle.SetBounds(16, 10, 460, 20); $headerTitle.Font = New-Object System.Drawing.Font('Segoe UI', 10, [System.Drawing.FontStyle]::Bold)
$headerText = New-Object System.Windows.Forms.Label
$headerText.SetBounds(30, 32, 450, 22)
$header.Controls.AddRange(@($headerTitle, $headerText))
if (Test-Path -LiteralPath $iconFile) {
    $logo = New-Object System.Windows.Forms.PictureBox
    $logo.SetBounds(504, 11, 40, 40); $logo.SizeMode = 'StretchImage'
    $logo.Image = (New-Object System.Drawing.Icon($iconFile, 48, 48)).ToBitmap()
    $header.Controls.Add($logo)
}
$line1 = New-Object System.Windows.Forms.Label
$line1.SetBounds(0, 62, 560, 2); $line1.BorderStyle = 'Fixed3D'
$line2 = New-Object System.Windows.Forms.Label
$line2.SetBounds(0, 326, 560, 2); $line2.BorderStyle = 'Fixed3D'

$back = New-Object System.Windows.Forms.Button
$back.SetBounds(282, 340, 82, 26); $back.Text = '< Atrás'
$next = New-Object System.Windows.Forms.Button
$next.SetBounds(368, 340, 82, 26); $next.Text = 'Siguiente >'
$cancel = New-Object System.Windows.Forms.Button
$cancel.SetBounds(462, 340, 82, 26); $cancel.Text = 'Cancelar'
$form.Controls.AddRange(@($header, $line1, $line2, $back, $next, $cancel))
$form.AcceptButton = $next

function New-Page { $p = New-Object System.Windows.Forms.Panel; $p.SetBounds(0, 64, 560, 262); $form.Controls.Add($p); $p.Visible = $false; return $p }
function New-Text([System.Windows.Forms.Panel]$page, [string]$text, [int]$y, [int]$h) {
    $l = New-Object System.Windows.Forms.Label; $l.SetBounds(30, $y, 500, $h); $l.Text = $text; $page.Controls.Add($l); return $l
}

# 1. Bienvenida
$pWelcome = New-Page
$welcomeTitle = New-Text $pWelcome "Bienvenido al asistente de instalación de Deriva $version" 18 24
$welcomeTitle.Font = New-Object System.Drawing.Font('Segoe UI', 11, [System.Drawing.FontStyle]::Bold)
New-Text $pWelcome ("Este asistente instalará Deriva en el equipo. Deriva registra qué aplicación o sitio " +
    "está en primer plano, detecta interrupciones y te ayuda a cumplir tus objetivos diarios. " +
    "Todos los datos se guardan solo en este equipo.") 56 54 | Out-Null
New-Text $pWelcome ("No necesitas permisos de administrador ni instalar Java: el programa lo incluye.") 116 22 | Out-Null
New-Text $pWelcome "Haz clic en Siguiente para continuar o en Cancelar para salir del asistente." 150 22 | Out-Null

# 2. Carpeta de destino
$pFolder = New-Page
New-Text $pFolder "Deriva se instalará en la siguiente carpeta. Para elegir otra, haz clic en Examinar." 18 36 | Out-Null
New-Text $pFolder "Carpeta de destino:" 62 18 | Out-Null
$folderBox = New-Object System.Windows.Forms.TextBox
$folderBox.SetBounds(30, 84, 400, 24); $folderBox.Text = $defaultTarget
$browse = New-Object System.Windows.Forms.Button
$browse.SetBounds(440, 83, 90, 26); $browse.Text = 'Examinar...'
$pFolder.Controls.AddRange(@($folderBox, $browse))
New-Text $pFolder "Espacio necesario: unos 180 MB." 124 18 | Out-Null
New-Text $pFolder ("Tus registros no se guardan aquí, sino en $dataFolder; " +
    "desinstalar el programa no los borra.") 150 40 | Out-Null
$browse.Add_Click({
    $dialog = New-Object System.Windows.Forms.FolderBrowserDialog
    $dialog.Description = 'Elige la carpeta donde instalar Deriva'
    if ($dialog.ShowDialog($form) -eq 'OK') { $folderBox.Text = Join-Path $dialog.SelectedPath 'Deriva' }
})

# 3. Accesos directos
$pShortcuts = New-Page
New-Text $pShortcuts "Elige dónde crear accesos directos para abrir Deriva:" 18 22 | Out-Null
$desktopCheck = New-Object System.Windows.Forms.CheckBox
$desktopCheck.SetBounds(34, 52, 460, 22); $desktopCheck.Text = 'Crear un acceso directo en el escritorio'; $desktopCheck.Checked = $true
$startCheck = New-Object System.Windows.Forms.CheckBox
$startCheck.SetBounds(34, 80, 460, 22); $startCheck.Text = 'Crear un acceso directo en el menú Inicio'; $startCheck.Checked = $true
$pShortcuts.Controls.AddRange(@($desktopCheck, $startCheck))
New-Text $pShortcuts "También se añadirá Deriva a «Aplicaciones instaladas» de Windows para poder desinstalarla." 120 40 | Out-Null

# 4. Listo para instalar
$pReady = New-Page
New-Text $pReady "El asistente está listo para instalar Deriva. Revisa la configuración:" 18 22 | Out-Null
$summary = New-Object System.Windows.Forms.TextBox
$summary.SetBounds(30, 48, 500, 150); $summary.Multiline = $true; $summary.ReadOnly = $true
$summary.BackColor = [System.Drawing.Color]::White
$pReady.Controls.Add($summary)
New-Text $pReady "Haz clic en Instalar para comenzar o en Atrás para cambiar algo." 210 22 | Out-Null

# 5. Instalando
$pProgress = New-Page
New-Text $pProgress "Copiando los archivos de Deriva. Espera un momento..." 18 22 | Out-Null
$bar = New-Object System.Windows.Forms.ProgressBar
$bar.SetBounds(30, 54, 500, 22)
$status = New-Text $pProgress "" 86 40
$pProgress.Controls.Add($bar)

# 6. Finalizado
$pDone = New-Page
$doneTitle = New-Text $pDone "Deriva se instaló correctamente" 18 24
$doneTitle.Font = New-Object System.Drawing.Font('Segoe UI', 11, [System.Drawing.FontStyle]::Bold)
$doneText = New-Text $pDone "" 56 100
$openCheck = New-Object System.Windows.Forms.CheckBox
$openCheck.SetBounds(34, 172, 460, 22); $openCheck.Text = 'Abrir Deriva ahora'; $openCheck.Checked = $true
$pDone.Controls.Add($openCheck)

$pages = @($pWelcome, $pFolder, $pShortcuts, $pReady, $pProgress, $pDone)
$heads = @(
    @('Bienvenida', 'Asistente de instalación de Deriva.'),
    @('Carpeta de destino', '¿Dónde quieres instalar Deriva?'),
    @('Accesos directos', 'Elige cómo abrir Deriva.'),
    @('Listo para instalar', 'Comprueba la configuración antes de instalar.'),
    @('Instalando', 'Se están copiando los archivos.'),
    @('Instalación completada', 'Deriva está lista para usarse.'))
$script:page = 0
$script:installedExe = $null

function Show-Step([int]$index) {
    $script:page = $index
    for ($i = 0; $i -lt $pages.Count; $i++) { $pages[$i].Visible = ($i -eq $index) }
    $headerTitle.Text = $heads[$index][0]; $headerText.Text = $heads[$index][1]
    $back.Enabled = $index -in 1, 2, 3
    $cancel.Enabled = $index -lt 4
    $next.Enabled = $index -ne 4
    $next.Text = switch ($index) { 3 { 'Instalar' } 5 { 'Finalizar' } default { 'Siguiente >' } }
    if ($index -eq 3) {
        $lines = @("Carpeta de destino:", "    $($folderBox.Text)", "", "Accesos directos:")
        $lines += $(if ($desktopCheck.Checked) { '    Escritorio' } else { '    (sin acceso en el escritorio)' })
        $lines += $(if ($startCheck.Checked) { '    Menú Inicio' } else { '    (sin acceso en el menú Inicio)' })
        $lines += @('', 'Datos de la aplicación:', "    $dataFolder")
        $summary.Text = $lines -join "`r`n"
    }
}

function Invoke-Install {
    Show-Step 4
    $form.Refresh()
    try {
        $script:installedExe = Install-Deriva $folderBox.Text $desktopCheck.Checked $startCheck.Checked {
            param($percent, $file)
            $bar.Value = [Math]::Min(100, $percent)
            $status.Text = "Copiando: $file"
            [System.Windows.Forms.Application]::DoEvents()
        }
        $doneText.Text = ("Deriva $version se instaló en:`r`n$(Split-Path -Parent $script:installedExe)`r`n`r`n" +
            "Puedes abrirla desde el acceso directo «Deriva». Para empezar a registrar, pulsa «Iniciar captura» en la barra lateral.")
        Show-Step 5
    } catch {
        [System.Windows.Forms.MessageBox]::Show("No se pudo completar la instalación: $($_.Exception.Message)",
            $title, 'OK', 'Error') | Out-Null
        Show-Step 3
    }
}

$next.Add_Click({
    switch ($script:page) {
        0 { Show-Step 1 }
        1 {
            if ([string]::IsNullOrWhiteSpace($folderBox.Text)) {
                [System.Windows.Forms.MessageBox]::Show('Indica una carpeta de destino.', $title, 'OK', 'Warning') | Out-Null
            } else { Show-Step 2 }
        }
        2 { Show-Step 3 }
        3 {
            if (Get-Process -Name 'Deriva' -ErrorAction SilentlyContinue) {
                [System.Windows.Forms.MessageBox]::Show(('Deriva está abierta. Ciérrala desde el icono de la bandeja ' +
                    '(clic derecho › Salir) y vuelve a pulsar Instalar.'), $title, 'OK', 'Warning') | Out-Null
            } else { Invoke-Install }
        }
        5 {
            if ($openCheck.Checked -and $script:installedExe) {
                Start-Process -FilePath $script:installedExe -WorkingDirectory (Split-Path -Parent $script:installedExe)
            }
            $form.Close()
        }
    }
})
$back.Add_Click({ if ($script:page -gt 0) { Show-Step ($script:page - 1) } })
$cancel.Add_Click({
    $answer = [System.Windows.Forms.MessageBox]::Show('¿Seguro que quieres salir de la instalación de Deriva?',
        $title, 'YesNo', 'Question')
    if ($answer -eq 'Yes') { $form.Close() }
})

if ($CaptureDir) {
    # Documentation mode: renders every step as an image; nothing is installed.
    New-Item -ItemType Directory -Path $CaptureDir -Force | Out-Null
    $form.Show()
    $names = @('1-bienvenida', '2-carpeta', '3-accesos', '4-listo', '5-instalando', '6-finalizado')
    for ($i = 0; $i -lt $pages.Count; $i++) {
        Show-Step $i
        if ($i -eq 4) { $bar.Value = 62; $status.Text = 'Copiando: runtime\bin\server\jvm.dll' }
        if ($i -eq 5) {
            $doneText.Text = ("Deriva $version se instaló en:`r`n$defaultTarget`r`n`r`n" +
                "Puedes abrirla desde el acceso directo «Deriva». Para empezar a registrar, pulsa «Iniciar captura» en la barra lateral.")
        }
        [System.Windows.Forms.Application]::DoEvents()
        Start-Sleep -Milliseconds 300
        [System.Windows.Forms.Application]::DoEvents()
        $bounds = $form.Bounds
        $bitmap = New-Object System.Drawing.Bitmap($bounds.Width, $bounds.Height)
        $form.DrawToBitmap($bitmap, (New-Object System.Drawing.Rectangle(0, 0, $bounds.Width, $bounds.Height)))
        $bitmap.Save((Join-Path $CaptureDir "instalador-$($names[$i]).png"), [System.Drawing.Imaging.ImageFormat]::Png)
        $bitmap.Dispose()
    }
    $form.Close()
    exit 0
}

Show-Step 0
[System.Windows.Forms.Application]::Run($form)
