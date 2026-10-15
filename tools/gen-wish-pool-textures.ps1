# Generate the Wish Pool block textures and the four-leaf-clover item icon (all 16x16).
#
# The Wish Pool (许愿池) is the gacha block: a low stone basin of water with a clover in it, and the
# clover is the draw currency. The author has not supplied artwork for either, so this script draws
# them procedurally (same deal as tools/gen-root-retreat-texture.ps1: placeholder art the author can
# replace at any time - the paths stay the same).
#
#   textures/block/wish_pool_top.png    the water surface seen from above (ripples + a clover sprig)
#   textures/block/wish_pool_side.png   the stone rim
#   textures/block/wish_pool_water.png  plain water (the water block's four sides)
#
# (the clover used to be an item with its own icon; it is a STAT now - see WishPool - so only the
#  block's water surface still draws it)
#
# The stone is deterministic value noise and the clover is a hand-written character map, so both are
# easy to nudge:  'g' = dark outline, 'G' = leaf green, 'H' = highlight, '.' = leave as-is.
#
# Preview: .preview/wish_pool_textures_preview.png (4x, over a mid grey)
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$blockDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\textures\block'
$itemDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\textures\item'
$previewDir = Join-Path $root '.preview'

$SIZE = 16

# ---- palette ----
# stone: the same lavender-grey family as the slates / 根之退
$STONE = @(0x8A, 0x84, 0x93)
$STONE_DARK = @(0x6E, 0x69, 0x78)
$STONE_LIGHT = @(0xA2, 0x9C, 0xAC)
# water: the mod's ice-blue family, a bit deeper so it reads as water
$WATER_DEEP = @(0x2E, 0x5E, 0x8F)
$WATER_MID = @(0x4E, 0x9F, 0xE8)
$WATER_LIGHT = @(0xA8, 0xE4, 0xFF)
# clover: the mod's slate-sprout greens
$LEAF = @(0x85, 0xC1, 0x86)
$LEAF_DARK = @(0x3F, 0x63, 0x44)
$LEAF_LIGHT = @(0xC8, 0xF5, 0xC8)

# ---- the clover: four round lobes around a centre + a stem (drawn procedurally) ----
# (a character map of four leaves kept merging into one blob at 16px, so the lobes are circles now)
$CLOVER_LOBE_OFFSET = 2.2   # how far each lobe's centre sits from the clover's centre
$CLOVER_LOBE_RADIUS = 2.7   # lobe radius (the dark rim sits in the outer 1px of it)
$CLOVER_CENTER_X = 5.5      # the clover is stamped from here (11x11 area: 0..10)
$CLOVER_CENTER_Y = 5.0
$CLOVER_STEM_TOP = 7        # stem rows (2px wide, under the lobes)
$CLOVER_STEM_BOTTOM = 10

function Mix([int[]]$a, [int[]]$b, [double]$t) {
    $t = [Math]::Max(0.0, [Math]::Min(1.0, $t))
    return @(
        [int][Math]::Round($a[0] + ($b[0] - $a[0]) * $t),
        [int][Math]::Round($a[1] + ($b[1] - $a[1]) * $t),
        [int][Math]::Round($a[2] + ($b[2] - $a[2]) * $t)
    )
}

# deterministic value noise (no Random: re-running gives the same texture)
function Noise([int]$x, [int]$y) {
    $h = ($x * 73856093) -bxor ($y * 19349663)
    $h = $h -bxor ($h -shr 13)
    $h = ($h * 1274126177) -band 0x7FFFFFFF
    return (($h % 1000) / 1000.0)
}

function New-Canvas {
    return New-Object System.Drawing.Bitmap($SIZE, $SIZE, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
}

function Set-Px($bmp, [int]$x, [int]$y, [int[]]$rgb) {
    if ($x -lt 0 -or $y -lt 0 -or $x -ge $SIZE -or $y -ge $SIZE) { return }
    $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $rgb[0], $rgb[1], $rgb[2]))
}

# 石身：底色 + 两档噪声（2px 块，看着像像素画）
function Paint-Stone($bmp) {
    for ($y = 0; $y -lt $SIZE; $y++) {
        for ($x = 0; $x -lt $SIZE; $x++) {
            $n = Noise ([int][Math]::Floor($x / 2)) ([int][Math]::Floor($y / 2))
            $colour = $STONE
            # 注意：elseif 必须跟在上一段的右花括号同一行（PowerShell 换行就报「意外的标记」）
            if ($n -lt 0.34) { $colour = Mix $STONE $STONE_DARK 0.5 } elseif ($n -gt 0.8) { $colour = Mix $STONE $STONE_LIGHT 0.45 }
            Set-Px $bmp $x $y $colour
        }
    }
}

