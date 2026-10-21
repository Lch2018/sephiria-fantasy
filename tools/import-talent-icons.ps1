# Build the talent tab icon and the twelve talent stage icons from the reference art.
#
#   textures/gui/talent_tab.png            20x20  tab icon (transparent background, black outline kept)
#   textures/gui/talent/<id>_stageN.png     22x22  unlit stage icon
#   textures/gui/talent/<id>_stageN_lit.png 22x22  lit stage icon (talented colour)
#
# The references in tools/artifact-ref/talent are the user's 60x60 / 75x75 mock-ups:
#   * talent_tab.png is a 60x60 tile = 20x20 art at 3x. Its background is the two dark plate
#     colours (#291F25 / #291E24) plus the black around the outside. We drop the plate and the
#     black that is only background, but keep a 1px black outline around the artwork itself
#     (the user asked to "keep the #000000 outline, make the background transparent").
#   * the stage icons are 75x75 = 22x22 art at 3x. The artwork is located by its non-background
#     bounding box (66x66), not by the first 3x-aligned window: background rows align too, and
#     starting there cut off the frame (the 2026-10-21 "incomplete border" bug).
#     #161319 is the transparent background; each page has its own plate / glyph / detail greys
#     (see $PALETTES below). The lit texture remaps the WHOLE palette - plate, frame and every
#     detail grey - to shades of the talent colour (user's 2026-10-21 reference pair).
#
# NOTE: keep this file ASCII - Windows PowerShell 5.1 reads .ps1 as GBK otherwise.

param([string[]]$Only)

if ($Only) {
    $Only = @($Only | ForEach-Object { $_ -split ',' } | Where-Object { $_ })
}

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$refDir = Join-Path $root 'tools\artifact-ref\talent'
$guiDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\textures\gui'
$talentDir = Join-Path $guiDir 'talent'
$previewDir = Join-Path $root '.preview'

foreach ($dir in @($guiDir, $talentDir, $previewDir)) {
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
}

$RAGE_COLOUR = 'BE2C33'
$SWIFT_COLOUR = 'ACD9C3'
$WILL_COLOUR = 'ADCF88'
$QUICK_WIT_COLOUR = 'F59F42'
$SURVIVAL_COLOUR = 'D49ED8'
$PATIENCE_COLOUR = 'D4D7DD'
$WISDOM_COLOUR = 'A6DFDE'

# Per-page palettes.
#   Glyph        frame + symbol grey: becomes the talent colour in the lit texture
#   Details      shading grey keys (also remapped to talent shades in the lit texture)
#   Plate        inner dark plate keys (remapped to the dark talent shade in the lit texture)
#   UnlitGlyph   grey used for the unlit texture (swift is normalised to the rage grey)
#   UnlitDetails mapping applied to detail keys in the unlit texture
$PALETTES = @{
    'rage' = @{ Glyph = '353535'; Details = @('262626'); Plate = @('131313');
        UnlitGlyph = '353535'; UnlitDetails = @{} }
    'swift' = @{ Glyph = '797979'; Details = @('4C4C4C'); Plate = @('1B1B1B');
        UnlitGlyph = '353535'; UnlitDetails = @{ '4C4C4C' = '262626' } }
    'will' = @{ Glyph = '717171'; Details = @('3C3C3C', '575757'); Plate = @('151515');
        UnlitGlyph = '717171'; UnlitDetails = @{} }
    'quick_wit' = @{ Glyph = '696969'; Details = @('575757', '494949', '3A3A3A', '2B2B2B');
        Plate = @('191919'); UnlitGlyph = '696969'; UnlitDetails = @{} }
    'survival' = @{ Glyph = '6C6C6C'; Details = @('555555', '3E3E3E'); Plate = @('141414');
        UnlitGlyph = '6C6C6C'; UnlitDetails = @{} }
    'patience' = @{ Glyph = '686868'; Details = @('4F4F4F', '353535'); Plate = @('1F1F1F');
        UnlitGlyph = '686868'; UnlitDetails = @{} }
    'wisdom' = @{ Glyph = '7B7B7B'; Details = @('616161', '454545'); Plate = @('252525');
        UnlitGlyph = '7B7B7B'; UnlitDetails = @{} }
}

function Get-Key([System.Drawing.Color]$colour) {
    # RRGGBB only: every reference is opaque, and the colour tables below are written that way
    return ('{0:X2}{1:X2}{2:X2}' -f $colour.R, $colour.G, $colour.B)
}

function New-Colour([string]$hex) {
    return [System.Drawing.Color]::FromArgb(255,
            [Convert]::ToInt32($hex.Substring(0, 2), 16),
            [Convert]::ToInt32($hex.Substring(2, 2), 16),
            [Convert]::ToInt32($hex.Substring(4, 2), 16))
}

