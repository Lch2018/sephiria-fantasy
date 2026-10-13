# The procedural icon designs that came before the user's own artwork (kept for reference).
#
# The shipped mod icon is built by tools/gen-mod-icon.ps1 from
# tools/artifact-ref/mod-icon/rabbit_grass_block.png; THIS script is the earlier exploration:
# the gold badge, the yellow ring with the three emblems, and the vector "rabbit on a grass block".
# It writes previews only (they are a good source of pieces, e.g. Draw-Crown / Draw-Slate).
#
# History of this icon (all three rounds came from the user):
#   1. badge    - gold frame, dark plate, three emblems in a grid (pixel-art sprites).
#   2. ring     - black background, golden ring, the three *pixel* sprites shrunk onto the ring.
#   3. ring v2  - same ring layout, but the ring is plain YELLOW and the emblems are redrawn as
#                 smooth vector shapes instead of pixel art (the user asked for "non-pixel").
#   4. current  - the user's newest plan: a RABBIT LYING ON A GRASS BLOCK (isometric block, the
#                 smooth vector rabbit sprawled on its top face). The ring and badge layouts are
#                 still rendered as previews so they can be compared or restored.
#
# The three emblems are drawn with ellipses / rounded rectangles / polygons (anti-aliased), and their
# colours are lifted from the three reference sprites the user supplied (tools/artifact-ref/mod-icon)
# so the redesign still reads as their design: the golden-orange crown, the mauve tablet and the
# pale-pink rabbit with its blush.
#
# Output : src/main/resources/assets/sephiria_fantasy/icon.png   (256x256, the mod-list icon)
#          tools/modrinth/icon.png                              (512x512, for the project page)
# Preview: .preview/mod_icon_preview.png                         (512, the ring)
#          .preview/mod_icon_badge.png                           (256, the older badge layout)
#
# Keep this ASCII (Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK and mis-parses it).

Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$modIcon = Join-Path $root 'src/main/resources/assets/sephiria_fantasy/icon.png'
$modrinthIcon = Join-Path $root 'tools/modrinth/icon.png'
$previewDir = Join-Path $root '.preview'

# ---- ring knobs (fractions of the canvas, so the whole thing scales as one piece) ----
$RING_CANVAS = 512
$RING_RADIUS = 0.355        # centre -> middle of the band
$RING_WIDTH = 0.045         # band thickness
$RING_YELLOW = '#FFE93B'    # plain yellow (was a metallic gold gradient before)
$RING_SPRITE = 0.24         # each emblem is drawn inside a box this fraction of the canvas

# ---- emblem palettes (taken from the user's three reference sprites) ----
$CROWN_GOLD = '#F5A83C'     # their crown's main gold-orange
$CROWN_GOLD_DARK = '#C97A22'
$CROWN_CREAM = '#FFE3B8'    # the pale inlay
$CROWN_GEM = '#D46834'

$SLATE_BODY = '#98848C'     # their tablet's mauve
$SLATE_DARK = '#7D6976'
$SLATE_RIM = '#B4A0A8'
$SLATE_RUNE = '#6E5B66'

$RABBIT_FUR = '#FBF0F2'
$RABBIT_PINK = '#F2A0AE'
$RABBIT_BLUSH = '#E8737F'
$RABBIT_EYE = '#5C4A52'
$RABBIT_TAIL = '#F8D7DF'

# ---- badge knobs (the older layout, kept as a preview) ----
$SIZE = 64
$FRAME = '#FFD24A'
$FRAME_DARK = '#B8862A'
$LINE = '#15141F'
$PLATE_TOP = '#2A2940'
$PLATE_BOTTOM = '#232238'

function Brush([string]$hex) {
    return New-Object System.Drawing.SolidBrush ([System.Drawing.ColorTranslator]::FromHtml($hex))
}

# a rounded rectangle as a GraphicsPath (the workhorse for the crown band, the ears, the tablet)
function New-RoundedPath([single]$x, [single]$y, [single]$w, [single]$h, [single]$r) {
    $d = 2 * $r
    if ($d -gt $w) { $d = $w }
    if ($d -gt $h) { $d = $h }
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc(($x + $w - $d), $y, $d, $d, 270, 90)
    $path.AddArc(($x + $w - $d), ($y + $h - $d), $d, $d, 0, 90)
    $path.AddArc($x, ($y + $h - $d), $d, $d, 90, 90)
    $path.CloseFigure()
    return $path
}

