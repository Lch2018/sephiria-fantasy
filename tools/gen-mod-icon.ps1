# Build the SEPHIRIA FANTASY mod icon from the artwork in tools/artifact-ref/mod-icon/.
#
# 2026-10-13 (final): the user drew the icon themselves - an orange rabbit asleep on a grass block
# with "z Z" above it. The newest artwork (sefa_transparent.png) IS the finished icon: a square PNG
# with its own alpha, so by DEFAULT this script copies it verbatim ("直接替换") - no cutout, no
# resample. -Process brings back the earlier pipeline for artwork that still needs work:
#   1. BACKGROUND CUTOUT - flood fill from the four borders through pixels close to the artwork's
#      plate colour (detected from the corners); only the plate *connected to the border* disappears,
#      so anything the artwork seals off (outlines, the white "Z", the rabbit's body) survives.
#      -NoCutout skips this step.
#   2. CROP - to the subject's bounding box with a 6% margin, padded back to a centred square, so the
#      subject fills the frame whatever the source's own margins are.
#   3. BACKGROUND FILL - `-Background '#7092BE'` (default) or 'transparent'; cutting the plate and
#      filling a colour back in keeps the anti-aliased edge pixels blending into the background.
#   4. RESAMPLE - HighQualityBicubic to $Small/$Large (256 for the mod list, 512 for Modrinth).
#      Not nearest-neighbour: at a 5x reduction the source's 2px grid cannot survive and nearest
#      gives uneven blocks and stair-step aliasing on the block's edges.
#
# Artwork history (all in tools/artifact-ref/mod-icon/): rabbit_grass_block.png = the current
# transparent artwork; _v2.png = the same art with the #7092BE plate; _v1.png = the earlier 1280
# orange rabbit on cream.
#
# The earlier procedural designs (gold badge, yellow ring, vector rabbit) are in
# tools/gen-mod-icon-procedural.ps1; they only write previews now.
#
# Keep this ASCII (Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK and mis-parses it).

param(
    # run the cutout / crop / fill / resample pipeline above; without it the artwork is copied as-is
    [switch]$Process,
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

if (-not $Process) {
    # direct mode: the artwork is already the finished icon (square, its own alpha); copy it as-is
    if ($art.Width -ne $art.Height) {
        throw "direct mode needs a square artwork; got $($art.Width)x$($art.Height) (use -Process to fit it)"
    }

    $Small = $art.Width
    $Large = $art.Width
    $Background = 'transparent'
    Write-Output ("direct copy at " + $art.Width + "x" + $art.Width + " (no cutout, no resample)")
}

if ($Process -and -not $NoCutout) {
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
if ($Process) {
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
} else {
    $srcRect = New-Object System.Drawing.RectangleF 0, 0, $art.Width, $art.Height
}

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