# 水面：竖向渐变 + 几道横向波纹
function Paint-Water($bmp) {
    for ($y = 0; $y -lt $SIZE; $y++) {
        for ($x = 0; $x -lt $SIZE; $x++) {
            $t = $y / ($SIZE - 1.0)
            $colour = Mix $WATER_DEEP $WATER_MID $t
            $n = Noise $x $y
            if ($n -gt 0.86) { $colour = Mix $colour $WATER_LIGHT 0.5 }
            Set-Px $bmp $x $y $colour
        }
    }

    # 波纹：两道横着的高光，错开一点
    foreach ($row in @(@(4, 6), @(10, 3))) {
        $y = $row[0]
        $from = $row[1]
        for ($x = $from; $x -lt $from + 6; $x++) {
            Set-Px $bmp $x $y (Mix $WATER_MID $WATER_LIGHT 0.75)
        }
    }
}

# 把四叶草盖上去（居中）
function Stamp-Clover($bmp, [int]$originX, [int]$originY) {
    # 四片圆叶：以中心为原点，四个象限各一个圆，最外一圈用深绿描边
    foreach ($lobe in @(@(-1, -1), @(1, -1), @(-1, 1), @(1, 1))) {
        $lx = $CLOVER_CENTER_X + $lobe[0] * $CLOVER_LOBE_OFFSET
        $ly = $CLOVER_CENTER_Y + $lobe[1] * $CLOVER_LOBE_OFFSET

        for ($dy = -4; $dy -le 4; $dy++) {
            for ($dx = -4; $dx -le 4; $dx++) {
                $px = [int][Math]::Floor($lx + $dx)
                $py = [int][Math]::Floor($ly + $dy)
                $distance = [Math]::Sqrt([Math]::Pow($px + 0.5 - $lx, 2) + [Math]::Pow($py + 0.5 - $ly, 2))

                if ($distance -gt $CLOVER_LOBE_RADIUS) { continue }

                $colour = if ($distance -gt ($CLOVER_LOBE_RADIUS - 1.0)) { $LEAF_DARK } else { $LEAF }
                # 每片叶子左上角点一颗高光
                if ($distance -le ($CLOVER_LOBE_RADIUS - 1.8) -and $dx -lt 0 -and $dy -lt 0) { $colour = $LEAF_LIGHT }
                Set-Px $bmp ($originX + $px) ($originY + $py) $colour
            }
        }
    }

    # 茎：中心往下两像素宽的一条（先描边再填芯）
    for ($y = $CLOVER_STEM_TOP; $y -le $CLOVER_STEM_BOTTOM; $y++) {
        Set-Px $bmp ($originX + 5) ($originY + $y) $LEAF_DARK
        Set-Px $bmp ($originX + 6) ($originY + $y) $LEAF_DARK
        Set-Px $bmp ($originX + 5) ($originY + $y) $LEAF
        Set-Px $bmp ($originX + 6) ($originY + $y) $LEAF
    }
}

New-Item -ItemType Directory -Force -Path $blockDir, $itemDir, $previewDir | Out-Null

# ---- 1) 水面（从上看）：水 + 中间一株四叶草 ----
$top = New-Canvas
Paint-Water $top
Stamp-Clover $top 2 1
$topPath = Join-Path $blockDir 'wish_pool_top.png'
$top.Save($topPath, [System.Drawing.Imaging.ImageFormat]::Png)

# ---- 2) 池边的石头 ----
$side = New-Canvas
Paint-Stone $side
# 石沿上刻一圈凹槽（与根之退同一套观感）
for ($i = 1; $i -lt ($SIZE - 1); $i++) {
    Set-Px $side $i 1 (Mix $STONE $STONE_DARK 0.8)
    Set-Px $side $i ($SIZE - 2) (Mix $STONE $STONE_DARK 0.8)
    Set-Px $side 1 $i (Mix $STONE $STONE_DARK 0.8)
    Set-Px $side ($SIZE - 2) $i (Mix $STONE $STONE_DARK 0.8)
}
$sidePath = Join-Path $blockDir 'wish_pool_side.png'
$side.Save($sidePath, [System.Drawing.Imaging.ImageFormat]::Png)

# ---- 3) 纯水（水池那格子的四个侧面） ----
$water = New-Canvas
Paint-Water $water
$waterPath = Join-Path $blockDir 'wish_pool_water.png'
$water.Save($waterPath, [System.Drawing.Imaging.ImageFormat]::Png)

# ---- 预览 ----
$scale = 4
$cell = $SIZE * $scale
$sheet = New-Object System.Drawing.Bitmap(($cell * 3), $cell, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb))
$g = [System.Drawing.Graphics]::FromImage($sheet)
$g.Clear([System.Drawing.Color]::FromArgb(255, 108, 108, 112))
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half

for ($i = 0; $i -lt 3; $i++) {
    $bmp = @($top, $side, $water)[$i]
    $g.DrawImage($bmp, ($i * $cell), 0, $cell, $cell)
}

$g.Dispose()
$preview = Join-Path $previewDir 'wish_pool_textures_preview.png'
$sheet.Save($preview, [System.Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()

foreach ($bmp in @($top, $side, $water)) { $bmp.Dispose() }

Write-Output ('wrote  ' + $topPath)
Write-Output ('wrote  ' + $sidePath)
Write-Output ('wrote  ' + $waterPath)
Write-Output ('preview ' + $preview)
