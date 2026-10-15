# Generate the Root Retreat block texture (16x16).
#
# The Root Retreat (根之退) is a stele-shaped utility block that opens the difficulty screen. It has
# no reference art from the author yet, so this script draws one in the palette of the mod's own
# slates (石板): a lavender-grey stone body (#98848C, the slate icons' colour) with a carved sprout
# in the slate greens (#324339 groove / #85C186 highlight). It is a placeholder the author can
# replace at any time - the file path stays the same.
#
# The stone is a deterministic noise field (no Random, so re-running the script gives the same
# texture) and the carving is a hand-written character map, so both are easy to nudge:
#   'g' = dark groove, 'G' = bright highlight, '.' = plain stone
#
# Output : src/main/resources/assets/sephiria_fantasy/textures/block/root_retreat.png (16x16)
# Preview: .preview/root_retreat_texture_preview.png (8x, over a mid grey)
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\textures\block'
$out = Join-Path $outDir 'root_retreat.png'
$previewDir = Join-Path $root '.preview'

$SIZE = 16

# ---- palette (the slate family) ----
$STONE = @(0x98, 0x84, 0x8C)   # the slate icons' body colour
$STONE_DARK = @(0x7D, 0x69, 0x76)
$STONE_LIGHT = @(0xAE, 0x9A, 0xA4)
$EDGE = @(0x62, 0x4E, 0x60)    # the carved border groove
$GROOVE = @(0x32, 0x43, 0x39)  # the carving's dark cut
$HIGHLIGHT = @(0x85, 0xC1, 0x86) # the carving's lit edge
$ACCENT = @(0x54, 0x7F, 0x97)  # a cold highlight in the stone

# ---- the carving: a sprout with two leaves over a root (10x10, centred) ----
$CARVING = @(
    '....gg....',
    '...gGGg...',
    '..gGGGGg..',
    '..gGGGGg..',
    '...gGGg...',
    '....gg....',
    '....gg....',
    '...g..g...',
    '..g....g..',
    '..g....g..'
)

function Mix([int[]]$a, [int[]]$b, [double]$t) {
    $t = [Math]::Max(0.0, [Math]::Min(1.0, $t))
    return @(
        [int][Math]::Round($a[0] + ($b[0] - $a[0]) * $t),
        [int][Math]::Round($a[1] + ($b[1] - $a[1]) * $t),
        [int][Math]::Round($a[2] + ($b[2] - $a[2]) * $t)
    )
}

# deterministic value noise: a cheap hash of (x, y) -> 0..1 (no Random, so runs are reproducible)
function Noise([int]$x, [int]$y) {
    $h = ($x * 73856093) -bxor ($y * 19349663)
    $h = $h -bxor ($h -shr 13)
    $h = ($h * 1274126177) -band 0x7FFFFFFF
    return (($h % 1000) / 1000.0)
}

New-Item -ItemType Directory -Force -Path $outDir, $previewDir | Out-Null

$bmp = New-Object System.Drawing.Bitmap($SIZE, $SIZE, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

for ($y = 0; $y -lt $SIZE; $y++) {
    for ($x = 0; $x -lt $SIZE; $x++) {
        # stone: base colour + a little two-tone noise (blocks of 2px so it reads as pixel art)
        $n = Noise ([int][Math]::Floor($x / 2)) ([int][Math]::Floor($y / 2))
        $colour = $STONE

        if ($n -lt 0.33) { $colour = Mix $STONE $STONE_DARK 0.55 }
        elseif ($n -gt 0.78) { $colour = Mix $STONE $STONE_LIGHT 0.45 }
        elseif ($n -gt 0.9) { $colour = Mix $colour $ACCENT 0.35 }

        # carved border groove: one pixel in, so the stele reads as a frame
        if ($x -eq 1 -or $y -eq 1 -or $x -eq ($SIZE - 2) -or $y -eq ($SIZE - 2)) {
            $colour = Mix $colour $EDGE 0.85
        }

        $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $colour[0], $colour[1], $colour[2]))
    }
}

# ---- stamp the carving (3px in from the border, 10x10) ----
$origin = 3

for ($cy = 0; $cy -lt $CARVING.Count; $cy++) {
    for ($cx = 0; $cx -lt $CARVING[$cy].Length; $cx++) {
        $mark = $CARVING[$cy][$cx]
        if ($mark -eq '.') { continue }

        $x = $origin + $cx
        $y = $origin + $cy
        # NOTE: -ceq (case sensitive) - PowerShell's -eq on strings is case INSENSITIVE, so
        # '-eq g' would be true for 'G' too and the highlight would never be drawn
        $colour = if ($mark -ceq 'g') { $GROOVE } else { $HIGHLIGHT }
        $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $colour[0], $colour[1], $colour[2]))
    }
}

$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)

# ---- preview: 8x over a mid grey ----
$scale = 8
$big = $SIZE * $scale
$pv = New-Object System.Drawing.Bitmap($big, $big, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($pv)
$g.Clear([System.Drawing.Color]::FromArgb(255, 108, 108, 112))
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.DrawImage($bmp, 0, 0, $big, $big)
$g.Dispose()
$preview = Join-Path $previewDir 'root_retreat_texture_preview.png'
$pv.Save($preview, [System.Drawing.Imaging.ImageFormat]::Png)
$pv.Dispose()
$bmp.Dispose()

Write-Output ('wrote  ' + $out)
Write-Output ('preview ' + $preview)
