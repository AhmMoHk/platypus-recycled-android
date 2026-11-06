<#
  Platypus Reclayed -> Android port kit: builds the Unity project from your own copy of the game.
  Run through setup_project.bat. Every step only READS the game folder; nothing in it is changed.

    setup_project.bat                       (finds the game: Steam default, Steam libraries, or asks you)
    setup_project.bat "E:\Games\Platypus Reclayed"
    setup_project.bat -GameDir "E:\Games\Platypus Reclayed" -OutDir "D:\PRA" -WorkshopDir "E:\...\2912430"

  Steps: find the game -> export it with AssetRipper 2.0.0 (base game + the HighEnd mod bundles) -> copy the
  loose mod files and your Workshop items -> drop Steam / Epic / Xbox / Rewired -> add the port (input, touch
  controls, shaders, build script) -> apply patches.json to the decompiled game code.
#>
param(
    [Parameter(Position = 0)][string]$GameDir = "",
    [string]$OutDir = "",
    [string]$WorkshopDir = "",
    [string]$AssetRipper = "",          # AssetRipper.GUI.Free.exe or AssetRipper_win_x64.zip (else downloaded)
    [int]$Port = 0,
    [switch]$Force                      # overwrite an existing project folder
)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$Kit = Split-Path -Parent $PSScriptRoot
$AppId = '2912430'
$RipperUrl = 'https://github.com/AssetRipper/AssetRipper/releases/download/2.0.0/AssetRipper_win_x64.zip'
$Removed = @('Rewired_Windows', 'Rewired_Windows_Functions', 'com.rlabrecque.steamworks.net', 'Blackdrop.Platforms.Steam',
             'Blackdrop.Platforms.EpicGames', 'Unity.Microsoft.GDK', 'Unity.Microsoft.GDK.Tools', 'XblPCSandbox')

function Say($m)  { Write-Host "==> $m" -ForegroundColor Cyan }
function Note($m) { Write-Host "    $m" }
function Warn($m) { Write-Host "    ! $m" -ForegroundColor Yellow }
function Fail($m) { Write-Host ""; Write-Host "ERROR: $m" -ForegroundColor Red; exit 1 }

# ------------------------------------------------------------------------------------------- 1. the game folder
function Resolve-GameDir([string]$p) {
    if ([string]::IsNullOrWhiteSpace($p)) { return $null }
    $p = $p.Trim().Trim('"')
    if (Test-Path -LiteralPath $p -PathType Leaf) { $p = Split-Path -Parent $p }          # Platypus.exe given
    if (-not (Test-Path -LiteralPath $p)) { return $null }
    $d = (Resolve-Path -LiteralPath $p).ProviderPath
    for ($i = 0; $i -lt 5 -and $d; $i++) {                                                  # also accepts subfolders
        if ((Test-Path -LiteralPath (Join-Path $d 'Platypus_Data\globalgamemanagers')) -and
            (Test-Path -LiteralPath (Join-Path $d 'Platypus_Data\StreamingAssets\Platypus Reclayed'))) { return $d }
        $d = Split-Path -Parent $d
    }
    return $null
}