# ------------------------------------------------------------------ crown
# a band with three rounded points (the middle one taller), a pale inlay and a gem
function Draw-Crown([System.Drawing.Graphics]$g, [single]$x, [single]$y, [single]$s) {
    $bandY = $y + $s * 0.70
    $bandH = $s * 0.22

    # the points first, so the band covers their feet
    $points = @(
        @{ cx = 0.5;   half = 0.15; tip = 0.08 },
        @{ cx = 0.215; half = 0.13; tip = 0.34 },
        @{ cx = 0.785; half = 0.13; tip = 0.34 }
    )

    foreach ($point in $points) {
        $cx = $x + $s * $point.cx
        $tipY = $y + $s * $point.tip
        $baseY = $bandY + $bandH * 0.4
        $halfW = $s * $point.half

        $triangle = New-Object System.Drawing.Drawing2D.GraphicsPath
        $triangle.AddPolygon(@(
            [System.Drawing.PointF]::new(($cx - $halfW), $baseY),
            [System.Drawing.PointF]::new($cx, $tipY),
            [System.Drawing.PointF]::new(($cx + $halfW), $baseY)
        ))
        $b = Brush $CROWN_GOLD
        $g.FillPath($b, $triangle)
        $b.Dispose()
        $triangle.Dispose()

        # the tip ball rounds the spike off
        $b = Brush $CROWN_GOLD
        $g.FillEllipse($b, ($cx - $s * 0.045), ($tipY - $s * 0.03), ($s * 0.09), ($s * 0.09))
        $b.Dispose()
    }

    # pale inlay inside the tall middle point
    $inlay = New-Object System.Drawing.Drawing2D.GraphicsPath
    $inlay.AddPolygon(@(
        [System.Drawing.PointF]::new(($x + $s * 0.435), ($bandY + $bandH * 0.3)),
        [System.Drawing.PointF]::new(($x + $s * 0.5), ($y + $s * 0.19)),
        [System.Drawing.PointF]::new(($x + $s * 0.565), ($bandY + $bandH * 0.3))
    ))
    $b = Brush $CROWN_CREAM
    $g.FillPath($b, $inlay)
    $b.Dispose()
    $inlay.Dispose()

    # the band: darker body with a lighter top half, so it reads as a band
    $b = Brush $CROWN_GOLD_DARK
    $path = New-RoundedPath ($x + $s * 0.05) $bandY ($s * 0.90) $bandH ($s * 0.08)
    $g.FillPath($b, $path)
    $b.Dispose()
    $path.Dispose()

    $b = Brush $CROWN_GOLD
    $path = New-RoundedPath ($x + $s * 0.05) $bandY ($s * 0.90) ($bandH * 0.55) ($s * 0.08)
    $g.FillPath($b, $path)
    $b.Dispose()
    $path.Dispose()

    # a gem in the middle of the band
    $gx = $x + $s * 0.5
    $gy = $bandY + $bandH * 0.62
    $gem = New-Object System.Drawing.Drawing2D.GraphicsPath
    $gem.AddPolygon(@(
        [System.Drawing.PointF]::new($gx, ($gy - $s * 0.06)),
        [System.Drawing.PointF]::new(($gx + $s * 0.055), $gy),
        [System.Drawing.PointF]::new($gx, ($gy + $s * 0.06)),
        [System.Drawing.PointF]::new(($gx - $s * 0.055), $gy)
    ))
    $b = Brush $CROWN_GEM
    $g.FillPath($b, $gem)
    $b.Dispose()
    $gem.Dispose()
}

