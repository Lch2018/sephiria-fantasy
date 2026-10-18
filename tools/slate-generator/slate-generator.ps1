# 赛菲莉娅 石板生成器（GUI）
#
# 用法：双击同目录的 run.bat（或 powershell -File tools\slate-generator\slate-generator.ps1）。
#
# 界面按用户 2026-10-18 的要求：最上方石板名 / 图标 / 品质 / 说明（自动生成，只读）/ 是否可旋转，
# 中间是 9×9 的「背包画布」（石板在中间，可拖动、可旋转，格子等级跟着一起动），右侧是「+ / -」
# 改选中格的等级，下方是「制作」按钮与 key 区域（每制作一块多一条），最下面是「复制」。
#
# 设计要点（与游戏里的机制一一对应，见 src/main/java/com/sephiria/slate/）：
#   · 画布上的每个数字都是相对石板的偏移 (dx, dy)：dx 右为正、dy <b>下</b>为正；
#   · 「石板自己那一格」不参与效果（游戏里所有石板都不影响自己所在的格子），所以石板格是
#     拖动把手，点它不会选中；
#   · 「旋转」= 逆时针 90°，与游戏里按 R 一致；旋转的是图案本身（等级跟着格子一起转）；
#   · 导出的 key 里 cells 就是「朝向 0」的图案——游戏里放下去默认就是这个样子，按 R 再转；
#   · 「说明」不再是作者写的：游戏里由 PatternSlateItem.effectLines() 按图案自动生成
#     （提高神器等级 / 降低神器等级 / 不可旋转），界面上那一行只是把它照出来给用户看。
#
# 图标：选一张图片（png/jpg/bmp）——**没选图标不让制作**（用户 2026-10-18 的要求，防止做出
# 没有图标的石板）；按「制作」会把它复制进 tools\slate-generator\inbox\，key 里记相对路径；
# 助手（AI）之后用它切 16×16 的贴图（与其它神器图标同一套加工）。
#
# ⚠ 本文件必须保持「带 BOM 的 UTF-8」：Windows PowerShell 5.1 否则按 GBK 读，
#    界面上的中文会变成乱码（同 tools/publish-modrinth.ps1；用 Edit 工具改完记得补 BOM）。

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
[System.Windows.Forms.Application]::EnableVisualStyles()

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$inboxDir = Join-Path $PSScriptRoot 'inbox'
New-Item -ItemType Directory -Force -Path $inboxDir | Out-Null

# ------------------------------------------------------------------ 状态

$CANVAS = 9          # 画布 9×9
$CELL = 34           # 每格像素
$script:cells = [ordered]@{}                              # "dx,dy" -> 增量（非 0 才在里面）
$script:selected = New-Object 'System.Collections.Generic.HashSet[string]'
$script:anchorX = 4                                       # 石板在画布上的位置（仅影响画布上的显示）
$script:anchorY = 4
$script:turns = 0                                         # 这次设计里按了几次「旋转」（只记录）
$script:dragging = $false
$script:iconPath = $null                                  # 用户选的源图（完整路径）
$script:rarity = 'common'
$script:rotatable = $true
$script:keys = New-Object System.Collections.ArrayList     # 已制作石板的 key 块
$script:gridBitmap = $null

$FONT = New-Object System.Drawing.Font('Microsoft YaHei', 9)
$FONT_SMALL = New-Object System.Drawing.Font('Microsoft YaHei', 8)
$FONT_GRID = New-Object System.Drawing.Font('Consolas', 10, [System.Drawing.FontStyle]::Bold)
$FONT_MONO = New-Object System.Drawing.Font('Consolas', 9)

# 品质：界面上的五个按钮与 ArtifactRarity 的 id 一一对应
$RARITIES = @(
    @{ id = 'common';    label = '白'; color = 0xFFFFFF },
    @{ id = 'advanced';  label = '蓝'; color = 0x5B8CFF },
    @{ id = 'rare';      label = '黄'; color = 0xFFD24A },
    @{ id = 'legendary'; label = '红'; color = 0xFF008E },
    @{ id = 'eternal';   label = '粉'; color = 0xFF7FD0 }
)

# ------------------------------------------------------------------ 小工具

function Key-Of([int]$dx, [int]$dy) {
    return "$dx,$dy"
}

function Parse-Key([string]$key) {
    $parts = $key -split ','
    return @([int]$parts[0], [int]$parts[1])
}

