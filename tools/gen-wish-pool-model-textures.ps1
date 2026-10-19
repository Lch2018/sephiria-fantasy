# Wish Pool (wish_pool) block-model textures - the 2026-10-19 redesign.
#
# The author supplied a front-view pixel reference: a square plinth on four small feet, a square
# basin with four corner posts, a keycap-shaped spout in the middle (narrow stem, wide cap, a
# shallow cup of water on top) and a four-leaf clover standing on the spout. This script paints
# the 16x16 tiles that model needs.
#
# Nothing is deployed yet - the model is still a Blockbench project under models/wish_pool/.
#
#   models/wish_pool/stone.png        basin walls + plinth
#   models/wish_pool/stone_light.png  corner posts + the cap rim
#   models/wish_pool/stone_dark.png   spout stem, plinth recess, feet
#   models/wish_pool/water.png        pool surface + the water held in the cup
#   models/wish_pool/waterfall.png    the basin's inner walls (wet) + the sheet off the cap
#   models/wish_pool/foam.png         the splash where the overflow hits the pool
#   models/wish_pool/clover.png       the clover sprite (transparent background - cutout)
#   models/wish_pool/blank.png        fully transparent, for the clover's thin edge faces
#   models/wish_pool/vine.png         the green band across the plinth front
#
# Preview: models/wish_pool/preview.png (8x montage over a checkerboard)
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $root 'models\wish_pool'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$SIZE = 16

# ---- palette, sampled off the author's reference render ----
$STONE_TOP = '#A99BB0'
$STONE_UP = '#9C8FA1'
$STONE_MID = '#938397'
$STONE_LOW = '#7F7183'
$STONE_BOT = '#6E6178'
$STONE_SPECK_D = '#8A7C8E'
$STONE_SPECK_L = '#A395A9'

$LIGHT_TOP = '#BBAEC2'
$LIGHT_UP = '#B0A3B8'
$LIGHT_MID = '#A99BB0'
$LIGHT_LOW = '#9C8FA1'
$LIGHT_BOT = '#857792'
$LIGHT_SPECK_D = '#9E91A6'
$LIGHT_SPECK_L = '#C6BACD'

$DARK_TOP = '#695D72'
$DARK_UP = '#63566C'
$DARK_MID = '#5A4E63'
$DARK_LOW = '#52465A'
$DARK_BOT = '#3E3446'
$DARK_SPECK_D = '#4F4359'
$DARK_SPECK_L = '#73667D'

$WATER_DEEP = '#3E6B85'
$WATER_MID = '#5C9FC3'
$WATER_LIGHT = '#9BD3EE'
$WATER_WHITE = '#E8F6FF'

$FOAM_WHITE = '#F4F8FD'
$FOAM_PALE = '#DCEEF8'
$FOAM_BLUE = '#B6DCF0'

$LEAF_OUT = '#2A3B2E'
$LEAF_DARK = '#34503D'
$LEAF_MID = '#87A570'
$LEAF_LIGHT = '#BFCD93'
$LEAF_HI = '#D8E3AE'

$VINE_DARK = '#34503D'
$VINE_MID = '#4E7A4E'
$VINE_LIGHT = '#6B9B5E'

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

# deterministic value noise (no Random: re-running gives the same texture).
# Every step is masked back to 30 bits so the multiplies cannot overflow Int64 in PS 5.1.
function Noise([int]$x, [int]$y, [int]$salt) {
    $h = ([long]$x * 73856093) -bxor ([long]$y * 19349663) -bxor ([long]$salt * 83492791)
    $h = $h -band 0x3FFFFFFF
    $h = $h -bxor ($h -shr 13)
    $h = ($h * 1103515245) -band 0x3FFFFFFF
    return (($h % 1000) / 1000.0)
}

# A stone tile: a light course on top, the base colour in the middle, a dark course at the
# bottom, with a little speckle so a 16x16 face does not read as flat paint.
function New-StoneTexture([string[]]$pal, [int]$salt) {
    $bmp = New-Canvas
    for ($y = 0; $y -lt $SIZE; $y++) {
        for ($x = 0; $x -lt $SIZE; $x++) {
            $col = $pal[2]
            if ($y -eq 0) {
                $col = $pal[0]
            } elseif ($y -le 2) {
                $col = $pal[1]
            } elseif ($y -ge 14) {
                $col = $pal[4]
            } elseif ($y -ge 12) {
                $col = $pal[3]
            } else {
                $n = Noise $x $y $salt
                if ($n -lt 0.10) {
                    $col = $pal[5]
                } elseif ($n -gt 0.90) {
                    $col = $pal[6]
                }
            }
            Set-Px $bmp $x $y $col
        }
    }
    return $bmp
}

