# Build the SEPHIRIA artifact / slate / combo icons from the reference images the user supplied.
#
#   textures/item/<name>.png   16x16   artifacts and slates
#   textures/gui/<name>.png    16x16   combo counter icons
#
# Every reference is a screenshot-style mock-up: the artwork (pattern + its black outline) sits on
# a flat plate (#32313E for artifacts / slates, #16151F for combos), sometimes inside a darker 2px
# frame (#222034). The icons must keep the outline and lose the plate.
#
# How the cut-out works (this is the whole point of the script):
#   * flood fill from the four image borders through pixels that match the plate colours. Only the
#     plate that is *connected to the border* disappears.
#   * plate-coloured pixels the artwork seals off stay opaque: the pinwheel's stick, the shadow's
#     dark core and similar "drawn with the background's own colour" details survive.
#   * the outline survives because the fill never walks over it (it is a different colour) - it is
#     the wall that stops the fill, so it is kept in full.
#   * the plate palette is the ring colours that make up <share>% of the border ring. The rare tail
#     is ignored: when a sprite touches an image edge (pinwheel stick) its own colour shows up in
#     the ring, and adopting that as "plate" would punch the sprite out.
# Then the sprite is scaled to the target with the majority-colour rule (crisp pixel-art blocks,
# binary alpha) and centred on a transparent canvas.
#
# Reference folders are keyed by `root`: the two zcode image-cache sessions and the wechat temp
# folders (recycled by the chat client, so those jobs are skipped once the folder, or the file
# inside it, is gone).
#
# NOTE: keep this file ASCII - Windows PowerShell 5.1 reads .ps1 as GBK otherwise.
#
#   -Only <name>[,<name>...]   run just those jobs (by name) instead of re-cutting every texture.
#                              A reference that has expired still skips with a warning; pass this
#                              when adding one icon so the committed textures are left untouched.

param([string[]]$Only)

# NOTE: powershell -File hands "-Only a,b,c" over as ONE string, the comma list is not split into
# an array - split it here so both "-Only a,b,c" and "-Only a b c" work.
if ($Only) {
    $Only = @($Only | ForEach-Object { $_ -split ',' } | Where-Object { $_ })
}

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$texDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\textures\item'
$guiDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\textures\gui'
$previewDir = Join-Path $root '.preview'
$cache = 'C:\Users\28237\.zcode\cli\image-cache\sess_30e8ce6e-bd9a-4f06-a1a0-7ff99b2cb644'
$cache2 = 'C:\Users\28237\.zcode\cli\image-cache\sess_ef7ea5c6-ffe1-4f26-8dae-12227ec34263'
# zcode sessions are also recycled eventually: cache3 holds the latest batch (sword_earring,
# keen_eye). Its jobs skip once the folder or the files are gone, like the wechat ones.
$cache3 = 'C:\Users\28237\.zcode\cli\image-cache\sess_223909ec-2207-4576-8f58-bc253e01b51d'
$wechat = 'C:\Users\28237\Documents\xwechat_files\wxid_v1oblkxkkuq222_3365\temp\RWTemp\2026-09\9e20f478899dc29eb19741386f9343c8'
# the chat client recycles the RWTemp slot, so the new batch (the four slates below) arrived in the
# same string that $wechat already names - but with the old textbook / potion files deleted. It
# keeps its own root so the two batches can expire independently.
$wechat2 = 'C:\Users\28237\Documents\xwechat_files\wxid_v1oblkxkkuq222_3365\temp\RWTemp\2026-09\9e20f478899dc29eb19741386f9343c8'
# the ember batch (combo icon) landed in a newer zcode session cache; same lifecycle as cache3.
$cache4 = 'C:\Users\28237\.zcode\cli\image-cache\sess_da3fb70d-3f37-4d0d-9565-6509fd39e4d0'

foreach ($dir in @($texDir, $guiDir, $previewDir)) {
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
}

function New-Canvas([int]$size) {
    return New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
}

function Save-Preview([System.Drawing.Bitmap]$bmp, [string]$name) {
    $scale = 8
    $big = New-Object System.Drawing.Bitmap ($bmp.Width * $scale), ($bmp.Height * $scale)
    $g = [System.Drawing.Graphics]::FromImage($big)
    # mid grey background so the icon is judged the way it shows up on a tab button
    $g.Clear([System.Drawing.Color]::FromArgb(255, 108, 108, 108))
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $g.DrawImage($bmp, 0, 0, $big.Width, $big.Height)
    $g.Dispose()
    $big.Save((Join-Path $previewDir ($name + '_8x.png')), [System.Drawing.Imaging.ImageFormat]::Png)
    $big.Dispose()
}

