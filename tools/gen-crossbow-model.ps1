# Rebuild the colossal crossbow's game files from the user's Blockbench model.
#
# Inputs:
#   models/colossal_crossbow.bbmodel  - the authored model (source of truth)
#   a Blockbench "Java Block/Item Model" export of the open project
#     (MCP: export_model codec_id=java_block, path=tmp_re/bb_export.json)
#     Blockbench performs the pixel-UV -> 0..16 UV and euler -> axis-angle conversions,
#     so nothing is re-derived by hand here.
#
# Outputs:
#   src/main/resources/assets/sephiria_fantasy/models/item/colossal_crossbow_in_hand.json
#   src/main/resources/assets/sephiria_fantasy/textures/item/colossal_crossbow_3d.png
#
# Display transforms: the model is authored lying flat (muzzle -z, top +y), which already
# is the correct held orientation, so only a placement offset is applied: the grip centre
# in model space is moved onto the hand origin. No invented rotation.
#
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

param(
    [string]$ExportPath = (Join-Path (Split-Path -Parent $PSScriptRoot) 'tmp_re\bb_export.json')
)

$root = Split-Path -Parent $PSScriptRoot
$itemDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\models\item'
$texDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\textures\item'
$bbPath = Join-Path $root 'models\colossal_crossbow.bbmodel'

$inv = [System.Globalization.CultureInfo]::InvariantCulture

function FmtNum($v) {
    $r = [Math]::Round([double]$v, 5)
    if ($r -eq [Math]::Floor($r)) { return ([long]$r).ToString($inv) }
    return $r.ToString('0.#####', $inv)
}
function FmtNums($arr) {
    $parts = @()
    foreach ($v in $arr) { $parts += (FmtNum $v) }
    return '[' + ($parts -join ', ') + ']'
}