# ------------------------------------------------------------------ slate
# the artifact backpack's tablet: a rounded slab with a carved rune
function Draw-Slate([System.Drawing.Graphics]$g, [single]$x, [single]$y, [single]$s) {
    $w = $s * 0.62
    $h = $s * 0.96
    $sx = $x + ($s - $w) / 2
    $sy = $y + ($s - $h) / 2
    $radius = $s * 0.16

    $b = Brush $SLATE_DARK
    $path = New-RoundedPath $sx $sy $w $h $radius
    $g.FillPath($b, $path)
    $b.Dispose()
    $path.Dispose()

    $inset = $s * 0.045
    $b = Brush $SLATE_BODY
    $path = New-RoundedPath ($sx + $inset) ($sy + $inset) ($w - 2 * $inset) ($h - 2 * $inset) ($radius * 0.85)
    $g.FillPath($b, $path)
    $b.Dispose()
    $path.Dispose()

    # a lighter rim on the upper third
    $b = Brush $SLATE_RIM
    $path = New-RoundedPath ($sx + $inset * 1.6) ($sy + $inset * 1.8) ($w - 2 * $inset * 1.6) ($h * 0.16) ($inset)
    $g.FillPath($b, $path)
    $b.Dispose()
    $path.Dispose()

    # the carved rune: a vertical bar with two cross bars (the shape on the reference tablet)
    $innerX = $sx + $w * 0.28
    $innerW = $w * 0.44
    $barW = $w * 0.16
    $b = Brush $SLATE_RUNE
    $path = New-RoundedPath (($sx + $w / 2) - $barW / 2) ($sy + $h * 0.24) $barW ($h * 0.52) $barW
    $g.FillPath($b, $path)
    $b.Dispose()
    $path.Dispose()

    foreach ($t in @(0.32, 0.70)) {
        $b = Brush $SLATE_RUNE
        $path = New-RoundedPath $innerX ($sy + $h * $t) $innerW ($h * 0.07) ($h * 0.035)
        $g.FillPath($b, $path)
        $b.Dispose()
        $path.Dispose()
    }
}

# ------------------------------------------------------------------ rabbit
# the project's mascot: long ears, round face, blush
function Draw-Rabbit([System.Drawing.Graphics]$g, [single]$x, [single]$y, [single]$s) {
    $furBrush = Brush $RABBIT_FUR
    $headW = $s * 0.90
    $headH = $s * 0.72
    $headX = $x + ($s - $headW) / 2
    $headY = $y + $s * 0.30

    # ears first (the head covers their bases)
    foreach ($side in @(-1, 1)) {
        $state = $g.Save()
        $g.TranslateTransform(($x + $s * (0.5 + $side * 0.19)), ($y + $s * 0.34))
        $g.RotateTransform(($side * 9))
        $earW = $s * 0.22
        $earH = $s * 0.50
        $path = New-RoundedPath (-$earW / 2) (-$earH) $earW $earH ($earW / 2)
        $g.FillPath($furBrush, $path)
        $path.Dispose()
        $inner = New-RoundedPath (-$earW * 0.24) (-$earH * 0.86) ($earW * 0.48) ($earH * 0.7) ($earW * 0.24)
        $b = Brush $RABBIT_PINK
        $g.FillPath($b, $inner)
        $b.Dispose()
        $inner.Dispose()
        $g.Restore($state)
    }

    # head
    $g.FillEllipse($furBrush, $headX, $headY, $headW, $headH)

    # chin tint at the bottom of the face
    $b = Brush $RABBIT_CHIN
    $g.FillEllipse($b, ($headX + $headW * 0.30), ($headY + $headH * 0.68), ($headW * 0.40), ($headH * 0.34))
    $b.Dispose()

    # eyes (small vertical ovals), nose and blush
    $eyeW = $s * 0.07
    $eyeH = $s * 0.14
    $eyeY = $headY + $headH * 0.40
    $b = Brush $RABBIT_EYE
    foreach ($side in @(-1, 1)) {
        $g.FillEllipse($b, ($x + $s * (0.5 + $side * 0.20) - $eyeW / 2), $eyeY, $eyeW, $eyeH)
    }
    $b.Dispose()

    $b = Brush $RABBIT_PINK
    $g.FillEllipse($b, ($x + $s * 0.5 - $s * 0.055), ($headY + $headH * 0.60), ($s * 0.11), ($s * 0.075))
    $b.Dispose()

    $b = Brush $RABBIT_BLUSH
    foreach ($side in @(-1, 1)) {
        $g.FillEllipse($b, ($x + $s * (0.5 + $side * 0.33) - $s * 0.10), ($headY + $headH * 0.56), ($s * 0.20), ($s * 0.085))
    }
    $b.Dispose()

    $furBrush.Dispose()
}

