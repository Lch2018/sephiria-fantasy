# Build the SEPHIRIA textures from the reference images the user supplied.
#
#   artifact_tab_icon  pouch.png      (114x114, 19x19 @6x)  -> textures/item/artifact_tab_icon.png
#   charm_of_strength  charm.png      ( 56x56,  7x12 @3x)   -> textures/item/charm_of_strength.png
#   backpack_tab       backpack.png   ( 65x65, 19x16 @3x)   -> textures/gui/backpack_tab.png
#   attributes_tab     attributes.png ( 65x65, 20x20 @3x)   -> textures/gui/attributes_tab.png
#   slate              slate.png      (120x120, 34x30 @3x)  -> textures/item/slate.png
#
# 两种处理方式：
#   native     受源图网格小于画布时，按原生像素贴进画布，不做重采样（像素画保持原样）；
#   downscaled 源图网格远大于画布时，按「覆盖率的多数色 + 二值 alpha」缩小
#              （面积平均会把平涂像素画糊成一团，点采样又会丢掉描边）。
# 参考图的背景（缓存渲染用的底色：深灰棋盘 / 纯黑 / 墨绿）一律判为透明，判定规则逐张给。
#
# 本脚本刻意只用 ASCII：PowerShell 5.1 会把没有 BOM 的 .ps1 按 GBK 读，中文注释会破坏解析
# （踩过的坑：注释乱码后把下一行代码吞掉，字面量被当成输出打出来）。

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$texDir = Join-Path $root 'src\main\resources\assets\sephiria\textures\item'
$guiDir = Join-Path $root 'src\main\resources\assets\sephiria\textures\gui'
$previewDir = Join-Path $root '.preview'
$cache = 'C:\Users\28237\.zcode\cli\image-cache\sess_30e8ce6e-bd9a-4f06-a1a0-7ff99b2cb644'

foreach ($dir in @($texDir, $guiDir, $previewDir)) {
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
}

function New-Canvas([int]$size) {
    return New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
}

function Save-Preview([System.Drawing.Bitmap]$bmp, [string]$name) {
    $scale = 8
    $big = New-Object System.Drawing.Bitmap ($bmp.Width * $scale), ($bmp.Height * $scale)
    $g = [System.Drawing.Graphics]::FromImage($big)
    # mid grey background so the icon is judged the way it shows up on a tab button
    $g.Clear([System.Drawing.Color]::FromArgb(255, 108, 108, 108))
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $g.DrawImage($bmp, 0, 0, $big.Width, $big.Height)
    $g.Dispose()
    $big.Save((Join-Path $previewDir ($name + '_8x.png')), [System.Drawing.Imaging.ImageFormat]::Png)
    $big.Dispose()
}

function Get-BackdropColours([System.Drawing.Bitmap]$img, [int]$cell) {
    # the outermost two cells are backdrop by definition; collecting them (instead of sampling one
    # pixel) also catches the cache's dithered checkerboard, which has several near-identical shades
    $colours = @{}
    $gridW = [int]($img.Width / $cell)
    $gridH = [int]($img.Height / $cell)

    for ($cy = 0; $cy -lt $gridH; $cy++) {
        for ($cx = 0; $cx -lt $gridW; $cx++) {
            if ($cx -ge 2 -and $cx -lt $gridW - 2 -and $cy -ge 2 -and $cy -lt $gridH - 2) { continue }

            $colours[$img.GetPixel($cx * $cell + 1, $cy * $cell + 1).ToArgb()] = $true
        }
    }

    return $colours
}

# true when the colour is one of the backdrop shades (with a small tolerance: the cache's
# checkerboard has several near-identical variants, and the deeper ones are not in the ring)
function Test-Backdrop([System.Drawing.Color]$c, [hashtable]$backdrop) {
    foreach ($key in $backdrop.Keys) {
        $b = [System.Drawing.Color]::FromArgb([int]$key)
        $delta = [Math]::Abs($c.R - $b.R) + [Math]::Abs($c.G - $b.G) + [Math]::Abs($c.B - $b.B)

        if ($delta -le 12) { return $true }
    }

    return $false
}

