# Build a labelled contact sheet of the six generated item textures (and the mod icon),
# scaled up with nearest neighbour so the pixels stay readable.
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$itemDir = Join-Path $root 'src\main\resources\assets\sephiria\textures\item'
$assetDir = Join-Path $root 'src\main\resources\assets\sephiria'
$outDir = Join-Path $root '.preview'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$items = [ordered]@{
    'default_sword_and_shield' = 'sword+shield'
    'steel_greatsword'         = 'greatsword'
    'dagger'                   = 'dagger'
    'colossal_crossbow'        = 'crossbow'
    'blade'                    = 'katana'
    'quarterstaff'             = 'staff'
}

$scale = 2
$cell = 128 * $scale
$pad = 12
$labelH = 30
$cols = 3
$rows = 2
$w = $pad + ($cell + $pad) * $cols
$h = $pad + ($cell + $labelH + $pad) * $rows

$sheet = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($sheet)
$g.Clear([System.Drawing.Color]::FromArgb(255, 198, 198, 198))
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$font = New-Object System.Drawing.Font('Consolas', 11, [System.Drawing.FontStyle]::Bold)
$brush = [System.Drawing.Brushes]::Black

$idx = 0
foreach ($id in $items.Keys) {
    $cx = $idx % $cols
    $cy = [int]($idx / $cols)
    $x = $pad + $cx * ($cell + $pad)
    $y = $pad + $cy * ($cell + $labelH + $pad)

    # label above the cell, so long names never overlap the next column
    $g.DrawString($id, $font, $brush, $x, $y)
    $g.DrawString(('[' + $items[$id] + ']'), $font, $brush, ($x + 150), $y)

    $bmp = [System.Drawing.Bitmap]::FromFile((Join-Path $itemDir ($id + '.png')))
    $g.DrawImage($bmp, (New-Object System.Drawing.Rectangle($x, ($y + $labelH), $cell, $cell)))
    $bmp.Dispose()
    $idx++
}
$g.Dispose()
$sheet.Save((Join-Path $outDir 'textures_sheet.png'), [System.Drawing.Imaging.ImageFormat]::Png)
Write-Output ("wrote " + (Join-Path $outDir 'textures_sheet.png') + "  " + $w + "x" + $h)

# mod icon on its own
$icon = [System.Drawing.Bitmap]::FromFile((Join-Path $assetDir 'icon.png'))
$big = New-Object System.Drawing.Bitmap(256, 256, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g2 = [System.Drawing.Graphics]::FromImage($big)
$g2.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g2.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g2.DrawImage($icon, (New-Object System.Drawing.Rectangle(0, 0, 256, 256)))
$g2.Dispose()
$big.Save((Join-Path $outDir 'icon_2x.png'), [System.Drawing.Imaging.ImageFormat]::Png)
Write-Output ("wrote " + (Join-Path $outDir 'icon_2x.png'))
$icon.Dispose(); $big.Dispose()
