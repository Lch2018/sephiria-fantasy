# 用 System.Drawing 按 16x16 像素图生成 SEPHIRIA 的武器贴图与 mod 图标。
# 运行：powershell -NoProfile -ExecutionPolicy Bypass -File tools\gen-textures.ps1
# 只依赖 .NET Framework 自带的 System.Drawing，不需要 Python / PIL。

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$texDir = Join-Path $root 'src\main\resources\assets\sephiria\textures\item'
$assetDir = Join-Path $root 'src\main\resources\assets\sephiria'
$previewDir = Join-Path $root '.preview'
New-Item -ItemType Directory -Force -Path $texDir, $assetDir, $previewDir | Out-Null

# 调色板：字符 -> ARGB。
# 必须用大小写敏感的字典：PowerShell 的 @{} 默认忽略大小写，'B' 与 'b' 会撞键。
$palette = New-Object 'System.Collections.Generic.Dictionary[string,int[]]' -ArgumentList ([System.StringComparer]::Ordinal)
$palette['B'] = @(255, 233, 233, 233)  # 刃·亮
$palette['b'] = @(255, 168, 168, 168)  # 刃·暗
$palette['d'] = @(255, 120, 120, 120)  # 刃·更暗
$palette['H'] = @(255, 122, 75, 39)    # 握柄·亮
$palette['h'] = @(255, 78, 47, 23)     # 握柄·暗
$palette['G'] = @(255, 216, 169, 60)   # 护手/金属饰·金
$palette['g'] = @(255, 168, 124, 34)   # 护手·暗金
$palette['R'] = @(255, 160, 113, 60)   # 木·亮
$palette['r'] = @(255, 110, 74, 37)    # 木·暗
$palette['W'] = @(255, 242, 242, 242)  # 弓弦
$palette['S'] = @(255, 138, 138, 138)  # 金属·亮
$palette['s'] = @(255, 85, 85, 85)     # 金属·暗

# 像素图：每张 16 行、每行 16 字符，'.' 为透明
$sprites = [ordered]@{}

$sprites['default_sword_and_shield'] = @(
    '.......B........',
    '......bB........',
    '......bB........',
    '......bB........',
    '......bB........',
    '......bB........',
    '......bB........',
    '......bB........',
    '.....GGGGGG.....',
    '....GGGGGGGG....',
    '....GGGGGGGG....',
    '.....GGGGGG.....',
    '......HH........',
    '......HH........',
    '......HH........',
    '.....gGGg.......'
)

$sprites['steel_greatsword'] = @(
    '......B.........',
    '.....bBB........',
    '.....bBB........',
    '.....bBB........',
    '.....bBB........',
    '.....bBB........',
    '.....bBB........',
    '.....bBB........',
    '.....bBB........',
    '.....bBB........',
    '..GGGGGGGGGG....',
    '.....HH.........',
    '.....HH.........',
    '.....HH.........',
    '.....HH.........',
    '....gGGGg.......'
)

$sprites['dagger'] = @(
    '................',
    '................',
    '......B.........',
    '.....bB.........',
    '.....bB.........',
    '.....bB.........',
    '.....bB.........',
    '...GGGGGGG......',
    '.....HH.........',
    '.....HH.........',
    '.....HH.........',
    '.....HH.........',
    '....gGGg........',
    '................',
    '................',
    '................'
)

$sprites['colossal_crossbow'] = @(
    '................',
    '................',
    '..SWWWWWWWWWWS..',
    '.S............S.',
    '.S............S.',
    '..S..........S..',
    '...SS......SS...',
    '.....SSSSSS.....',
    '.......SS.......',
    '.......SS.......',
    '......sSSs......',
    '.......SS.......',
    '.......SS.......',
    '.......SS.......',
    '......sSSs......',
    '................'
)

$sprites['blade'] = @(
    '........BB......',
    '........BB......',
    '.......BB.......',
    '.......BB.......',
    '......BB........',
    '......BB........',
    '.....BB.........',
    '.....BB.........',
    '....BB..........',
    '...GGGGG........',
    '....HH..........',
    '....HH..........',
    '...HH...........',
    '...HH...........',
    '..HH............',
    '..HH............'
)

