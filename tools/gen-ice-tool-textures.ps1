# Generate the ICE-BLUE and FLAME-YELLOW versions of the vanilla iron_axe / iron_spear / arrow sprites.
#
# The Frost Armament projectiles (hammer / ice spear / ice bow arrows) are drawn with vanilla item
# models as stand-ins until real models exist (user's call: "先用原版铁斧 / 铁长矛 / 箭"). The user
# then asked for all three to be tinted ice blue, so this script remaps each vanilla sprite onto the
# mod's ice ramp (the same palette as the frost-qi arc texture: deep blue -> pale blue -> white),
# driven purely by the source pixel's luminance - i.e. the whole object is "dyed", handle included.
#
# 2026-10-14: the Eternal Eclipse artifact turns the whole Frost Armament fire-based while it sits in
# the LEFT half of the backpack, so the same three sprites are also needed in flame yellow (the warm
# ramp below, taken from the Sun Sword palette). The Display entity cannot tint an item, so each
# variant is a separate texture and the carrier item's model picks between them by a component
# (FrostArmament.FLAME_TOOL).
#
# The result is a hidden carrier item (like ModItems.SUN_SWORD / DARK_CLOUD): registered, has an
# icon + model definition, but never handed to the player - the Display.ItemDisplay entities use it.
#
# Source sprites are read straight out of the loom minecraft jar (no Mojang art is copied into the
# repo). The jar is located under the gradle cache; if it is missing the script stops with a hint.
#
# Output : src/main/resources/assets/sephiria_fantasy/textures/item/ice_hammer.png  (16x16)
#          .../ice_spear.png, .../ice_arrow.png
#          .../flame_hammer.png, .../flame_spear.png, .../flame_arrow.png
#          + items/<id>.json and models/item/<id>.json (only when absent)
# Preview: .preview/ice_tools_preview.png (all six, enlarged, over a mid grey)
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$texDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\textures\item'
$previewDir = Join-Path $root '.preview'

# ---- the two ramps: dark -> light ----
# ice: the frost-qi arc's palette (deep steel blue -> the arc's fringe -> pale ice -> white core)
# flame: the Sun Sword's palette (dark ember -> orange -> gold -> white), so the fire version of the
# hammer / spear / arrow sits in the same warm family as the sun-sword mark and the flame qi
$RAMPS = @{
    'ice'   = @(@(0x2F, 0x4A, 0x7E), @(0x4E, 0x9F, 0xE8), @(0xA8, 0xE4, 0xFF), @(0xF2, 0xFE, 0xFF))
    'flame' = @(@(0x8A, 0x2B, 0x12), @(0xDF, 0x71, 0x26), @(0xFB, 0xF2, 0x36), @(0xFF, 0xFD, 0xF0))
}
# the source luminance range the ramp is stretched over: pixels darker than $LUM_MIN all land on the
# first stop, brighter than $LUM_MAX on the last. The vanilla sprites never reach pure black/white,
# so clipping a little gives more visible banding than a flat 0..255 stretch.
$LUM_MIN = 40.0
$LUM_MAX = 215.0

# source sprite (inside the minecraft jar) -> output name + ramp + whether it needs an item definition
#
# The ice ones ARE the carrier items (ModItems.ICE_HAMMER / ICE_SPEAR / ICE_ARROW), so they need
# items/<id>.json + models/item/<id>.json. The flame ones are only extra MODELS: the carriers keep
# their ids and their model switches texture by the FLAME_TOOL component, so an items/flame_*.json
# would be an orphan definition file (the game loads every file in items/ regardless of a matching
# item) - that is why those jobs pass def = $false.
$JOBS = @(
    @{ name = 'ice_hammer';   source = 'assets/minecraft/textures/item/iron_axe.png';    ramp = 'ice';   def = $true },
    @{ name = 'ice_spear';    source = 'assets/minecraft/textures/item/iron_spear.png';  ramp = 'ice';   def = $true },
    @{ name = 'ice_arrow';    source = 'assets/minecraft/textures/item/arrow.png';       ramp = 'ice';   def = $true },
    @{ name = 'flame_hammer'; source = 'assets/minecraft/textures/item/iron_axe.png';    ramp = 'flame'; def = $false },
    @{ name = 'flame_spear';  source = 'assets/minecraft/textures/item/iron_spear.png';  ramp = 'flame'; def = $false },
    @{ name = 'flame_arrow';  source = 'assets/minecraft/textures/item/arrow.png';       ramp = 'flame'; def = $false }
)

function Get-MinecraftJar {
    $pattern = Join-Path $env:USERPROFILE '.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf'
    $jars = Get-ChildItem -Path $pattern -Filter 'minecraft-merged-deobf-*.jar' -Recurse -ErrorAction SilentlyContinue |
            Sort-Object FullName
    if (-not $jars) {
        throw "minecraft jar not found under $pattern - run a Gradle build once so loom downloads it"
    }
    return $jars[-1].FullName
}

function Mix([int[]]$a, [int[]]$b, [double]$t) {
    $t = [Math]::Max(0.0, [Math]::Min(1.0, $t))
    return @(
        [int][Math]::Round($a[0] + ($b[0] - $a[0]) * $t),
        [int][Math]::Round($a[1] + ($b[1] - $a[1]) * $t),
        [int][Math]::Round($a[2] + ($b[2] - $a[2]) * $t)
    )
}

