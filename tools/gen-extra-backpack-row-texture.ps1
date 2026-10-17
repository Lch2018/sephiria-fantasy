# Generates textures/item/extra_backpack_row.png for the "Extra Backpack Row" item.
#
# User spec (2026-10-16): the item's icon must be the SAME art as the "Sephiria backpack"
# icon, i.e. the backpack tab button icon at textures/gui/backpack_tab.png (20x20, the size
# tab buttons are drawn at).
#
# It is copied verbatim instead of being resampled down to 16x16: resampling a 20x20 pixel-art
# icon onto a 16x16 grid smears the outline and the flap lines, and this mod already ships
# item textures that are not 16x16 (enchant_coin is 24x24, some artifacts are 32x32/128x128),
# so keeping the original size gives exactly the same look as the tab icon.
#
# ASCII only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 files as GBK.
#
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File tools\gen-extra-backpack-row-texture.ps1

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$src = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\textures\gui\backpack_tab.png'
$dst = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\textures\item\extra_backpack_row.png'

if (-not (Test-Path $src)) { throw "source icon not found: $src" }

$source = New-Object System.Drawing.Bitmap($src)
$out = New-Object System.Drawing.Bitmap($source.Width, $source.Height,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [System.Drawing.Graphics]::FromImage($out)
$graphics.DrawImageUnscaled($source, 0, 0)
$graphics.Dispose()
$source.Dispose()
$out.Save($dst, [System.Drawing.Imaging.ImageFormat]::Png)

# report how much is opaque, so a fully transparent result is caught here instead of in game
$opaque = 0
for ($y = 0; $y -lt $out.Height; $y++) {
    for ($x = 0; $x -lt $out.Width; $x++) {
        if ($out.GetPixel($x, $y).A -ge 16) { $opaque++ }
    }
}
$w = $out.Width
$h = $out.Height
$out.Dispose()

Write-Output ("wrote " + $dst + "  (" + $w + "x" + $h + ", " + $opaque + " opaque pixels)")
if ($opaque -lt 64) { throw "generated icon looks empty ($opaque opaque pixels) - check the source icon" }