function Set-Status([string]$text, [string]$tone) {
    $script:statusLabel.Text = $text

    if ($tone -eq 'warn') {
        $script:statusLabel.ForeColor = [System.Drawing.Color]::FromArgb(255, 230, 170, 60)
    } elseif ($tone -eq 'bad') {
        $script:statusLabel.ForeColor = [System.Drawing.Color]::FromArgb(255, 235, 90, 90)
    } elseif ($tone -eq 'good') {
        $script:statusLabel.ForeColor = [System.Drawing.Color]::FromArgb(255, 110, 210, 110)
    } else {
        $script:statusLabel.ForeColor = [System.Drawing.Color]::FromArgb(255, 210, 210, 215)
    }
}

# ------------------------------------------------------------------ 画布渲染

function Render-Grid {
    $size = $CANVAS * $CELL
    $bmp = New-Object System.Drawing.Bitmap($size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb))
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
    $g.Clear([System.Drawing.Color]::FromArgb(255, 22, 22, 27))

    $boxBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 42, 42, 50))
    $plainPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 74, 74, 82), 1)
    $anchorBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 58, 46, 78))
    $anchorPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 200, 160, 48), 2)
    $selectPen = New-Object System.Drawing.Pen([System.Drawing.Color]::White, 2)
    $posBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 85, 255, 85))
    $negBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 85, 85))
    $anchorFont = New-Object System.Drawing.Font('Microsoft YaHei', 9, [System.Drawing.FontStyle]::Bold)
    $center = New-Object System.Drawing.StringFormat
    $center.Alignment = [System.Drawing.StringAlignment]::Center
    $center.LineAlignment = [System.Drawing.StringAlignment]::Center

    for ($row = 0; $row -lt $CANVAS; $row++) {
        for ($col = 0; $col -lt $CANVAS; $col++) {
            $x = $col * $CELL
            $y = $row * $CELL

            if ($col -eq $script:anchorX -and $row -eq $script:anchorY) {
                $g.FillRectangle($anchorBrush, $x, $y, $CELL, $CELL)
                $g.DrawRectangle($anchorPen, $x + 1, $y + 1, $CELL - 3, $CELL - 3)
                $g.DrawString('石板', $anchorFont, [System.Drawing.Brushes]::White,
                        (New-Object System.Drawing.RectangleF($x, $y, $CELL, $CELL)), $center)
                continue
            }

            $dx = $col - $script:anchorX
            $dy = $row - $script:anchorY
            $key = Key-Of $dx $dy

            $g.FillRectangle($boxBrush, $x, $y, $CELL, $CELL)
            $g.DrawRectangle($plainPen, $x, $y, $CELL - 1, $CELL - 1)

            if ($script:cells.Contains($key)) {
                $delta = [int]$script:cells[$key]
                $text = if ($delta -gt 0) { "+$delta" } else { "$delta" }
                $brush = if ($delta -gt 0) { $posBrush } else { $negBrush }
                $g.DrawString($text, $FONT_GRID, $brush,
                        (New-Object System.Drawing.RectangleF($x, $y, $CELL, $CELL)), $center)
            }

            if ($script:selected.Contains($key)) {
                $g.DrawRectangle($selectPen, $x + 1, $y + 1, $CELL - 3, $CELL - 3)
            }
        }
    }

    $g.Dispose()
    $boxBrush.Dispose()
    $plainPen.Dispose()
    $anchorBrush.Dispose()
    $anchorPen.Dispose()
    $selectPen.Dispose()
    $posBrush.Dispose()
    $negBrush.Dispose()
    $anchorFont.Dispose()
    $center.Dispose()

    $old = $script:gridBitmap
    $script:gridBitmap = $bmp
    $script:gridBox.Image = $bmp

    if ($old -ne $null) { $old.Dispose() }

    Update-Counts
    Update-Description
}

function Update-Counts {
    $outside = 0

    foreach ($key in $script:cells.Keys) {
        $parts = Parse-Key $key
        $col = $script:anchorX + $parts[0]
        $row = $script:anchorY + $parts[1]

        if ($col -lt 0 -or $col -ge $CANVAS -or $row -lt 0 -or $row -ge $CANVAS) { $outside++ }
    }

    $text = "图案 $($script:cells.Count) 格"

    if ($outside -gt 0) { $text += "（画布外 $outside 格，挪一下石板就能看到）" }

    $script:countLabel.Text = $text
}

