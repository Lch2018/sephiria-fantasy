# Root Retreat (root_retreat) block-model textures - the 2026-10-19 redesign.
#
# The author supplied a front-view pixel reference: a stump whose roots spread across the base,
# cradling a book tilted back like a lectern, with a horizontal root beam behind the book's top
# edge and two root branches flaring up and out.
#
# The author then asked for the stump to use VANILLA stone tiles:
#   bark -> minecraft:block/stone            (the trunk sides)
#   wood -> minecraft:block/cobblestone      (the roots, beam and branches)
#   cut  -> minecraft:block/smooth_stone     (the stump's cut top)
# so those three are EXTRACTED from the client jar, not painted here. Only the book tiles are
# generated. (When this model is finally installed into the mod, point the model's texture keys
# straight at minecraft:block/... - do not ship copies of vanilla art in the mod jar.)
#
#   models/root_retreat/bark.png        <- vanilla stone.png
#   models/root_retreat/wood.png        <- vanilla cobblestone.png
#   models/root_retreat/cut.png         <- vanilla smooth_stone.png
#   models/root_retreat/pages.png       the book's page block (white with fine striations)
#   models/root_retreat/cover.png       the cover's face: dark panel with the light text blocks
#   models/root_retreat/cover_plain.png the cover's edges, without the text pattern
#
# Preview: models/root_retreat/preview.png (8x montage over a checkerboard)
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $root 'models\root_retreat'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$SIZE = 16

# ---- palette for the book, sampled off the author's reference render ----
$LIT = '#A1A3BB'
$PALE = '#C3C4D6'
$WHITE = '#FDFDFB'

$COVER = '#625A7C'
$COVER_TEXT = '#7F7F9B'
$COVER_EDGE = '#4E4762'

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

# ---- vanilla stone tiles: find a client jar and pull three PNGs out of it ----------
# The jar is not part of the repo; try the Gradle/Loom cache first (what the dev build uses),
# then the PCL instance. Any client jar will do - these three tiles are unchanged across versions.
function Find-ClientJar {
    $candidates = @()
    $loom = Join-Path $env:USERPROFILE '.gradle\caches\fabric-loom'
    if (Test-Path $loom) {
        $candidates += Get-ChildItem $loom -Recurse -Filter 'minecraft-client.jar' -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending | Select-Object -ExpandProperty FullName
    }
    $pcl = 'D:\PCL\.minecraft\versions'
    if (Test-Path $pcl) {
        $candidates += Get-ChildItem $pcl -Recurse -Filter '*.jar' -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '^\d.*\.jar$' } |
            Sort-Object FullName -Descending | Select-Object -ExpandProperty FullName
    }
    foreach ($c in $candidates) {
        try {
            $zip = [System.IO.Compression.ZipFile]::OpenRead($c)
            $hit = $zip.Entries | Where-Object { $_.FullName -eq 'assets/minecraft/textures/block/stone.png' }
            $zip.Dispose()
            if ($hit) { return $c }
        } catch {
            # not a zip / locked; try the next one
        }
    }
    return $null
}

function Extract-Vanilla($jar, [string]$entry, [string]$outPath) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
    try {
        $e = $zip.Entries | Where-Object { $_.FullName -eq $entry }
        if (-not $e) { throw ("missing in jar: " + $entry) }
        $in = $e.Open()
        $outStream = [System.IO.File]::Create($outPath)
        $in.CopyTo($outStream)
        $outStream.Close()
        $in.Close()
    } finally {
        $zip.Dispose()
    }
}

$jar = Find-ClientJar
if (-not $jar) {
    throw 'no Minecraft client jar found for the vanilla stone tiles (Gradle cache / PCL instance)'
}
Write-Output ('client jar: ' + $jar)
$vanilla = [ordered]@{
    'bark' = 'assets/minecraft/textures/block/stone.png'
    'wood' = 'assets/minecraft/textures/block/cobblestone.png'
    'cut' = 'assets/minecraft/textures/block/smooth_stone.png'
}
foreach ($key in $vanilla.Keys) {
    $p = Join-Path $outDir ($key + '.png')
    Extract-Vanilla $jar $vanilla[$key] $p
    Write-Output ('wrote ' + $p + '  <- ' + $vanilla[$key])
}

