# Move the leftover "shotgun" GeckoLib resource copies out of the resource pack.
#
# When hunting for GeckoLib's resource-path convention I staged the katana geo/animation
# in every plausible folder. Only one layout is actually read:
#   assets/sephiria/geckolib/models/<name>.geo.json
#   assets/sephiria/geckolib/animations/<name>.animation.json
# Everything else is unreferenced (verified: no JSON mentions them), so it is moved to
# tmp_fix/removed-resources/ instead of deleted -- easy to restore if the convention
# ever changes back.
#
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

$root = Split-Path -Parent $PSScriptRoot
$res = Join-Path $root 'src\main\resources'
$bin = Join-Path $root 'tmp_fix\removed-resources'

$targets = @(
    'assets\sephiria\geo',
    'assets\sephiria\animations',
    'assets\sephiria\item',
    'assets\sephiria\geckolib\geo',
    'assets\sephiria\geckolib\katana.geo.json',
    'assets\sephiria\geckolib\katana.animation.json',
    'assets\sephiria\models\item\katana_3d.json',
    'assets\sephiria\models\item\katana_drawing.json',
    'assets\sephiria\models\item\katana_in_hand.json',
    'assets\sephiria\models\item\katana_sheathed.json',
    'assets\sephiria\textures\item\katana_3d.png',
    'assets\sephiria\textures\item\katana_drawing.png',
    'assets\sephiria\textures\item\katana_sheathed.png',
    'data\sephiria'
)

$moved = 0

foreach ($rel in $targets) {
    $src = Join-Path $res $rel
    if (-not (Test-Path $src)) {
        Write-Output ('skip (absent)  ' + $rel)
        continue
    }

    $dst = Join-Path $bin $rel
    $dstParent = Split-Path -Parent $dst
    New-Item -ItemType Directory -Force -Path $dstParent | Out-Null

    if (Test-Path $dst) { Remove-Item $dst -Recurse -Force }

    Move-Item -Path $src -Destination $dst
    Write-Output ('moved          ' + $rel)
    $moved++
}

Write-Output ('moved ' + $moved + ' entries -> ' + $bin)
Write-Output '--- surviving katana resources ---'
Get-ChildItem (Join-Path $res 'assets\sephiria') -Recurse -File |
    Where-Object { $_.Name -like '*katana*' -or $_.Name -eq 'blade.png' -or $_.Name -eq 'blade.json' } |
    ForEach-Object { Write-Output ('  ' + $_.FullName.Substring($res.Length + 1)) }