# An item texture alone does not make the item render: it also needs the item model definition
# (assets/sephiria_fantasy/items/<id>.json, the 1.21.4+ system) and the model itself
# (assets/sephiria_fantasy/models/item/<id>.json). Batch 15 shipped without them and the icons showed up
# as missing models in game, so every item-dir job now writes both - only when absent, so
# hand-made special models (weapons, animated sprites) are never overwritten.
function Write-ItemDefinitions([string]$name) {
    $defDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\items'
    $modelDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\models\item'
    New-Item -ItemType Directory -Force -Path $defDir, $modelDir | Out-Null

    $utf8 = New-Object System.Text.UTF8Encoding($false)
    $defFile = Join-Path $defDir ($name + '.json')

    if (-not (Test-Path $defFile)) {
        # NOTE: the concatenation MUST stay in parentheses. In a PowerShell array literal an
        # unparenthesised 'a' + $name + '"' is parsed as three separate elements, and joining them
        # with newlines writes the value split across lines - invalid JSON (batch 15's slate.json
        # came out exactly like that).
        $lines = @('{', '  "model": {', '    "type": "minecraft:model",',
                   ('    "model": "sephiria_fantasy:item/' + $name + '"'), '  }', '}')
        [IO.File]::WriteAllText($defFile, (($lines -join "`n") + "`n"), $utf8)
        Write-Output ("wrote items/" + $name + '.json')
    }

    $modelFile = Join-Path $modelDir ($name + '.json')

    if (-not (Test-Path $modelFile)) {
        $lines = @('{', '  "parent": "minecraft:item/generated",', '  "textures": {',
                   ('    "layer0": "sephiria_fantasy:item/' + $name + '"'), '  }', '}')
        [IO.File]::WriteAllText($modelFile, (($lines -join "`n") + "`n"), $utf8)
        Write-Output ("wrote models/item/" + $name + '.json')
    }
}

function Get-Pixels([System.Drawing.Bitmap]$img) {    # one GetPixel per pixel: the flood fill walks every pixel several times, and going through
    # System.Drawing each time is what makes a naive version of this script take minutes
    $w = $img.Width
    $h = $img.Height
    $px = New-Object 'int[]' ($w * $h)

    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            $c = $img.GetPixel($x, $y)
            # the [int] casts matter: Color.R is a byte, and a byte shifted by 8 or 16 silently
            # truncates to 0 instead of widening
            $px[$y * $w + $x] = ([int]$c.R -shl 16) -bor ([int]$c.G -shl 8) -bor [int]$c.B
        }
    }

    return ,$px
}

function Get-PlatePalette([int[]]$px, [int]$w, [int]$h, [int]$ring, [int]$share) {
    $counts = @{}
    $edgePixels = 0

    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            $edge = ($x -lt $ring) -or ($y -lt $ring) -or ($x -ge $w - $ring) -or ($y -ge $h - $ring)
            if (-not $edge) { continue }
            $edgePixels++
            $key = $px[$y * $w + $x]
            if ($counts.ContainsKey($key)) { $counts[$key]++ } else { $counts[$key] = 1 }
        }
    }

    $palette = @()
    $seen = 0

    foreach ($entry in ($counts.GetEnumerator() | Sort-Object Value -Descending)) {
        if ($palette.Count -gt 0 -and (100 * $seen / $edgePixels) -ge $share) { break }
        if ($palette.Count -ge 16) { break }

        # the comma is a tighter operator than -band, so every term needs its own parentheses
        $r = ($entry.Key -shr 16) -band 0xFF
        $g = ($entry.Key -shr 8) -band 0xFF
        $b = $entry.Key -band 0xFF
        $palette += ,@($r, $g, $b)
        $seen += $entry.Value
    }

    return ,$palette
}

function Get-PlateMask([int[]]$px, [int]$w, [int]$h, $palette, [int]$tol) {
    # 4-connected flood fill from the border through plate-coloured pixels
    $mask = New-Object 'bool[]' ($w * $h)
    $queue = New-Object System.Collections.Queue

    for ($x = 0; $x -lt $w; $x++) {
        foreach ($y in @(0, ($h - 1))) {
            if (-not $mask[$y * $w + $x]) { $mask[$y * $w + $x] = $true; $queue.Enqueue($y * $w + $x) }
        }
    }

    for ($y = 0; $y -lt $h; $y++) {
        foreach ($x in @(0, ($w - 1))) {
            if (-not $mask[$y * $w + $x]) { $mask[$y * $w + $x] = $true; $queue.Enqueue($y * $w + $x) }
        }
    }

    while ($queue.Count -gt 0) {
        $p = $queue.Dequeue()
        $x = $p % $w
        $y = [int][Math]::Floor($p / $w)

        foreach ($d in @(@(1, 0), @(-1, 0), @(0, 1), @(0, -1))) {
            $nx = $x + $d[0]
            $ny = $y + $d[1]

            if ($nx -lt 0 -or $ny -lt 0 -or $nx -ge $w -or $ny -ge $h) { continue }
            $np = $ny * $w + $nx
            if ($mask[$np]) { continue }

            $c = $px[$np]
            $r = ($c -shr 16) -band 0xFF
            $g = ($c -shr 8) -band 0xFF
            $b = $c -band 0xFF
            $hit = $false

            foreach ($p2 in $palette) {
                if (([Math]::Abs($r - $p2[0]) + [Math]::Abs($g - $p2[1]) + [Math]::Abs($b - $p2[2])) -le $tol) { $hit = $true; break }
            }

            if (-not $hit) { continue }

            $mask[$np] = $true
            $queue.Enqueue($np)
        }
    }

    return ,$mask
}