# ---- the book tiles, painted here ------------------------------------------------

# The page block: white, with the fine horizontal lines of stacked page edges. Kept very light so
# the white border of the reference reads as paper against the dark cover.
function New-PagesTexture {
    $bmp = New-Canvas
    for ($y = 0; $y -lt $SIZE; $y++) {
        for ($x = 0; $x -lt $SIZE; $x++) {
            $c = $WHITE
            if (($y % 4) -eq 1) { $c = $PALE }
            if (($y % 4) -eq 3) { $c = $LIT }
            Set-Px $bmp $x $y $c
        }
    }
    return $bmp
}

# The cover face: the reference's pattern of light blocks on the dark panel, like lines of text
# and small ornaments. Hand-placed so it matches the reference instead of looking random.
function New-CoverTexture {
    $bmp = New-Canvas
    for ($y = 0; $y -lt $SIZE; $y++) {
        for ($x = 0; $x -lt $SIZE; $x++) {
            Set-Px $bmp $x $y $COVER
        }
    }
    # a subtle dark vignette at the edges so the panel has some depth
    for ($i = 0; $i -lt $SIZE; $i++) {
        Set-Px $bmp $i 0 $COVER_EDGE
        Set-Px $bmp $i 15 $COVER_EDGE
        Set-Px $bmp 0 $i $COVER_EDGE
        Set-Px $bmp 15 $i $COVER_EDGE
    }
    $rows = @(
        @(3, 2, 3), @(5, 2, 10), @(12, 2, 13),   # short - long - short
        @(5, 6, 10), @(12, 6, 14),               # long - medium
        @(3, 9, 4), @(5, 9, 9), @(12, 9, 13),    # short - medium - short
        @(3, 12, 4), @(5, 12, 10), @(12, 12, 13) # short - long - short
    )
    for ($r = 0; $r -lt $rows.Count; $r++) {
        $row = $rows[$r]
        $x1 = $row[0]; $w = $row[1]; $x2 = $row[2]
        for ($x = $x1; $x -le $x2; $x++) {
            Set-Px $bmp $x $w $COVER_TEXT
            Set-Px $bmp $x ($w + 1) $COVER_TEXT
        }
    }
    return $bmp
}

function New-CoverPlainTexture {
    $bmp = New-Canvas
    for ($y = 0; $y -lt $SIZE; $y++) {
        for ($x = 0; $x -lt $SIZE; $x++) {
            $c = $COVER_EDGE
            if ($y -eq 0) { $c = $COVER }
            Set-Px $bmp $x $y $c
        }
    }
    return $bmp
}

# NOTE: PowerShell variable names are case-insensitive, so the generated bitmaps must not be
# called $cover - that would silently overwrite the palette constant $COVER.
$texPages = New-PagesTexture
$texCover = New-CoverTexture
$texCoverPlain = New-CoverPlainTexture

$textures = [ordered]@{
    'pages' = $texPages
    'cover' = $texCover
    'cover_plain' = $texCoverPlain
}
foreach ($key in $textures.Keys) {
    $path = Join-Path $outDir ($key + '.png')
    $textures[$key].Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Output ('wrote ' + $path)
}

# ---- 8x montage over a checkerboard ----
$scale = 8
$gap = 4
$names = @('bark', 'cut', 'wood', 'pages', 'cover', 'cover_plain')
$cell = $SIZE * $scale
$sheet = New-Object System.Drawing.Bitmap((($names.Count * ($cell + $gap)) + $gap), ($cell + $gap * 2))
$g = [System.Drawing.Graphics]::FromImage($sheet)
$g.Clear([System.Drawing.Color]::FromArgb(255, 40, 40, 48))
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$i = 0
foreach ($n in $names) {
    $ox = $gap + $i * ($cell + $gap)
    for ($cy = 0; $cy -lt $SIZE; $cy++) {
        for ($cx = 0; $cx -lt $SIZE; $cx++) {
            $shade = 60
            if ((($cx + $cy) % 2) -eq 0) { $shade = 92 }
            $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, $shade, $shade, $shade))
            $g.FillRectangle($brush, ($ox + $cx * $scale), ($gap + $cy * $scale), $scale, $scale)
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