# 「描述」是自动生成的（用户 2026-10-18：界面里删掉输入框）——与游戏里
# PatternSlateItem.effectLines() 的那几行一一对应：有正增量 =「提高神器等级」（绿）、
# 有负增量 =「降低神器等级」（红）、不可旋转 =「不可旋转」（红）。这里只是把它画出来给用户看。
function Update-Description {
    $boosts = $false
    $lowers = $false

    foreach ($key in $script:cells.Keys) {
        $value = [int]$script:cells[$key]

        if ($value -gt 0) { $boosts = $true }
        if ($value -lt 0) { $lowers = $true }
    }

    $box = $script:descPreview
    $box.Clear()

    $parts = New-Object System.Collections.ArrayList

    if ($boosts) {
        [void]$parts.Add(@{ text = '提高神器等级'; color = [System.Drawing.Color]::FromArgb(255, 85, 255, 85) })
    }

    if ($lowers) {
        [void]$parts.Add(@{ text = '降低神器等级'; color = [System.Drawing.Color]::FromArgb(255, 255, 85, 85) })
    }

    if (-not $script:rotatable) {
        [void]$parts.Add(@{ text = '不可旋转'; color = [System.Drawing.Color]::FromArgb(255, 255, 85, 85) })
    }

    if ($parts.Count -eq 0) {
        $box.SelectionColor = [System.Drawing.Color]::FromArgb(255, 150, 150, 160)
        $box.AppendText('（这个图案不影响任何格子）')
        return
    }

    for ($i = 0; $i -lt $parts.Count; $i++) {
        if ($i -gt 0) {
            $box.SelectionColor = [System.Drawing.Color]::FromArgb(255, 150, 150, 160)
            $box.AppendText('，')
        }

        $box.SelectionColor = $parts[$i].color
        $box.AppendText($parts[$i].text)
    }
}

# ------------------------------------------------------------------ 品质 / 旋转按钮

function Update-RarityButtons {
    foreach ($entry in $script:rarityButtons) {
        if ($entry.id -eq $script:rarity) {
            $entry.button.BackColor = [System.Drawing.Color]::FromArgb($entry.color)
            $entry.button.ForeColor = [System.Drawing.Color]::Black
        } else {
            $entry.button.BackColor = [System.Drawing.SystemColors]::Control
            $entry.button.ForeColor = [System.Drawing.SystemColors]::ControlText
        }
    }
}

function Update-RotateButtons {
    if ($script:rotatable) {
        $script:yesButton.BackColor = [System.Drawing.Color]::FromArgb(255, 60, 170, 80)
        $script:yesButton.ForeColor = [System.Drawing.Color]::White
        $script:noButton.BackColor = [System.Drawing.SystemColors]::Control
        $script:noButton.ForeColor = [System.Drawing.SystemColors]::ControlText
    } else {
        $script:noButton.BackColor = [System.Drawing.Color]::FromArgb(255, 190, 60, 60)
        $script:noButton.ForeColor = [System.Drawing.Color]::White
        $script:yesButton.BackColor = [System.Drawing.SystemColors]::Control
        $script:yesButton.ForeColor = [System.Drawing.SystemColors]::ControlText
    }
}

# ------------------------------------------------------------------ 旋转 / 加 / 减

function Rotate-Pattern {
    # 逆时针 90°： (x, y) -> (y, -x)，与 SlateItem.rotateOffset 同一套
    $next = [ordered]@{}

    foreach ($key in $script:cells.Keys) {
        $parts = Parse-Key $key
        $next[ (Key-Of $parts[1] (-$parts[0])) ] = [int]$script:cells[$key]
    }

    $script:cells = $next

    $nextSelected = New-Object 'System.Collections.Generic.HashSet[string]'

    foreach ($key in $script:selected) {
        $parts = Parse-Key $key
        [void]$nextSelected.Add( (Key-Of $parts[1] (-$parts[0])) )
    }

    $script:selected = $nextSelected
    $script:turns++
    Render-Grid
    Set-Status "已逆时针旋转 90°（游戏里按 R 也是这个方向）" 'good'
}

function Adjust-Selection([int]$delta) {
    if ($script:selected.Count -eq 0) {
        Set-Status '先点几个格子选中（白框），再按 + / -' 'warn'
        return
    }

    foreach ($key in $script:selected) {
        $value = 0

        if ($script:cells.Contains($key)) { $value = [int]$script:cells[$key] }

        $value += $delta

        if ($value -eq 0) {
            $script:cells.Remove($key)
        } else {
            $script:cells[$key] = $value
        }
    }

    Render-Grid

    if ($delta -gt 0) {
        Set-Status "选中 $($script:selected.Count) 格的等级 +1" 'good'
    } else {
        Set-Status "选中 $($script:selected.Count) 格的等级 -1" 'good'
    }
}

