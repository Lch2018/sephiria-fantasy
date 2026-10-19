# Snapshot the current working state of the mod into a timestamped archive.
#
# Included: mod sources, resource pack assets, the Blockbench authoring copies under
# models/, the tool scripts, and the built jar. Snapshots live in snapshots/ and are
# plain zip files, so restoring is just extracting over the project root.
#
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File tools\snapshot.ps1 [-Label katana]

param([string]$Label = '')

$root = Split-Path -Parent $PSScriptRoot
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$name = if ($Label) { 'sephiria-fantasy-' + $Label + '-' + $stamp } else { 'sephiria-fantasy-' + $stamp }

$outDir = Join-Path $root 'snapshots'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$stage = Join-Path $env:TEMP $name
New-Item -ItemType Directory -Force -Path $stage | Out-Null

function Copy-Tree {
    param([string]$Rel)

    $src = Join-Path $root $Rel
    if (-not (Test-Path $src)) { return }

    $dst = Join-Path $stage $Rel
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $dst) | Out-Null
    Copy-Item $src $dst -Recurse -Force
}

foreach ($rel in @('src', 'models', 'tools')) { Copy-Tree -Rel $rel }

# the jar name follows gradle.properties, so read it instead of pinning a version here
# (this used to say 0.1.0, so every snapshot silently shipped without a jar)
$version = 'unknown'
$gradleProps = Join-Path $root 'gradle.properties'
if (Test-Path $gradleProps) {
    $line = Select-String -Path $gradleProps -Pattern '^version=' | Select-Object -First 1
    if ($line) { $version = ($line.Line -split '=', 2)[1].Trim() }
}
$jarName = 'sephiria-fantasy-' + $version + '.jar'
$jar = Join-Path $root ('build\libs\' + $jarName)
if (Test-Path $jar) {
    New-Item -ItemType Directory -Force -Path (Join-Path $stage 'build') | Out-Null
    Copy-Item $jar (Join-Path $stage ('build\' + $jarName)) -Force
}

# a short note so a snapshot is self-describing months later.
# Interpolated strings, not 'x' + (command): inside @() PowerShell 5.1 splits that into two
# array elements, which quietly put every value on a line of its own.
$note = @(
    'SEPHIRIA mod snapshot',
    "taken   : $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')",
    "project : $root",
    "version : $version",
    '',
    'contents: src/ (mod sources), models/ (Blockbench authoring copies),',
    "          tools/ (build + import scripts), build/$jarName",
    '',
    "deploy target: D:\PCL\.minecraft\versions\26.3-Fabric 0.19.5\mods\$jarName"
)
[System.IO.File]::WriteAllLines((Join-Path $stage 'SNAPSHOT.txt'), $note, (New-Object System.Text.UTF8Encoding($false)))

$zip = Join-Path $outDir ($name + '.zip')
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path (Join-Path $stage '*') -DestinationPath $zip -CompressionLevel Optimal

Remove-Item $stage -Recurse -Force

$info = Get-Item $zip
Write-Output ('snapshot: ' + $info.FullName)
Write-Output ('          ' + [math]::Round($info.Length / 1MB, 2) + ' MB  ' + $info.LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss'))