# drop isolated cells: a couple of stray backdrop-shade pixels far from the sprite would widen the
# bounding box and then show up as dirt in the icon
function Remove-Specks([hashtable]$cells) {
    for ($pass = 0; $pass -lt 2; $pass++) {
        $drop = @()

        foreach ($key in @($cells.Keys)) {
            $p = $key -split ','
            $x = [int]$p[0]
            $y = [int]$p[1]
            $neighbours = 0

            for ($dy = -1; $dy -le 1; $dy++) {
                for ($dx = -1; $dx -le 1; $dx++) {
                    if ($dx -eq 0 -and $dy -eq 0) { continue }
                    if ($cells.ContainsKey("$($x + $dx),$($y + $dy)")) { $neighbours++ }
                }
            }

            if ($neighbours -le 1) { $drop += $key }
        }

        foreach ($key in $drop) { $cells.Remove($key) }
    }
}

function Get-Bounds([hashtable]$cells) {
    $minX = 9999; $maxX = -1; $minY = 9999; $maxY = -1

    foreach ($key in $cells.Keys) {
        $p = $key -split ','
        if ([int]$p[0] -lt $minX) { $minX = [int]$p[0] }
        if ([int]$p[0] -gt $maxX) { $maxX = [int]$p[0] }
        if ([int]$p[1] -lt $minY) { $minY = [int]$p[1] }
        if ([int]$p[1] -gt $maxY) { $maxY = [int]$p[1] }
    }

    return @($minX, $minY, $maxX, $maxY)
}

# ------------------------------------------------------------------ native paste

# rule: 'outline-or-bright' = the pouch, drawn on the cache's dark checkerboard
#       'non-background'    = every cell that differs from the top-left pixel (with a tolerance,
#                             the cache's backdrop dithers by a shade or two)
function Import-Native([string]$file, [string]$outFile, [string]$label, [int]$cell, [int]$canvasSize, [string]$rule) {
    $src = [System.Drawing.Bitmap]::FromFile($file)
    $gridW = [int]($src.Width / $cell)
    $gridH = [int]($src.Height / $cell)
    $background = $src.GetPixel(1, 1)
    $backdrop = Get-BackdropColours $src $cell

    $cells = @{}

    for ($cy = 0; $cy -lt $gridH; $cy++) {
        for ($cx = 0; $cx -lt $gridW; $cx++) {
            $c = $src.GetPixel($cx * $cell + 1, $cy * $cell + 1)

            if ($rule -eq 'outline-or-bright') {
                if (($c.R -gt 150) -or (($c.R -eq 0) -and ($c.G -eq 0) -and ($c.B -eq 0))) {
                    $cells["$cx,$cy"] = $c
                }
            } elseif ($rule -eq 'ring') {
                if (-not (Test-Backdrop $c $backdrop)) { $cells["$cx,$cy"] = $c }
            } else {
                $delta = [Math]::Abs($c.R - $background.R)
                $delta = $delta + [Math]::Abs($c.G - $background.G)
                $delta = $delta + [Math]::Abs($c.B - $background.B)

                if ($delta -gt 12) { $cells["$cx,$cy"] = $c }
            }
        }
    }
    $src.Dispose()
    Remove-Specks $cells

    $bounds = Get-Bounds $cells
    $minX = $bounds[0]
    $minY = $bounds[1]
    $w = $bounds[2] - $minX + 1
    $h = $bounds[3] - $minY + 1

    if ($w -gt $canvasSize -or $h -gt $canvasSize) {
        throw "$label : sprite ${w}x${h} does not fit in ${canvasSize}x${canvasSize}"
    }

    $canvas = New-Canvas $canvasSize
    $offX = [int](($canvasSize - $w) / 2)
    $offY = [int](($canvasSize - $h) / 2)

    foreach ($key in $cells.Keys) {
        $p = $key -split ','
        $canvas.SetPixel($offX + ([int]$p[0] - $minX), $offY + ([int]$p[1] - $minY), $cells[$key])
    }

    $canvas.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Host ("${label}: sprite ${w}x${h} -> ${canvasSize}x${canvasSize} at $offX,$offY (native)  => $outFile")
    Save-Preview $canvas $label
    $canvas.Dispose()
}

