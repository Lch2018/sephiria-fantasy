# Generate the Sun Sword slash-mark particle texture.
#
# The vanilla sweep-attack mark (assets/minecraft/textures/particle/sweep_0..7.png) is an arc
# painted with hard white pixels; the Sun Sword mark is the same idea as a STRAIGHT cut.
# Unlike the vanilla one it is painted with a soft alpha falloff so it can be drawn on the
# translucent particle layer and faded out over its short life (client/particle/SunSlashParticle).
#
# The three colours are the ones the previous dust-particle line used, so the mark keeps the
# Sun Sword palette: hot white core, yellow flanks, orange fringe (and the ends turn yellow).
#
# Two palettes (the shape is identical, only the ramp changes):
#   sun (default) -> sun_slash_0.png   the Sun Sword mark as it always was
#   ice           -> ice_slash_0.png   the same mark in ice blue, used while the Eternal Eclipse
#                                      artifact sits in the RIGHT half of the backpack (the sun
#                                      sword turns ice-based; see EternalEclipseItem)
#
# Output : src/main/resources/assets/sephiria_fantasy/textures/particle/<name>_0.png (64x64)
# Preview: .preview/<name>_preview.png  (alpha over a mid grey, enlarged, for eyeballing)
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

param([string]$Palette = 'sun')

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\textures\particle'
$previewDir = Join-Path $root '.preview'

# ---- shape and colour knobs (lengths are texture pixels; the canvas is SIZE x SIZE) ----
# The mark is a THIN bright line inside a soft aura (2026-10-11: the user asked for 70% of the
# first version's thickness so the cut looks sharper, and for the mark to read as light rather
# than as a painted stroke). So the alpha profile is: a flat-topped core, then one continuous
# falloff out to the aura radius - the aura is faint (that is what glows) while the core is
# what makes the cut look sharp.
$SIZE = 64
$MARGIN_X = 1            # the cut spans MARGIN_X .. SIZE-1-MARGIN_X
$CORE_HALF = 1.82        # half thickness of the cut at the centre (70% of the first version's 2.6)
$CORE_PLATEAU = 0.7      # share of CORE_HALF that stays at full alpha before the falloff starts
$CORE_TAPER = 0.85       # share of the core lost by the tip (1.0 would taper to a point)
$CORE_EXP = 1.8          # taper curve: 2.0 = quadratic, <2 keeps the cut fat for longer
$CORE_ALPHA = 0.88       # peak alpha: a little see-through, so it glows instead of glaring
$AURA_SCALE = 2.5        # aura half thickness = core * AURA_SCALE + AURA_MIN (the faint glow)
$AURA_MIN = 0.35
$AURA_FALLOFF = 1.6      # exponent of the alpha falloff across the aura (lower = softer bleed)
$TIP_SHIFT = 0.45        # fraction of the half length after which the core turns to the pale stop
$FRINGE_EXP = 0.6        # how early the aura turns from the pale stop to the deep stop (1.0 = late)

# sun palette: hot white core -> yellow flanks -> orange fringe (the Sun Sword's own colours)
# ice palette: the frost-qi ramp (white -> pale ice blue -> deep blue), so the ice version of the
# mark matches the Frost Armament's arc and the frozen-overlay tint family
$PALETTES = @{
    'sun' = @{ name = 'sun_slash'; white = @(0xFD, 0xFD, 0xFB); pale = @(0xFD, 0xFD, 0x64); deep = @(0xFD, 0xA5, 0x42) }
    'ice' = @{ name = 'ice_slash'; white = @(0xF2, 0xFE, 0xFF); pale = @(0xA8, 0xE4, 0xFF); deep = @(0x4E, 0x9F, 0xE8) }
}

if (-not $PALETTES.ContainsKey($Palette)) {
    throw ("unknown palette '" + $Palette + "' (use sun or ice)")
}

$chosen = $PALETTES[$Palette]
$out = Join-Path $outDir ($chosen.name + '_0.png')
$preview = Join-Path $previewDir ($chosen.name + '_preview.png')
$WHITE = $chosen.white
$PALE = $chosen.pale
$DEEP = $chosen.deep

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
$halfLen = ($SIZE - 1 - 2 * $MARGIN_X) / 2.0

for ($x = 0; $x -lt $SIZE; $x++) {
    $t = [Math]::Abs($x - $cx) / $halfLen
    if ($t -gt 1.0) { continue }

    $taper = 1.0 - $CORE_TAPER * [Math]::Pow($t, $CORE_EXP)
    $core = $CORE_HALF * $taper
    $plateau = $core * $CORE_PLATEAU
    $aura = $core * $AURA_SCALE + $AURA_MIN
    $band = $aura - $plateau

    # the core turns from white to the pale stop in the outer part of the cut
    $tipShift = ($t - $TIP_SHIFT) / (1.0 - $TIP_SHIFT)
    $coreColour = Mix $WHITE $PALE $tipShift

    for ($y = 0; $y -lt $SIZE; $y++) {
        $dy = [Math]::Abs($y - $cx)
        if ($dy -ge $aura) { continue }

        # 0 while still inside the solid core, 1 at the outer end of the aura
        $k = [Math]::Max(0.0, ($dy - $plateau) / $band)
        $alpha = [int][Math]::Round(255 * $CORE_ALPHA * [Math]::Pow(1.0 - $k, $AURA_FALLOFF))
        if ($alpha -le 0) { continue }
        $colour = Mix $coreColour $DEEP ([Math]::Pow($k, $FRINGE_EXP))
        $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($alpha, $colour[0], $colour[1], $colour[2]))
    }
}

$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)

# ---- preview: the texture enlarged over a mid grey so the fringe is visible ----
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