function Get-SteamLibraries {
    $roots = New-Object System.Collections.Generic.List[string]
    foreach ($k in @('HKCU:\Software\Valve\Steam', 'HKLM:\SOFTWARE\WOW6432Node\Valve\Steam', 'HKLM:\SOFTWARE\Valve\Steam')) {
        try {
            $v = Get-ItemProperty -Path $k -ErrorAction Stop
            foreach ($n in @('SteamPath', 'InstallPath')) { if ($v.$n) { $roots.Add(($v.$n -replace '/', '\')) } }
        } catch {}
    }
    $roots.Add("${env:ProgramFiles(x86)}\Steam"); $roots.Add("$env:ProgramFiles\Steam")
    $libs = New-Object System.Collections.Generic.List[string]
    foreach ($r in ($roots | Select-Object -Unique)) {
        if (-not (Test-Path -LiteralPath $r)) { continue }
        $libs.Add($r)
        $vdf = Join-Path $r 'steamapps\libraryfolders.vdf'
        if (Test-Path -LiteralPath $vdf) {
            foreach ($m in [regex]::Matches((Get-Content -LiteralPath $vdf -Raw), '"path"\s+"([^"]+)"')) {
                $libs.Add(($m.Groups[1].Value -replace '\\\\', '\'))
            }
        }
    }
    foreach ($drive in [IO.DriveInfo]::GetDrives()) {                                     # common manual libraries
        if ($drive.DriveType -eq 'Fixed') { foreach ($n in @('SteamLibrary', 'Steam', 'Games\Steam')) { $libs.Add((Join-Path $drive.RootDirectory.FullName $n)) } }
    }
    return $libs | Select-Object -Unique
}

function Pick-Folder {
    try {
        Add-Type -AssemblyName System.Windows.Forms
        $owner = New-Object System.Windows.Forms.Form -Property @{ TopMost = $true; ShowInTaskbar = $false }
        $dlg = New-Object System.Windows.Forms.FolderBrowserDialog
        $dlg.Description = 'Select your "Platypus Reclayed" game folder (the one with Platypus.exe)'
        $dlg.ShowNewFolderButton = $false
        if ($dlg.ShowDialog($owner) -eq [System.Windows.Forms.DialogResult]::OK) { return $dlg.SelectedPath }
    } catch {}
    return $null
}

Say 'Finding Platypus Reclayed'
$Game = Resolve-GameDir $GameDir
if (-not $Game -and $GameDir) { Warn "not a Platypus Reclayed folder: $GameDir" }
if (-not $Game) {
    foreach ($lib in Get-SteamLibraries) {
        $Game = Resolve-GameDir (Join-Path $lib 'steamapps\common\Platypus Reclayed')
        if ($Game) { break }
    }
}
while (-not $Game) {
    Note 'The game was not found in your Steam libraries. Choose its folder (a window opens),'
    Note 'or paste the path here (Enter = open the folder window, Q = quit):'
    $ans = Read-Host '    path'
    if ($ans -match '^[qQ]$') { exit 1 }
    if ([string]::IsNullOrWhiteSpace($ans)) { $ans = Pick-Folder }
    $Game = Resolve-GameDir $ans
    if (-not $Game) { Warn "no Platypus_Data\StreamingAssets\Platypus Reclayed under: $ans" }
}
Note "game: $Game"
$Data = Join-Path $Game 'Platypus_Data'
$GameSA = Join-Path $Data 'StreamingAssets'

if (-not $WorkshopDir) { $WorkshopDir = Join-Path (Split-Path -Parent (Split-Path -Parent $Game)) "workshop\content\$AppId" }
if (Test-Path -LiteralPath $WorkshopDir) { Note "workshop: $WorkshopDir" } else { Note 'workshop: none found (no Workshop items will be added)'; $WorkshopDir = $null }

# ------------------------------------------------------------------------------------------- 2. folders
if (-not $OutDir) { $OutDir = Join-Path $env:USERPROFILE 'PRA' }                 # short: Unity dislikes long paths
$OutDir = [IO.Path]::GetFullPath($OutDir)
$Project = Join-Path $OutDir 'Project'
$Work = Join-Path $OutDir '_work'
if (Test-Path -LiteralPath (Join-Path $Project 'Assets')) {
    if (-not $Force) { Fail "a project already exists at $Project  (run again with -Force to rebuild it, or pick another -OutDir)" }
    Say "Removing the old project $Project"
    Remove-Item -LiteralPath $Project -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $OutDir, $Work | Out-Null
$free = (New-Object IO.DriveInfo ([IO.Path]::GetPathRoot($OutDir))).AvailableFreeSpace / 1GB
Note ("output: $OutDir  ({0:N0} GB free)" -f $free)
if ($free -lt 25) { Warn 'less than 25 GB free: the export, the Unity import and the APK need about 20 GB' }

# ------------------------------------------------------------------------------------------- 3. AssetRipper
Say 'AssetRipper 2.0.0'
$exe = $null
$cands = @($AssetRipper, (Join-Path $Kit 'tools\AssetRipper\AssetRipper.GUI.Free.exe'), (Join-Path $Kit 'tools\AssetRipper_win_x64.zip'), (Join-Path $Work 'AssetRipper\AssetRipper.GUI.Free.exe')) | Where-Object { $_ }
foreach ($c in $cands) {
    if (-not (Test-Path -LiteralPath $c)) { continue }
    if ($c -like '*.exe') { $exe = $c; break }
    if ($c -like '*.zip') { Expand-Archive -LiteralPath $c -DestinationPath (Join-Path $Work 'AssetRipper') -Force; break }
}
if (-not $exe) { $exe = Join-Path $Work 'AssetRipper\AssetRipper.GUI.Free.exe' }
if (-not (Test-Path -LiteralPath $exe)) {
    $zip = Join-Path $Work 'AssetRipper_win_x64.zip'
    Note "downloading $RipperUrl"
    try {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $RipperUrl -OutFile $zip -UseBasicParsing
    } catch { Fail "could not download AssetRipper ($($_.Exception.Message)). Download AssetRipper_win_x64.zip (2.0.0) yourself and put it in $Kit\tools\" }
    Expand-Archive -LiteralPath $zip -DestinationPath (Join-Path $Work 'AssetRipper') -Force
}
if (-not (Test-Path -LiteralPath $exe)) {
    $found = Get-ChildItem -LiteralPath (Join-Path $Work 'AssetRipper') -Recurse -Filter 'AssetRipper.GUI.Free.exe' | Select-Object -First 1
    if ($found) { $exe = $found.FullName } else { Fail 'AssetRipper.GUI.Free.exe not found in the AssetRipper zip' }
}
Note $exe

# ------------------------------------------------------------------------------------------- 4. a read-only view of the game
# AssetRipper gets the base game plus only the HighEnd bundles of the mods (the PC LowEnd copies and the loose mod
# files are not needed for the export). Folders are junctions, files hard links (copies on another drive).
Say 'Preparing the export view'
$View = Join-Path $Work 'view'
$junctions = New-Object System.Collections.Generic.List[string]
function Remove-View {
    foreach ($j in $junctions) { if (Test-Path -LiteralPath $j) { [IO.Directory]::Delete($j, $false) } }   # the link only, never its target
    $junctions.Clear()
    if (Test-Path -LiteralPath $View) {
        Get-ChildItem -LiteralPath $View -Recurse -Force -Attributes ReparsePoint -ErrorAction SilentlyContinue | ForEach-Object { [IO.Directory]::Delete($_.FullName, $false) }
        Remove-Item -LiteralPath $View -Recurse -Force
    }
}
function Link-File($src, $dst) {
    try { New-Item -ItemType HardLink -Path $dst -Target $src -ErrorAction Stop | Out-Null }
    catch { Copy-Item -LiteralPath $src -Destination $dst -Force }
}
Remove-View
New-Item -ItemType Directory -Force -Path (Join-Path $View 'Platypus_Data\StreamingAssets') | Out-Null
foreach ($f in @('Platypus.exe', 'UnityPlayer.dll')) { if (Test-Path -LiteralPath (Join-Path $Game $f)) { Link-File (Join-Path $Game $f) (Join-Path $View $f) } }
foreach ($item in Get-ChildItem -LiteralPath $Data -Force) {
    if ($item.Name -eq 'StreamingAssets') { continue }
    $dst = Join-Path $View "Platypus_Data\$($item.Name)"
    if ($item.PSIsContainer) { New-Item -ItemType Junction -Path $dst -Target $item.FullName | Out-Null; $junctions.Add($dst) }
    else { Link-File $item.FullName $dst }
}
$bundled = @()
foreach ($mod in Get-ChildItem -LiteralPath $GameSA -Directory) {
    $he = Join-Path $mod.FullName 'Bundle\HighEnd'
    if (-not (Test-Path -LiteralPath $he)) { continue }
    $dst = Join-Path $View "Platypus_Data\StreamingAssets\$($mod.Name)\Bundle\HighEnd"
    New-Item -ItemType Directory -Force -Path $dst | Out-Null
    foreach ($f in Get-ChildItem -LiteralPath $he -File) { Link-File $f.FullName (Join-Path $dst $f.Name) }
    $bundled += $mod.Name
}
Note ("mod bundles: " + ($bundled -join ', '))

# ------------------------------------------------------------------------------------------- 5. export
Say 'Exporting with AssetRipper (10-30 minutes)'
if ($Port -le 0) { $l = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0); $l.Start(); $Port = $l.LocalEndpoint.Port; $l.Stop() }
$log = Join-Path $Work 'assetripper.log'
$proc = Start-Process -FilePath $exe -ArgumentList @('--headless', '--port', "$Port", '--log-path', "`"$log`"") -PassThru -WindowStyle Minimized
Add-Type -AssemblyName System.Net.Http
$http = New-Object System.Net.Http.HttpClient
$http.Timeout = [TimeSpan]::FromHours(8)
$base = "http://127.0.0.1:$Port"
function Post($path, [hashtable]$form) {
    $pairs = New-Object 'System.Collections.Generic.List[System.Collections.Generic.KeyValuePair[string,string]]'
    foreach ($k in $form.Keys) { $pairs.Add((New-Object 'System.Collections.Generic.KeyValuePair[string,string]' ($k, [string]$form[$k]))) }
    $r = $http.PostAsync("$base$path", (New-Object System.Net.Http.FormUrlEncodedContent($pairs))).GetAwaiter().GetResult()
    if (-not $r.IsSuccessStatusCode) { throw "AssetRipper $path -> HTTP $([int]$r.StatusCode)" }
}
try {
    $ready = $false
    for ($i = 0; $i -lt 120 -and -not $ready; $i++) {
        Start-Sleep -Seconds 1
        if ($proc.HasExited) { throw "AssetRipper quit at start (see $log)" }
        try { $ready = $http.GetAsync("$base/").GetAwaiter().GetResult().IsSuccessStatusCode } catch {}
    }
    if (-not $ready) { throw 'AssetRipper did not start' }
    Post '/Settings/Update' @{
        ScriptContentLevel = 'Level2'; StreamingAssetsMode = 'Extract'; BundledAssetsExportMode = 'DirectExport'
        AudioExportFormat = 'Default'; ImageExportFormat = 'Png'; LightmapTextureExportFormat = 'Yaml'
        ScriptExportMode = 'Decompiled'; ScriptLanguageVersion = 'AutoSafe'; ShaderExportMode = 'Dummy'
        SpriteExportMode = 'Yaml'; TextExportMode = 'Parse'
    }
    Post '/Reset' @{}
    Note 'loading the game ...'
    Post '/LoadFolder' @{ Path = $View }
    $exportDir = Join-Path $Work 'export'
    if (Test-Path -LiteralPath $exportDir) { Remove-Item -LiteralPath $exportDir -Recurse -Force }
    Note 'writing the Unity project ...'
    Post '/Export/UnityProject' @{ Path = $exportDir }
}
finally {
    if (-not $proc.HasExited) { Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue }
    $http.Dispose()
    Start-Sleep -Seconds 1
    Remove-View
}
$exported = Join-Path $exportDir 'ExportedProject'
if (-not (Test-Path -LiteralPath (Join-Path $exported 'Assets\Scripts\Platypus\Platypus.asmdef'))) { Fail "the export is incomplete (see $log)" }
Move-Item -LiteralPath $exported -Destination $Project
Remove-Item -LiteralPath $exportDir -Recurse -Force -ErrorAction SilentlyContinue
$Assets = Join-Path $Project 'Assets'
$Scripts = Join-Path $Assets 'Scripts'
Note "project: $Project"

# ------------------------------------------------------------------------------------------- 6. mod files
Say 'Copying the mod files (Reclayed, Original, Spooky, Winter, add-ons) and Workshop items'
$SA = Join-Path $Assets 'StreamingAssets'
New-Item -ItemType Directory -Force -Path $SA | Out-Null
foreach ($b in Get-ChildItem -LiteralPath $SA -Recurse -Directory -Filter 'Bundle' -ErrorAction SilentlyContinue) {   # the PC bundles: rebuilt for Android later
    Remove-Item -LiteralPath $b.FullName -Recurse -Force
    if (Test-Path -LiteralPath "$($b.FullName).meta") { Remove-Item -LiteralPath "$($b.FullName).meta" -Force }
}
& robocopy "$GameSA" "$SA" /E /XD Bundle /NFL /NDL /NJH /NJS /NP /R:1 /W:1 | Out-Null
if ($LASTEXITCODE -ge 8) { Fail "copying StreamingAssets failed (robocopy $LASTEXITCODE)" }
$ws = 0
if ($WorkshopDir) {
    foreach ($item in Get-ChildItem -LiteralPath $WorkshopDir -Directory) {
        $metaDirs = @()
        if (Test-Path -LiteralPath (Join-Path $item.FullName 'modmeta.json')) { $metaDirs += $item }
        else { $metaDirs += Get-ChildItem -LiteralPath $item.FullName -Directory | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'modmeta.json') } }
        foreach ($m in $metaDirs) {
            $name = if ($m.FullName -eq $item.FullName) { $item.Name } else { $m.Name }
            & robocopy "$($m.FullName)" (Join-Path $SA $name) /E /NFL /NDL /NJH /NJS /NP /R:1 /W:1 | Out-Null
            if ($LASTEXITCODE -lt 8) { $ws++ }
        }
    }
}
Note "workshop items: $ws"

# ------------------------------------------------------------------------------------------- 7. code
Say 'Removing Steam, Epic, Xbox and the Windows input library'
foreach ($n in $Removed) {
    $d = Join-Path $Scripts $n
    if (Test-Path -LiteralPath $d) { Remove-Item -LiteralPath $d -Recurse -Force }
    if (Test-Path -LiteralPath "$d.meta") { Remove-Item -LiteralPath "$d.meta" -Force }
}
# Rewired is replaced by the port's small stand-in. Its scripts that scenes reference keep their file (and .meta, so
# the GUID) and get the stand-in's content from Overrides; all other Rewired sources go.
$ovScripts = Join-Path $Kit 'Overrides\Assets\Scripts'
foreach ($sub in @('Rewired_Core', 'Assembly-CSharp\Rewired')) {
    $d = Join-Path $Scripts $sub
    if (-not (Test-Path -LiteralPath $d)) { continue }
    foreach ($cs in Get-ChildItem -LiteralPath $d -Recurse -Filter '*.cs' -File) {
        $rel = $cs.FullName.Substring($Scripts.Length + 1)
        if (-not (Test-Path -LiteralPath (Join-Path $ovScripts $rel))) {
            Remove-Item -LiteralPath $cs.FullName -Force
            if (Test-Path -LiteralPath "$($cs.FullName).meta") { Remove-Item -LiteralPath "$($cs.FullName).meta" -Force }
        }
    }
}

Say 'Adding the Android port (input, clay touch controls, shaders, build script)'
& robocopy (Join-Path $Kit 'Overrides') "$Project" /E /NFL /NDL /NJH /NJS /NP /R:1 /W:1 | Out-Null
if ($LASTEXITCODE -ge 8) { Fail "copying Overrides failed (robocopy $LASTEXITCODE)" }
foreach ($png in @('1000045163.png', '1000045164.png', '1000045153.png')) {
    if (-not (Test-Path -LiteralPath (Join-Path $Assets "PlatypusAndroid\Resources\$png"))) { Fail "touch control image missing: $png" }
}

Say 'Applying patches.json to the game code'
$P = Get-Content -LiteralPath (Join-Path $Kit 'patches.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$utf8 = New-Object System.Text.UTF8Encoding($false)
foreach ($rel in $P.delete) {
    $f = Join-Path $Scripts $rel
    if (Test-Path -LiteralPath $f) { Remove-Item -LiteralPath $f -Recurse -Force }
    if (Test-Path -LiteralPath "$f.meta") { Remove-Item -LiteralPath "$f.meta" -Force }
}
$bad = 0
foreach ($e in $P.patches) {
    $f = Join-Path $Scripts $e.file
    if (-not (Test-Path -LiteralPath $f)) { Warn "missing file $($e.file)"; $bad++; continue }
    $s = [IO.File]::ReadAllText($f).Replace("`r`n", "`n")
    $want = if ($e.PSObject.Properties['count']) { [int]$e.count } else { 1 }
    $n = ([regex]::Matches($s, [regex]::Escape($e.old))).Count
    if ($n -ne $want) { Warn ("patch not applied ({0} matches, expected {1}): {2}: {3}" -f $n, $want, $e.file, $e.old.Substring(0, [Math]::Min(60, $e.old.Length))); $bad++; continue }
    [IO.File]::WriteAllText($f, $s.Replace($e.old, $e.new), $utf8)
}
Note ("{0}/{1} patches applied" -f ($P.patches.Count - $bad), $P.patches.Count)
if ($bad) { Fail 'some patches did not apply: this export differs from the one the kit was made for (game update?)' }

Set-Content -LiteralPath (Join-Path $OutDir 'project.txt') -Value $Project -Encoding UTF8
Remove-View                                                   # links first: never recurse through a junction
Remove-Item -LiteralPath $Work -Recurse -Force -ErrorAction SilentlyContinue
Write-Host ""
Write-Host "Done. Unity project: $Project" -ForegroundColor Green
Write-Host "Next: build_android.bat   (it finds this project through $OutDir\project.txt)"