# ------------------------------------------------------------------ majority-colour downscale

function Import-Downscaled([string]$file, [string]$outFile, [string]$label, [int]$target, [int]$trim, [string]$rule) {
    $src = [System.Drawing.Bitmap]::FromFile($file)
    $cell = 3
    $gridW = [int]($src.Width / $cell)
    $gridH = [int]($src.Height / $cell)

    # mask rule: 'not-black' for references drawn on a flat black backdrop,
    # 'ring' when the backdrop is the cache's dithered checkerboard (collect the outer ring's
    # colours instead of guessing one shade)
    $sprite = New-Object 'bool[,]' $gridW, $gridH
    $backdrop = Get-BackdropColours $src $cell
    $minX = 9999; $maxX = -1; $minY = 9999; $maxY = -1

    for ($cy = 0; $cy -lt $gridH; $cy++) {
        for ($cx = 0; $cx -lt $gridW; $cx++) {
            $c = $src.GetPixel($cx * $cell + 1, $cy * $cell + 1)

            if ($rule -eq 'not-black') {
                $isSprite = -not (($c.R -eq 0) -and ($c.G -eq 0) -and ($c.B -eq 0))
            } else {
                $isSprite = -not (Test-Backdrop $c $backdrop)
            }

            if ($isSprite) {
                $sprite[($cx), ($cy)] = $true
                if ($cx -lt $minX) { $minX = $cx }
                if ($cx -gt $maxX) { $maxX = $cx }
                if ($cy -lt $minY) { $minY = $cy }
                if ($cy -gt $maxY) { $maxY = $cy }
            }
        }
    }

    # the dotted decorative border cannot survive the reduction anyway, drop it first
    $minX = $minX + $trim
    $minY = $minY + $trim
    $maxX = $maxX - $trim
    $maxY = $maxY - $trim
    $w = $maxX - $minX + 1
    $h = $maxY - $minY + 1

    $scale = [Math]::Min($target / $w, $target / $h)
    $destW = [Math]::Min($target, [int][Math]::Round($w * $scale))
    $destH = [Math]::Min($target, [int][Math]::Round($h * $scale))
    $canvas = New-Canvas $target
    $offX = [int](($target - $destW) / 2)
    $offY = [int](($target - $destH) / 2)

    for ($dy = 0; $dy -lt $destH; $dy++) {
        for ($dx = 0; $dx -lt $destW; $dx++) {
            $sx0 = [int][Math]::Floor($dx * $w / $destW)
            $sx1 = [int][Math]::Ceiling(($dx + 1) * $w / $destW)
            $sy0 = [int][Math]::Floor($dy * $h / $destH)
            $sy1 = [int][Math]::Ceiling(($dy + 1) * $h / $destH)

            $counts = @{}
            $total = 0
            $hits = 0

            for ($cy = $sy0; $cy -lt [Math]::Min($sy1, $h); $cy++) {
                for ($cx = $sx0; $cx -lt [Math]::Min($sx1, $w); $cx++) {
                    $total++

                    if (-not $sprite[($minX + $cx), ($minY + $cy)]) { continue }

                    $c = $src.GetPixel(($minX + $cx) * $cell + 1, ($minY + $cy) * $cell + 1)
                    $key = $c.ToArgb()

                    if ($counts.ContainsKey($key)) { $counts[$key]++ } else { $counts[$key] = 1 }

                    $hits++
                }
            }

            if ($hits -eq 0 -or $total -eq 0) { continue }
            # binary alpha: a soft fringe only reads as noise at this size
            if (($hits / $total) -lt 0.5) { continue }

            # majority colour keeps the flat pixel-art blocks crisp
            $best = 0
            $bestCount = 0

            foreach ($key in $counts.Keys) {
                if ($counts[$key] -gt $bestCount) { $bestCount = $counts[$key]; $best = $key }
            }

            $rgb = [System.Drawing.Color]::FromArgb([int]$best)
            $canvas.SetPixel($offX + $dx, $offY + $dy, [System.Drawing.Color]::FromArgb(255, $rgb.R, $rgb.G, $rgb.B))
        }
    }

    $src.Dispose()
    $canvas.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Host ("${label}: sprite ${w}x${h} (trim $trim) -> ${target}x${target} (dest ${destW}x${destH}, majority)  => $outFile")
    Save-Preview $canvas $label
    $canvas.Dispose()
}

