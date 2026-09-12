# Build the SEPHIRIA item textures from the original in-game pixel sprites.
#
# Source : tools/weapon-ref/*.png  (102x120, the game's branch weapon sprites)
# Output : src/main/resources/assets/sephiria/textures/item/<item>.png  (128x128)
#          src/main/resources/assets/sephiria/icon.png                (128x128)
#
# The sprites are copied pixel for pixel onto a 128x128 canvas and centred, so nothing
# is resampled: the textures keep the original artwork exactly.
#
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$refDir = Join-Path $root 'tools\weapon-ref'
$itemDir = Join-Path $root 'src\main\resources\assets\sephiria\textures\item'
$assetDir = Join-Path $root 'src\main\resources\assets\sephiria'

New-Item -ItemType Directory -Force -Path $itemDir | Out-Null

$canvas = 128

# branch sprite -> mod item id
$map = [ordered]@{
    'shield_sword' = 'default_sword_and_shield'
    'great_sword'  = 'steel_greatsword'
    'dagger'       = 'dagger'
    'crossbow'     = 'colossal_crossbow'
    'katana'       = 'blade'
    'staff'        = 'quarterstaff'
}

function New-CentredCanvas {
    param([System.Drawing.Bitmap]$Source, [int]$Size)

    $bmp = New-Object System.Drawing.Bitmap($Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $ox = [int](($Size - $Source.Width) / 2)
    $oy = [int](($Size - $Source.Height) / 2)

    for ($y = 0; $y -lt $Source.Height; $y++) {
        for ($x = 0; $x -lt $Source.Width; $x++) {
            $p = $Source.GetPixel($x, $y)
            if ($p.A -eq 0) { continue }
            $bmp.SetPixel($x + $ox, $y + $oy, $p)
        }
    }

    return @{ Bitmap = $bmp; OffsetX = $ox; OffsetY = $oy }
}

function Show-Palette {
    param([System.Drawing.Bitmap]$Bmp, [string]$Label)

    $counts = @{}
    for ($y = 0; $y -lt $Bmp.Height; $y++) {
        for ($x = 0; $x -lt $Bmp.Width; $x++) {
            $p = $Bmp.GetPixel($x, $y)
            if ($p.A -lt 200) { continue }
            $key = ('{0:X2}{1:X2}{2:X2}' -f $p.R, $p.G, $p.B)
            if ($counts.ContainsKey($key)) { $counts[$key]++ } else { $counts[$key] = 1 }
        }
    }

    $top = $counts.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 10
    $desc = ($top | ForEach-Object { '#' + $_.Key + 'x' + $_.Value }) -join ' '
    Write-Output ($Label.PadRight(26) + 'colors=' + $counts.Count.ToString().PadLeft(5) + '  top: ' + $desc)
}

$firstSprite = $null

foreach ($src in $map.Keys) {
    $srcPath = Join-Path $refDir ($src + '.png')
    if (-not (Test-Path $srcPath)) { throw ("missing reference sprite: " + $srcPath) }

    $sprite = [System.Drawing.Bitmap]::FromFile($srcPath)
    $built = New-CentredCanvas -Source $sprite -Size $canvas
    $outPath = Join-Path $itemDir ($map[$src] + '.png')
    $built.Bitmap.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)

    Write-Output ("item  " + $map[$src].PadRight(26) + $sprite.Width + 'x' + $sprite.Height + ' -> ' + $canvas + 'x' + $canvas + '  placed at (' + $built.OffsetX + ',' + $built.OffsetY + ')')
    Show-Palette -Bmp $sprite -Label ('      palette ' + $src)

    if ($src -eq 'shield_sword') { $firstSprite = $sprite } else { $sprite.Dispose() }
    $built.Bitmap.Dispose()
}

# mod icon: same look as before (dark panel + gold frame) but with the real sword-and-shield sprite
$icon = New-Object System.Drawing.Bitmap($canvas, $canvas, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($icon)
$g.Clear([System.Drawing.Color]::FromArgb(255, 27, 20, 48))
$g.FillRectangle((New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 42, 33, 69))), 3, 3, 122, 122)
$gold = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 216, 169, 60))
$g.FillRectangle($gold, 0, 0, 128, 3)
$g.FillRectangle($gold, 0, 125, 128, 3)
$g.FillRectangle($gold, 0, 0, 3, 128)
$g.FillRectangle($gold, 125, 0, 3, 128)
$g.Dispose()

# paste the sprite on top, pixel for pixel, centred
$ox = [int]((128 - $firstSprite.Width) / 2)
$oy = [int]((128 - $firstSprite.Height) / 2)
for ($y = 0; $y -lt $firstSprite.Height; $y++) {
    for ($x = 0; $x -lt $firstSprite.Width; $x++) {
        $p = $firstSprite.GetPixel($x, $y)
        if ($p.A -eq 0) { continue }
        $icon.SetPixel($x + $ox, $y + $oy, $p)
    }
}
$icon.Save((Join-Path $assetDir 'icon.png'), [System.Drawing.Imaging.ImageFormat]::Png)
Write-Output ""
Write-Output ("wrote " + $itemDir + '\*.png')
Write-Output ("wrote " + (Join-Path $assetDir 'icon.png'))
