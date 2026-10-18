# Generate the "Book" artifact icon: the vanilla enchanted-book sprite, remapped onto a gold ramp.
#
# The compatible book ("书", rare, no combo) has no hand-drawn art yet, so it borrows the vanilla
# enchanted_book silhouette and is dyed into the mod's gold/amber family - the same trick
# tools/gen-ice-tool-textures.ps1 uses for the frost-armament carriers. When the user supplies real
# art, swap the output file (and park this job like the other generated ones).
#
# Source sprite is read straight out of the loom minecraft jar (no Mojang art is copied into the repo).
# The jar is located under the gradle cache; if it is missing the script stops with a hint.
#
# Output : src/main/resources/assets/sephiria_fantasy/textures/item/book.png   (16x16)
#          + items/book.json and models/item/book.json (only when absent)
# Preview: .preview/book_preview.png (8x, over a mid grey)
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$texDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\textures\item'
$previewDir = Join-Path $root '.preview'

# gold ramp, dark -> light: dark bronze cover -> warm gold -> pale gold -> near-white pages
$RAMP = @(@(0x6B, 0x45, 0x12), @(0xC8, 0x9A, 0x2E), @(0xF2, 0xD4, 0x6A), @(0xFF, 0xFB, 0xE8))
# the source luminance range the ramp is stretched over (the vanilla sprite never hits pure black/white)
$LUM_MIN = 40.0
$LUM_MAX = 215.0

$NAME = 'book'
$SOURCE = 'assets/minecraft/textures/item/enchanted_book.png'

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

# luminance -> a colour on the ramp (3 segments between the 4 stops)
function Ramp-Colour($ramp, [double]$lum) {
    $t = ($lum - $LUM_MIN) / ($LUM_MAX - $LUM_MIN)
    $t = [Math]::Max(0.0, [Math]::Min(1.0, $t))
    $scaled = $t * ($ramp.Count - 1)
    $index = [int][Math]::Floor($scaled)
    if ($index -ge ($ramp.Count - 1)) { $index = $ramp.Count - 2 }
    return Mix $ramp[$index] $ramp[($index + 1)] ($scaled - $index)
}

# an icon needs the item definition + the model (same helpers as tools/gen-ice-tool-textures.ps1)
function Write-ItemDefinitions([string]$name) {
    $defDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\items'
    $modelDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\models\item'
    New-Item -ItemType Directory -Force -Path $defDir, $modelDir | Out-Null
    $utf8 = New-Object System.Text.UTF8Encoding($false)

    $defFile = Join-Path $defDir ($name + '.json')
    if (-not (Test-Path $defFile)) {
        $lines = @('{', '  "model": {', '    "type": "minecraft:model",',
                   ('    "model": "sephiria_fantasy:item/' + $name + '"'), '  }', '}')
        [IO.File]::WriteAllText($defFile, (($lines -join "`n") + "`n"), $utf8)
        Write-Output ('wrote items/' + $name + '.json')
    }

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

try {
    $entry = $zip.Entries | Where-Object { $_.FullName -eq $SOURCE }
    if (-not $entry) { throw ('missing in jar: ' + $SOURCE) }

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
            $rgb = Ramp-Colour $RAMP $lum
            $out.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($c.A, $rgb[0], $rgb[1], $rgb[2]))
            $tinted++
        }
    }

    $outFile = Join-Path $texDir ($NAME + '.png')
    $out.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Output ('{0,-12} {1}x{2}  {3} px tinted -> {4}' -f $NAME, $src.Width, $src.Height, $tinted, $outFile)

    Write-ItemDefinitions $NAME

    # preview sheet: the icon 8x over a mid grey
    $scale = 8
    $cell = $src.Width * $scale
    $sheet = New-Object System.Drawing.Bitmap($cell, $cell, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb))
    $g = [System.Drawing.Graphics]::FromImage($sheet)
    $g.Clear([System.Drawing.Color]::FromArgb(255, 108, 108, 108))
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $g.DrawImage($out, 0, 0, $cell, $cell)
    $g.Dispose()
    $sheetPath = Join-Path $previewDir 'book_preview.png'
    $sheet.Save($sheetPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $sheet.Dispose()

    $src.Dispose()
    $ms.Dispose()
    $out.Dispose()

    Write-Output ('preview ' + $sheetPath)
}
finally {
    $zip.Dispose()
}
