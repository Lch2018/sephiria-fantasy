# Verify the generated 16x16 weapon textures by decoding them back to character maps.
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

Add-Type -AssemblyName System.Drawing

$texDir = 'E:\workspace\Zcode\mcmode\src\main\resources\assets\sephiria\textures\item'
$map = @{
    '233,233,233' = 'B'; '168,168,168' = 'b'; '120,120,120' = 'd'
    '122,75,39'   = 'H'; '78,47,23'   = 'h'; '216,169,60'  = 'G'
    '168,124,34'  = 'g'; '160,113,60' = 'R'; '110,74,37'   = 'r'
    '242,242,242' = 'W'; '138,138,138' = 'S'; '85,85,85'   = 's'
}

foreach ($f in Get-ChildItem $texDir -Filter *.png | Sort-Object Name) {
    $bmp = [System.Drawing.Bitmap]::FromFile($f.FullName)
    $nonTransparent = 0
    Write-Output ("=== " + $f.Name + "  " + $bmp.Width + "x" + $bmp.Height + "  " + $bmp.PixelFormat + " ===")

    for ($y = 0; $y -lt $bmp.Height; $y++) {
        $line = ''
        for ($x = 0; $x -lt $bmp.Width; $x++) {
            $p = $bmp.GetPixel($x, $y)
            if ($p.A -eq 0) {
                $line += '.'
            } else {
                $nonTransparent++
                $k = "$($p.R),$($p.G),$($p.B)"
                if ($map.ContainsKey($k)) { $line += $map[$k] } else { $line += '?' }
            }
        }
        Write-Output $line
    }

    Write-Output ("opaque pixels: " + $nonTransparent)
    $bmp.Dispose()
}

$icon = [System.Drawing.Bitmap]::FromFile('E:\workspace\Zcode\mcmode\src\main\resources\assets\sephiria\icon.png')
Write-Output ("=== icon.png " + $icon.Width + "x" + $icon.Height + " " + $icon.PixelFormat + " ===")
$icon.Dispose()
