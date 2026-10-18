# Parse the slate keys produced by tools/slate-generator/slate-generator.ps1 into a draft JSON.
#
# The user designs slates in the GUI, hits "copy" and pastes the whole key text to the assistant;
# the assistant saves it (tools/slate-generator/inbox/keys.txt), runs this script, fills in the
# fields that need a human (id / nameEn / descEn in tools/slate-generator/out/slates.json) and then
# runs tools/slate-generator/apply-slates.ps1, which writes the mod-side pieces.
#
# Key block format, one block per slate; fields the generator always writes:
#
#   === SLATE v2 ===
#   name:      <chinese name>
#   rarity:    common | advanced | rare | legendary | eternal       (= ArtifactRarity ids)
#   rotatable: true | false
#   icon:      inbox/<file>   or  -           (file lives in tools/slate-generator/inbox/)
#   turns:     <how many times the design was rotated in the GUI; informational>
#   anchor:    <col,row on the 9x9 canvas; informational, NOT used for generation>
#   cells:     dx,dy,delta dx,dy,delta ...    (dx right +, dy DOWN +, relative to the slate)
#   === END ===
#
# v2 (2026-10-18) dropped the desc field: the slate's description is generated in-game by
# PatternSlateItem.effectLines() (raises / lowers artifact level, cannot rotate). A v1 key may
# still carry 'desc:' (the author written flavor) - it is accepted and written to the flavor key.
#
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

param(
    [string]$File = '',
    [string]$Out = ''
)

$ErrorActionPreference = 'Stop'

$generatorDir = $PSScriptRoot
$root = Split-Path -Parent (Split-Path -Parent $generatorDir)

if (-not $File) { $File = Join-Path $generatorDir 'inbox\keys.txt' }
if (-not $Out) { $Out = Join-Path $generatorDir 'out\slates.json' }

if (-not (Test-Path $File)) {
    throw "key file not found: $File (save the text the user pasted as this file first)"
}

$rarities = @('common', 'advanced', 'rare', 'legendary', 'eternal')

