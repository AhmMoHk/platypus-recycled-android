<#
  Platypus Reclayed -> Android: builds the APK from the project made by setup_project.bat.
    build_android.bat                                  (finds Unity 6 or 2022.3.62f2 through Unity Hub)
    build_android.bat "D:\Unity\2022.3.62f2\Editor\Unity.exe"
    build_android.bat -Astc 4x4                        (texture quality: 4x4 best/biggest, 6x6 default, 8x8 smallest)
#>
param(
    [Parameter(Position = 0)][string]$Unity = "",
    [string]$Project = "",
    [string]$OutDir = "",
    [ValidateSet('4x4', '5x5', '6x6', '8x8')][string]$Astc = '6x6'
)
$ErrorActionPreference = 'Stop'
$Version = '2022.3.62f2'
function Say($m)  { Write-Host "==> $m" -ForegroundColor Cyan }
function Fail($m) { Write-Host ""; Write-Host "ERROR: $m" -ForegroundColor Red; exit 1 }

# ------------------------------------------------------------------------------------------- the project
if (-not $OutDir) { $OutDir = Join-Path $env:USERPROFILE 'PRA' }
if (-not $Project) {
    $marker = Join-Path $OutDir 'project.txt'
    if (Test-Path -LiteralPath $marker) { $Project = (Get-Content -LiteralPath $marker -Raw).Trim() } else { $Project = Join-Path $OutDir 'Project' }
}
if (-not (Test-Path -LiteralPath (Join-Path $Project 'Assets\PlatypusAndroid\Editor\BuildScript.cs'))) { Fail "no port project at $Project (run setup_project.bat first, or pass -Project)" }
$OutDir = Split-Path -Parent $Project

# ------------------------------------------------------------------------------------------- Unity
# Unity 6 (6000.x, free Personal licence) can build without the "Made with Unity" splash, so it is preferred;
# 2022.3.62f2 (the game's own version) also works but always shows the splash.
function Get-UnityVersion([string]$exe) { $v = Split-Path -Leaf (Split-Path -Parent (Split-Path -Parent $exe)); if ($v -match '^\d+\.\d+\.\d+') { $v } else { '' } }
function Find-Unity {
    $roots = New-Object System.Collections.Generic.List[string]
    $exes = New-Object System.Collections.Generic.List[string]
    if ($env:UNITY_EXE) { $exes.Add($env:UNITY_EXE) }
    foreach ($r in @("$env:ProgramFiles\Unity\Hub\Editor", "${env:ProgramFiles(x86)}\Unity\Hub\Editor")) { $roots.Add($r) }
    foreach ($drive in [IO.DriveInfo]::GetDrives()) { if ($drive.DriveType -eq 'Fixed') { $roots.Add((Join-Path $drive.RootDirectory.FullName 'Program Files\Unity\Hub\Editor')); $roots.Add((Join-Path $drive.RootDirectory.FullName 'Unity\Hub\Editor')) } }
    foreach ($f in @("$env:APPDATA\UnityHub\secondaryInstallPath.json", "$env:APPDATA\UnityHub\editors.json", "$env:APPDATA\UnityHub\editors-v2.json")) {
        if (-not (Test-Path -LiteralPath $f)) { continue }
        foreach ($m in [regex]::Matches((Get-Content -LiteralPath $f -Raw), '"([A-Za-z]:\\\\[^"]+?)"')) {
            $p = $m.Groups[1].Value -replace '\\\\', '\'
            if ($p -like '*.exe') { $exes.Add($p) } else { $roots.Add($p) }
        }
    }
    foreach ($r in ($roots | Select-Object -Unique)) {
        if (-not (Test-Path -LiteralPath $r)) { continue }
        foreach ($d in Get-ChildItem -LiteralPath $r -Directory -ErrorAction SilentlyContinue) { $exes.Add((Join-Path $d.FullName 'Editor\Unity.exe')) }
    }
    $found = $exes | Select-Object -Unique | Where-Object { $_ -and (Test-Path -LiteralPath $_) -and (Test-Path -LiteralPath (Join-Path (Split-Path -Parent $_) 'Data\PlaybackEngines\AndroidPlayer')) }
    $rank = {
        $v = Get-UnityVersion $_
        if ($v -like '6000.0.*') { 0 } elseif ($v -like '6000.*') { 1 } elseif ($v -eq $Version) { 2 } elseif ($v -like '2022.3.*') { 3 } else { 9 }
    }
    $best = $found | Sort-Object @{ Expression = $rank }, @{ Expression = { try { [version]((Get-UnityVersion $_) -replace '[a-z].*$', '') } catch { [version]'0.0' } }; Descending = $true } | Select-Object -First 1
    return $best
}
if ($Unity) { $Unity = $Unity.Trim('"'); if (Test-Path -LiteralPath $Unity -PathType Container) { $Unity = Join-Path $Unity 'Unity.exe' } }
else { $Unity = Find-Unity }
if (-not $Unity -or -not (Test-Path -LiteralPath $Unity)) { Fail "no Unity with Android Build Support found. Install Unity 6 (6000.0 LTS) with Unity Hub and tick Android Build Support (+ SDK & NDK, OpenJDK), or pass the path to Unity.exe" }
$android = Join-Path (Split-Path -Parent $Unity) 'Data\PlaybackEngines\AndroidPlayer'
if (-not (Test-Path -LiteralPath $android)) { Fail "Android Build Support is not installed for $Unity (Unity Hub > Installs > Add modules > Android Build Support + SDK & NDK + OpenJDK)" }
$uv = Get-UnityVersion $Unity
if ($uv -like '6000.*') { Write-Host "    Unity ${uv}: no Made with Unity splash" -ForegroundColor Green }
else { Write-Host "    ! Unity $uv shows the Made with Unity splash (Personal licence). Install Unity 6 (6000.0 LTS) to remove it." -ForegroundColor Yellow }
Write-Host "    unity:   $Unity"
Write-Host "    project: $Project"