$sprites['quarterstaff'] = @(
    '.......ss.......',
    '.......SS.......',
    '.......rR.......',
    '.......rR.......',
    '.......rR.......',
    '.......rR.......',
    '.......rR.......',
    '.......ss.......',
    '.......SS.......',
    '.......rR.......',
    '.......rR.......',
    '.......rR.......',
    '.......rR.......',
    '.......rR.......',
    '.......SS.......',
    '.......ss.......'
)

function New-SpriteBitmap {
    param([string[]]$Map, [string]$Name)

    if ($Map.Count -ne 16) { throw "$Name : 需要 16 行，实际 $($Map.Count) 行" }

    $bmp = New-Object System.Drawing.Bitmap(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

    for ($y = 0; $y -lt 16; $y++) {
        $row = $Map[$y]
        if ($row.Length -ne 16) { throw "$Name : 第 $y 行需要 16 字符，实际 $($row.Length) 字符" }

        for ($x = 0; $x -lt 16; $x++) {
            $ch = $row[$x]
            if ($ch -eq '.') { continue }
            if (-not $palette.ContainsKey([string]$ch)) { throw "$Name : 未知颜色字符 '$ch'（第 $y 行第 $x 列）" }

            $c = $palette[[string]$ch]
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($c[0], $c[1], $c[2], $c[3]))
        }
    }

    return $bmp
}

function New-ScaledBitmap {
    param([System.Drawing.Bitmap]$Source, [int]$Scale)

    $w = $Source.Width * $Scale
    $h = $Source.Height * $Scale
    $bmp = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $g.DrawImage($Source, (New-Object System.Drawing.Rectangle(0, 0, $w, $h)))
    $g.Dispose()
    return $bmp
}

# 1) 物品贴图
$bitmaps = [ordered]@{}
foreach ($name in $sprites.Keys) {
    $bmp = New-SpriteBitmap -Map $sprites[$name] -Name $name
    $bitmaps[$name] = $bmp
    $bmp.Save((Join-Path $texDir "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Host "写入 $texDir\$name.png"
}

# 2) mod 图标 128x128：深色底 + 金边 + 放大的剑
$icon = New-Object System.Drawing.Bitmap(128, 128, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gi = [System.Drawing.Graphics]::FromImage($icon)
$gi.Clear([System.Drawing.Color]::FromArgb(255, 27, 20, 48))
$gi.FillRectangle((New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 42, 33, 69))), 6, 6, 116, 116)
$goldBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 216, 169, 60))
$gi.FillRectangle($goldBrush, 0, 0, 128, 4)
$gi.FillRectangle($goldBrush, 0, 124, 128, 4)
$gi.FillRectangle($goldBrush, 0, 0, 4, 128)
$gi.FillRectangle($goldBrush, 124, 0, 4, 128)
$gi.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$gi.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$gi.DrawImage($bitmaps['default_sword_and_shield'], (New-Object System.Drawing.Rectangle(8, 8, 112, 112)))
$gi.Dispose()
$icon.Save((Join-Path $assetDir 'icon.png'), [System.Drawing.Imaging.ImageFormat]::Png)
Write-Host "写入 $assetDir\icon.png"

# 3) 预览图：六张放大 12 倍横排，方便肉眼检查
$scale = 12
$pad = 8
$cell = 16 * $scale
$pw = $pad + ($cell + $pad) * $bitmaps.Count
$ph = $cell + $pad * 2
$preview = New-Object System.Drawing.Bitmap($pw, $ph, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gp = [System.Drawing.Graphics]::FromImage($preview)
$gp.Clear([System.Drawing.Color]::FromArgb(255, 40, 40, 48))
$gp.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$gp.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$x = $pad
foreach ($name in $bitmaps.Keys) {
    $gp.DrawImage($bitmaps[$name], (New-Object System.Drawing.Rectangle($x, $pad, $cell, $cell)))
    $x += $cell + $pad
}
$gp.Dispose()
$preview.Save((Join-Path $previewDir 'textures_preview.png'), [System.Drawing.Imaging.ImageFormat]::Png)
Write-Host "写入 $previewDir\textures_preview.png"
Write-Host ("顺序: " + ($bitmaps.Keys -join ', '))
