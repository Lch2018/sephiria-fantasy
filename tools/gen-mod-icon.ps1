# Build the SEPHIRIA FANTASY mod icon from the artwork in tools/artifact-ref/mod-icon/.
#
# 2026-10-13 (final round): the user drew the icon themselves - an orange rabbit asleep on a grass
# block, with "z Z" floating above it - and said "use this as the new icon", then asked to keep only
# the rabbit and the block, i.e. to cut the cream background out. The source is
# tools/artifact-ref/mod-icon/rabbit_grass_block.png (1280x1280, a fine pixel painting whose
# smallest block is 2 source pixels).
#
# Two processing steps:
#   1. BACKGROUND CUTOUT - flood fill from the four borders through pixels close to the artwork's
#      cream plate; everything reached becomes transparent. Same rule as
#      tools/import-artifact-icons.ps1: only the plate *connected to the border* disappears, so the
#      dark outlines seal off the things they enclose (the white "Z" glyph, the rabbit's body) and
#      they survive. Pass -NoCutout to keep the plate (the artwork as drawn).
#   2. RESAMPLE - a smooth HighQualityBicubic instead of nearest-neighbour: the 2px grid cannot
#      survive a 5x reduction to 256 anyway (a nearest-neighbour pass was rendered for comparison
#      and showed uneven pixels and stair-step aliasing on the block's edges), and the smooth one
#      reads cleaner at the ~32px a mod list actually displays.
#
# The non-square case is handled too: whatever comes out of the cutout is cropped to the subject's
# bounding box (with a small margin), so the artwork always ends up framed the same way whatever the
# source's own margins are.
#
# The background is a parameter: `-Background '#7092BE'` (the default, the plate colour the user's
# artwork uses) or `-Background transparent`. Cutting the plate out and filling this colour back in
# is deliberate - the anti-aliased pixels along the artwork's edge stay and blend into the fill.
#
# Output : src/main/resources/assets/sephiria_fantasy/icon.png   (256x256, the mod-list icon)
#          tools/modrinth/icon.png                              (512x512, for the project page)
# Preview: .preview/mod_icon_preview.png                         (the 512 one, for eyeballing)
#
# The earlier procedural designs (gold badge, yellow ring, vector rabbit) are in
# tools/gen-mod-icon-procedural.ps1; they only write previews now.
#
# Keep this ASCII (Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK and mis-parses it).

param(
    [int]$Small = 256,
    [int]$Large = 512,
    # how far a pixel may sit from the plate colour and still count as plate (sum of |dR|+|dG|+|dB|)
    [int]$Tolerance = 40,
    # keep the plate (skip step 1) - handy for comparing versions
    [switch]$NoCutout,
    # what goes behind the cut-out subject: a colour, or "transparent" (2026-10-13: the user asked
    # for #7092BE - the same blue-grey plate their artwork uses, so cutting and refilling keeps the
    # anti-aliased edge pixels blending into the background)
    [string]$Background = '#7092BE'
)

Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$source = Join-Path $root 'tools/artifact-ref/mod-icon/rabbit_grass_block.png'
$modIcon = Join-Path $root 'src/main/resources/assets/sephiria_fantasy/icon.png'
$modrinthIcon = Join-Path $root 'tools/modrinth/icon.png'
$previewDir = Join-Path $root '.preview'

if (-not (Test-Path $source)) {
    throw "missing artwork: $source (the mod icon is built from it)"
}

New-Item -ItemType Directory -Force -Path $previewDir, (Split-Path -Parent $modrinthIcon) | Out-Null

$art = [System.Drawing.Bitmap]::FromFile($source)
Write-Output ("artwork " + $art.Width + "x" + $art.Height)

