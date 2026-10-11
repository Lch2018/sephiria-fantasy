# Make an artifact reference image whose artwork BODY is painted in the same dark tone as the
# mock-up plate's frame extractable by tools/import-artifact-icons.ps1.
#
# Why it is needed (2026-10-11, 睿智护符): that reference draws the amulet's body in #222034,
# which is also the colour of the plate's outer frame. The importer floods the background from the
# image border through the plate palette, and since the body has the frame's colour it is reachable
# from the border - so the body was removed and only the bright beads survived.
#
# Fix: nudge the ENCLOSED dark pixels (the body) a few steps darker, so they fall outside the
# palette tolerance and stay as artwork. "Enclosed" is decided by connectivity: the frame is
# 4-connected to the border, the body is not. The nudge (8/8/12) is invisible to the eye.
#
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File tools\ink-dark-body.ps1 <in.png> <out.png>
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

param(
    [Parameter(Mandatory = $true)][string]$In,
    [Parameter(Mandatory = $true)][string]$Out,
    [int]$Nudge = 12
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$src = [System.Drawing.Bitmap]::FromFile($In)
$w = $src.Width
$h = $src.Height
$bmp = New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

# the darkest tone of the image = the plate frame, and (unfortunately) the artwork body
$counts = @{}
for ($y = 0; $y -lt $h; $y++) {
    for ($x = 0; $x -lt $w; $x++) {
        # -shl keeps the left operand's type: shifting a Byte by 16 wraps to 0, so mask the ARGB int
        $key = $src.GetPixel($x, $y).ToArgb() -band 0xFFFFFF
        if ($counts.ContainsKey($key)) { $counts[$key]++ } else { $counts[$key] = 1 }
    }
}
# the frame is the most common among the darkest half of the distinct colours
$distinct = $counts.GetEnumerator() | Sort-Object Value -Descending
$dark = $null
foreach ($entry in $distinct) {
    $r = ($entry.Key -shr 16) -band 0xFF
    $g = ($entry.Key -shr 8) -band 0xFF
    $b = $entry.Key -band 0xFF
    if (($r + $g + $b) -lt 150) { $dark = $entry.Key; break }
}
if ($null -eq $dark) { throw "no dark tone found in $In" }

# flood fill that tone from the border: everything reached is the plate frame
$frame = New-Object 'bool[]' ($w * $h)
$queue = New-Object System.Collections.Queue
$darkR = ($dark -shr 16) -band 0xFF
$darkG = ($dark -shr 8) -band 0xFF
$darkB = $dark -band 0xFF
$darkArgb = [System.Drawing.Color]::FromArgb(255, $darkR, $darkG, $darkB).ToArgb()
for ($i = 0; $i -lt ($w * $h); $i++) {
    $x = $i % $w
    $y = [int][Math]::Floor($i / $w)
    if ($x -ne 0 -and $y -ne 0 -and $x -ne ($w - 1) -and $y -ne ($h - 1)) { continue }
    if ($src.GetPixel($x, $y).ToArgb() -ne $darkArgb) { continue }
    $frame[$i] = $true
    $queue.Enqueue($i)
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
        if ($frame[$np]) { continue }
        $key = $src.GetPixel($nx, $ny).ToArgb() -band 0xFFFFFF
        if ($key -ne $dark) { continue }
        $frame[$np] = $true
        $queue.Enqueue($np)
    }
}

$dr = (($dark -shr 16) -band 0xFF) - $Nudge
$dg = (($dark -shr 8) -band 0xFF) - $Nudge
$db = ($dark -band 0xFF) - $Nudge
if ($dr -lt 0) { $dr = 0 }
if ($dg -lt 0) { $dg = 0 }
if ($db -lt 0) { $db = 0 }
$inked = 0

for ($y = 0; $y -lt $h; $y++) {
    for ($x = 0; $x -lt $w; $x++) {
        $c = $src.GetPixel($x, $y)
        $key = $c.ToArgb() -band 0xFFFFFF

        # enclosed dark pixels: the artwork body
        if ($key -eq $dark -and -not $frame[$y * $w + $x]) {
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $dr, $dg, $db))
            $inked++
            continue
        }

        $bmp.SetPixel($x, $y, $c)
    }
}

$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
$src.Dispose()
Write-Output ("inked " + $inked + " body pixel(s) -> " + $Out)