# Water surface: diagonal ripples with a few white glints.
function New-WaterTexture {
    $bmp = New-Canvas
    for ($y = 0; $y -lt $SIZE; $y++) {
        for ($x = 0; $x -lt $SIZE; $x++) {
            $wave = [Math]::Sin(($x * 0.9) + ($y * 1.5))
            $col = $WATER_MID
            if ($wave -gt 0.55) {
                $col = $WATER_LIGHT
            } elseif ($wave -lt -0.55) {
                $col = $WATER_DEEP
            }
            $n = Noise $x $y 71
            if ($n -gt 0.94) {
                $col = $WATER_WHITE
            }
            Set-Px $bmp $x $y $col
        }
    }
    return $bmp
}

# Vertical streaks, for the basin's inner walls (water running down them) and for the sheet
# that spills off the cap.
function New-WaterfallTexture {
    $bmp = New-Canvas
    for ($y = 0; $y -lt $SIZE; $y++) {
        for ($x = 0; $x -lt $SIZE; $x++) {
            # columns of different brightness make the streaks read as running water
            $col = $WATER_MID
            if (($x % 5) -eq 0) {
                $col = $WATER_LIGHT
            } elseif (($x % 5) -eq 2) {
                $col = $WATER_DEEP
            }
            $n = Noise $x $y 173
            if ($n -gt 0.88) {
                $col = $WATER_WHITE
            }
            Set-Px $bmp $x $y $col
        }
    }
    return $bmp
}

# Foam: near-white with pale blue holes - the splash ring around the spout.
function New-FoamTexture {
    $bmp = New-Canvas
    for ($y = 0; $y -lt $SIZE; $y++) {
        for ($x = 0; $x -lt $SIZE; $x++) {
            $n = Noise $x $y 97
            $col = $FOAM_WHITE
            if ($n -lt 0.22) {
                $col = $FOAM_PALE
            } elseif ($n -lt 0.34) {
                $col = $FOAM_BLUE
            }
            Set-Px $bmp $x $y $col
        }
    }
    return $bmp
}

# The four-leaf clover is built from four separate lobes (see New-LeafLobeTexture) around a dark
# hub, so no single 16x16 tile has to hold the whole sprite. The stem is a thin cube with the
# leaf-edge tile on it.


# One clover lobe. A superellipse (rounded square), not a circle: the tile has to fill the whole
# cube face, because four of these butted together are what make the clover read as one leaf
# cluster with a dark cross between the lobes. A circle leaves transparent corners and the clover
# falls apart into four separate blobs.
function New-LeafLobeTexture {
    $bmp = New-Canvas
    $p = 4.0    # superellipse exponent: 2 = circle, larger reads as a squarer leaf
    for ($y = 0; $y -lt $SIZE; $y++) {
        for ($x = 0; $x -lt $SIZE; $x++) {
            $dx = ($x + 0.5 - 8.0) / 8.0
            $dy = ($y + 0.5 - 8.0) / 8.0
            $d = [Math]::Pow([Math]::Abs($dx), $p) + [Math]::Pow([Math]::Abs($dy), $p)
            if ($d -gt 1.0) { continue }     # the rounded-off corners stay transparent
            $col = $LEAF_MID
            if ($d -gt 0.62) {
                $col = $LEAF_OUT
            } else {
                # light falls from the upper left: a narrow highlight arc, not a broad band
                $t = (-$dx - $dy) / 1.414
                if ($t -gt 0.70) {
                    $col = $LEAF_HI
                } elseif ($t -gt 0.48) {
                    $col = $LEAF_LIGHT
                } elseif ($t -lt -0.62) {
                    $col = $LEAF_DARK
                } elseif ($t -lt -0.40) {
                    $col = '#6E8F5C'
                }
            }
            Set-Px $bmp $x $y $col
        }
    }
    return $bmp
}

