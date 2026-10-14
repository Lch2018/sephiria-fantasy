# Generate the Eternal Eclipse artifact's recoloured textures.
#
# The Eternal Eclipse (太阳剑 + 冰霜武具 bond artifact) reads which half of the Sephiria backpack it
# sits in: in the LEFT half the Frost Armament turns fire-based and the icon turns flame yellow, in
# the RIGHT half the Sun Sword turns ice-based and the icon turns ice blue (see EternalEclipseItem /
# items/eternal_eclipse.json, which picks between these three textures by the SIDE component).
#
# Three outputs, all recolours of art that is already in the repo - no new artwork:
#   eternal_eclipse_flame : every BLUE-dominant bright pixel of the cut icon goes onto the warm ramp
#                           (so the blue moon turns yellow: the whole icon reads as "blazing")
#   eternal_eclipse_frost : every WARM bright pixel goes onto the ice ramp
#                           (so the yellow sun turns blue: the whole icon reads as "frozen")
#   sun_sword_ice         : the dropped Sun Sword's sprite, whole, onto the ice ramp - the sword that
#                           falls out of an ice-based Sun Sword is the ice-blue one
#
# The icon's dark medallion (the carving's stone body, and the outline around it) is deliberately left
# alone: it is what keeps the icon reading as a carved disc instead of a flat blob of colour. Only the
# sun / moon pixels move, and those are exactly the pixels the user sees change.
#
# Output : src/main/resources/assets/sephiria_fantasy/textures/item/{eternal_eclipse_flame,
#          eternal_eclipse_frost,sun_sword_ice}.png
# Preview: .preview/eclipse_variants_preview.png (all three + their sources, enlarged)
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$texDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\textures\item'
$previewDir = Join-Path $root '.preview'

# ---- the ramps (dark -> light), shared with the rest of the mod's art ----
# warm: the Sun Sword's palette (dark ember -> orange -> gold -> white)
# ice:  the frost-qi / ice-tool palette (deep steel blue -> blue -> pale ice -> white)
$WARM = @(@(0x8A, 0x2B, 0x12), @(0xDF, 0x71, 0x26), @(0xFB, 0xF2, 0x36), @(0xFF, 0xFD, 0xF0))
$ICE = @(@(0x2F, 0x4A, 0x7E), @(0x4E, 0x9F, 0xE8), @(0xA8, 0xE4, 0xFF), @(0xF2, 0xFE, 0xFF))

# pixels darker than this keep their colour (the medallion / the sword's outline). The medallion is
# #222034 -> luma ~35, the darkest sun pixel (#B73227) is ~89, so 70 sits comfortably between.
$KEEP_DARK_LUMA = 70.0
# the luminance window the ramps are stretched over
$LUM_MIN = 60.0
$LUM_MAX = 235.0

function Mix([int[]]$a, [int[]]$b, [double]$t) {
    $t = [Math]::Max(0.0, [Math]::Min(1.0, $t))
    return @(
        [int][Math]::Round($a[0] + ($b[0] - $a[0]) * $t),
        [int][Math]::Round($a[1] + ($b[1] - $a[1]) * $t),
        [int][Math]::Round($a[2] + ($b[2] - $a[2]) * $t)
    )
}

function Ramp-Colour($ramp, [double]$lum) {
    $t = ($lum - $LUM_MIN) / ($LUM_MAX - $LUM_MIN)
    $t = [Math]::Max(0.0, [Math]::Min(1.0, $t))
    $scaled = $t * ($ramp.Count - 1)
    $index = [int][Math]::Floor($scaled)
    if ($index -ge ($ramp.Count - 1)) { $index = $ramp.Count - 2 }
    return Mix $ramp[$index] $ramp[($index + 1)] ($scaled - $index)
}