# ------------------------------------------------------------------------------------------- build
$apk = Join-Path $OutDir 'PlatypusReclayed.apk'
$log = Join-Path $OutDir 'build_android.log'
if (Test-Path -LiteralPath $log) { Remove-Item -LiteralPath $log -Force }
Say "Building (the first run imports and encodes all the art: 30-90 minutes; later runs are much faster)"
$uargs = @('-batchmode', '-quit', '-accept-apiupdate', '-projectPath', "`"$Project`"", '-buildTarget', 'Android',
          '-executeMethod', 'PlatypusAndroid.EditorTools.BuildScript.BuildAndroid',
          '-astc', $Astc, '-output', "`"$apk`"", '-logFile', "`"$log`"")
$proc = Start-Process -FilePath $Unity -ArgumentList $uargs -PassThru -WindowStyle Hidden
$pos = 0L; $started = Get-Date
$show = '\[PlatypusAndroid\]|error CS|Exception|BUILD FAILED|Build Finished|Importing|Compiling|Asset Pipeline Refresh|License'
while ($true) {
    $done = $proc.HasExited
    if (Test-Path -LiteralPath $log) {
        try {
            $fs = [IO.File]::Open($log, 'Open', 'Read', 'ReadWrite')
            if ($fs.Length -gt $pos) {
                $fs.Position = $pos
                $rd = New-Object IO.StreamReader($fs)
                $chunk = $rd.ReadToEnd(); $pos = $fs.Position
                foreach ($line in $chunk -split "`n") {
                    if ($line -match $show) { Write-Host ("  [{0:hh\:mm\:ss}] {1}" -f ((Get-Date) - $started), $line.TrimEnd()) }
                }
            }
            $fs.Close()
        } catch {}
    }
    if ($done) { break }
    Start-Sleep -Seconds 3
}
$code = $proc.ExitCode
if ($code -ne 0 -or -not (Test-Path -LiteralPath $apk)) { Fail "the build failed (exit code $code). The full log: $log" }
$mb = [Math]::Round((Get-Item -LiteralPath $apk).Length / 1MB)
Write-Host ""
Write-Host "Done: $apk ($mb MB)" -ForegroundColor Green
Write-Host "Copy it to the phone and install it (allow installing from this source). The first launch copies the mod files (about 400 MB) once."