# ---- layout tables ----
# ring angles are degrees in screen space (y grows downwards): -90 = top, +30 = lower-right,
# +150 = lower-left, i.e. the emblems sit 120 degrees apart on the band
$RING_PLACEMENT = @(
    @{ name = 'crown';  angle = -90.0 },
    @{ name = 'rabbit'; angle = 30.0 },
    @{ name = 'slate';  angle = 150.0 }
)

$PLACEMENTS = @(
    @{ name = 'crown';  x = 3;  y = 3;  w = 27; h = 25 },
    @{ name = 'slate';  x = 34; y = 3;  w = 27; h = 26 },
    @{ name = 'rabbit'; x = 15; y = 29; w = 34; h = 32 }
)

function Draw-Emblem([System.Drawing.Graphics]$g, [string]$name, [single]$x, [single]$y, [single]$size) {
    switch ($name) {
        'crown' { Draw-Crown $g $x $y $size }
        'slate' { Draw-Slate $g $x $y $size }
        'rabbit' { Draw-Rabbit $g $x $y $size }
        default { throw "unknown emblem: $name" }
    }
}

# ---- composition 1: the yellow ring on black ----
function New-Ring {
    $size = $RING_CANVAS
    $bmp = New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    # background: black, as asked
    $g.Clear([System.Drawing.Color]::Black)

    # the plain yellow band
    $centre = $size / 2.0
    $radius = $size * $RING_RADIUS
    $rect = New-Object System.Drawing.RectangleF (($centre - $radius), ($centre - $radius), (2 * $radius), (2 * $radius))
    $penBrush = Brush $RING_YELLOW
    $pen = New-Object System.Drawing.Pen ($penBrush, ($size * $RING_WIDTH))
    $g.DrawEllipse($pen, $rect)
    $pen.Dispose()
    $penBrush.Dispose()

    # the emblems, centred on the band
    $box = [single]($size * $RING_SPRITE)
    foreach ($place in $RING_PLACEMENT) {
        $radians = $place.angle * [Math]::PI / 180.0
        $px = [single]($centre + [Math]::Cos($radians) * $radius)
        $py = [single]($centre + [Math]::Sin($radians) * $radius)
        Draw-Emblem $g $place.name ($px - $box / 2) ($py - $box / 2) $box
        Write-Host ("  {0,-8} at {1,4:N0},{2,4:N0}" -f $place.name, $px, $py)
    }

    $g.Dispose()
    return $bmp
}

# ---- composition 2: the older badge (gold frame, dark plate) ----
function New-Badge {
    $bmp = New-Object System.Drawing.Bitmap $SIZE, $SIZE, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

    for ($y = 0; $y -lt $SIZE; $y++) {
        for ($x = 0; $x -lt $SIZE; $x++) {
            $edge = [Math]::Min([Math]::Min($x, $y), [Math]::Min($SIZE - 1 - $x, $SIZE - 1 - $y))
            if ($edge -eq 0) { $bmp.SetPixel($x, $y, [System.Drawing.ColorTranslator]::FromHtml($FRAME)) }
            elseif ($edge -eq 1) { $bmp.SetPixel($x, $y, [System.Drawing.ColorTranslator]::FromHtml($FRAME_DARK)) }
            elseif ($edge -eq 2) { $bmp.SetPixel($x, $y, [System.Drawing.ColorTranslator]::FromHtml($LINE)) }
            elseif ($y -lt ($SIZE / 2)) { $bmp.SetPixel($x, $y, [System.Drawing.ColorTranslator]::FromHtml($PLATE_TOP)) }
            else { $bmp.SetPixel($x, $y, [System.Drawing.ColorTranslator]::FromHtml($PLATE_BOTTOM)) }
        }
    }

    foreach ($place in $PLACEMENTS) {
        Draw-Emblem $g $place.name ($place.x + 1) ($place.y + 1) ($place.w - 2)
    }

    $g.Dispose()
    return $bmp
}