# drop tiny opaque blobs: noise in the plate, and the dashed decorative border of the slate
# reference, are a few pixels each and cannot be part of an icon
function Remove-Specks($mask, [int]$w, [int]$h, [int]$minBlob) {
    $seen = New-Object 'bool[]' ($w * $h)

    for ($i = 0; $i -lt ($w * $h); $i++) {
        if ($mask[$i] -or $seen[$i]) { continue }

        $stack = New-Object System.Collections.Stack
        $stack.Push($i)
        $seen[$i] = $true
        $blob = @()

        while ($stack.Count -gt 0) {
            $p = $stack.Pop()
            $blob += $p
            $x = $p % $w
            $y = [int][Math]::Floor($p / $w)

            foreach ($d in @(@(1, 0), @(-1, 0), @(0, 1), @(0, -1))) {
                $nx = $x + $d[0]
                $ny = $y + $d[1]

                if ($nx -lt 0 -or $ny -lt 0 -or $nx -ge $w -or $ny -ge $h) { continue }
                $np = $ny * $w + $nx
                if ($seen[$np] -or $mask[$np]) { continue }

                $seen[$np] = $true
                $stack.Push($np)
            }
        }

        if ($blob.Count -lt $minBlob) {
            foreach ($b in $blob) { $mask[$b] = $true }
        }
    }
}