# ------------------------------------------------------------------ 图标

function Choose-Icon {
    $dialog = New-Object System.Windows.Forms.OpenFileDialog
    $dialog.Title = '选一张石板图标（png / jpg / bmp）'
    $dialog.Filter = '图片 (*.png;*.jpg;*.jpeg;*.bmp)|*.png;*.jpg;*.jpeg;*.bmp|所有文件 (*.*)|*.*'

    if ($dialog.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK) { return }

    $script:iconPath = $dialog.FileName
    $script:iconLabel.Text = [System.IO.Path]::GetFileName($dialog.FileName)

    try {
        $image = [System.Drawing.Image]::FromFile($dialog.FileName)
        $old = $script:iconPreview.Image
        $script:iconPreview.Image = $image
        if ($old -ne $null) { $old.Dispose() }
    } catch {
        Set-Status "这张图读不出来：$($_.Exception.Message)" 'bad'
    }
}

function Sanitize-Name([string]$name) {
    $bad = [System.IO.Path]::GetInvalidFileNameChars()
    $text = $name

    foreach ($ch in $bad) { $text = $text.Replace($ch, '_') }

    return $text.Trim()
}

# 制作时把源图复制进 inbox，返回相对路径（tools\slate-generator\ 下的相对路径）
function Copy-Icon-To-Inbox([string]$slateName) {
    if (-not $script:iconPath) { return '-' }

    $ext = [System.IO.Path]::GetExtension($script:iconPath)
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $file = $stamp + '_' + (Sanitize-Name $slateName) + $ext
    $target = Join-Path $inboxDir $file

    Copy-Item -LiteralPath $script:iconPath -Destination $target -Force

    return "inbox/$file"
}

# ------------------------------------------------------------------ 制作 / 复制

function Make-Slate {
    $name = $script:nameBox.Text.Trim()

    if ($name -eq '') {
        Set-Status '先填石板名' 'bad'
        return
    }

    if (-not $script:iconPath) {
        Set-Status '先点「图标…」选一张图——没有图标的石板不给制作' 'bad'
        return
    }

    if ($script:cells.Count -eq 0) {
        Set-Status '图案是空的：点几个格子、再用 + / - 给它们定等级' 'bad'
        return
    }

    $iconKey = Copy-Icon-To-Inbox $name

    $cellText = @()

    foreach ($key in $script:cells.Keys) {
        $parts = Parse-Key $key
        $cellText += "$($parts[0]),$($parts[1]),$([int]$script:cells[$key])"
    }

    # v2 没有 desc 字段：描述由游戏里的 effectLines() 自动生成（见上面的 Update-Description）
    $block = @()
    $block += '=== SLATE v2 ==='
    $block += "name: $name"
    $block += "rarity: $($script:rarity)"
    $block += "rotatable: $(if ($script:rotatable) { 'true' } else { 'false' })"
    $block += "icon: $iconKey"
    $block += "turns: $($script:turns)"
    $block += "anchor: $($script:anchorX),$($script:anchorY)"
    $block += "cells: $($cellText -join ' ')"
    $block += '=== END ==='

    [void]$script:keys.Add(($block -join "`r`n"))
    $script:keyBox.Text = $script:keys -join "`r`n`r`n"

    # 提醒：游戏里的背包只有 6 列宽（默认 5 行），图案越界的方向会被裁掉
    $wide = $false

    foreach ($key in $script:cells.Keys) {
        $parts = Parse-Key $key

        if ([Math]::Abs($parts[0]) -gt 2 -or $parts[1] -lt -4 -or $parts[1] -gt 4) { $wide = $true }
    }

    $message = "已记录第 $($script:keys.Count) 块：$name（$($script:cells.Count) 格，图标已收进 inbox）"

    if ($wide) {
        Set-Status ($message + '　⚠ 图案超出背包范围（6 列宽）：放进游戏可能会被裁掉') 'warn'
    } else {
        Set-Status $message 'good'
    }
}

function Clear-Form {
    $script:nameBox.Text = ''
    $script:cells = [ordered]@{}
    $script:selected = New-Object 'System.Collections.Generic.HashSet[string]'
    $script:anchorX = 4
    $script:anchorY = 4
    $script:turns = 0
    $script:iconPath = $null
    $script:iconLabel.Text = '(没有选图标)'

    if ($script:iconPreview.Image -ne $null) {
        $script:iconPreview.Image.Dispose()
        $script:iconPreview.Image = $null
    }

    Render-Grid
    Set-Status '清空了，可以做下一块' ''
}