# ---- texture: pull the embedded PNG out of the bbmodel ----
$bb = [System.IO.File]::ReadAllText($bbPath, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
$tex = @($bb.textures)[0]
$src = [string]$tex.source
if ($src -notlike 'data:image/png;base64,*') { Write-Output 'ABORT: bbmodel texture is not an embedded png'; exit 1 }
$b64 = $src.Substring('data:image/png;base64,'.Length)
$bytes = [Convert]::FromBase64String($b64)
[System.IO.File]::WriteAllBytes((Join-Path $texDir 'colossal_crossbow_3d.png'), $bytes)
Write-Output ('texture written: colossal_crossbow_3d.png (' + $bytes.Length + ' bytes, ' + $tex.width + 'x' + $tex.height + ')')

# ---- model: consume the Blockbench export ----
if (-not (Test-Path $ExportPath)) { Write-Output ('ABORT: missing export ' + $ExportPath); exit 1 }
$export = [System.IO.File]::ReadAllText($ExportPath, [System.Text.Encoding]::UTF8) | ConvertFrom-Json

# placement: grip centre (from the authored model) onto the hand origin
$grip = @(8.0, 6.51, 10.31)
$fpScale = 0.68
$tpScale = 0.9

$lines = @()
foreach ($el in @($export.elements)) {
    $parts = @('{ "from": ' + (FmtNums @($el.from)) + ', "to": ' + (FmtNums @($el.to)))
    if ($el.rotation) {
        $ro = FmtNums @($el.rotation.origin)
        if ($el.rotation.axis) {
            $parts += ', "rotation": { "origin": ' + $ro + ', "axis": "' + $el.rotation.axis + '", "angle": ' + (FmtNum $el.rotation.angle) + ' }'
        } else {
            # euler form, with Blockbench's tiny float noise cleaned off
            $rx = [Math]::Round([double]$el.rotation.x, 4); $ry = [Math]::Round([double]$el.rotation.y, 4); $rz = [Math]::Round([double]$el.rotation.z, 4)
            $parts += ', "rotation": { "origin": ' + $ro + ', "x": ' + (FmtNum $rx) + ', "y": ' + (FmtNum $ry) + ', "z": ' + (FmtNum $rz) + ' }'
        }
    }
    $faceTexts = @()
    foreach ($fk in @('north', 'east', 'south', 'west', 'up', 'down')) {
        $face = $el.faces.$fk
        if ($null -eq $face) { continue }
        $faceTexts += '"' + $fk + '": { "texture": "' + $face.texture + '", "uv": ' + (FmtNums @($face.uv)) + ' }'
    }
    $parts += ', "faces": { ' + ($faceTexts -join ', ') + ' } }'
    $lines += ("`t`t" + ($parts -join ''))
}

# display transforms: taken verbatim from the model's own Display settings (Blockbench's
# Display tab preview is what the author tuned against), mirrored onto the left-hand slots
# when a slot was left unset.
function Get-SlotTransform($slot) {
    $rot = @(0.0, 0.0, 0.0); $tr = @(0.0, 0.0, 0.0); $sc = @(1.0, 1.0, 1.0)
    if ($slot) {
        if ($slot.rotation) { $rot = @($slot.rotation) }
        if ($slot.translation) { $tr = @($slot.translation) }
        if ($slot.scale) { $sc = @($slot.scale) }
    }
    return @{ rot = $rot; tr = $tr; sc = $sc }
}
function Mirror-SlotTransform($t) {
    $r0 = [double]($t.rot[0]); $r1 = [double]($t.rot[1]); $r2 = [double]($t.rot[2])
    $p0 = [double]($t.tr[0]); $p1 = [double]($t.tr[1]); $p2 = [double]($t.tr[2])
    $mr1 = $r1 * -1
    $mr2 = $r2 * -1
    $mp0 = $p0 * -1
    return @{
        rot = @($r0, $mr1, $mr2)
        tr  = @($mp0, $p1, $p2)
        sc  = $t.sc
    }
}

$d = $export.display
$fpR = Get-SlotTransform $d.firstperson_righthand
$fpL0 = Get-SlotTransform $d.firstperson_lefthand
$fpL = if ($d.firstperson_lefthand) { $fpL0 } else { Mirror-SlotTransform $fpR }
$tpR = Get-SlotTransform $d.thirdperson_righthand
$tpL0 = Get-SlotTransform $d.thirdperson_lefthand
$tpL = if ($d.thirdperson_lefthand) { $tpL0 } else { Mirror-SlotTransform $tpR }

$displayLines = @()
foreach ($entry in @(
        @{ name = 'thirdperson_righthand'; t = $tpR },
        @{ name = 'thirdperson_lefthand'; t = $tpL },
        @{ name = 'firstperson_righthand'; t = $fpR },
        @{ name = 'firstperson_lefthand'; t = $fpL })) {
    $t = $entry.t
    $displayLines += ("`t`t`"" + $entry.name + "`": { `"rotation`": " + (FmtNums $t.rot) + ", `"translation`": " + (FmtNums $t.tr) + ", `"scale`": " + (FmtNums $t.sc) + " }")
}

$text = @()
$text += '{'
$text += "`t`"gui_light`": `"front`","
$text += "`t`"ambientocclusion`": false,"
$text += "`t`"textures`": {"
$text += "`t`t`"0`": `"sephiria_fantasy:item/colossal_crossbow_3d`","
$text += "`t`t`"particle`": `"sephiria_fantasy:item/colossal_crossbow_3d`""
$text += "`t},"
$text += "`t`"display`": {"
$text += ($displayLines -join ",`n")
$text += "`t},"
$text += "`t`"elements`": ["
$text += ($lines -join ",`n")
$text += "`t]"
$text += '}'

$outPath = Join-Path $itemDir 'colossal_crossbow_in_hand.json'
$final = ($text -join "`n") + "`n"
$check = $final | ConvertFrom-Json
if ((@($check.elements)).Count -ne (@($export.elements)).Count) { Write-Output 'ABORT: element count mismatch'; exit 1 }
[System.IO.File]::WriteAllText($outPath, $final, (New-Object System.Text.UTF8Encoding($false)))
Write-Output ('model written: colossal_crossbow_in_hand.json (' + (@($check.elements).Count) + ' elements)')
Write-Output ('  display firstperson_righthand: ' + ($check.display.firstperson_righthand | ConvertTo-Json -Compress))
