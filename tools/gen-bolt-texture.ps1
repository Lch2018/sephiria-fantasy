# Generate the crossbow bolt icon (16x16 pixel art) in the mod's palette.
# ASCII-only on purpose.
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$out = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\textures\item\crossbow_bolt.png'

# H = steel head, S = wooden shaft, W = white fletching, . = transparent
$art = @(
    '................',
    '.............HH.',
    '............HHH.',
    '...........HHH..',
    '..........HHS...',
    '.........SSS....',
    '........SSS.....',
    '.......SSS......',
    '......SSS.......',
    '.....SSS........',
    '....SSS.........',
    '...WSS..........',
    '..WW............',
    '.WW.............',
    '.W..............',
    '................'
)

$palette = @{
    'H' = [System.Drawing.Color]::FromArgb(255, 0xBD, 0xBE, 0xCF)
    'S' = [System.Drawing.Color]::FromArgb(255, 0x70, 0x50, 0x52)
    'W' = [System.Drawing.Color]::FromArgb(255, 0xFF, 0xFF, 0xFF)
}

$size = $art.Count
$bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

for ($y = 0; $y -lt $size; $y++) {
    $row = $art[$y]
    for ($x = 0; $x -lt $size; $x++) {
        $c = $row[$x]
        if ($palette.ContainsKey([string]$c)) {
            $bmp.SetPixel($x, $y, $palette[[string]$c])
        }
    }
}
$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output ('wrote ' + $out)