# one recolour pass: $flip = 'blue-to-warm' (flame) or 'warm-to-ice' (frost)
# $all = $true recolours every bright pixel (the sun sword); $false only the flipped half (the icon)
function Recolour([string]$inFile, [string]$outFile, [string]$flip, [bool]$all) {
    $src = [System.Drawing.Bitmap]::FromFile($inFile)
    $out = New-Object System.Drawing.Bitmap($src.Width, $src.Height, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb))
    $changed = 0

    for ($y = 0; $y -lt $src.Height; $y++) {
        for ($x = 0; $x -lt $src.Width; $x++) {
            $c = $src.GetPixel($x, $y)

            if ($c.A -le 0) { continue }

            $lum = 0.299 * $c.R + 0.587 * $c.G + 0.114 * $c.B
            $isBlue = $c.B -gt $c.R
            $flipThis = $false

            if ($all) {
                $flipThis = $true
            } elseif ($flip -eq 'blue-to-warm') {
                $flipThis = $isBlue
            } elseif ($flip -eq 'warm-to-ice') {
                $flipThis = -not $isBlue
            }

            if (-not $flipThis -or $lum -lt $KEEP_DARK_LUMA) {
                $out.SetPixel($x, $y, $c)
                continue
            }

            $ramp = if ($flip -eq 'blue-to-warm') { $WARM } else { $ICE }
            $rgb = Ramp-Colour $ramp $lum
            $out.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($c.A, $rgb[0], $rgb[1], $rgb[2]))
            $changed++
        }
    }

    $out.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
    # Write-Host, NOT Write-Output: a function's output stream is its return value, so a diagnostic
    # line written to it would be collected alongside the Bitmap by the caller (a known trap here)
    Write-Host ('{0,-24} {1}x{2}  {3} px recoloured -> {4}' -f (Split-Path -Leaf $outFile), $src.Width, $src.Height, $changed, $outFile)
    return $out
}

# the model definition of an extra sprite (only when absent - the file is hand-checked in)
# NOTE: these are models only, NOT items/<id>.json definitions: nothing is named after them, the
# eclipse's select / the carriers' condition reference them by name
function Write-ModelDefinition([string]$name) {
    $modelDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\models\item'
    New-Item -ItemType Directory -Force -Path $modelDir | Out-Null
    $utf8 = New-Object System.Text.UTF8Encoding($false)

    $modelFile = Join-Path $modelDir ($name + '.json')
    if (-not (Test-Path $modelFile)) {
        # the concatenation MUST stay in parentheses (an unparenthesised 'a' + $name inside an array
        # literal is parsed as several elements and the value lands across lines - invalid JSON)
        $lines = @('{', '  "parent": "minecraft:item/generated",', '  "textures": {',
                   ('    "layer0": "sephiria_fantasy:item/' + $name + '"'), '  }', '}')
        [IO.File]::WriteAllText($modelFile, (($lines -join "`n") + "`n"), $utf8)
        Write-Host ('wrote models/item/' + $name + '.json')
    }
}

New-Item -ItemType Directory -Force -Path $previewDir | Out-Null

$icon = Join-Path $texDir 'eternal_eclipse.png'
$sword = Join-Path $texDir 'sun_sword.png'
$previews = @()

$previews += Recolour $icon (Join-Path $texDir 'eternal_eclipse_flame.png') 'blue-to-warm' $false
$previews += Recolour $icon (Join-Path $texDir 'eternal_eclipse_frost.png') 'warm-to-ice' $false
$previews += Recolour $sword (Join-Path $texDir 'sun_sword_ice.png') 'warm-to-ice' $true

Write-ModelDefinition 'eternal_eclipse_flame'
Write-ModelDefinition 'eternal_eclipse_frost'
Write-ModelDefinition 'sun_sword_ice'

# ---- preview sheet: sources first, then the three results, 8x, over a mid grey ----
$sources = @()
foreach ($p in @($icon, $sword)) {
    $b = New-Object System.Drawing.Bitmap($p)
    $sources += $b
}

$scale = 8
$cell = 16 * $scale
$sheet = New-Object System.Drawing.Bitmap(($cell * 5), $cell, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb))
$g = [System.Drawing.Graphics]::FromImage($sheet)
$g.Clear([System.Drawing.Color]::FromArgb(255, 108, 108, 112))
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half

$order = @($sources[0], $previews[0], $previews[1], $sources[1], $previews[2])

for ($i = 0; $i -lt $order.Count; $i++) {
    $g.DrawImage($order[$i], ($i * $cell), 0, $cell, $cell)
}

$g.Dispose()
$sheetPath = Join-Path $previewDir 'eclipse_variants_preview.png'
$sheet.Save($sheetPath, [System.Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()

foreach ($b in $sources) { $b.Dispose() }
foreach ($b in $previews) { $b.Dispose() }

Write-Output ('preview ' + $sheetPath)
