# The Wish Pool's four-leaf clover sprite, in vanilla-plant style.
#
# The clover is a cutout sprite on two crossed planes, the way vanilla draws grass, flowers and
# saplings, so only this one tile is needed. Greens are a vanilla-ish plant ramp (the mod's own
# slate-sprout greens were too olive for the vanilla look the author asked for).
#
# Output: models/wish_pool/clover.png   (16x16, transparent)
#         models/wish_pool/clover_preview.png (10x over a checkerboard, for eyeballing)
#
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $root 'models\wish_pool'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$SIZE = 16

# vanilla plant greens, dark rim first
$C_OUT = '#2B4D18'
$C_DARK = '#3F6B25'
$C_MID = '#5C9C35'
$C_LIGHT = '#79B84D'
$C_HI = '#9CD46C'

function New-Canvas {
    return New-Object System.Drawing.Bitmap($SIZE, $SIZE, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
}

function Set-Px($bmp, [int]$x, [int]$y, [string]$hex) {
    if ($x -lt 0 -or $y -lt 0 -or $x -ge $SIZE -or $y -ge $SIZE) { return }
    $r = [Convert]::ToInt32($hex.Substring(1, 2), 16)
    $g = [Convert]::ToInt32($hex.Substring(3, 2), 16)
    $b = [Convert]::ToInt32($hex.Substring(5, 2), 16)
    $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $r, $g, $b))
}

function New-Clover {
    $bmp = New-Canvas

    # Four leaves in a 2x2. Superellipse lobes (not circles) so the four butted together read as
    # one clover with a dark cross down the middle, the way the reference draws it.
    $cx = @(4.4, 11.6, 4.4, 11.6)
    $cy = @(3.9, 3.9, 9.5, 9.5)
    $rx = 3.9
    $ry = 3.6
    $p = 3.0
    for ($y = 0; $y -lt $SIZE; $y++) {
        for ($x = 0; $x -lt $SIZE; $x++) {
            $best = 99.0
            $bi = -1
            for ($i = 0; $i -lt 4; $i++) {
                $dx = ($x + 0.5 - $cx[$i]) / $rx
                $dy = ($y + 0.5 - $cy[$i]) / $ry
                $d = [Math]::Pow([Math]::Abs($dx), $p) + [Math]::Pow([Math]::Abs($dy), $p)
                if ($d -lt $best) { $best = $d; $bi = $i }
            }
            if ($best -gt 1.0) { continue }
            $col = $C_MID
            if ($best -gt 0.80) {
                $col = $C_OUT
            } else {
                # light comes from the upper left, the way vanilla shades its plants
                $dx = ($x + 0.5 - $cx[$bi]) / $rx
                $dy = ($y + 0.5 - $cy[$bi]) / $ry
                $t = (-$dx - $dy) / 1.414
                if ($t -gt 0.55) {
                    $col = $C_HI
                } elseif ($t -gt 0.28) {
                    $col = $C_LIGHT
                } elseif ($t -lt -0.55) {
                    $col = $C_OUT
                } elseif ($t -lt -0.28) {
                    $col = $C_DARK
                }
            }
            Set-Px $bmp $x $y $col
        }
    }

    # the dark knot where the four leaves meet
    for ($y = 6; $y -le 8; $y++) {
        for ($x = 7; $x -le 8; $x++) {
            Set-Px $bmp $x $y $C_DARK
        }
    }

    # stem: one dark pixel and one lit, running down between the lower leaves
    for ($y = 9; $y -lt $SIZE; $y++) {
        Set-Px $bmp 7 $y $C_DARK
        Set-Px $bmp 8 $y $C_MID
        if ($y -ge 15) { Set-Px $bmp 8 $y $C_DARK }
    }

    return $bmp
}

$bmp = New-Clover
$path = Join-Path $outDir 'clover.png'
$bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
Write-Output ('wrote ' + $path)

# 10x preview over a checkerboard
$scale = 10
$sheet = New-Object System.Drawing.Bitmap(($SIZE * $scale), ($SIZE * $scale))
$g = [System.Drawing.Graphics]::FromImage($sheet)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
for ($cy2 = 0; $cy2 -lt $SIZE; $cy2++) {
    for ($cx2 = 0; $cx2 -lt $SIZE; $cx2++) {
        $shade = 60
        if ((($cx2 + $cy2) % 2) -eq 0) { $shade = 92 }
        $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, $shade, $shade, $shade))
        $g.FillRectangle($brush, ($cx2 * $scale), ($cy2 * $scale), $scale, $scale)
        $brush.Dispose()
    }
}
$g.DrawImage($bmp, 0, 0, ($SIZE * $scale), ($SIZE * $scale))
$g.Dispose()
$preview = Join-Path $outDir 'clover_preview.png'
$sheet.Save($preview, [System.Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()
Write-Output ('wrote ' + $preview)