# Map one greyscale source key to the talent-colour ramp used by the lit icon.
#
# The 2026-10-21 reference pair (quick_wit) shows the rule: the plate, frame and every detail
# grey are remapped, not just the glyph. We use a hue-preserving brightness ramp fitted to the
# reference: the target luminance is ~0.23 of the talent colour at the plate, ~0.665 at
# t = 0.6 and 1.0 at the glyph, so every shade stays the talent hue instead of drifting.
# t = (grey - plate) / (glyph - plate), clamped to 0..1.
# Every palette colour is greyscale, so the R channel is the grey value.
function Get-LitColour([string]$key, [string]$plateHex, [string]$glyphHex, [string]$talentHex) {
    $grey = [Convert]::ToInt32($key.Substring(0, 2), 16)
    $plate = [Convert]::ToInt32($plateHex.Substring(0, 2), 16)
    $glyph = [Convert]::ToInt32($glyphHex.Substring(0, 2), 16)
    $t = if ($glyph -eq $plate) { 1.0 } else {
        [Math]::Max(0.0, [Math]::Min(1.0, ($grey - $plate) / [double]($glyph - $plate)))
    }

    if ($t -le 0.6) {
        $factor = 0.23 + (0.665 - 0.23) * ($t / 0.6)
    } else {
        $factor = 0.665 + (1.0 - 0.665) * (($t - 0.6) / 0.4)
    }

    $tr = [Convert]::ToInt32($talentHex.Substring(0, 2), 16)
    $tg = [Convert]::ToInt32($talentHex.Substring(2, 2), 16)
    $tb = [Convert]::ToInt32($talentHex.Substring(4, 2), 16)
    $r = [int][Math]::Round($tr * $factor)
    $g = [int][Math]::Round($tg * $factor)
    $b = [int][Math]::Round($tb * $factor)

    return ('{0:X2}{1:X2}{2:X2}' -f $r, $g, $b)
}

function Save-Preview([System.Drawing.Bitmap]$bmp, [string]$name) {
    $scale = 10
    $big = New-Object System.Drawing.Bitmap ($bmp.Width * $scale), ($bmp.Height * $scale)
    $g = [System.Drawing.Graphics]::FromImage($big)
    # mid grey so the transparent parts and the dark plate are easy to judge
    $g.Clear([System.Drawing.Color]::FromArgb(255, 108, 108, 108))
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $g.DrawImage($bmp, 0, 0, $big.Width, $big.Height)
    $g.Dispose()
    $big.Save((Join-Path $previewDir ($name + '_10x.png')), [System.Drawing.Imaging.ImageFormat]::Png)
    $big.Dispose()
}

function Test-Selected([string]$name) {
    if (-not $Only) {
        return $true
    }

    return $Only -contains $name
}

# ---------------------------------------------------------------------------
# Tab icon
# ---------------------------------------------------------------------------