# ---- composition 3 (current): a rabbit lying on a grass block ----
# The user's newest plan: "a rabbit lying on a grass block". So: an isometric grass block (the
# recognisable Minecraft block - green top, dirt sides with the grass overhang and a couple of
# speckles) with the smooth vector rabbit sprawled on its top face, one front paw hanging over the
# front-left edge, a tail at the back, and a few grass tufts next to it.
$GRASS_TOP = '#7CC24F'      # lit grass
$GRASS_TOP_DARK = '#61A339' # shading on the top face
$GRASS_SIDE = '#74BE46'     # the grass overhang on the sides
$DIRT_LEFT = '#8A5B3A'      # dirt, lit side
$DIRT_RIGHT = '#6E452B'     # dirt, shaded side
$DIRT_SPECK = '#5A3722'

# the top diamond of the block, shared by the grass fill and the rabbit's shadow clip
function New-TopFacePath([single]$x, [single]$y, [single]$s) {
    $cx = $x + $s * 0.5
    $topY = $y + $s * 0.33
    $half = $s * 0.35
    $isoH = $half * 0.5
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddPolygon(@(
        [System.Drawing.PointF]::new($cx, $topY),
        [System.Drawing.PointF]::new(($cx + $half), ($topY + $isoH)),
        [System.Drawing.PointF]::new($cx, ($topY + 2 * $isoH)),
        [System.Drawing.PointF]::new(($cx - $half), ($topY + $isoH))
    ))
    return $path
}

# an isometric cube: a diamond top plus two parallelogram sides
function Draw-GrassBlock([System.Drawing.Graphics]$g, [single]$x, [single]$y, [single]$s) {
    $cx = $x + $s * 0.5
    $topY = $y + $s * 0.33
    $half = $s * 0.35
    $isoH = $half * 0.5
    $side = $s * 0.21

    $top = New-TopFacePath $x $y $s
    $b = Brush $GRASS_TOP
    $g.FillPath($b, $top)
    $b.Dispose()
    $top.Dispose()

    # a lighter wedge on the top face so it does not read flat
    $b = Brush $GRASS_TOP_DARK
    $wedge = New-Object System.Drawing.Drawing2D.GraphicsPath
    $wedge.AddPolygon(@(
        [System.Drawing.PointF]::new($cx, ($topY + 2 * $isoH)),
        [System.Drawing.PointF]::new(($cx + $half), ($topY + $isoH)),
        [System.Drawing.PointF]::new($cx, ($topY + 1.45 * $isoH))
    ))
    $g.FillPath($b, $wedge)
    $b.Dispose()
    $wedge.Dispose()

    # the two dirt sides
    $left = New-Object System.Drawing.Drawing2D.GraphicsPath
    $left.AddPolygon(@(
        [System.Drawing.PointF]::new(($cx - $half), ($topY + $isoH)),
        [System.Drawing.PointF]::new($cx, ($topY + 2 * $isoH)),
        [System.Drawing.PointF]::new($cx, ($topY + 2 * $isoH + $side)),
        [System.Drawing.PointF]::new(($cx - $half), ($topY + $isoH + $side))
    ))
    $b = Brush $DIRT_LEFT
    $g.FillPath($b, $left)
    $b.Dispose()
    $left.Dispose()

    $right = New-Object System.Drawing.Drawing2D.GraphicsPath
    $right.AddPolygon(@(
        [System.Drawing.PointF]::new(($cx + $half), ($topY + $isoH)),
        [System.Drawing.PointF]::new($cx, ($topY + 2 * $isoH)),
        [System.Drawing.PointF]::new($cx, ($topY + 2 * $isoH + $side)),
        [System.Drawing.PointF]::new(($cx + $half), ($topY + $isoH + $side))
    ))
    $b = Brush $DIRT_RIGHT
    $g.FillPath($b, $right)
    $b.Dispose()
    $right.Dispose()

    # the grass overhang along the top of both sides
    $overhang = $s * 0.045
    $leftGrass = New-Object System.Drawing.Drawing2D.GraphicsPath
    $leftGrass.AddPolygon(@(
        [System.Drawing.PointF]::new(($cx - $half), ($topY + $isoH)),
        [System.Drawing.PointF]::new($cx, ($topY + 2 * $isoH)),
        [System.Drawing.PointF]::new($cx, ($topY + 2 * $isoH + $overhang)),
        [System.Drawing.PointF]::new(($cx - $half), ($topY + $isoH + $overhang))
    ))
    $b = Brush $GRASS_SIDE
    $g.FillPath($b, $leftGrass)
    $b.Dispose()
    $leftGrass.Dispose()

    $rightGrass = New-Object System.Drawing.Drawing2D.GraphicsPath
    $rightGrass.AddPolygon(@(
        [System.Drawing.PointF]::new(($cx + $half), ($topY + $isoH)),
        [System.Drawing.PointF]::new($cx, ($topY + 2 * $isoH)),
        [System.Drawing.PointF]::new($cx, ($topY + 2 * $isoH + $overhang)),
        [System.Drawing.PointF]::new(($cx + $half), ($topY + $isoH + $overhang))
    ))
    $b = Brush $GRASS_SIDE
    $g.FillPath($b, $rightGrass)
    $b.Dispose()
    $rightGrass.Dispose()

    # dirt speckles
    $b = Brush $DIRT_SPECK
    foreach ($speck in @(
        @{ u = 0.62; v = 0.30 }, @{ u = 0.80; v = 0.55 }, @{ u = 0.40; v = 0.62 },
        @{ u = 0.70; v = 0.78 }, @{ u = 0.20; v = 0.45 }, @{ u = 0.90; v = 0.20 }
    )) {
        $px = $cx - $half + $s * ($speck.u * 0.66)
        $py = $topY + $isoH + $side * $speck.v
        $g.FillEllipse($b, $px, ($py - $s * 0.012), ($s * 0.028), ($s * 0.022))
    }
    $b.Dispose()
}

