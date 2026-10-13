# Generate the Frost Armament sword-qi particle texture.
#
# The Sun Sword mark (gen-sun-slash-texture.ps1) is a STRAIGHT cut; the scabbard's sword qi is
# an ARC instead - the user asked for "an ice-blue arc". So this one draws a crescent: an arc of
# a circle centred below the canvas, thin bright core, soft aura, and tapered tips so it reads as
# a sweeping slash rather than a ring segment.
#
# Same alpha philosophy as the sun slash: flat-topped core (alpha CORE_ALPHA, not fully opaque so
# it glows instead of glaring) plus one continuous falloff out to the faint aura - drawn on the
# translucent particle layer and faded out over its lifetime by client/particle/FrostQiParticle.
#
# Output : src/main/resources/assets/sephiria_fantasy/textures/particle/frost_qi_0.png (64x64)
# Preview: .preview/frost_qi_preview.png  (alpha over a mid grey, enlarged, for eyeballing)
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\textures\particle'
$out = Join-Path $outDir 'frost_qi_0.png'
$previewDir = Join-Path $root '.preview'
$preview = Join-Path $previewDir 'frost_qi_preview.png'

# ---- shape and colour knobs (all lengths are texture pixels; the canvas is SIZE x SIZE) ----
$SIZE = 64
$ARC_RADIUS = 26.0       # radius of the arc; its centre sits below the canvas
$ARC_TOP_MARGIN = 24.0   # where the top of the arc sits (the arc's midpoint): centred on the canvas,
                         # so the quad the particle draws it on is centred on the target
$ARC_HALF_ANGLE = 66.0   # half sweep, degrees - the span across the canvas and the droop of the tips
$CORE_HALF = 1.7         # half thickness of the bright line at the middle of the arc
$CORE_PLATEAU = 0.65     # share of CORE_HALF that stays at full alpha before the falloff starts
$CORE_TAPER = 0.85       # share of the core lost by the tip (1.0 would taper to a point)
$CORE_EXP = 1.7          # taper curve: 2.0 = quadratic, <2 keeps the line fat for longer
$CORE_ALPHA = 0.9        # peak alpha: a little see-through, so it glows instead of glaring
$AURA_SCALE = 2.6        # aura half thickness = core * AURA_SCALE + AURA_MIN (the faint glow)
$AURA_MIN = 0.4
$AURA_FALLOFF = 1.5      # exponent of the alpha falloff across the aura (lower = softer bleed)
$TIP_SHIFT = 0.5         # fraction of the sweep after which the line turns from white to pale blue
$FRINGE_EXP = 0.6        # how early the aura turns from pale blue to deep blue (1.0 = late)

# ice palette: white core -> pale ice blue -> deeper blue at the fringe (the mod's ice tint family,
# #8FDCFF for the frozen overlay / #7EFAFF for the electric spark are the neighbours)
$WHITE = @(0xF2, 0xFE, 0xFF)
$PALE = @(0xA8, 0xE4, 0xFF)
$DEEP = @(0x4E, 0x9F, 0xE8)

function Mix([int[]]$a, [int[]]$b, [double]$t) {
    $t = [Math]::Max(0.0, [Math]::Min(1.0, $t))
    return @(
        [int][Math]::Round($a[0] + ($b[0] - $a[0]) * $t),
        [int][Math]::Round($a[1] + ($b[1] - $a[1]) * $t),
        [int][Math]::Round($a[2] + ($b[2] - $a[2]) * $t)
    )
}

New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$bmp = New-Object System.Drawing.Bitmap($SIZE, $SIZE, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$cx = ($SIZE - 1) / 2.0
$cy = $ARC_RADIUS + $ARC_TOP_MARGIN
$halfAngle = $ARC_HALF_ANGLE * [Math]::PI / 180.0

for ($x = 0; $x -lt $SIZE; $x++) {
    for ($y = 0; $y -lt $SIZE; $y++) {
        # polar coordinates around the arc's centre of curvature: t = 0 is the top of the arc
        $dx = $x + 0.5 - $cx
        $dy = $cy - ($y + 0.5)
        $t = [Math]::Atan2($dx, $dy)

        if ([Math]::Abs($t) -gt $halfAngle) { continue }

        $r = [Math]::Sqrt($dx * $dx + $dy * $dy)
        $away = [Math]::Abs($r - $ARC_RADIUS)

        # the line thins out towards the tips
        $sweep = [Math]::Abs($t) / $halfAngle
        $taper = 1.0 - $CORE_TAPER * [Math]::Pow($sweep, $CORE_EXP)
        $core = $CORE_HALF * $taper
        $plateau = $core * $CORE_PLATEAU
        $aura = $core * $AURA_SCALE + $AURA_MIN
        $band = $aura - $plateau

        if ($away -ge $aura) { continue }

        $k = [Math]::Max(0.0, ($away - $plateau) / $band)
        $alpha = [int][Math]::Round(255 * $CORE_ALPHA * [Math]::Pow(1.0 - $k, $AURA_FALLOFF))
        if ($alpha -le 0) { continue }

        # the core turns from white to pale blue in the outer part of the sweep
        $tipShift = ($sweep - $TIP_SHIFT) / (1.0 - $TIP_SHIFT)
        $coreColour = Mix $WHITE $PALE $tipShift
        $colour = Mix $coreColour $DEEP ([Math]::Pow($k, $FRINGE_EXP))
        $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($alpha, $colour[0], $colour[1], $colour[2]))
    }
}

$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)

# ---- preview: the texture enlarged over a mid grey so the aura is visible ----
New-Item -ItemType Directory -Force -Path $previewDir | Out-Null
$scale = 6
$big = $SIZE * $scale   # PowerShell parses "a * b, c * d" as "a * (b, c) * d", so pre-compute
$pv = New-Object System.Drawing.Bitmap($big, $big, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($pv)
$g.Clear([System.Drawing.Color]::FromArgb(255, 90, 90, 95))
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.DrawImage($bmp, 0, 0, $big, $big)
$g.Dispose()
$pv.Save($preview, [System.Drawing.Imaging.ImageFormat]::Png)
$pv.Dispose()

# ---- report ----
$solid = 0
$partial = 0
for ($y = 0; $y -lt $SIZE; $y++) {
    for ($x = 0; $x -lt $SIZE; $x++) {
        $a = $bmp.GetPixel($x, $y).A
        if ($a -eq 255) { $solid++ } elseif ($a -gt 0) { $partial++ }
    }
}
$bmp.Dispose()
Write-Output ("wrote  " + $out)
Write-Output ("solid  " + $solid + " px,  soft " + $partial + " px")
Write-Output ("preview " + $preview)