function Build-TabIcon {
    $src = Join-Path $refDir 'talent_tab.png'
    $bmp = [System.Drawing.Bitmap]::FromFile($src)
    $size = 20

    if ($bmp.Width -ne 60 -or $bmp.Height -ne 60) {
        throw "talent_tab.png must be 60x60 (got $($bmp.Width)x$($bmp.Height))"
    }

    # majority colour of every 3x3 cell -> 20x20
    $grid = New-Object 'string[,]' $size, $size

    for ($y = 0; $y -lt $size; $y++) {
        for ($x = 0; $x -lt $size; $x++) {
            $counts = @{}

            for ($dy = 0; $dy -lt 3; $dy++) {
                for ($dx = 0; $dx -lt 3; $dx++) {
                    $key = Get-Key $bmp.GetPixel($x * 3 + $dx, $y * 3 + $dy)

                    if ($counts.ContainsKey($key)) {
                        $counts[$key] = $counts[$key] + 1
                    } else {
                        $counts[$key] = 1
                    }
                }
            }

            $grid[$x, $y] = ($counts.GetEnumerator() | Sort-Object Value -Descending |
                    Select-Object -First 1).Key
        }
    }

    $bmp.Dispose()

    $plate = @('291F25', '291E24')
    $art = @('B27156', 'DE9853', '2C2A44', '575967')
    $black = '000000'
    $last = $size - 1

    # flood fill from the border through plate / black: that is the "outside" background
    $outside = New-Object 'bool[,]' $size, $size
    $stack = New-Object System.Collections.Stack

    # seed the four borders (explicitly, so PowerShell never has to unroll nested arrays)
    for ($i = 0; $i -lt $size; $i++) {
        $seedKeys = @(
                @($i, 0), @($i, $last), @(0, $i), @($last, $i)
        )

        foreach ($seed in $seedKeys) {
            $sx = [int]$seed[0]
            $sy = [int]$seed[1]

            if ($outside[$sx, $sy]) {
                continue
            }

            $key = $grid[$sx, $sy]

            if ($key -eq $black -or $plate -contains $key) {
                $outside[$sx, $sy] = $true
                $stack.Push([System.Drawing.Point]::new($sx, $sy))
            }
        }
    }

    while ($stack.Count -gt 0) {
        $point = [System.Drawing.Point]$stack.Pop()
        $x = $point.X
        $y = $point.Y

        if ($x + 1 -lt $size -and -not $outside[($x + 1), $y]) {
            $key = $grid[($x + 1), $y]
            if ($key -eq $black -or $plate -contains $key) {
                $outside[($x + 1), $y] = $true
                $stack.Push([System.Drawing.Point]::new(($x + 1), $y))
            }
        }

        if ($x -gt 0 -and -not $outside[($x - 1), $y]) {
            $key = $grid[($x - 1), $y]
            if ($key -eq $black -or $plate -contains $key) {
                $outside[($x - 1), $y] = $true
                $stack.Push([System.Drawing.Point]::new(($x - 1), $y))
            }
        }

        if ($y + 1 -lt $size -and -not $outside[$x, ($y + 1)]) {
            $key = $grid[$x, ($y + 1)]
            if ($key -eq $black -or $plate -contains $key) {
                $outside[$x, ($y + 1)] = $true
                $stack.Push([System.Drawing.Point]::new($x, ($y + 1)))
            }
        }

        if ($y -gt 0 -and -not $outside[$x, ($y - 1)]) {
            $key = $grid[$x, ($y - 1)]
            if ($key -eq $black -or $plate -contains $key) {
                $outside[$x, ($y - 1)] = $true
                $stack.Push([System.Drawing.Point]::new($x, ($y - 1)))
            }
        }
    }

    $out = New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

    for ($y = 0; $y -lt $size; $y++) {
        for ($x = 0; $x -lt $size; $x++) {
            $key = $grid[$x, $y]
            $transparent = $false

            if ($plate -contains $key) {
                # the plate is the icon's background, not part of the chest
                $transparent = $true
            } elseif ($key -eq $black -and $outside[$x, $y]) {
                # keep only the 1px black outline that hugs the artwork
                $near = $false

                for ($dy = -1; $dy -le 1; $dy++) {
                    for ($dx = -1; $dx -le 1; $dx++) {
                        $nx = $x + $dx
                        $ny = $y + $dy

                        if ($nx -ge 0 -and $nx -lt $size -and $ny -ge 0 -and $ny -lt $size) {
                            if ($art -contains $grid[$nx, $ny]) {
                                $near = $true
                            }
                        }
                    }
                }

                if (-not $near) {
                    $transparent = $true
                }
            }

            if ($transparent) {
                $out.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
            } else {
                $out.SetPixel($x, $y, (New-Colour $key))
            }
        }
    }

    $out.Save((Join-Path $guiDir 'talent_tab.png'), [System.Drawing.Imaging.ImageFormat]::Png)
    Save-Preview $out 'talent_tab'
    $out.Dispose()
    Write-Host 'talent_tab.png (20x20)'
}

# ---------------------------------------------------------------------------
# Stage icons
# ---------------------------------------------------------------------------

# Find the tight 66x66 (22x22 cells at 3x) artwork inside the 75x75 canvas.
#
# IMPORTANT: do not search for the first 3x-aligned window. Background rows before the artwork
# also align, and starting there cuts off the bottom/right of the frame (the 2026-10-21 bug:
# the 21x21 cut lost part of the border). The non-background bounding box is the real grid.
function Find-Content([System.Drawing.Bitmap]$bmp) {
    $minX = $bmp.Width
    $minY = $bmp.Height
    $maxX = -1
    $maxY = -1

    for ($y = 0; $y -lt $bmp.Height; $y++) {
        for ($x = 0; $x -lt $bmp.Width; $x++) {
            if ((Get-Key $bmp.GetPixel($x, $y)) -ne '161319') {
                if ($x -lt $minX) { $minX = $x }
                if ($x -gt $maxX) { $maxX = $x }
                if ($y -lt $minY) { $minY = $y }
                if ($y -gt $maxY) { $maxY = $y }
            }
        }
    }

    $width = $maxX - $minX + 1
    $height = $maxY - $minY + 1

    if ($width -ne 66 -or $height -ne 66) {
        throw "artwork must be 66x66 at 3x (got ${width}x${height})"
    }

    # the artwork must be an exact 3x upscale, otherwise the 22x22 sample would be wrong
    for ($y = 0; $y -lt 66; $y++) {
        for ($x = 0; $x -lt 66; $x++) {
            $colour = $bmp.GetPixel($minX + $x, $minY + $y)
            $anchor = $bmp.GetPixel($minX + ([int][Math]::Floor($x / 3)) * 3,
                    $minY + ([int][Math]::Floor($y / 3)) * 3)

            if ($colour.ToArgb() -ne $anchor.ToArgb()) {
                throw 'artwork is not a clean 3x pixel grid'
            }
        }
    }

    return @($minX, $minY)
}