function Import-Icon([string]$file, [string]$outFile, [string]$label, [int]$target, [int]$tol, [int]$ring, [int]$share, [int]$trim, [int]$minBlob, [int]$frames = 1) {
    $src = [System.Drawing.Bitmap]::FromFile($file)
    $w = $src.Width
    $h = $src.Height
    $px = Get-Pixels $src
    $src.Dispose()

    $palette = Get-PlatePalette $px $w $h $ring $share
    $mask = Get-PlateMask $px $w $h $palette $tol
    Remove-Specks $mask $w $h $minBlob

    $plate = 0
    $minX = $w; $maxX = -1; $minY = $h; $maxY = -1

    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            if ($mask[$y * $w + $x]) { $plate++; continue }
            if ($x -lt $minX) { $minX = $x }
            if ($x -gt $maxX) { $maxX = $x }
            if ($y -lt $minY) { $minY = $y }
            if ($y -gt $maxY) { $maxY = $y }
        }
    }

    if ($maxX -lt 0) { throw "$label : nothing left after the plate was cut out" }

    $minX += $trim; $minY += $trim; $maxX -= $trim; $maxY -= $trim
    $sw = $maxX - $minX + 1
    $sh = $maxY - $minY + 1

    # sprite already fits the slot 1:1 -> paste native, never resample (that is how a 16x16
    # reference keeps every pixel)
    $scale = 1.0
    if ($sw -gt $target -or $sh -gt $target) {
        $scale = [Math]::Min($target / $sw, $target / $sh)
    }

    $dw = [Math]::Max(1, [int][Math]::Round($sw * $scale))
    $dh = [Math]::Max(1, [int][Math]::Round($sh * $scale))
    $canvas = New-Canvas $target
    $offX = [int](($target - $dw) / 2)
    $offY = [int](($target - $dh) / 2)

    for ($dy = 0; $dy -lt $dh; $dy++) {
        for ($dx = 0; $dx -lt $dw; $dx++) {
            $sx0 = $minX + [int][Math]::Floor($dx * $sw / $dw)
            $sx1 = $minX + [int][Math]::Ceiling(($dx + 1) * $sw / $dw)
            $sy0 = $minY + [int][Math]::Floor($dy * $sh / $dh)
            $sy1 = $minY + [int][Math]::Ceiling(($dy + 1) * $sh / $dh)
            if ($sx1 -gt $maxX + 1) { $sx1 = $maxX + 1 }
            if ($sy1 -gt $maxY + 1) { $sy1 = $maxY + 1 }

            $counts = @{}
            $total = 0
            $hits = 0

            for ($sy = $sy0; $sy -lt $sy1; $sy++) {
                for ($sx = $sx0; $sx -lt $sx1; $sx++) {
                    $total++
                    if ($mask[$sy * $w + $sx]) { continue }
                    $hits++
                    $key = $px[$sy * $w + $sx]
                    if ($counts.ContainsKey($key)) { $counts[$key]++ } else { $counts[$key] = 1 }
                }
            }

            if ($total -eq 0 -or $hits -eq 0) { continue }
            # binary alpha: a soft fringe only reads as noise at this size
            if (($hits / $total) -lt 0.5) { continue }

            # majority colour keeps the flat pixel-art blocks crisp
            $best = 0
            $bestCount = 0

            foreach ($key in $counts.Keys) {
                if ($counts[$key] -gt $bestCount) { $bestCount = $counts[$key]; $best = $key }
            }

            $canvas.SetPixel($offX + $dx, $offY + $dy,
                [System.Drawing.Color]::FromArgb(255, ($best -shr 16) -band 0xFF, ($best -shr 8) -band 0xFF, $best -band 0xFF))
        }
    }

    if ($frames -gt 1) {
        # single-frame art -> a "churning" animation: keep the base frame untouched and stack a
        # 50%-alpha copy of the same sprite shifted by 2px, cycling the direction per frame. The
        # silhouette grows a soft 2px bulge that walks around while the outline itself never
        # breaks (the base frame is always fully opaque), which is what sells "clouds rolling".
        $stacked = New-Object System.Drawing.Bitmap $target, ($target * $frames), ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g = [System.Drawing.Graphics]::FromImage($stacked)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
        $offsets = @(@(2, 0), @(0, -2), @(-2, 2), @(0, 2))
        $ghost = New-Object System.Drawing.Imaging.ImageAttributes
        $matrix = New-Object System.Drawing.Imaging.ColorMatrix
        $matrix.Matrix33 = 0.5
        $ghost.SetColorMatrix($matrix)

        for ($f = 0; $f -lt $frames; $f++) {
            $g.DrawImage($canvas, 0, ($f * $target))
            $o = $offsets[$f % $offsets.Count]
            $dest = New-Object System.Drawing.Rectangle -ArgumentList 0, ($f * $target), $target, $target
            $g.DrawImage($canvas, $dest, $o[0], $o[1], $target, $target, [System.Drawing.GraphicsUnit]::Pixel, $ghost)
        }

        $g.Dispose()
        $ghost.Dispose()
        $stacked.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
        $stacked.Dispose()
        # 4 ticks a frame (0.2s), 4 frames -> a 0.8s loop
        Set-Content -Path ($outFile + '.mcmeta') -Value '{ "animation": { "frametime": 4 } }' -Encoding Ascii
    } else {
        $canvas.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
    }

    $pal = ($palette | ForEach-Object { '#{0:X2}{1:X2}{2:X2}' -f $_[0], $_[1], $_[2] }) -join ' '
    Write-Host ("{0,-22} src {1,3}x{2,-3} plate {3,5}px [{4}]  sprite {5,3}x{6,-3} -> {7,2}x{8,-2} at {9},{10}  tol={11}" -f `
        $label, $w, $h, $plate, $pal, $sw, $sh, $dw, $dh, $offX, $offY, $tol)
    Save-Preview $canvas $label
    $canvas.Dispose()
}

# where each reference comes from, how it is processed, where it goes
#   name   output file name (no extension)
#   root   'cache' / 'cache2' / 'cache3' / 'wechat' / 'wechat2' (which reference folder the source lives in)
#   source reference file name inside that root
#   target canvas size (16 for items, 16 for combo icons)
#   dir    'item' or 'gui'
#   tol    flood-fill tolerance, ABS(dR)+ABS(dG)+ABS(dB) against the plate palette (default 16)
#   ring   width of the border ring the plate palette is sampled from (default 2)
#   share  percentage of the ring the palette has to cover (default 96)
#   trim   pixels to shave off each side of the sprite box (slate: the dashed decoration)
#   minBlob smallest opaque blob kept, in source pixels (default 4)
$jobs = @(
    # batch 1: the original artifact icons (75x75 = 25 art cells at 3x, on the #32313E plate)
    @{ name = 'charm_of_strength'; root = 'cache'; source = 'image-1d019ab0e7545779bfcaf948ac4276da.png';
       target = 16; dir = 'item' },
    @{ name = 'warriors_proof'; root = 'cache'; source = 'image-c87ad1ff1cd702645d73ccc07ff3d9c0.png';
       target = 16; dir = 'item' },
    @{ name = 'sword_textbook'; root = 'cache'; source = 'image-5f52a21905f37f9ec10745cb7afd1dad.png';
       target = 16; dir = 'item' },
    @{ name = 'wind_score'; root = 'cache'; source = 'image-f916fd3ba7516aa64dcc5b2ac4681aa0.png';
       target = 16; dir = 'item' },
    @{ name = 'slate'; root = 'cache'; source = 'image-ca60fa5463a9a7dbff644a1e5b2e9d23.png';
       target = 16; dir = 'item'; tol = 20; trim = 7; minBlob = 24 },
    # the two tab icons sit on a dithered purple plate (8+ shades): share 100 keeps every shade in
    # the palette, otherwise the tail survives as dirt around the sprite
    @{ name = 'slate_tab_icon'; root = 'cache'; source = 'image-02819efdb2970d9e7b051a5e06f349e5.png';
       target = 16; dir = 'item'; tol = 30; share = 100 },
    @{ name = 'artifact_tab_icon'; root = 'cache'; source = 'image-89d5d9733c1e7651cbbe39f826aae885.png';
       target = 16; dir = 'item'; tol = 30; share = 100 },
    @{ name = 'combo_sturdy'; root = 'cache'; source = 'image-fd3f1ff1b1c2bb356b3035c500a2affc.png';
       target = 16; dir = 'gui' },

    # batch 2: pressure bandage / golden cloak / wanderer necklace / projection sword
    @{ name = 'pressure_bandage'; root = 'cache'; source = 'image-a628fc58440863833d9289271e0128ed.png';
       target = 16; dir = 'item' },
    @{ name = 'golden_cloak'; root = 'cache'; source = 'image-8b227800af38498190e66891e1cca0d1.png';
       target = 16; dir = 'item' },
    @{ name = 'wanderer_necklace'; root = 'cache'; source = 'image-844cd14b84647f6d338fd2a698eec9af.png';
       target = 16; dir = 'item' },
    @{ name = 'projection_sword'; root = 'cache'; source = 'image-5797598120567ac7015707fd5a65efb6.png';
       target = 16; dir = 'item' },
    @{ name = 'combo_wind_song'; root = 'cache'; source = 'image-47da6411294340e6fb72a2dc162282fa.png';
       target = 16; dir = 'gui' },
    @{ name = 'slate_of_future'; root = 'cache'; source = 'image-84628a6d87ac4c3a2b8dc0287d8122a0.png';
       target = 16; dir = 'item' },
    @{ name = 'leaf'; root = 'cache'; source = 'image-09c22f1cd7431951e527f5126b8d4400.png';
       target = 16; dir = 'gui' },
    # shop_tab is parked: its reference draws the scales on a black badge, and cutting only the
    # purple plate leaves black holes inside the pans. The committed texture (clean scales) stands.
    @{ name = 'dice'; root = 'cache'; source = 'image-1930778f7324f8dfe5e250b50cfbfc79.png';
       target = 16; dir = 'item' },
    @{ name = 'upgrade_chest'; root = 'cache'; source = 'image-1591196f455e2354d278f04d9b78cb40.png';
       target = 16; dir = 'item' },

    # batch 3: the four slates. The mock-up sheets are gone with the wechat folder, but the user
    # re-sent the icons themselves at 80x80, so each job points straight at its icon.
    #
    # These MUST keep tol = 10. Their plate is #29283E with a #222034 mock-up frame, so the ring
    # palette holds both, and the artwork's black outline is #1F1A31 - which is only 12 away from
    # #222034 (3+6+3). At the default tol 16 the flood fill walks straight through the outline and
    # eats the whole ring (it left 0 outline pixels and a 42x54 sprite); tol 10 is one step below
    # that 12, so the outline survives as the ~2px dark border the references show and the sprite
    # box becomes the outline's box (54x66 for oath). Do not raise it back to the default.
    @{ name = 'slate_of_oath'; root = 'cache'; source = 'image-1eec57a9e1c286f1171555a9ed80bc3e.png';
       target = 16; dir = 'item'; tol = 10 },
    @{ name = 'slate_of_belief'; root = 'cache'; source = 'image-65bcce0c54001293a7055d0082278681.png';
       target = 16; dir = 'item'; tol = 10 },
    @{ name = 'slate_of_entrance'; root = 'cache'; source = 'image-d6b18a4c42446738d6e6be830ee5a61a.png';
       target = 16; dir = 'item'; tol = 10 },
    @{ name = 'slate_of_competition'; root = 'cache'; source = 'image-8e62bfb316bc997f17d770af6458d73d.png';
       target = 16; dir = 'item'; tol = 10 },

    # batch 5: artifacts drawn on the cache's dark checkerboard (75x75)
    @{ name = 'colorless_cube'; root = 'cache2'; source = 'image-940ab8479d738812dc5d75b5de641953.png';
       target = 16; dir = 'item' },
    @{ name = 'silver_plate'; root = 'cache2'; source = 'image-7fc490685de96e90f5895cf2ddc3126d.png';
       target = 16; dir = 'item' },
    @{ name = 'encouragement_banner'; root = 'cache2'; source = 'image-2ce70b3e143665f6439e94970c807e6d.png';
       target = 16; dir = 'item' },
    @{ name = 'haste_grimoire'; root = 'cache2'; source = 'image-8534d4218e4b32950468f383497c5adc.png';
       target = 16; dir = 'item' },
    @{ name = 'red_dew'; root = 'cache2'; source = 'image-a1029fef697aca4219ea57627c507b18.png';
       target = 16; dir = 'item' },
    @{ name = 'longing_amulet'; root = 'cache2'; source = 'image-88bf9af65a57d7db61ff8229616c5b9c.png';
       target = 16; dir = 'item' },
    @{ name = 'fault_probe'; root = 'cache2'; source = 'image-32a48f4676850249a1ec064ba3be9aef.png';
       target = 16; dir = 'item' },

    # batch 6: six more artifacts, same 75x75 layout
    @{ name = 'deft_amulet'; root = 'cache2'; source = 'image-db75e9b688e81456d09e20102f4a6420.png';
       target = 16; dir = 'item' },
    # pinwheel: blades + the black sticker outline + the stick below. The stick and the halo around
    # the blades are painted in the plate's own shades (#2A2539 / #323045), which is exactly what
    # the flood fill is for - they are sealed off by the outline, so they stay. tol 14 keeps the
    # #2A2539 outline (18 away from the #222034 frame) on the opaque side of the line.
    @{ name = 'pinwheel'; root = 'cache2'; source = 'image-785bbab539a441eff1796514a75d92ee.png';
       target = 16; dir = 'item'; tol = 14 },
    @{ name = 'specimen_beak'; root = 'cache2'; source = 'image-7329682a3f6550d766c3302d968644d0.png';
       target = 16; dir = 'item' },
    @{ name = 'evergreen_cloak'; root = 'cache2'; source = 'image-ea14f56c02d40828c4776f89f45be307.png';
       target = 16; dir = 'item' },
    @{ name = 'resistance_band'; root = 'cache2'; source = 'image-400509a99650825ca98b578727385683.png';
       target = 16; dir = 'item' },
    @{ name = 'warm_stone'; root = 'cache2'; source = 'image-b37f3bfcfdd16fe510ee900339d00fe2.png';
       target = 16; dir = 'item' },

    # combo icons: 60x60 references on the #16151F plate (the combo counter draws them next to the
    # combo number). combo_shadow keeps its black ink blob, which is a plate shade itself but sits
    # behind the outline; combo_precision is the aiming reticle cursor.
    @{ name = 'combo_shadow'; root = 'cache2'; source = 'image-5eade15e9cd5c9a3f9d0a1c8a795e78a.png';
       target = 16; dir = 'gui' },
    @{ name = 'combo_precision'; root = 'cache2'; source = 'image-b2e3064bd76dfb77bd63dff71d6e39ff.png';
       target = 16; dir = 'gui' },

    # no reference left: the wechat temp folder is recycled, so shield_textbook keeps the texture it
    # already has. Re-running it from that texture was tried and made it worse (the flood fill ate
    # the book's pages, which touch the border), so the job is parked here instead.
    @{ name = 'shield_textbook'; root = 'wechat'; source = 'f2d76651bbc04fe14c995fc683196387.png';
       target = 16; dir = 'item' },

    # batch 4 potions: same wechat folder, skipped until the user sends them again
    @{ name = 'regeneration_potion'; root = 'wechat'; source = '38af9bf5fa04c2b82f33547784727131.png';
       target = 16; dir = 'item' },
    @{ name = 'apple_juice'; root = 'wechat'; source = '67ec95565d54e49a4090d158a1076511.png';
       target = 16; dir = 'item' },
    @{ name = 'trappist_sacred'; root = 'wechat'; source = '1a1b1f5222c25416c3bd396f294f71a1.png';
       target = 16; dir = 'item' },
    @{ name = 'big_dice_potion'; root = 'wechat'; source = '6f80f18e7f5f5ac3600f4a848d6bfda1.png';
       target = 16; dir = 'item' },
    @{ name = 'vampire_lord_oath'; root = 'wechat'; source = '414e4f638cfc9a75cce608cb5e29b3d5.png';
       target = 16; dir = 'item' },
    @{ name = 'enchant_coin'; root = 'cache'; source = 'image-479fa47d5d5c63efb1b9e9b6fee3de20.png';
       target = 24; dir = 'item' },
    @{ name = 'backpack_tab'; root = 'cache'; source = 'image-846cb651178c4912ce670c438e02f1f3.png';
       target = 20; dir = 'gui' },
    @{ name = 'attributes_tab'; root = 'cache'; source = 'image-2af2323c6c184090a56e3b0d5de35c55.png';
       target = 20; dir = 'gui' },

    # batch 7: the four new slates. Same 80x80 mock-up layout as batch 3, but this plate is
    # #29283E with a #222034 frame around the artwork, and the artwork's black outline is #1F1A31 -
    # which is only 12 away from #222034, so the default tol 16 floods straight through the outline
    # and eats it. The outline is 37 away from #29283E and 12 from #222034, so tol 10 is what keeps
    # it: at 16x16 it reads as the ~2px dark border the reference shows.
    # minBlob 24 drops the mock-up's corner ticks (#3B3B58 / #65708C, <= 12px); they are not plate
    # coloured, so without it they survive at the image corners and stretch the sprite box to 80x80.
    @{ name = 'slate_of_rally'; root = 'wechat2'; source = 'ef1f6366da6fbb340a44b56a453cfbf7.png';
       target = 16; dir = 'item'; tol = 10; minBlob = 24 },
    @{ name = 'slate_of_wave'; root = 'wechat2'; source = '958b3ef99b40e621c50d2af0699e85e6.png';
       target = 16; dir = 'item'; tol = 10; minBlob = 24 },
    @{ name = 'slate_of_double_star'; root = 'wechat2'; source = 'e701a22f22f43c5229046f544405017c.png';
       target = 16; dir = 'item'; tol = 10; minBlob = 24 },
    @{ name = 'slate_of_handshake'; root = 'wechat2'; source = '7197415cfcf6fb6c0a32ee0cc4b36211.png';
       target = 16; dir = 'item'; tol = 10; minBlob = 24 },

    # batch 8: the three negotiation artifacts + the real negotiation combo icon. Same wechat RWTemp
    # slot as before, now holding this batch (the earlier files are gone, their jobs skip). Artwork
    # sits straight on the dark plate with its own outline, like batch 6 - default tol/params,
    # previews checked in .preview/.
    @{ name = 'crittons_seal'; root = 'wechat'; source = 'ef1bb7c18c0c69f2534cfd050af33338.png';
       target = 16; dir = 'item' },
    @{ name = 'lucky_medal'; root = 'wechat'; source = 'cb8c9fc32dafc4c2ab4ef86f147315d3.png';
       target = 16; dir = 'item' },
    @{ name = 'golden_maple_leaf'; root = 'wechat'; source = '443688cd86c89f361b4dfeaa0c42997f.png';
       target = 16; dir = 'item' },
    @{ name = 'combo_negotiation'; root = 'wechat'; source = 'a4653a64f74d7809bba15cbda580ec42.png';
       target = 16; dir = 'gui' },

    # batch 9: two precision artifacts (sword_earring, keen_eye) from the newest zcode session
    # cache. Artwork sits straight on the dark plate with its own outline, like batch 8 - default
    # tol/params, previews checked in .preview/.
    @{ name = 'sword_earring'; root = 'cache3'; source = 'image-bb54fc94e6afd0772c93934c69bf0c18.png';
       target = 16; dir = 'item' },
    @{ name = 'keen_eye'; root = 'cache3'; source = 'image-ea7fb834d63687053a0fd44ee7bca8df.png';
       target = 16; dir = 'item' },

    # batch 10: the element combo icon + its three artifacts (magic_carrot, sharp_flint,
    # resonance_stone), same cache3 session. Artwork sits straight on the dark plate with its own
    # outline, like batch 9 - default tol/params, previews checked in .preview/.
    @{ name = 'combo_element'; root = 'cache3'; source = 'image-ccadf59e7bd0132ad1533882a1ef6f54.png';
       target = 16; dir = 'gui' },
    @{ name = 'magic_carrot'; root = 'cache3'; source = 'image-61bf87780310e0d291652b9d6131ac95.png';
       target = 16; dir = 'item' },
    @{ name = 'sharp_flint'; root = 'cache3'; source = 'image-b1b3c504bf4fe2ec35efc20a960984cf.png';
       target = 16; dir = 'item' },
    @{ name = 'resonance_stone'; root = 'cache3'; source = 'image-f5e59fd4bc9658a41f0abf551e51ca61.png';
       target = 16; dir = 'item' },

    # batch 11: the magic_tech combo icon, same cache3 session. Artwork sits straight on the dark
    # plate with its own outline, like batch 10 - default tol/params, previews checked in .preview/.
    @{ name = 'combo_magic_tech'; root = 'cache3'; source = 'image-dfed20b690e2f920ef613de9fa9a0eb3.png';
       target = 16; dir = 'gui' },

    # batch 12: the eight magic_tech artifacts (branch, bug, horn, amulet, earrings, grimoire,
    # compass, firefly), same cache3 session. Artwork sits straight on the dark plate with its own
    # outline, like batches 9-11 - default tol/params, previews checked in .preview/.
    @{ name = 'lightning_struck_branch'; root = 'cache3'; source = 'image-703e9bd81db1cec3b6b4a3f4a777795c.png';
       target = 16; dir = 'item' },
    @{ name = 'electric_bug'; root = 'cache3'; source = 'image-5ecd4db7543309b85d6d97343a4b1bb1.png';
       target = 16; dir = 'item' },
    @{ name = 'qilin_horn'; root = 'cache3'; source = 'image-1877797ca80e3f3b267498653ed4850c.png';
       target = 16; dir = 'item' },
    @{ name = 'electric_amulet'; root = 'cache3'; source = 'image-9a868ab007001ad4dd55a8c18756b65d.png';
       target = 16; dir = 'item' },
    @{ name = 'sande_earrings'; root = 'cache3'; source = 'image-7d0ec09fd0f187371a4328be94695cf1.png';
       target = 16; dir = 'item' },
    @{ name = 'thunder_verdict'; root = 'cache3'; source = 'image-aa6b6f67e7520ddbcf478b6f6f91244c.png';
       target = 16; dir = 'item' },
    @{ name = 'storm_compass'; root = 'cache3'; source = 'image-5f1aa3bb932ee0abc25657e520540a6a.png';
       target = 16; dir = 'item' },
    @{ name = 'firefly'; root = 'cache3'; source = 'image-bb46f8e4d4017ed927c1f677d6606e5a.png';
       target = 16; dir = 'item' },

    # batch 13: the dark_cloud combo icon + the typhoon score + the cloud sprite the combo floats
    # above the player's head. The cloud sprite is written as a 4-frame churn animation (16x64 +
    # .mcmeta) built from the single reference frame - see the $frames branch in Import-Icon.
    @{ name = 'combo_dark_cloud'; root = 'wechat'; source = 'a6f2cd9598f1bc68fc7bfde4553c95fc.png';
       target = 16; dir = 'gui' },
    @{ name = 'typhoon_score'; root = 'wechat'; source = '6431f5bd40aa555cc1f118512096fd99.png';
       target = 16; dir = 'item' },
    @{ name = 'dark_cloud'; root = 'wechat'; source = 'a6f2cd9598f1bc68fc7bfde4553c95fc.png';
       target = 16; dir = 'item'; frames = 4 },

    # batch 14: the first three plain dark-cloud artifacts (stone flower, sapote fruit, lightning
    # rod), same cache3 session. Artwork sits straight on the dark plate with its own outline,
    # like batches 9-12 - default tol/params, previews checked in .preview/.
    @{ name = 'stone_flower'; root = 'cache3'; source = 'image-47a9431dee034a1a5c9ead35bf91e364.png';
       target = 16; dir = 'item' },
    @{ name = 'sapote_fruit'; root = 'cache3'; source = 'image-4680feb40a19db2230de92e7ede6800a.png';
       target = 16; dir = 'item' },
    @{ name = 'lightning_rod'; root = 'cache3'; source = 'image-410e88d2dc36ae5a7c01e7cf9db3a5a5.png';
       target = 16; dir = 'item' },

    # batch 15: five more dark-cloud artifacts (pointy acorn, thunder stone, cloudseed arrow, mast
    # model, raven tablet), same cache3 session. Artwork sits straight on the dark plate with its
    # own outline, like batches 9-14 - default tol/params, previews checked in .preview/.
    @{ name = 'pointy_acorn'; root = 'cache3'; source = 'image-f2ea1211a6e86d2ca82923485e62f3dc.png';
       target = 16; dir = 'item' },
    @{ name = 'thunder_stone'; root = 'cache3'; source = 'image-7503b3b057dce1599f188d5bfecb6f11.png';
       target = 16; dir = 'item' },
    @{ name = 'cloudseed_arrow'; root = 'cache3'; source = 'image-876433656e40954eee47fda119d0e759.png';
       target = 16; dir = 'item' },
    @{ name = 'mast_model'; root = 'cache3'; source = 'image-6b1ffa5b185fb42e9fbb23913ccdca45.png';
       target = 16; dir = 'item' },
    @{ name = 'raven_tablet'; root = 'cache3'; source = 'image-71f344e59e0108fd36bd4eae495d9263.png';
       target = 16; dir = 'item' },

    # batch 16: the sun_sword combo icon (also reused as the sun_sword item sprite - the hidden
    # carrier the dropped swords use) plus the two sun_sword artifacts. Same cache3 session,
    # artwork straight on the dark plate with its own outline - default tol/params.
    @{ name = 'combo_sun_sword'; root = 'cache3'; source = 'image-fc141ed9cf66debced03b5e4fd0b5ee8.png';
       target = 16; dir = 'gui' },
    @{ name = 'sun_sword'; root = 'cache3'; source = 'image-fc141ed9cf66debced03b5e4fd0b5ee8.png';
       target = 16; dir = 'item' },
    @{ name = 'solis_fracto'; root = 'cache3'; source = 'image-75b5b7e23f8ca4a90efc17dac084933b.png';
       target = 16; dir = 'item' },
    @{ name = 'solis_parvo'; root = 'cache3'; source = 'image-747ffd11b5448cc114753e859e7c7b79.png';
       target = 16; dir = 'item' },

    # batch 17: the ember combo icon, from the newest zcode session cache (the reference is the
    # 60x60 flame on the #16151F plate, same as batches 13/16) - default tol/params.
    @{ name = 'combo_ember'; root = 'cache4'; source = 'image-6960c5d86ba46c654f47fad1622baef1.png';
       target = 16; dir = 'gui' },

    # batch 18: the six ember artifacts (red snake eye, ambergris, red yarn ball, oak charcoal,
    # fire bug, lava bead), same cache4 session. Artwork sits straight on the dark plate with its
    # own outline, like batches 9-16 - default tol/params, previews checked in .preview/.
    @{ name = 'red_snake_eye'; root = 'cache4'; source = 'image-22de5de63e0a59a1210ebf516e32a55c.png';
       target = 16; dir = 'item' },
    @{ name = 'ambergris'; root = 'cache4'; source = 'image-8538589edb40c96426304dfc26f76b77.png';
       target = 16; dir = 'item' },
    @{ name = 'red_yarn_ball'; root = 'cache4'; source = 'image-d6037930a374d644ea5107107fd0bb74.png';
       target = 16; dir = 'item' },
    @{ name = 'oak_charcoal'; root = 'cache4'; source = 'image-2644ae0740c36c82d58cf232c42c7adc.png';
       target = 16; dir = 'item' },
    @{ name = 'fire_bug'; root = 'cache4'; source = 'image-449098c42453aa58162db15911626e34.png';
       target = 16; dir = 'item' },
    @{ name = 'lava_bead'; root = 'cache4'; source = 'image-fdd055ac3e8bcd69e4b95b9fb76e772d.png';
       target = 16; dir = 'item' }
)

$skipped = @()

foreach ($job in $jobs) {
    $name = $job.name

    if ($Only -and ($Only -notcontains $name)) {
        continue
    }

    $outDir = if ($job.dir -eq 'gui') { $guiDir } else { $texDir }
    $outFile = Join-Path $outDir ($name + '.png')

    $sourceRoot = if ($job.root -eq 'wechat2') { $wechat2 } elseif ($job.root -eq 'wechat') { $wechat } elseif ($job.root -eq 'cache4') { $cache4 } elseif ($job.root -eq 'cache3') { $cache3 } elseif ($job.root -eq 'cache2') { $cache2 } else { $cache }

    if (-not (Test-Path $sourceRoot)) {
        # the wechat temp folder is recycled by the chat client: once a whole batch's reference
        # folder is gone its textures are already committed, so skip those jobs instead of failing a
        # run that only cares about the current batch.
        $skipped += $name
        Write-Warning "$name : reference folder no longer exists, skipped ($sourceRoot)"
        continue
    }

    $file = Join-Path $sourceRoot $job.source

    if (-not (Test-Path $file)) {
        # same story one level down: the client reuses one RWTemp slot, so a folder that is still
        # there can hold a newer batch while an older batch's files are already gone. For the wechat
        # roots and the newest cache roots (cache3 / cache4) that is an expired reference whose
        # texture is committed, not a typo; a missing file under cache / cache2 still stops the run.
        if (@('wechat', 'wechat2', 'cache3', 'cache4') -contains $job.root) {
            $skipped += $name
            Write-Warning "$name : reference file no longer in its temp folder, skipped"
            continue
        }

        throw "reference image not found: $file"
    }

    $tol = if ($job.tol) { [int]$job.tol } else { 16 }
    $ring = if ($job.ring) { [int]$job.ring } else { 2 }
    $share = if ($job.share) { [int]$job.share } else { 96 }
    $trim = if ($job.trim) { [int]$job.trim } else { 0 }
    $minBlob = if ($job.minBlob) { [int]$job.minBlob } else { 4 }
    $frames = if ($job.frames) { [int]$job.frames } else { 1 }

    Import-Icon $file $outFile $name ([int]$job.target) $tol $ring $share $trim $minBlob $frames

    if ($job.dir -ne 'gui') {
        Write-ItemDefinitions $name
    }
}

if ($skipped.Count -gt 0) {
    Write-Output ("skipped (reference folder gone): " + ($skipped -join ', '))
}

Write-Output 'previews written to .preview/'