function Write-JsonString([string]$value) {
    # the handbook warns against ConvertTo-Json with large Chinese blobs (it hangs); a 10-line
    # escaper keeps this script independent of that, and the escaping rules are trivial here
    $text = $value.Replace('\', '\\').Replace('"', '\"').Replace("`r", '').Replace("`n", '\n')
    return '"' + $text + '"'
}

$lines = [System.IO.File]::ReadAllText($File, [System.Text.Encoding]::UTF8) -split "`n"
$slates = New-Object System.Collections.ArrayList
$current = $null
$problems = New-Object System.Collections.ArrayList
$blockNumber = 0

foreach ($rawLine in $lines) {
    $line = $rawLine.TrimEnd("`r").Trim()

    if ($line -eq '') { continue }

    if ($line -match '^===\s*SLATE') {
        if ($current) { $problems.Add("block $blockNumber was not closed with === END ===") }
        $blockNumber++
        $current = @{ number = $blockNumber; cells = New-Object System.Collections.ArrayList }
        continue
    }

    if ($line -match '^===\s*END') {
        if (-not $current) { $problems.Add('=== END === without a === SLATE ==='); continue }

        # ---- validate the block ----
        $name = $current['name']
        $desc = $current['desc']

        if (-not $name) { $problems.Add("block $($current.number): no name") }
        # desc 是可选的：v2 的 key 不带它（「描述」改由游戏里的 effectLines 自动生成），
        # 只有旧的 v1 key（作者手写描述）才会带——带上就照旧写进 flavor
        if (-not $desc) { $desc = '' }

        $rarity = $current['rarity']
        if ($rarities -notcontains $rarity) { $problems.Add("block $($current.number): bad rarity '$rarity'") }

        $rotatable = $false
        if ($current['rotatable'] -eq 'true') { $rotatable = $true }
        elseif ($current['rotatable'] -ne 'false') { $problems.Add("block $($current.number): bad rotatable '$($current['rotatable'])'") }

        if ($current['cells'].Count -eq 0) { $problems.Add("block $($current.number): no cells painted") }

        $icon = $current['icon']
        if (-not $icon) { $icon = '-' }

        if ($icon -ne '-') {
            $iconPath = Join-Path $generatorDir ($icon -replace '/', '\')
            if (-not (Test-Path $iconPath)) {
                $problems.Add("block $($current.number): icon file missing ($icon)")
            }
        }

        [void]$slates.Add(@{
            id = ''
            name = $name
            nameEn = ''
            desc = $desc
            descEn = ''
            rarity = $rarity
            rotatable = $rotatable
            icon = $icon
            cells = $current['cells']
            source = $current['number']
        })

        $current = $null
        continue
    }

    if (-not $current) { $problems.Add("stray line outside a block: $line"); continue }

    $at = $line.IndexOf(':')

    if ($at -lt 0) { $problems.Add("block $($current.number): line without ':' -> $line"); continue }

    $key = $line.Substring(0, $at).Trim()
    $value = $line.Substring($at + 1).Trim()

    if ($key -eq 'cells') {
        foreach ($triple in ($value -split '\s+')) {
            if ($triple -eq '') { continue }

            $parts = $triple -split ','

            if ($parts.Count -ne 3) {
                $problems.Add("block $($current.number): bad cell '$triple'")
                continue
            }

            $dx = 0
            $dy = 0
            $delta = 0

            if (-not [int]::TryParse($parts[0], [ref]$dx) -or
                    -not [int]::TryParse($parts[1], [ref]$dy) -or
                    -not [int]::TryParse($parts[2], [ref]$delta)) {
                $problems.Add("block $($current.number): bad cell numbers '$triple'")
                continue
            }

            if ($dx -eq 0 -and $dy -eq 0) {
                # slates never affect their own cell (see Slates.java): drop it, the game would too
                $problems.Add("block $($current.number): cell 0,0 dropped (a slate never affects its own slot)")
                continue
            }

            if ($delta -eq 0) {
                $problems.Add("block $($current.number): cell $dx,$dy has delta 0 - dropped")
                continue
            }

            [void]$current['cells'].Add(@($dx, $dy, $delta))
        }

        continue
    }

    $current[$key] = $value
}

if ($current) { $problems.Add("block $blockNumber was not closed with === END ===") }

# ---- write the draft JSON ----
$json = New-Object System.Collections.ArrayList
[void]$json.Add('[')

for ($i = 0; $i -lt $slates.Count; $i++) {
    $s = $slates[$i]
    $cellText = @()

    foreach ($cell in $s.cells) {
        $cellText += ('[' + $cell[0] + ', ' + $cell[1] + ', ' + $cell[2] + ']')
    }

    $linesOut = New-Object System.Collections.ArrayList
    [void]$linesOut.Add('  {')
    [void]$linesOut.Add('    "id": "",')
    [void]$linesOut.Add('    "name": ' + (Write-JsonString $s.name) + ',')
    [void]$linesOut.Add('    "nameEn": "",')
    [void]$linesOut.Add('    "desc": ' + (Write-JsonString $s.desc) + ',')
    [void]$linesOut.Add('    "descEn": "",')
    [void]$linesOut.Add('    "rarity": ' + (Write-JsonString $s.rarity) + ',')
    [void]$linesOut.Add('    "rotatable": ' + ($s.rotatable.ToString().ToLowerInvariant()) + ',')
    [void]$linesOut.Add('    "icon": ' + (Write-JsonString $s.icon) + ',')
    [void]$linesOut.Add('    "cells": [' + ($cellText -join ', ') + '],')
    [void]$linesOut.Add('    "source": ' + $s.source)
    [void]$linesOut.Add('  }' + $(if ($i -lt $slates.Count - 1) { ',' } else { '' }))

    foreach ($outLine in $linesOut) { [void]$json.Add($outLine) }
}

[void]$json.Add(']')

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Out) | Out-Null
[IO.File]::WriteAllText($Out, (($json -join "`n") + "`n"), (New-Object System.Text.UTF8Encoding($false)))

# ---- report ----
Write-Output ("parsed " + $slates.Count + " slate(s) from " + (Split-Path -Leaf $File))

foreach ($s in $slates) {
    $outside = 0

    foreach ($cell in $s.cells) {
        if ([Math]::Abs($cell[0]) -gt 2 -or $cell[1] -lt -4 -or $cell[1] -gt 4) { $outside++ }
    }

    $note = ''
    if ($outside -gt 0) { $note = '  (' + $outside + ' cell(s) outside a 6-wide backpack)' }

    Write-Output ('  #' + $s.source + '  ' + $s.name + '  [' + $s.rarity + ']  ' + $s.cells.Count + ' cells  icon=' + $s.icon + $note)
}

if ($problems.Count -gt 0) {
    Write-Output ''
    Write-Output 'WARNINGS:'
    foreach ($problem in $problems) { Write-Output ('  ! ' + $problem) }
}

Write-Output ''
Write-Output ('draft written: ' + $Out)
Write-Output 'next: fill id / nameEn / descEn in that file, then run tools/slate-generator/apply-slates.ps1'