# a few blades of grass sticking up from the top face
function Draw-GrassTufts([System.Drawing.Graphics]$g, [single]$x, [single]$y, [single]$s) {
    $b = Brush $GRASS_TOP
    foreach ($tuft in @(
        @{ u = 0.60; v = 0.60; h = 0.055 }, @{ u = 0.70; v = 0.53; h = 0.045 },
        @{ u = 0.44; v = 0.645; h = 0.05 }
    )) {
        $px = $x + $s * $tuft.u
        $py = $y + $s * $tuft.v
        foreach ($blade in @(-1, 0, 1)) {
            $lean = $s * 0.02 * $blade
            $path = New-Object System.Drawing.Drawing2D.GraphicsPath
            $path.AddPolygon(@(
                [System.Drawing.PointF]::new(($px + $s * 0.012 * $blade), $py),
                [System.Drawing.PointF]::new(($px + $lean), ($py - $s * $tuft.h)),
                [System.Drawing.PointF]::new(($px + $s * 0.03 + $s * 0.012 * $blade), $py)
            ))
            $g.FillPath($b, $path)
            $path.Dispose()
        }
    }
    $b.Dispose()
}

# the rabbit sprawled on the top face: body + head + ears + tail
# Everything is sized against the block's top diamond (which spans x 0.15..0.85, y 0.33..0.68 of the
# canvas): the rabbit is deliberately smaller than the face so it reads as *lying on* it.
function Draw-LyingRabbit([System.Drawing.Graphics]$g, [single]$x, [single]$y, [single]$s) {
    $furBrush = Brush $RABBIT_FUR
    $bodyW = $s * 0.40
    $bodyH = $s * 0.155
    $bodyX = $x + $s * 0.34
    $bodyY = $y + $s * 0.475

    # tail (behind the body, on the upper-right corner of the face); its own soft pink, a touch
    # lighter than the ear pink so it reads as fluff rather than a cherry
    $b = Brush $RABBIT_TAIL
    $g.FillEllipse($b, ($x + $s * 0.655), ($y + $s * 0.44), ($s * 0.095), ($s * 0.095))
    $b.Dispose()

    # shadows first: a translucent black blob clipped to the top face, so the rabbit reads as
    # sitting *on* the grass (no fake shading painted on the fur itself)
    $clip = New-TopFacePath $x $y $s
    $g.SetClip($clip)
    $shadow = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(64, 0, 0, 0))
    $g.FillEllipse($shadow, ($bodyX - $s * 0.02), ($bodyY + $bodyH * 0.45), ($bodyW * 1.1), ($bodyH * 0.95))
    $g.FillEllipse($shadow, ($headCx - $headR * 1.15), ($headCy - $headR * 0.55), ($headR * 2.3), ($headR * 2.0))
    $shadow.Dispose()
    $g.ResetClip()
    $clip.Dispose()

    # body: a lying oval
    $g.FillEllipse($furBrush, $bodyX, $bodyY, $bodyW, $bodyH)

    # ears (before the head, so the head covers their bases)
    foreach ($ears in @(@{ a = -14.0 }, @{ a = -1.0 })) {
        $state = $g.Save()
        $g.TranslateTransform(($x + $s * 0.335), ($y + $s * 0.40))
        $g.RotateTransform($ears.a)
        $earW = $s * 0.085
        $earH = $s * 0.225
        $path = New-RoundedPath (-$earW / 2) (-$earH) $earW $earH ($earW / 2)
        $g.FillPath($furBrush, $path)
        $path.Dispose()
        $inner = New-RoundedPath (-$earW * 0.24) (-$earH * 0.84) ($earW * 0.48) ($earH * 0.62) ($earW * 0.24)
        $b = Brush $RABBIT_PINK
        $g.FillPath($b, $inner)
        $b.Dispose()
        $inner.Dispose()
        $g.Restore($state)
    }

    # head
    $headR = $s * 0.105
    $headCx = $x + $s * 0.335
    $headCy = $y + $s * 0.475
    $g.FillEllipse($furBrush, ($headCx - $headR), ($headCy - $headR), (2 * $headR), (2 * $headR))

    # eyes, nose, blush (the face looks to the left)
    $b = Brush $RABBIT_EYE
    foreach ($dx in @(-0.58, -0.10)) {
        $g.FillEllipse($b, ($headCx + $headR * $dx - $s * 0.013), ($headCy - $headR * 0.26), ($s * 0.026), ($s * 0.046))
    }
    $b.Dispose()

    $b = Brush $RABBIT_PINK
    $g.FillEllipse($b, ($headCx - $headR * 0.92), ($headCy + $headR * 0.22), ($s * 0.024), ($s * 0.019))
    $b.Dispose()

    $b = Brush $RABBIT_BLUSH
    $g.FillEllipse($b, ($headCx - $headR * 0.46), ($headCy + $headR * 0.34), ($s * 0.058), ($s * 0.03))
    $b.Dispose()

    $furBrush.Dispose()
}

