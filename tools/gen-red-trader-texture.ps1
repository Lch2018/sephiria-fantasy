# Generates textures/entity/sephiria_wandering_trader.png for the Sephiria wandering trader.
#
# User spec (2026-10-16): unlike the blue-robed vanilla wandering trader, the Sephiria one wears
# a RED robe ("the only difference from the vanilla wandering trader is the look and the goods").
#
# So the vanilla wandering trader texture is read out of the Minecraft jar and its robe recoloured:
# every blue-dominant pixel (B > R - that is exactly the robe, measured hues cluster around 218 deg)
# keeps its saturation and value but its hue is shifted to red. Skin, hat, hair, belt and buttons
# are not blue-dominant and stay untouched, and the mod repo never stores the unmodified vanilla art.
#
# ASCII only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 files as GBK.
#
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File tools\gen-red-trader-texture.ps1

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$entry = 'assets/minecraft/textures/entity/wandering_trader/wandering_trader.png'
$dst = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\textures\entity\sephiria_wandering_trader.png'

# Hue of the vanilla blue robe is about 218 deg; shift the hue so that it lands on 0 (red).
$ROBE_HUE = 218.0

# Candidate jars: the loom dev caches first, then the PCL test instance (its client jar holds assets).
$candidates = @(
    (Join-Path $env:USERPROFILE '.gradle\caches\fabric-loom\26.3\minecraft-client.jar'),
    (Join-Path $env:USERPROFILE '.gradle\caches\fabric-loom\26.3\minecraft-merged.jar'),
    (Join-Path $env:USERPROFILE '.gradle\caches\fabric-loom\26.3\minecraft-merged-deobf.jar'),
    'D:\PCL\.minecraft\versions\26.3-Fabric 0.19.5\26.3-Fabric 0.19.5.jar'
)

$merged = Get-ChildItem -Path (Join-Path $env:USERPROFILE '.gradle\caches\fabric-loom\minecraftMaven\net\minecraft') `
        -Filter 'minecraft-merged-deobf-*.jar' -Recurse -ErrorAction SilentlyContinue |
    Sort-Object FullName
if ($merged) { $candidates += $merged[-1].FullName }

function Read-JarEntry([string]$jarPath, [string]$name) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
    try {
        $file = $zip.Entries | Where-Object { $_.FullName -eq $name } | Select-Object -First 1
        if (-not $file) { return $null }
        $stream = $file.Open()
        $memory = New-Object System.IO.MemoryStream
        $stream.CopyTo($memory)
        $stream.Dispose()
        $memory.Position = 0
        return New-Object System.Drawing.Bitmap($memory)
    }
    finally {
        $zip.Dispose()
    }
}

$bmp = $null
foreach ($jar in $candidates) {
    if (-not (Test-Path $jar)) { continue }
    $bmp = Read-JarEntry $jar $entry
    if ($bmp) { Write-Output ("source: " + $jar); break }
}
if (-not $bmp) { throw "vanilla wandering trader texture not found in any known jar - run a Gradle build once" }

function Get-Hue([int]$r, [int]$g, [int]$b) {
    $rf = $r / 255.0; $gf = $g / 255.0; $bf = $b / 255.0
    $max = [Math]::Max($rf, [Math]::Max($gf, $bf))
    $min = [Math]::Min($rf, [Math]::Min($gf, $bf))
    $d = $max - $min
    if ($d -le 0) { return 0.0 }
    if ($max -eq $rf) { $h = 60.0 * ((($gf - $bf) / $d) % 6.0) }
    elseif ($max -eq $gf) { $h = 60.0 * ((($bf - $rf) / $d) + 2.0) }
    else { $h = 60.0 * ((($rf - $gf) / $d) + 4.0) }
    if ($h -lt 0) { $h += 360.0 }
    return $h
}

# hue in [0,360), saturation/value in [0,1] -> colour, via the usual HSV sector formula
function From-Hsv([double]$h, [double]$s, [double]$v) {
    $c = $v * $s
    $hp = $h / 60.0
    $x = $c * (1.0 - [Math]::Abs(($hp % 2.0) - 1.0))
    if ($hp -lt 1) { $rgb = @($c, $x, 0.0) }
    elseif ($hp -lt 2) { $rgb = @($x, $c, 0.0) }
    elseif ($hp -lt 3) { $rgb = @(0.0, $c, $x) }
    elseif ($hp -lt 4) { $rgb = @(0.0, $x, $c) }
    elseif ($hp -lt 5) { $rgb = @($x, 0.0, $c) }
    else { $rgb = @($c, 0.0, $x) }
    $m = $v - $c
    return @(
        [int][Math]::Round(($rgb[0] + $m) * 255.0),
        [int][Math]::Round(($rgb[1] + $m) * 255.0),
        [int][Math]::Round(($rgb[2] + $m) * 255.0)
    )
}

$outDir = Split-Path -Parent $dst
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

$out = New-Object System.Drawing.Bitmap($bmp.Width, $bmp.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$changed = 0

for ($y = 0; $y -lt $bmp.Height; $y++) {
    for ($x = 0; $x -lt $bmp.Width; $x++) {
        $pixel = $bmp.GetPixel($x, $y)

        if ($pixel.A -lt 16 -or $pixel.B -le $pixel.R) {
            # transparent, or not a blue-dominant pixel (skin / hat / hair / belt / buttons)
            $out.SetPixel($x, $y, $pixel)
            continue
        }

        $h = Get-Hue $pixel.R $pixel.G $pixel.B
        $s = 0.0
        $v = [Math]::Max($pixel.R, [Math]::Max($pixel.G, $pixel.B)) / 255.0
        $minV = [Math]::Min($pixel.R, [Math]::Min($pixel.G, $pixel.B)) / 255.0
        if ($v -gt 0) { $s = ($v - $minV) / $v }
        $newHue = $h - $ROBE_HUE
        if ($newHue -lt 0) { $newHue += 360.0 }
        $rgb = From-Hsv $newHue $s $v
        $out.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($pixel.A, $rgb[0], $rgb[1], $rgb[2]))
        $changed++
    }
}

$bmp.Dispose()
$out.Save($dst, [System.Drawing.Imaging.ImageFormat]::Png)
$size = "{0}x{1}" -f $out.Width, $out.Height
$out.Dispose()

Write-Output ("wrote " + $dst + "  (" + $size + ", " + $changed + " robe pixels recoloured to red)")
if ($changed -lt 100) { throw "only $changed pixels were recoloured - the robe detection broke (expected a few hundred)" }
