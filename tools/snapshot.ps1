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
$name = if ($Label) { 'sephiria-' + $Label + '-' + $stamp } else { 'sephiria-' + $stamp }

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

$jar = Join-Path $root 'build\libs\sephiria-0.1.0.jar'
if (Test-Path $jar) {
    New-Item -ItemType Directory -Force -Path (Join-Path $stage 'build') | Out-Null
    Copy-Item $jar (Join-Path $stage 'build\sephiria-0.1.0.jar') -Force
}

# a short note so a snapshot is self-describing months later
$note = @(
    'SEPHIRIA mod snapshot',
    'taken   : ' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'),
    'project : ' + $root,
    '',
    'contents: src/ (mod sources), models/ (Blockbench authoring copies),',
    '          tools/ (build + import scripts), build/sephiria-0.1.0.jar',
    '',
    'katana state at this snapshot:',
    '  - model/animation imported from the Blockbench GeckoLib project (katana.bbmodel)',
    '  - texture path returns sephiria:textures/item/katana.png (full file path, GeckoLib requirement)',
    '  - controller: state handler loops animation.idle_unsheathed; switch_to_sheathed/',
    '    switch_to_unsheathed are triggerable animations (no receiveTriggeredAnimations!)',
    '  - render pose rotated +90 deg about the model Y axis so the blade edge faces forward',
    '  - scabbard hidden by the idle_unsheathed animation scaling it to 0',
    '',
    'deploy target: D:\PCL\.minecraft\versions\26.2-Fabric 0.19.5\mods\sephiria-0.1.0.jar'
)
[System.IO.File]::WriteAllLines((Join-Path $stage 'SNAPSHOT.txt'), $note, (New-Object System.Text.UTF8Encoding($false)))

$zip = Join-Path $outDir ($name + '.zip')
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path (Join-Path $stage '*') -DestinationPath $zip -CompressionLevel Optimal

Remove-Item $stage -Recurse -Force

$info = Get-Item $zip
Write-Output ('snapshot: ' + $info.FullName)
Write-Output ('          ' + [math]::Round($info.Length / 1MB, 2) + ' MB  ' + $info.LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss'))