function New-GrassRabbit {
    $size = $RING_CANVAS
    $bmp = New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    $g.Clear([System.Drawing.Color]::Black)

    Draw-GrassBlock $g 0 0 ([single]$size)
    Draw-GrassTufts $g 0 0 ([single]$size)
    Draw-LyingRabbit $g 0 0 ([single]$size)

    $g.Dispose()
    return $bmp
}

# ---- render both, publish the ring as the icon ----
function Save-Scaled([System.Drawing.Bitmap]$src, [int]$w, [int]$h, [string]$path) {
    $big = New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($big)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.DrawImage($src, 0, 0, $w, $h)
    $g.Dispose()
    $big.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $big.Dispose()
}

New-Item -ItemType Directory -Force -Path $previewDir, (Split-Path -Parent $modrinthIcon) | Out-Null

# the icon itself: the grass-block rabbit (the user's newest plan)
$grass = New-GrassRabbit
Save-Scaled $grass 256 256 $modIcon
Save-Scaled $grass 512 512 $modrinthIcon
Save-Scaled $grass 512 512 (Join-Path $previewDir 'mod_icon_vector_rabbit.png')
$grass.Dispose()

# the two earlier layouts stay around as previews so they can be compared / restored
$ring = New-Ring
Save-Scaled $ring 512 512 (Join-Path $previewDir 'mod_icon_ring.png')
$ring.Dispose()

$badge = New-Badge
Save-Scaled $badge 256 256 (Join-Path $previewDir 'mod_icon_badge.png')
$badge.Dispose()

Write-Output ('wrote ' + $modIcon + '  (grass rabbit, 256x256)')
Write-Output ('wrote ' + $modrinthIcon + '  (grass rabbit, 512x512)')
Write-Output ('preview ' + (Join-Path $previewDir 'mod_icon_vector_rabbit.png') + '  (grass rabbit)')
Write-Output ('preview ' + (Join-Path $previewDir 'mod_icon_ring.png') + '  (ring, earlier)')
Write-Output ('preview ' + (Join-Path $previewDir 'mod_icon_badge.png') + '  (badge, earlier)')