function Copy-Keys {
    if ($script:keys.Count -eq 0) {
        Set-Status '还没有制作任何石板' 'warn'
        return
    }

    try {
        [System.Windows.Forms.Clipboard]::SetText($script:keyBox.Text)
        Set-Status "已复制 $($script:keys.Count) 条 key 到剪贴板——直接粘给助手就行" 'good'
    } catch {
        Set-Status '复制失败（剪贴板被别的程序占着？）：可以手动框选 key 区里的文字复制' 'bad'
    }
}

# ------------------------------------------------------------------ 界面

$form = New-Object System.Windows.Forms.Form
$form.Text = '赛菲莉娅 石板生成器'
$form.ClientSize = New-Object System.Drawing.Size(548, 748)
$form.StartPosition = 'CenterScreen'
$form.FormBorderStyle = 'FixedSingle'
$form.MaximizeBox = $false
$form.Font = $FONT
$form.BackColor = [System.Drawing.Color]::FromArgb(255, 30, 30, 36)
$form.ForeColor = [System.Drawing.Color]::FromArgb(255, 225, 225, 230)

function Add-Label([string]$text, [int]$x, [int]$y, [int]$w) {
    $label = New-Object System.Windows.Forms.Label
    $label.Text = $text
    $label.Location = New-Object System.Drawing.Point($x, $y)
    $label.Size = New-Object System.Drawing.Size($w, 20)
    $form.Controls.Add($label)
    return $label
}

# --- 第一行：石板名 / 图标 ---
[void](Add-Label '石板名：' 12 17 70)

$script:nameBox = New-Object System.Windows.Forms.TextBox
$script:nameBox.Location = New-Object System.Drawing.Point(84, 14)
$script:nameBox.Size = New-Object System.Drawing.Size(180, 24)
$form.Controls.Add($script:nameBox)

$iconButton = New-Object System.Windows.Forms.Button
$iconButton.Text = '图标…'
$iconButton.Location = New-Object System.Drawing.Point(272, 13)
$iconButton.Size = New-Object System.Drawing.Size(60, 26)
$iconButton.FlatStyle = 'Flat'
$iconButton.Add_Click({ Choose-Icon })
$form.Controls.Add($iconButton)

$script:iconPreview = New-Object System.Windows.Forms.PictureBox
$script:iconPreview.Location = New-Object System.Drawing.Point(338, 10)
$script:iconPreview.Size = New-Object System.Drawing.Size(32, 32)
$script:iconPreview.BorderStyle = 'FixedSingle'
$script:iconPreview.SizeMode = 'Zoom'
$script:iconPreview.BackColor = [System.Drawing.Color]::FromArgb(255, 45, 45, 54)
$form.Controls.Add($script:iconPreview)

$script:iconLabel = New-Object System.Windows.Forms.Label
$script:iconLabel.Text = '(没有选图标)'
$script:iconLabel.Location = New-Object System.Drawing.Point(376, 17)
$script:iconLabel.Size = New-Object System.Drawing.Size(160, 20)
$script:iconLabel.AutoEllipsis = $true
$script:iconLabel.ForeColor = [System.Drawing.Color]::FromArgb(255, 170, 170, 180)
$form.Controls.Add($script:iconLabel)

# --- 第二行：品质 ---
[void](Add-Label '品质：' 12 52 70)

$script:rarityButtons = @()
$rarityX = 84

foreach ($entry in $RARITIES) {
    $button = New-Object System.Windows.Forms.Button
    $button.Text = $entry.label
    $button.Location = New-Object System.Drawing.Point($rarityX, 48)
    $button.Size = New-Object System.Drawing.Size(46, 26)
    $button.FlatStyle = 'Flat'
    $button.Tag = $entry.id
    $button.Add_Click({
        param($sender, $eventArgs)
        $script:rarity = $sender.Tag
        Update-RarityButtons
    })
    $form.Controls.Add($button)
    $script:rarityButtons += @{ id = $entry.id; color = $entry.color; button = $button }
    $rarityX += 52
}

Update-RarityButtons

# --- 第三行：描述（自动生成，不能手填；显示的就是游戏里那几行）---
[void](Add-Label '说明（自动）：' 12 88 70)

