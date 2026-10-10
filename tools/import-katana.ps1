# Stage the katana model resources exported from the Blockbench project.
#
# Source of truth: the user's Blockbench project (GeckoLib Animated Model format).
#   C:\Users\28237\Desktop\Blockbench_JIANMO\刀\katana.bbmodel   (never modified by us)
# Export via Blockbench (File > Export > Export GeckoLib Model / Animations) into
# tmp_fix/export/, then this script installs them into the resource pack.
#
# GeckoLib only reads these two locations (verified against GeckoLibResources):
#   assets/sephiria_fantasy/geckolib/models/<name>.geo.json
#   assets/sephiria_fantasy/geckolib/animations/<name>.animation.json
#
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

$root = Split-Path -Parent $PSScriptRoot
# The authoring folder name is CJK, and PowerShell 5.1 reads .ps1 files as GBK, so a
# literal CJK path in this file would be mis-decoded and silently not match. Resolve it
# with a wildcard instead -- this script stays ASCII-only.
$src = (Get-ChildItem 'C:\Users\28237\Desktop\Blockbench_JIANMO' -Directory |
        Where-Object { Test-Path (Join-Path $_.FullName 'katana.bbmodel') } |
        Select-Object -First 1).FullName

if (-not $src) { throw 'could not locate the katana authoring folder on the Desktop' }

$export = Join-Path $root 'tmp_fix\export'

$resModels = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\geckolib\models'
$resAnims = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\geckolib\animations'
$keep = Join-Path $root 'models'

$geo = Join-Path $export 'katana.geo.json'
$anim = Join-Path $export 'katana.animation.json'

foreach ($f in @($geo, $anim)) {
    if (-not (Test-Path $f)) { throw "missing export: $f (export it from Blockbench first)" }
}

New-Item -ItemType Directory -Force -Path $resModels, $resAnims, $keep | Out-Null

Copy-Item $geo (Join-Path $resModels 'katana.geo.json') -Force
Copy-Item $geo (Join-Path $keep 'katana.geo.json') -Force
Copy-Item $anim (Join-Path $resAnims 'katana.animation.json') -Force
Copy-Item $anim (Join-Path $keep 'katana.animation.json') -Force

# Blockbench writes the Bedrock version it targets into format_version (e.g. "1.21.110").
# GeckoLib 5.5.5 only knows the classic geometry versions, and warns
# "Unknown geo model format version" for anything newer -- the geometry itself is plain
# cubes/bones, so pin it back to the version GeckoLib expects.
function Set-FormatVersion {
    param([string]$Path, [string]$Version)

    $text = [System.IO.File]::ReadAllText($Path)
    $new = [regex]::Replace($text, '"format_version"\s*:\s*"[^"]*"', ('"format_version": "' + $Version + '"'), 1)
    if ($new -ne $text) {
        [System.IO.File]::WriteAllText($Path, $new, (New-Object System.Text.UTF8Encoding($false)))
        Write-Output ('  normalized format_version -> ' + $Version + '  ' + (Split-Path -Leaf $Path))
    }
}

Set-FormatVersion -Path (Join-Path $resModels 'katana.geo.json') -Version '1.12.0'
Set-FormatVersion -Path (Join-Path $keep 'katana.geo.json') -Version '1.12.0'
Set-FormatVersion -Path (Join-Path $resAnims 'katana.animation.json') -Version '1.8.0'
Set-FormatVersion -Path (Join-Path $keep 'katana.animation.json') -Version '1.8.0'

# keep a copy of the authoring project + its texture alongside the model
foreach ($pair in @(@('katana.bbmodel', 'katana.bbmodel'), @('katana.png', 'katana.png'))) {
    $from = Join-Path $src $pair[0]
    if (Test-Path $from) { Copy-Item $from (Join-Path $keep $pair[1]) -Force }
}

Write-Output '--- installed ---'
foreach ($f in @(
        (Join-Path $resModels 'katana.geo.json'),
        (Join-Path $resAnims 'katana.animation.json'),
        (Join-Path $keep 'katana.bbmodel'))) {
    $i = Get-Item $f
    Write-Output ('  ' + $i.LastWriteTime.ToString('HH:mm:ss') + '  ' + $i.Length.ToString().PadLeft(8) + '  ' + $i.FullName.Substring($root.Length + 1))
}
