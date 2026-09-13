# Adopt the vanilla crossbow models but recolour their textures into the SEPHIRIA palette, so the
# weapon reads as part of this mod instead of looking like a plain vanilla crossbow.
#
# Vanilla assets are extracted to a temp folder by the build notes; this script only reads them
# and writes recoloured copies into the mod.
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$src = Join-Path $env:TEMP 'vanillamodels\assets\minecraft\textures\item'
$dst = Join-Path $root 'src\main\resources\assets\sephiria\textures\item'
$models = Join-Path $root 'src\main\resources\assets\sephiria\models\item'

New-Item -ItemType Directory -Force -Path $dst, $models | Out-Null

# SEPHIRIA palette
$WHITE = @(255, 255, 255)
$WOOD = @(112, 80, 82)
$WOOD_DARK = @(73, 56, 67)
$STEEL_LIGHT = @(189, 190, 207)
$STEEL_MID = @(126, 135, 167)
$STEEL_DARK = @(75, 75, 100)
$NAVY = @(55, 54, 76)
$NEAR_BLACK = @(34, 32, 52)

function Convert-Colour($r, $g, $b) {
    $min = [Math]::Min($r, [Math]::Min($g, $b))
    $lum = 0.299 * $r + 0.587 * $g + 0.114 * $b
    if ($min -gt 200) { return $WHITE }
    if (($r - $b) -gt 10) {
        # warm: wood and leather
        if ($lum -gt 110) { return $WOOD } else { return $WOOD_DARK }
    }
    if ($lum -gt 170) { return $STEEL_LIGHT }
    if ($lum -gt 110) { return $STEEL_MID }
    if ($lum -gt 60) { return $STEEL_DARK }
    if ($lum -gt 25) { return $NAVY }
    return $NEAR_BLACK
}

# vanilla state texture -> mod state texture
$map = [ordered]@{
    'crossbow_standby'   = 'colossal_crossbow_standby'
    'crossbow_pulling_0' = 'colossal_crossbow_pulling_0'
    'crossbow_pulling_1' = 'colossal_crossbow_pulling_1'
    'crossbow_pulling_2' = 'colossal_crossbow_pulling_2'
    'crossbow_arrow'     = 'colossal_crossbow_arrow'
}

foreach ($key in $map.Keys) {
    $in = Join-Path $src ($key + '.png')
    if (-not (Test-Path $in)) { Write-Output ("missing source " + $in); continue }

    $bmp = [System.Drawing.Bitmap]::FromFile($in)
    $out = New-Object System.Drawing.Bitmap($bmp.Width, $bmp.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt $bmp.Height; $y++) {
        for ($x = 0; $x -lt $bmp.Width; $x++) {
            $p = $bmp.GetPixel($x, $y)
            if ($p.A -lt 32) { continue }
            $c = Convert-Colour $p.R $p.G $p.B
            $out.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($p.A, $c[0], $c[1], $c[2]))
        }
    }
    $outPath = Join-Path $dst ($map[$key] + '.png')
    $out.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Output ("recoloured " + $key + " -> " + $map[$key] + ".png (" + $bmp.Width + "x" + $bmp.Height + ")")
    $bmp.Dispose(); $out.Dispose()
}

# models: the vanilla crossbow model carries the display transforms, the state models just swap
# the texture, exactly like vanilla does
$base = @'
{
	"parent": "minecraft:item/generated",
	"textures": {
		"layer0": "sephiria:item/colossal_crossbow_standby"
	},
	"display": {
		"thirdperson_righthand": {
			"rotation": [ -90, 0, -60 ],
			"translation": [ 2, 0.1, -3 ],
			"scale": [ 0.9, 0.9, 0.9 ]
		},
		"thirdperson_lefthand": {
			"rotation": [ -90, 0, 30 ],
			"translation": [ 2, 0.1, -3 ],
			"scale": [ 0.9, 0.9, 0.9 ]
		},
		"firstperson_righthand": {
			"rotation": [ -90, 0, -55 ],
			"translation": [ 1.13, 3.2, 1.13 ],
			"scale": [ 0.68, 0.68, 0.68 ]
		},
		"firstperson_lefthand": {
			"rotation": [ -90, 0, 35 ],
			"translation": [ 1.13, 3.2, 1.13 ],
			"scale": [ 0.68, 0.68, 0.68 ]
		}
	}
}
'@
[IO.File]::WriteAllText((Join-Path $models 'colossal_crossbow_in_hand.json'), $base, (New-Object System.Text.UTF8Encoding($false)))
Write-Output 'wrote colossal_crossbow_in_hand.json (vanilla standby)'

$states = [ordered]@{
    'crossbow_pulling_0'       = 'colossal_crossbow_pulling_0'
    'crossbow_pulling_1'       = 'colossal_crossbow_pulling_1'
    'crossbow_pulling_2'       = 'colossal_crossbow_pulling_2'
    'crossbow_loaded_in_hand'  = 'colossal_crossbow_arrow'
}
foreach ($state in $states.Keys) {
    $json = @'
{
	"parent": "sephiria:item/colossal_crossbow_in_hand",
	"textures": {
		"layer0": "sephiria:item/__TEX__"
	}
}
'@.Replace('__TEX__', $states[$state])
    [IO.File]::WriteAllText((Join-Path $models ($state + '.json')), $json, (New-Object System.Text.UTF8Encoding($false)))
    Write-Output ("wrote " + $state + '.json')
}

# the previous hand-designed crossbow pieces are no longer used
foreach ($stale in @('crossbow_3d.json', 'crossbow.json')) {
    $p = Join-Path $models $stale
    if (Test-Path $p) { Remove-Item $p -Force; Write-Output ("removed unused models/" + $stale) }
}
foreach ($stale in @('crossbow_pulling_0.png', 'crossbow_pulling_1.png', 'crossbow_pulling_2.png', 'crossbow_loaded_3d.png')) {
    $p = Join-Path $dst $stale
    if (Test-Path $p) { Remove-Item $p -Force; Write-Output ("removed unused textures/" + $stale) }
}