if (-not $NoCutout) {
    # the plate colour is whatever fills the four corners
    $corners = @(
        $art.GetPixel(0, 0), $art.GetPixel(($art.Width - 1), 0),
        $art.GetPixel(0, ($art.Height - 1)), $art.GetPixel(($art.Width - 1), ($art.Height - 1))
    )
    $plateR = ($corners | Measure-Object -Property R -Average).Average
    $plateG = ($corners | Measure-Object -Property G -Average).Average
    $plateB = ($corners | Measure-Object -Property B -Average).Average
    $plateHex = '#{0:X2}{1:X2}{2:X2}' -f [int]$plateR, [int]$plateG, [int]$plateB
    Write-Output ("plate " + $plateHex + "  tolerance " + $Tolerance)

    $w = $art.Width
    $h = $art.Height
    $cut = New-Object 'bool[]' ($w * $h)
    $queue = New-Object System.Collections.Queue

    for ($x = 0; $x -lt $w; $x++) {
        foreach ($y in @(0, ($h - 1))) { $queue.Enqueue(($y * $w + $x)) }
    }
    for ($y = 0; $y -lt $h; $y++) {
        foreach ($x in @(0, ($w - 1))) { $queue.Enqueue(($y * $w + $x)) }
    }

    $removed = 0
    while ($queue.Count -gt 0) {
        $p = $queue.Dequeue()
        if ($cut[$p]) { continue }
        $x = $p % $w
        $y = [int][Math]::Floor($p / $w)
        $c = $art.GetPixel($x, $y)
        $distance = [Math]::Abs($c.R - $plateR) + [Math]::Abs($c.G - $plateG) + [Math]::Abs($c.B - $plateB)
        if ($c.A -gt 0 -and $distance -gt $Tolerance) { continue }
        $cut[$p] = $true
        $removed++
        foreach ($d in @(@(1, 0), @(-1, 0), @(0, 1), @(0, -1))) {
            $nx = $x + $d[0]
            $ny = $y + $d[1]
            if ($nx -lt 0 -or $ny -lt 0 -or $nx -ge $w -or $ny -ge $h) { continue }
            $queue.Enqueue(($ny * $w + $nx))
        }
    }

    $cleared = New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            if ($cut[$y * $w + $x]) { continue }
            $cleared.SetPixel($x, $y, $art.GetPixel($x, $y))
        }
    }

    $art.Dispose()
    $art = $cleared
    $percent = 100.0 * $removed / ($w * $h)
    Write-Output ("background cut: {0:N1}% of the canvas removed" -f $percent)
}

# crop to what is left (the artwork's own silhouette), then pad back to a centred square
$minX = $art.Width; $maxX = -1; $minY = $art.Height; $maxY = -1
for ($y = 0; $y -lt $art.Height; $y++) {
    for ($x = 0; $x -lt $art.Width; $x++) {
        if ($art.GetPixel($x, $y).A -lt 40) { continue }
        if ($x -lt $minX) { $minX = $x }
        if ($x -gt $maxX) { $maxX = $x }
        if ($y -lt $minY) { $minY = $y }
        if ($y -gt $maxY) { $maxY = $y }
    }
}
if ($maxX -lt 0) { throw 'nothing left after the background cutout' }

# a small margin so the silhouette does not touch the icon's edge
$content = [Math]::Max(($maxX - $minX + 1), ($maxY - $minY + 1))
$margin = [int]($content * 0.06)
$side = $content + 2 * $margin
$srcRect = New-Object System.Drawing.RectangleF (($minX - $margin), ($minY - $margin), $side, $side)
Write-Output ("silhouette {0}x{1} at {2},{3} -> square {4}" -f ($maxX - $minX + 1), ($maxY - $minY + 1), $minX, $minY, $side)

function Save-Icon([int]$size, [string]$path) {
    $bmp = New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)

    if ($Background -and $Background -ne 'transparent') {
        $g.Clear([System.Drawing.ColorTranslator]::FromHtml($Background))
    } else {
        $g.Clear([System.Drawing.Color]::Transparent)
    }

    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.DrawImage($art, (New-Object System.Drawing.RectangleF 0, 0, $size, $size), $srcRect,
            [System.Drawing.GraphicsUnit]::Pixel)
    $g.Dispose()
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output ("wrote " + $path + "  (" + $size + "x" + $size + ")")
}

Save-Icon $Small $modIcon
Save-Icon $Large $modrinthIcon
Save-Icon $Large (Join-Path $previewDir 'mod_icon_preview.png')

$art.Dispose()
