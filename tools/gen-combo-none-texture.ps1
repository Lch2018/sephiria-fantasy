# Generate the "no combo" filter icon for the wish pool picker (16x16, textures/gui/combo_none.png).
#
# The wish-pool picker filters artifacts by combo and draws one 16x16 icon button per combo; the
# "Book" artifact has no combo, so it gets a thirteenth cell whose icon is this little "no" disc
# (a grey ring with a slash - reads as "no combo" at 16 px). The other combo icons are imported
# from the user's art (tools/import-artifact-icons.ps1); this one has no source image, so it is
# drawn here.
#
# Output : src/main/resources/assets/sephiria_fantasy/textures/gui/combo_none.png
# Preview: .preview/combo_none_preview.png (8x, over a mid grey)
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$guiDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\textures\gui'
$previewDir = Join-Path $root '.preview'

# colours: a soft grey ring, a slightly brighter slash, both with a dark outline so the icon reads
# on the picker's dark boxes as well as on the lighter hover highlight
$RING = [System.Drawing.Color]::FromArgb(255, 0xA8, 0xA8, 0xB6)
$SLASH = [System.Drawing.Color]::FromArgb(255, 0xD8, 0xD8, 0xE4)
$OUTLINE = [System.Drawing.Color]::FromArgb(255, 0x20, 0x20, 0x28)

New-Item -ItemType Directory -Force -Path $guiDir, $previewDir | Out-Null

$size = 16
$bmp = New-Object System.Drawing.Bitmap($size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb))
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
$g.Clear([System.Drawing.Color]::FromArgb(0, 0, 0, 0))

$cx = 7.5
$cy = 7.5
$outer = 6.4
$inner = 4.6

# ring: thick outline circle, drawn as a filled disc minus the inner disc
$outerBrush = New-Object System.Drawing.SolidBrush($RING)
$outlineBrush = New-Object System.Drawing.SolidBrush($OUTLINE)
$clearBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(0, 0, 0, 0))

# outline pass (1 px wider both ways)
$g.FillEllipse($outlineBrush, ($cx - $outer - 1), ($cy - $outer - 1), (2 * ($outer + 1)), (2 * ($outer + 1)))
$g.FillEllipse($clearBrush, ($cx - $inner + 1), ($cy - $inner + 1), (2 * ($inner - 1)), (2 * ($inner - 1)))
# body pass
$g.FillEllipse($outerBrush, ($cx - $outer), ($cy - $outer), (2 * $outer), (2 * $outer))
$g.FillEllipse($clearBrush, ($cx - $inner), ($cy - $inner), (2 * $inner), (2 * $inner))

# slash: bottom-left to top-right, 2 px thick, with the same dark outline
$pen = New-Object System.Drawing.Pen($OUTLINE, 4.0)
$g.DrawLine($pen, 3.6, 12.4, 12.4, 3.6)
$pen.Dispose()
$pen = New-Object System.Drawing.Pen($SLASH, 2.0)
$g.DrawLine($pen, 3.6, 12.4, 12.4, 3.6)
$pen.Dispose()

$g.Dispose()
$outerBrush.Dispose()
$outlineBrush.Dispose()
$clearBrush.Dispose()

$outFile = Join-Path $guiDir 'combo_none.png'
$bmp.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
Write-Output ('wrote ' + $outFile)

# preview sheet: 8x over a mid grey
$scale = 8
$cell = $size * $scale
$sheet = New-Object System.Drawing.Bitmap($cell, $cell, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb))
$sg = [System.Drawing.Graphics]::FromImage($sheet)
$sg.Clear([System.Drawing.Color]::FromArgb(255, 108, 108, 108))
$sg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$sg.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$sg.DrawImage($bmp, 0, 0, $cell, $cell)
$sg.Dispose()
$bmp.Dispose()
$sheetPath = Join-Path $previewDir 'combo_none_preview.png'
$sheet.Save($sheetPath, [System.Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()

Write-Output ('preview ' + $sheetPath)