function Build-StageIcon([string]$id, [string]$file, [int]$stage, [string]$colour) {
    $src = Join-Path $refDir $file
    $bmp = [System.Drawing.Bitmap]::FromFile($src)

    if ($bmp.Width -ne 75 -or $bmp.Height -ne 75) {
        $bmp.Dispose()
        throw "$file must be 75x75"
    }

    $box = Find-Content $bmp
    $ox = [int]$box[0]
    $oy = [int]$box[1]
    $palette = $PALETTES[$id]

    if ($palette -eq $null) {
        $bmp.Dispose()
        throw "no palette for $id"
    }

    $allowed = @('161319') + $palette.Plate + @($palette.Glyph) + $palette.Details

    # 66x66 at 3x = the full 22x22 artwork, including the outer border. Keep all 22 cells:
    # cutting the first/last cell made the frame look incomplete.
    foreach ($lit in @($false, $true)) {
        $out = New-Object System.Drawing.Bitmap 22, 22, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

        for ($y = 0; $y -lt 22; $y++) {
            for ($x = 0; $x -lt 22; $x++) {
                $key = Get-Key $bmp.GetPixel($ox + $x * 3, $oy + $y * 3)

                if ($allowed -notcontains $key) {
                    $bmp.Dispose()
                    $out.Dispose()
                    throw "$file has an unexpected colour #$key"
                }

                if ($key -eq '161319') {
                    $out.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
                    continue
                }

                if ($lit) {
                    # full remap: plate / frame / every detail grey becomes a talent-colour shade
                    $key = Get-LitColour $key $palette.Plate[0] $palette.Glyph $colour
                } else {
                    if ($key -eq $palette.Glyph) {
                        $key = $palette.UnlitGlyph
                    }

                    if ($palette.UnlitDetails.ContainsKey($key)) {
                        $key = $palette.UnlitDetails[$key]
                    }
                }

                $out.SetPixel($x, $y, (New-Colour $key))
            }
        }

        $suffix = if ($lit) { '_lit' } else { '' }
        $name = "$id`_stage$stage$suffix"
        $out.Save((Join-Path $talentDir ($name + '.png')), [System.Drawing.Imaging.ImageFormat]::Png)
        Save-Preview $out $name
        $out.Dispose()
    }

    $bmp.Dispose()
    Write-Host ("{0} stage {1} (grid {2},{3}; full talent-colour remap)" -f
            $id, $stage, $ox, $oy)
}

if (Test-Selected 'talent_tab') {
    Build-TabIcon
}

$jobs = @(
        @('rage', 'rage_stage1.png', 1, $RAGE_COLOUR),
        @('rage', 'rage_stage2.png', 2, $RAGE_COLOUR),
        @('rage', 'rage_stage3.png', 3, $RAGE_COLOUR),
        @('swift', 'swift_stage1.png', 1, $SWIFT_COLOUR),
        @('swift', 'swift_stage2.png', 2, $SWIFT_COLOUR),
        @('swift', 'swift_stage3.png', 3, $SWIFT_COLOUR),
        @('will', 'will_stage1.png', 1, $WILL_COLOUR),
        @('will', 'will_stage2.png', 2, $WILL_COLOUR),
        @('will', 'will_stage3.png', 3, $WILL_COLOUR),
        @('quick_wit', 'quick_wit_stage1.png', 1, $QUICK_WIT_COLOUR),
        @('quick_wit', 'quick_wit_stage2.png', 2, $QUICK_WIT_COLOUR),
        @('quick_wit', 'quick_wit_stage3.png', 3, $QUICK_WIT_COLOUR),
        @('survival', 'survival_stage1.png', 1, $SURVIVAL_COLOUR),
        @('survival', 'survival_stage2.png', 2, $SURVIVAL_COLOUR),
        @('survival', 'survival_stage3.png', 3, $SURVIVAL_COLOUR),
        @('patience', 'patience_stage1.png', 1, $PATIENCE_COLOUR),
        @('patience', 'patience_stage2.png', 2, $PATIENCE_COLOUR),
        @('patience', 'patience_stage3.png', 3, $PATIENCE_COLOUR),
        @('wisdom', 'wisdom_stage1.png', 1, $WISDOM_COLOUR),
        @('wisdom', 'wisdom_stage2.png', 2, $WISDOM_COLOUR),
        @('wisdom', 'wisdom_stage3.png', 3, $WISDOM_COLOUR)
)

foreach ($job in $jobs) {
    $id = $job[0]

    if (-not (Test-Selected $id)) {
        continue
    }

    Build-StageIcon $id $job[1] ([int]$job[2]) $job[3]
}