$script:descPreview = New-Object System.Windows.Forms.RichTextBox
$script:descPreview.Location = New-Object System.Drawing.Point(84, 83)
$script:descPreview.Size = New-Object System.Drawing.Size(452, 24)
$script:descPreview.ReadOnly = $true
$script:descPreview.BorderStyle = 'None'
$script:descPreview.BackColor = $form.BackColor
$script:descPreview.ForeColor = [System.Drawing.Color]::FromArgb(255, 225, 225, 230)
$script:descPreview.Font = $FONT
$script:descPreview.TabStop = $false
$form.Controls.Add($script:descPreview)

# --- 第四行：是否可以旋转 ---
[void](Add-Label '是否可以旋转：' 12 122 100)

$script:yesButton = New-Object System.Windows.Forms.Button
$script:yesButton.Text = '是'
$script:yesButton.Location = New-Object System.Drawing.Point(116, 118)
$script:yesButton.Size = New-Object System.Drawing.Size(50, 26)
$script:yesButton.FlatStyle = 'Flat'
$script:yesButton.Add_Click({ $script:rotatable = $true; Update-RotateButtons; Update-Description })
$form.Controls.Add($script:yesButton)

$script:noButton = New-Object System.Windows.Forms.Button
$script:noButton.Text = '否'
$script:noButton.Location = New-Object System.Drawing.Point(170, 118)
$script:noButton.Size = New-Object System.Drawing.Size(50, 26)
$script:noButton.FlatStyle = 'Flat'
$script:noButton.Add_Click({ $script:rotatable = $false; Update-RotateButtons; Update-Description })
$form.Controls.Add($script:noButton)

Update-RotateButtons

$hintLabel = New-Object System.Windows.Forms.Label
$hintLabel.Text = '（可旋转 = 游戏里鼠标放石板上按 R，逆时针 90°）'
$hintLabel.Location = New-Object System.Drawing.Point(232, 122)
$hintLabel.Size = New-Object System.Drawing.Size(300, 20)
$hintLabel.ForeColor = [System.Drawing.Color]::FromArgb(255, 150, 150, 160)
$hintLabel.Font = $FONT_SMALL
$form.Controls.Add($hintLabel)

# --- 画布 ---
$script:gridBox = New-Object System.Windows.Forms.PictureBox
$script:gridBox.Location = New-Object System.Drawing.Point(12, 156)
$script:gridBox.Size = New-Object System.Drawing.Size(($CANVAS * $CELL), ($CANVAS * $CELL))
$script:gridBox.BackColor = [System.Drawing.Color]::FromArgb(255, 22, 22, 27)

$script:gridBox.Add_MouseDown({
    param($sender, $eventArgs)

    if ($eventArgs.Button -ne [System.Windows.Forms.MouseButtons]::Left) { return }

    $col = [int][Math]::Floor($eventArgs.X / $CELL)
    $row = [int][Math]::Floor($eventArgs.Y / $CELL)

    if ($col -lt 0 -or $col -ge $CANVAS -or $row -lt 0 -or $row -ge $CANVAS) { return }

    if ($col -eq $script:anchorX -and $row -eq $script:anchorY) {
        $script:dragging = $true
        return
    }

    $key = Key-Of ($col - $script:anchorX) ($row - $script:anchorY)

    if ($script:selected.Contains($key)) {
        [void]$script:selected.Remove($key)
    } else {
        [void]$script:selected.Add($key)
    }

    Render-Grid
})

$script:gridBox.Add_MouseMove({
    param($sender, $eventArgs)

    if (-not $script:dragging) { return }

    $col = [int][Math]::Floor($eventArgs.X / $CELL)
    $row = [int][Math]::Floor($eventArgs.Y / $CELL)
    $col = [Math]::Max(0, [Math]::Min($CANVAS - 1, $col))
    $row = [Math]::Max(0, [Math]::Min($CANVAS - 1, $row))

    if ($col -eq $script:anchorX -and $row -eq $script:anchorY) { return }

    $script:anchorX = $col
    $script:anchorY = $row
    Render-Grid
})

$script:gridBox.Add_MouseUp({
    param($sender, $eventArgs)
    $script:dragging = $false
})

$form.Controls.Add($script:gridBox)

# --- 画布右侧：旋转 / 加减 / 清空选择 ---
$rotButton = New-Object System.Windows.Forms.Button
$rotButton.Text = '旋转 90°'
$rotButton.Location = New-Object System.Drawing.Point(332, 156)
$rotButton.Size = New-Object System.Drawing.Size(96, 30)
$rotButton.FlatStyle = 'Flat'
$rotButton.Add_Click({ Rotate-Pattern })
$form.Controls.Add($rotButton)