# The clover's thin side faces: a leaf-green strip with a dark rim top and bottom.
function New-LeafEdgeTexture {
    $bmp = New-Canvas
    for ($y = 0; $y -lt $SIZE; $y++) {
        for ($x = 0; $x -lt $SIZE; $x++) {
            $col = $LEAF_MID
            if ($y -lt 2 -or $y -gt 13) {
                $col = $LEAF_OUT
            } elseif ($y -lt 4) {
                $col = $LEAF_LIGHT
            } elseif ($y -gt 11) {
                $col = $LEAF_DARK
            }
            $n = Noise $x $y 211
            if ($n -lt 0.14) { $col = $LEAF_DARK }
            Set-Px $bmp $x $y $col
        }
    }
    return $bmp
}

# The vine band on the plinth front: green with darker knots and lighter leaves.
function New-VineTexture {
    $bmp = New-Canvas
    for ($y = 0; $y -lt $SIZE; $y++) {
        for ($x = 0; $x -lt $SIZE; $x++) {
            $n = Noise $x $y 53
            $col = $VINE_MID
            if ($n -lt 0.20) {
                $col = $VINE_DARK
            } elseif ($n -gt 0.80) {
                $col = $VINE_LIGHT
            }
            Set-Px $bmp $x $y $col
        }
    }
    return $bmp
}

$stone = New-StoneTexture @($STONE_TOP, $STONE_UP, $STONE_MID, $STONE_LOW, $STONE_BOT, $STONE_SPECK_D, $STONE_SPECK_L) 11
$stoneLight = New-StoneTexture @($LIGHT_TOP, $LIGHT_UP, $LIGHT_MID, $LIGHT_LOW, $LIGHT_BOT, $LIGHT_SPECK_D, $LIGHT_SPECK_L) 23
$stoneDark = New-StoneTexture @($DARK_TOP, $DARK_UP, $DARK_MID, $DARK_LOW, $DARK_BOT, $DARK_SPECK_D, $DARK_SPECK_L) 41
$water = New-WaterTexture
$waterfall = New-WaterfallTexture
$foam = New-FoamTexture
$leaf = New-LeafLobeTexture
$leafEdge = New-LeafEdgeTexture
$vine = New-VineTexture

$textures = [ordered]@{
    'stone' = $stone
    'stone_light' = $stoneLight
    'stone_dark' = $stoneDark
    'water' = $water
    'waterfall' = $waterfall
    'foam' = $foam
    'leaf' = $leaf
    'leaf_edge' = $leafEdge
    'vine' = $vine
}

foreach ($key in $textures.Keys) {
    $path = Join-Path $outDir ($key + '.png')
    $textures[$key].Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Output ('wrote ' + $path)
}

# ---- 8x montage over a checkerboard so transparency is obvious ----
$scale = 8
$gap = 4
$names = @('stone', 'stone_light', 'stone_dark', 'water', 'waterfall', 'foam', 'leaf', 'leaf_edge', 'vine')
$cell = $SIZE * $scale
$sheetW = ($names.Count * ($cell + $gap)) + $gap
$sheetH = $cell + ($gap * 2)
$sheet = New-Object System.Drawing.Bitmap($sheetW, $sheetH)
$g = [System.Drawing.Graphics]::FromImage($sheet)
$g.Clear([System.Drawing.Color]::FromArgb(255, 40, 40, 48))
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$i = 0
foreach ($n in $names) {
    $ox = $gap + $i * ($cell + $gap)
    # checkerboard behind the tile
    for ($cy2 = 0; $cy2 -lt $SIZE; $cy2++) {
        for ($cx2 = 0; $cx2 -lt $SIZE; $cx2++) {
            $shade = 60
            if ((($cx2 + $cy2) % 2) -eq 0) { $shade = 90 }
            $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, $shade, $shade, $shade))
            $g.FillRectangle($brush, ($ox + $cx2 * $scale), ($gap + $cy2 * $scale), $scale, $scale)
            $brush.Dispose()
        }
    }
    $img = [System.Drawing.Bitmap]::FromFile((Join-Path $outDir ($n + '.png')))
    $g.DrawImage($img, $ox, $gap, $cell, $cell)
    $img.Dispose()
    $i++
}
$g.Dispose()
$sheet.Save((Join-Path $outDir 'preview.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()
Write-Output ('wrote ' + (Join-Path $outDir 'preview.png'))