# luminance -> a colour on the given ramp (3 segments between the 4 stops)
function Ramp-Colour($ramp, [double]$lum) {
    $t = ($lum - $LUM_MIN) / ($LUM_MAX - $LUM_MIN)
    $t = [Math]::Max(0.0, [Math]::Min(1.0, $t))
    $scaled = $t * ($ramp.Count - 1)
    $index = [int][Math]::Floor($scaled)
    if ($index -ge ($ramp.Count - 1)) { $index = $ramp.Count - 2 }
    return Mix $ramp[$index] $ramp[($index + 1)] ($scaled - $index)
}

# same helper as tools/import-artifact-icons.ps1: an icon needs the item definition + the model
function Write-ItemDefinitions([string]$name) {
    $defDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\items'
    $modelDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\models\item'
    New-Item -ItemType Directory -Force -Path $defDir, $modelDir | Out-Null
    $utf8 = New-Object System.Text.UTF8Encoding($false)

    $defFile = Join-Path $defDir ($name + '.json')
    if (-not (Test-Path $defFile)) {
        # NOTE: the concatenation MUST stay in parentheses (an unparenthesised 'a' + $name inside an
        # array literal is parsed as several elements and the value lands across lines - invalid JSON)
        $lines = @('{', '  "model": {', '    "type": "minecraft:model",',
                   ('    "model": "sephiria_fantasy:item/' + $name + '"'), '  }', '}')
        [IO.File]::WriteAllText($defFile, (($lines -join "`n") + "`n"), $utf8)
        Write-Output ('wrote items/' + $name + '.json')
    }

    Write-ModelDefinition $name
}

# the model alone (no item definition): the flame variants are extra models for the ice carriers
function Write-ModelDefinition([string]$name) {
    $modelDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\models\item'
    New-Item -ItemType Directory -Force -Path $modelDir | Out-Null
    $utf8 = New-Object System.Text.UTF8Encoding($false)

    $modelFile = Join-Path $modelDir ($name + '.json')
    if (-not (Test-Path $modelFile)) {
        $lines = @('{', '  "parent": "minecraft:item/generated",', '  "textures": {',
                   ('    "layer0": "sephiria_fantasy:item/' + $name + '"'), '  }', '}')
        [IO.File]::WriteAllText($modelFile, (($lines -join "`n") + "`n"), $utf8)
        Write-Output ('wrote models/item/' + $name + '.json')
    }
}

New-Item -ItemType Directory -Force -Path $texDir, $previewDir | Out-Null

$jar = Get-MinecraftJar
Write-Output ('jar ' + $jar)
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)

$previews = @()

try {
    foreach ($job in $JOBS) {
        $entry = $zip.Entries | Where-Object { $_.FullName -eq $job.source }
        if (-not $entry) { throw ('missing in jar: ' + $job.source) }

        $ramp = $RAMPS[$job.ramp]
        $ms = New-Object System.IO.MemoryStream
        $stream = $entry.Open()
        $stream.CopyTo($ms)
        $stream.Dispose()
        $src = New-Object System.Drawing.Bitmap($ms)
        $out = New-Object System.Drawing.Bitmap($src.Width, $src.Height, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb))
        $tinted = 0

        for ($y = 0; $y -lt $src.Height; $y++) {
            for ($x = 0; $x -lt $src.Width; $x++) {
                $c = $src.GetPixel($x, $y)
                if ($c.A -le 0) { continue }
                $lum = 0.299 * $c.R + 0.587 * $c.G + 0.114 * $c.B
                $rgb = Ramp-Colour $ramp $lum
                $out.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($c.A, $rgb[0], $rgb[1], $rgb[2]))
                $tinted++
            }
        }

        $outFile = Join-Path $texDir ($job.name + '.png')
        $out.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
        Write-Output ('{0,-12} {1}x{2}  {3} px tinted -> {4}' -f $job.name, $src.Width, $src.Height, $tinted, $outFile)

        $previews += $out
        $src.Dispose()
        $ms.Dispose()
        # note: $out is kept for the preview sheet and disposed below
        if ($job.def) {
            Write-ItemDefinitions $job.name
        } else {
            Write-ModelDefinition $job.name
        }
    }
}
finally {
    $zip.Dispose()
}

# ---- preview sheet: the three tinted sprites side by side, 8x, over a mid grey ----
$scale = 8
$cell = 16 * $scale
$sheet = New-Object System.Drawing.Bitmap(($cell * $previews.Count), $cell, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb))
$g = [System.Drawing.Graphics]::FromImage($sheet)
$g.Clear([System.Drawing.Color]::FromArgb(255, 108, 108, 108))
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half

for ($i = 0; $i -lt $previews.Count; $i++) {
    $g.DrawImage($previews[$i], ($i * $cell), 0, $cell, $cell)
}

$g.Dispose()
$sheetPath = Join-Path $previewDir 'ice_tools_preview.png'
$sheet.Save($sheetPath, [System.Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()

foreach ($bmp in $previews) { $bmp.Dispose() }

Write-Output ('preview ' + $sheetPath)