$plusButton = New-Object System.Windows.Forms.Button
$plusButton.Text = '+'
$plusButton.Font = New-Object System.Drawing.Font('Consolas', 14, [System.Drawing.FontStyle]::Bold)
$plusButton.Location = New-Object System.Drawing.Point(332, 194)
$plusButton.Size = New-Object System.Drawing.Size(46, 40)
$plusButton.FlatStyle = 'Flat'
$plusButton.Add_Click({ Adjust-Selection 1 })
$form.Controls.Add($plusButton)

$minusButton = New-Object System.Windows.Forms.Button
$minusButton.Text = '-'
$minusButton.Font = New-Object System.Drawing.Font('Consolas', 14, [System.Drawing.FontStyle]::Bold)
$minusButton.Location = New-Object System.Drawing.Point(382, 194)
$minusButton.Size = New-Object System.Drawing.Size(46, 40)
$minusButton.FlatStyle = 'Flat'
$minusButton.Add_Click({ Adjust-Selection -1 })
$form.Controls.Add($minusButton)

$clearSelButton = New-Object System.Windows.Forms.Button
$clearSelButton.Text = '清空选择'
$clearSelButton.Location = New-Object System.Drawing.Point(332, 242)
$clearSelButton.Size = New-Object System.Drawing.Size(96, 26)
$clearSelButton.FlatStyle = 'Flat'
$clearSelButton.Add_Click({
    $script:selected.Clear()
    Render-Grid
})
$form.Controls.Add($clearSelButton)

$script:countLabel = New-Object System.Windows.Forms.Label
$script:countLabel.Text = '图案 0 格'
$script:countLabel.Location = New-Object System.Drawing.Point(332, 276)
$script:countLabel.Size = New-Object System.Drawing.Size(120, 20)
$script:countLabel.ForeColor = [System.Drawing.Color]::FromArgb(255, 170, 170, 180)
$form.Controls.Add($script:countLabel)

$helpLabel = New-Object System.Windows.Forms.Label
$helpLabel.Text = "左键点格子 = 选中 / 取消`r`n拖石板 = 移动整张图案`r`n+ / - = 改选中格的等级`r`n等级归零 = 这一格取消`r`n`r`n游戏里背包只有 6 列宽`r`n（默认 5 行），图案尽量`r`n贴着石板放。"
$helpLabel.Location = New-Object System.Drawing.Point(332, 306)
$helpLabel.Size = New-Object System.Drawing.Size(120, 150)
$helpLabel.ForeColor = [System.Drawing.Color]::FromArgb(255, 150, 150, 160)
$helpLabel.Font = $FONT_SMALL
$form.Controls.Add($helpLabel)

# --- 制作 / 状态 ---
$makeButton = New-Object System.Windows.Forms.Button
$makeButton.Text = '制作'
$makeButton.Location = New-Object System.Drawing.Point(12, 474)
$makeButton.Size = New-Object System.Drawing.Size(96, 32)
$makeButton.FlatStyle = 'Flat'
$makeButton.BackColor = [System.Drawing.Color]::FromArgb(255, 60, 110, 180)
$makeButton.ForeColor = [System.Drawing.Color]::White
$makeButton.Add_Click({ Make-Slate })
$form.Controls.Add($makeButton)

$clearButton = New-Object System.Windows.Forms.Button
$clearButton.Text = '清空（做下一块）'
$clearButton.Location = New-Object System.Drawing.Point(116, 474)
$clearButton.Size = New-Object System.Drawing.Size(140, 32)
$clearButton.FlatStyle = 'Flat'
$clearButton.Add_Click({ Clear-Form })
$form.Controls.Add($clearButton)

$script:statusLabel = New-Object System.Windows.Forms.Label
$script:statusLabel.Text = '填好名字 / 描述，点格子选范围，再用 + / - 定等级'
$script:statusLabel.Location = New-Object System.Drawing.Point(266, 480)
$script:statusLabel.Size = New-Object System.Drawing.Size(280, 40)
$script:statusLabel.ForeColor = [System.Drawing.Color]::FromArgb(255, 210, 210, 215)
$script:statusLabel.Font = $FONT_SMALL
$form.Controls.Add($script:statusLabel)

# --- key 区 ---
[void](Add-Label '石板 key（做完一批点下面的「复制」，整段发给助手）：' 12 514 500)