# where each reference comes from, how it is processed, where it goes
$jobs = @(
    @{ name = 'artifact_tab_icon'; source = 'image-89d5d9733c1e7651cbbe39f826aae885.png';
       mode = 'native'; cell = 6; canvas = 16; rule = 'outline-or-bright'; dir = 'item' },
    @{ name = 'charm_of_strength'; source = 'image-1d019ab0e7545779bfcaf948ac4276da.png';
       mode = 'native'; cell = 3; canvas = 16; rule = 'non-background'; dir = 'item' },
    @{ name = 'backpack_tab'; source = 'image-846cb651178c4912ce670c438e02f1f3.png';
       mode = 'native'; cell = 3; canvas = 20; rule = 'non-background'; dir = 'gui' },
    @{ name = 'attributes_tab'; source = 'image-2af2323c6c184090a56e3b0d5de35c55.png';
       mode = 'native'; cell = 3; canvas = 20; rule = 'non-background'; dir = 'gui' },
    @{ name = 'slate'; source = 'image-ca60fa5463a9a7dbff644a1e5b2e9d23.png';
       mode = 'downscaled'; target = 16; trim = 2; rule = 'not-black'; dir = 'item' },
    # slate tab icon (fig 4: the grey stone face)
    @{ name = 'combo_sturdy'; source = 'image-fd3f1ff1b1c2bb356b3035c500a2affc.png';
       mode = 'native'; cell = 3; canvas = 16; rule = 'non-background'; dir = 'gui' },
    @{ name = 'warriors_proof'; source = 'image-c87ad1ff1cd702645d73ccc07ff3d9c0.png';
       mode = 'native'; cell = 3; canvas = 18; rule = 'ring'; dir = 'item' },
    # slate tab icon (fig 4: the grey stone face)
    @{ name = 'slate_tab_icon'; source = 'image-02819efdb2970d9e7b051a5e06f349e5.png';
       mode = 'downscaled'; target = 16; trim = 0; rule = 'ring'; dir = 'item' },
    @{ name = 'slate_of_future'; source = 'image-84628a6d87ac4c3a2b8dc0287d8122a0.png';
       mode = 'native'; cell = 3; canvas = 16; rule = 'ring'; dir = 'item' },
    # the coin's gem is 22x21: pasting it natively keeps the art exact, Minecraft scales it down
    @{ name = 'enchant_coin'; source = 'image-0904240550fd05ab97639985e0a49a75.png';
       mode = 'native'; cell = 3; canvas = 24; rule = 'ring'; dir = 'item' }
)

foreach ($job in $jobs) {
    $name = $job.name
    $file = Join-Path $cache $job.source

    if (-not (Test-Path $file)) {
        throw "reference image not found: $file"
    }

    $outDir = if ($job.dir -eq 'gui') { $guiDir } else { $texDir }
    $outFile = Join-Path $outDir ($name + '.png')

    if ($job.mode -eq 'native') {
        Import-Native $file $outFile $name ([int]$job.cell) ([int]$job.canvas) $job.rule
    } else {
        Import-Downscaled $file $outFile $name ([int]$job.target) ([int]$job.trim) $job.rule $job.rule
    }
}

Write-Output 'previews written to .preview/'
