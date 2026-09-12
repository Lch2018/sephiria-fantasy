# Verify the generated item textures are a pixel-exact copy of the reference weapon sprites.
#
# The generator pastes each 102x120 game sprite onto a 128x128 canvas without resampling,
# so the check is: same number of opaque pixels, same colour histogram, content inside canvas.
#
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$refDir = Join-Path $root 'tools\weapon-ref'
$itemDir = Join-Path $root 'src\main\resources\assets\sephiria\textures\item'

$map = [ordered]@{
    'shield_sword' = 'default_sword_and_shield'
    'great_sword'  = 'steel_greatsword'
    'dagger'       = 'dagger'
    'crossbow'     = 'colossal_crossbow'
    'katana'       = 'blade'
    'staff'        = 'quarterstaff'
}

$errors = 0
function Fail($m) { Write-Output ("FAIL: " + $m); $script:errors++ }

function Get-Histogram {
    param([System.Drawing.Bitmap]$Bmp)
    $h = @{}
    $opaque = 0
    for ($y = 0; $y -lt $Bmp.Height; $y++) {
        for ($x = 0; $x -lt $Bmp.Width; $x++) {
            $p = $Bmp.GetPixel($x, $y)
            if ($p.A -lt 200) { continue }
            $opaque++
            $k = ('{0:X2}{1:X2}{2:X2}' -f $p.R, $p.G, $p.B)
            if ($h.ContainsKey($k)) { $h[$k]++ } else { $h[$k] = 1 }
        }
    }
    return @{ Opaque = $opaque; Hist = $h }
}

foreach ($src in $map.Keys) {
    $srcPath = Join-Path $refDir ($src + '.png')
    $itemPath = Join-Path $itemDir ($map[$src] + '.png')

    if (-not (Test-Path $srcPath)) { Fail ("missing reference " + $src); continue }
    if (-not (Test-Path $itemPath)) { Fail ("missing texture " + $map[$src]); continue }

    $s = [System.Drawing.Bitmap]::FromFile($srcPath)
    $t = [System.Drawing.Bitmap]::FromFile($itemPath)

    if ($t.Width -ne 128 -or $t.Height -ne 128) { Fail ($map[$src] + " is " + $t.Width + "x" + $t.Height + ", expected 128x128") }

    $hs = Get-Histogram $s
    $ht = Get-Histogram $t

    if ($hs.Opaque -ne $ht.Opaque) {
        Fail ($map[$src] + " opaque pixel count changed: " + $hs.Opaque + " -> " + $ht.Opaque + " (resampling?)")
    }

    $missing = 0; $extra = 0
    foreach ($k in $hs.Hist.Keys) { if (-not $ht.Hist.ContainsKey($k)) { $missing++ } }
    foreach ($k in $ht.Hist.Keys) { if (-not $hs.Hist.ContainsKey($k)) { $extra++ } }

    if ($missing -ne 0 -or $extra -ne 0) { Fail ($map[$src] + " palette differs (missing=" + $missing + " extra=" + $extra + ")") }

    Write-Output ("ok  : " + $map[$src].PadRight(26) + "source " + $s.Width + "x" + $s.Height + " -> 128x128, opaque=" + $ht.Opaque + ", colors=" + $ht.Hist.Count + " (identical)")

    $s.Dispose(); $t.Dispose()
}

# the mod icon must also exist at 128x128
$iconPath = Join-Path $root 'src\main\resources\assets\sephiria\icon.png'
if (-not (Test-Path $iconPath)) {
    Fail 'missing icon.png'
} else {
    $i = [System.Drawing.Bitmap]::FromFile($iconPath)
    if ($i.Width -ne 128 -or $i.Height -ne 128) { Fail ("icon.png is " + $i.Width + "x" + $i.Height) } else { Write-Output "ok  : icon.png 128x128" }
    $i.Dispose()
}

Write-Output ""
if ($errors -eq 0) { Write-Output "ALL TEXTURE CHECKS PASSED (pixel-exact copy, no resampling)" } else { Write-Output ($errors.ToString() + " PROBLEM(S)") }
exit $errors