$script:keyBox = New-Object System.Windows.Forms.TextBox
$script:keyBox.Location = New-Object System.Drawing.Point(12, 536)
$script:keyBox.Size = New-Object System.Drawing.Size(524, 160)
$script:keyBox.Multiline = $true
$script:keyBox.ReadOnly = $true
$script:keyBox.ScrollBars = 'Vertical'
$script:keyBox.WordWrap = $false
$script:keyBox.Font = $FONT_MONO
$script:keyBox.BackColor = [System.Drawing.Color]::FromArgb(255, 45, 45, 54)
$script:keyBox.ForeColor = [System.Drawing.Color]::FromArgb(255, 220, 220, 230)
$form.Controls.Add($script:keyBox)

# --- 复制 ---
$copyButton = New-Object System.Windows.Forms.Button
$copyButton.Text = '复制'
$copyButton.Location = New-Object System.Drawing.Point(12, 704)
$copyButton.Size = New-Object System.Drawing.Size(96, 32)
$copyButton.FlatStyle = 'Flat'
$copyButton.BackColor = [System.Drawing.Color]::FromArgb(255, 60, 140, 90)
$copyButton.ForeColor = [System.Drawing.Color]::White
$copyButton.Add_Click({ Copy-Keys })
$form.Controls.Add($copyButton)

$pathLabel = New-Object System.Windows.Forms.Label
$pathLabel.Text = "图标归档：$inboxDir"
$pathLabel.Location = New-Object System.Drawing.Point(116, 710)
$pathLabel.Size = New-Object System.Drawing.Size(420, 20)
$pathLabel.ForeColor = [System.Drawing.Color]::FromArgb(255, 140, 140, 150)
$pathLabel.Font = $FONT_SMALL
$form.Controls.Add($pathLabel)

Render-Grid

# 自测模式（开发用，界面不会弹出）：SLATE_GEN_TEST=1 时灌一组假数据、走一遍「旋转 → 三道校验 →
# 制作」的流程，把画布渲染成 .preview\slate_generator_test.png 并打印 key 后退出。
# 助手在没有人的时候用它检查绘制、校验与 key 生成（正常运行不受影响）。
if ($env:SLATE_GEN_TEST -eq '1') {
    $testDir = Join-Path $root '.preview'
    New-Item -ItemType Directory -Force -Path $testDir | Out-Null

    # 造一张 75×75 的假图标（板 + 主体，和真图标同一套加工），测试用的，不往 inbox 里留东西
    $testIcon = Join-Path $testDir 'slate_generator_test_icon.png'
    $iconBitmap = New-Object System.Drawing.Bitmap(75, 75, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb))
    $iconGraphics = [System.Drawing.Graphics]::FromImage($iconBitmap)
    $iconGraphics.Clear([System.Drawing.Color]::FromArgb(255, 34, 32, 52))
    $iconBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 200, 200, 220))
    $iconGraphics.FillRectangle($iconBrush, 18, 18, 40, 40)
    $iconGraphics.Dispose()
    $iconBrush.Dispose()
    $iconBitmap.Save($testIcon, [System.Drawing.Imaging.ImageFormat]::Png)
    $iconBitmap.Dispose()

    # 校验三条：没名字 / 没图标 / 空图案，各自都该被拦下
    Make-Slate
    $script:nameBox.Text = '测试石板'
    Make-Slate
    $script:iconPath = $testIcon
    $script:iconLabel.Text = [System.IO.Path]::GetFileName($testIcon)
    Make-Slate

    $script:cells['0,-1'] = 1
    $script:cells['-1,0'] = 2
    $script:cells['1,1'] = -1
    [void]$script:selected.Add('0,-1')
    $script:rarity = 'rare'
    $script:rotatable = $false

    Rotate-Pattern
    Render-Grid

    $testFile = Join-Path $testDir 'slate_generator_test.png'
    $script:gridBitmap.Save($testFile, [System.Drawing.Imaging.ImageFormat]::Png)

    Make-Slate

    Write-Host '--- 画布预览 ---'
    Write-Host $testFile
    Write-Host '--- 自动说明（界面上那行，应与游戏提示框一致）---'
    Write-Host $script:descPreview.Text
    Write-Host '--- key ---'
    Write-Host ($script:keys -join "`r`n")

    # 清掉测试留下的 inbox 副本
    Get-ChildItem -Path $inboxDir -Filter '*测试石板*' -ErrorAction SilentlyContinue | Remove-Item -Force

    $form.Dispose()
    return
}

Write-Host '石板生成器已打开。做完一批记得点「复制」，把 key 整段发给助手。'
[void]$form.ShowDialog()

$form.Dispose()
