# Build the SEPHIRIA katana: Blockbench project + game models.
#
# Structure (per spec):
#   group 刀身 (blade + tsuba + handle)  and  group 刀鞘 (scabbard)
#   drawn state   : blade up, handle in hand, scabbard absent
#   sheathed state: whole model flipped 180 deg about the grip, scabbard covering the blade,
#                   handle pointing up
#
# The state switch animation ("switch", 0.25 s) is baked into game model frames:
#   frames 0..N : scabbard rises from below while the katana flips around the grip,
#                 ending sheathed. Frame 0 = drawn, frame N = sheathed, so the same frames
#   played backwards give the draw-out animation.
#
# Outputs:
#   models/katana.bbmodel                                  (Blockbench project, opens & previews)
#   textures/item/katana_3d.png                            (16x16 palette)
#   models/item/katana_drawn.json                          (= frame 0)
#   models/item/katana_sheathed.json                       (= frame N)
#   models/item/katana_switch_1..N-1.json                  (in-between frames)
# ASCII-only on purpose.
param(
    [int]$Frames = 5,
    [switch]$GameModels
)

$root = Split-Path -Parent $PSScriptRoot
$itemDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\models\item'
$texDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\textures\item'
$inv = [System.Globalization.CultureInfo]::InvariantCulture

Add-Type -AssemblyName System.Drawing

# ---------------------------------------------------------------- palette texture (16x16)
# each patch is a solid 2x2 area; uv coordinates below point at them
$patches = [ordered]@{
    'blade'   = @(0, 0)   # 0,0
    'edge'    = @(2, 0)   # 2,0
    'handle'  = @(4, 0)   # 4,0
    'wrap'    = @(6, 0)   # 6,0
    'tsuba'   = @(8, 0)   # 8,0
    'sheath'  = @(10, 0)  # 10,0
    'fitting' = @(12, 0)  # 12,0
    'pommel'  = @(14, 0)  # 14,0
}
$colors = @{
    'blade'   = @(0xBD, 0xBE, 0xCF)   # light steel
    'edge'    = @(0xF2, 0xF3, 0xF7)   # near-white edge
    'handle'  = @(0x38, 0x38, 0x52)   # dark navy
    'wrap'    = @(0x25, 0x25, 0x36)   # darker wrap
    'tsuba'   = @(0x5A, 0x5F, 0x7A)   # steel
    'sheath'  = @(0x2E, 0x2E, 0x44)   # deep navy
    'fitting' = @(0x6E, 0x73, 0x8C)   # fitting steel
    'pommel'  = @(0x46, 0x46, 0x60)   # pommel
}
$bmp = New-Object System.Drawing.Bitmap(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
foreach ($k in $patches.Keys) {
    $p = $patches[$k]
    $c = $colors[$k]
    $col = [System.Drawing.Color]::FromArgb(255, $c[0], $c[1], $c[2])
    for ($dy = 0; $dy -lt 2; $dy++) {
        for ($dx = 0; $dx -lt 2; $dx++) {
            $bmp.SetPixel($p[0] + $dx, $p[1] + $dy, $col)
        }
    }
}
$texPath = Join-Path $texDir 'katana_3d.png'
$bmp.Save($texPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output ('texture: katana_3d.png (16x16 palette)')

function Uv($name) {
    $p = $patches[$name]
    return @($p[0], $p[1], $p[0] + 2, $p[1] + 2)
}

# ---------------------------------------------------------------- geometry
$daoName = [string][char]0x5200
$bladeGroup = $daoName + [string][char]0x8EAB      # 刀身
$sheathGroup = $daoName + [string][char]0x9798     # 刀鞘
$edgeName = $daoName + [string][char]0x5203        # 刀刃
$tsubaName = $daoName + [string][char]0x9561       # 刀镡
$handleName = $daoName + [string][char]0x67C4      # 刀柄

$grip = @(8.0, 3.0, 8.0)                            # rotation pivot for the flip

$parts = New-Object System.Collections.Generic.List[object]
function AddPart($name, $group, $f, $t, $patch) {
    $parts.Add(@{ name = $name; group = $group; from = $f; to = $t; patch = $patch })
}

# handle: pommel, grip and two wrap bands (y 0.4 .. 4.6 = in the hand)
AddPart $handleName 'body' @(7.1, 0.4, 7.35) @(8.9, 1.0, 8.65) 'pommel'
AddPart $handleName 'body' @(7.25, 1.0, 7.5) @(8.75, 4.6, 8.5) 'handle'
AddPart $handleName 'body' @(7.2, 2.0, 7.45) @(8.8, 2.35, 8.55) 'wrap'
AddPart $handleName 'body' @(7.2, 3.3, 7.45) @(8.8, 3.65, 8.55) 'wrap'
# tsuba
AddPart $tsubaName 'body' @(6.4, 4.6, 7.1) @(9.6, 5.2, 8.9) 'tsuba'
# blade: six stacked segments with a slight curve towards +x, plus the edge strip
for ($i = 0; $i -lt 6; $i++) {
    $y0 = 5.2 + $i * 1.7
    $y1 = $y0 + 1.7
    $curve = 0.06 * $i
    $narrow = 0.0
    if ($i -eq 5) { $narrow = 0.3 }
    $x0 = 7.35 + $curve + $narrow
    $x1 = 8.65 + $curve
    AddPart $edgeName 'body' @($x0, $y0, 7.6) @($x1, $y1, 8.4) 'blade'
    if ($i -lt 5) {
        $ex = $x0 - 0.18
        AddPart $edgeName 'body' @($ex, $y0, 7.68) @($x0, $y1, 8.32) 'edge'
    }
}
# scabbard: body, mouth and tip (covers the blade from y 4.9 to 16.2)
AddPart $sheathGroup 'sheath' @(7.15, 4.9, 7.45) @(9.0, 15.8, 8.55) 'sheath'
AddPart $sheathGroup 'sheath' @(7.0, 4.6, 7.3) @(9.15, 5.1, 8.7) 'fitting'
AddPart $sheathGroup 'sheath' @(7.2, 15.8, 7.5) @(8.95, 16.2, 8.5) 'fitting'

# ---------------------------------------------------------------- animation
# switch (0.25s): first half the scabbard rises from below (y -8 -> 0), second half the
# whole katana flips 180 degrees about the grip, ending with the handle up and the blade
# inside the scabbard.
function FrameState($t) {
    $rise = [Math]::Min(1.0, $t / 0.5)
    $dy = -8.0 * (1.0 - $rise)
    $theta = 0.0
    if ($t -gt 0.5) { $theta = 180.0 * (($t - 0.5) / 0.5) }
    return @{ dy = $dy; deg = $theta }
}
function TransformPoint($p, $dy, $deg) {
    $rad = $deg * [Math]::PI / 180.0
    $c = [Math]::Cos($rad)
    $s = [Math]::Sin($rad)
    $x = $p[0] - $grip[0]
    $y = ($p[1] + $dy) - $grip[1]
    $out = New-Object double[] 3
    $out[0] = $c * $x - $s * $y + $grip[0]
    $out[1] = $s * $x + $c * $y + $grip[1]
    $out[2] = $p[2]
    return , $out
}

function FmtNum($v) {
    $r = [Math]::Round([double]$v, 5)
    if ($r -eq [Math]::Floor($r)) { return ([long]$r).ToString($inv) }
    return $r.ToString('0.#####', $inv)
}
function FmtNums($arr) {
    $out = @()
    foreach ($v in $arr) { $out += (FmtNum $v) }
    return '[' + ($out -join ', ') + ']'
}
$faceOrder = @('north', 'east', 'south', 'west', 'up', 'down')

function EmitModel($path, $includeSheath, $dy, $deg, $parent) {
    $lines = @()
    foreach ($part in $parts) {
        if ($part.group -eq 'sheath' -and -not $includeSheath) { continue }
        $pdy = 0.0
        if ($part.group -eq 'sheath') { $pdy = $dy }
        $f = TransformPoint $part.from $pdy $deg
        $t = TransformPoint $part.to $pdy $deg
        $o = TransformPoint $part.from $pdy $deg
        $shift = New-Object double[] 3
        for ($i = 0; $i -lt 3; $i++) { $shift[$i] = $o[$i] - $part.from[$i] }
        $bf = New-Object double[] 3
        $bt = New-Object double[] 3
        for ($i = 0; $i -lt 3; $i++) {
            $bf[$i] = $part.from[$i] + $shift[$i]
            $bt[$i] = $part.to[$i] + $shift[$i]
        }
        $uv = Uv $part.patch
        $parts_str = @('{ "from": ' + (FmtNums $bf) + ', "to": ' + (FmtNums $bt))
        if ([Math]::Abs($deg) -gt 0.001) {
            $parts_str += ', "rotation": { "origin": ' + (FmtNums $o) + ', "x": 0, "y": 0, "z": ' + (FmtNum $deg) + ' }'
        }
        $faces = @()
        foreach ($fk in $faceOrder) {
            $faces += '"' + $fk + '": { "texture": "#0", "uv": ' + (FmtNums $uv) + ' }'
        }
        $parts_str += ', "faces": { ' + ($faces -join ', ') + ' } }'
        $lines += ("`t`t" + ($parts_str -join ''))
    }
    $nl = [char]10
    $out = '{' + $nl
    if ($parent) { $out += "`t`"parent`": `"$parent`"," + $nl }
    else {
        $out += "`t`"gui_light`": `"front`"," + $nl
        $out += "`t`"ambientocclusion`": false," + $nl
        $out += "`t`"textures`": {" + $nl + "`t`t`"0`": `"sephiria_fantasy:item/katana_3d`"," + $nl + "`t`t`"particle`": `"sephiria_fantasy:item/katana_3d`"" + $nl + "`t}," + $nl
        $out += "`t`"display`": {" + $nl
        $out += "`t`t`"thirdperson_righthand`": { `"rotation`": [0, -90, 10], `"translation`": [0, 4.2, 0.5], `"scale`": [0.95, 0.95, 0.95] }," + $nl
        $out += "`t`t`"thirdperson_lefthand`": { `"rotation`": [0, 90, -10], `"translation`": [0, 4.2, 0.5], `"scale`": [0.95, 0.95, 0.95] }," + $nl
        $out += "`t`t`"firstperson_righthand`": { `"rotation`": [0, -90, -20], `"translation`": [1.13, 3.2, 1.13], `"scale`": [0.78, 0.78, 0.78] }," + $nl
        $out += "`t`t`"firstperson_lefthand`": { `"rotation`": [0, 90, 20], `"translation`": [1.13, 3.2, 1.13], `"scale`": [0.78, 0.78, 0.78] }" + $nl
        $out += "`t}," + $nl
    }
    $out += "`t`"elements`": [" + $nl + ($lines -join ("," + $nl)) + $nl + "`t]" + $nl + '}' + $nl
    $check = $out | ConvertFrom-Json
    [System.IO.File]::WriteAllText($path, $out, (New-Object System.Text.UTF8Encoding($false)))
    return (@($check.elements)).Count
}

Write-Output '--- game models (only with -GameModels) ---'
if ($GameModels) {
    $n0 = EmitModel (Join-Path $itemDir 'katana_drawn.json') $false -8.0 0.0 $null
    Write-Output ('  katana_drawn.json    elements=' + $n0)
    for ($f = 1; $f -lt $Frames - 1; $f++) {
        $t = $f / ($Frames - 1)
        $st = FrameState $t
        $n = EmitModel (Join-Path $itemDir ('katana_switch_' + $f + '.json')) $true $st.dy $st.deg 'sephiria_fantasy:item/katana_drawn'
        Write-Output ('  katana_switch_' + $f + '.json  t=' + (FmtNum $t) + ' dy=' + (FmtNum $st.dy) + ' deg=' + (FmtNum $st.deg) + ' elements=' + $n)
    }
    $stN = FrameState 1.0
    $nN = EmitModel (Join-Path $itemDir 'katana_sheathed.json') $true $stN.dy $stN.deg 'sephiria_fantasy:item/katana_drawn'
    Write-Output ('  katana_sheathed.json elements=' + $nN)
} else {
    Write-Output '  skipped (no -GameModels)'
}

# ---------------------------------------------------------------- Blockbench project
# "Modded Entity" (GeckoLib) format: the four parts live in two bone groups under one root
# bone, and the state switch is a real skeletal animation ("switch", 0.25s):
#   刀   : rotates 180 degrees around the grip during the second half
#   刀鞘 : rises from y -8 to 0 during the first half
# The Java Block/Item format cannot carry bone animation, hence this project format.
$projectDir = Join-Path $root 'models'
Copy-Item $texPath (Join-Path $projectDir 'katana_texture0.png') -Force

$pngBytes = [System.IO.File]::ReadAllBytes($texPath)
$b64 = [Convert]::ToBase64String($pngBytes)

function NewUuid($i) {
    return ('00000000-0000-0000-0000-' + $i.ToString('000000000000'))
}

$rootUuid = NewUuid 900
$bodyUuid = NewUuid 901
$sheathUuid = NewUuid 902
$texUuid = NewUuid 903

$elements = @()
$bodyChildren = @()
$sheathChildren = @()
$i = 0
foreach ($part in $parts) {
    $i++
    $uuid = NewUuid $i
    $uv = Uv $part.patch
    $faces = @()
    foreach ($fk in $faceOrder) {
        $faces += '"' + $fk + '": { "uv": [' + (($uv | ForEach-Object { [double]$_ }) -join ', ') + '], "texture": 0 }'
    }
    $el = '{ "name": "' + $part.name + '", "box_uv": false, "rescale": false, "locked": false, "light_emission": 0, "from": ' +
    (FmtNums $part.from) + ', "to": ' + (FmtNums $part.to) + ', "autouv": 0, "color": 0, "origin": ' + (FmtNums $part.from) +
    ', "faces": { ' + ($faces -join ', ') + ' }, "type": "cube", "uuid": "' + $uuid + '" }'
    $elements += ("`t`t" + $el)
    if ($part.group -eq 'sheath') { $sheathChildren += ('"' + $uuid + '"') } else { $bodyChildren += ('"' + $uuid + '"') }
}

$nl = [char]10
$bb = @()
$bb += '{'
$bb += "`t`"meta`": { `"format_version`": `"4.10`", `"model_format`": `"modded_entity`", `"box_uv`": false },"
$bb += "`t`"name`": `"katana`","
$bb += "`t`"parent`": `"`","
$bb += "`t`"ambientocclusion`": false,"
$bb += "`t`"front_gui_light`": false,"
$bb += "`t`"visible_box`": [1, 1, 0],"
$bb += "`t`"variable_placeholders`": `"`","
$bb += "`t`"variable_placeholder_buttons`": [],"
$bb += "`t`"unhandled_root_fields`": {},"
$bb += "`t`"resolution`": { `"width`": 16, `"height`": 16 },"
$bb += "`t`"elements`": ["
$bb += ($elements -join ("," + $nl))
$bb += "`t],"
$bb += "`t`"groups`": ["
$bb += "`t`t{ `"name`": `"$bladeGroup`", `"origin`": [8, 5, 8], `"color`": 0, `"uuid`": `"$bodyUuid`" },"
$bb += "`t`t{ `"name`": `"$sheathGroup`", `"origin`": [8, 10, 8], `"color`": 0, `"uuid`": `"$sheathUuid`" }"
$bb += "`t],"
$bb += "`t`"outliner`": ["
$bb += "`t`t{ `"name`": `"$daoName`", `"origin`": [8, 3, 8], `"color`": 0, `"uuid`": `"$rootUuid`", `"children`": ["
$bb += "`t`t`t{ `"name`": `"$bladeGroup`", `"origin`": [8, 5, 8], `"color`": 0, `"uuid`": `"$bodyUuid`", `"children`": [" + ($bodyChildren -join ', ') + "] },"
$bb += "`t`t`t{ `"name`": `"$sheathGroup`", `"origin`": [8, 10, 8], `"color`": 0, `"uuid`": `"$sheathUuid`", `"children`": [" + ($sheathChildren -join ', ') + "] }"
$bb += "`t`t] }"
$bb += "`t],"
$bb += "`t`"textures`": ["
$bb += "`t`t{ `"path`": `"`", `"name`": `"katana`", `"folder`": `"`", `"namespace`": `"`", `"id`": `"0`", `"group`": `"`", `"width`": 16, `"height`": 16, `"uv_width`": 16, `"uv_height`": 16, `"particle`": false, `"use_as_default`": true, `"layers_enabled`": false, `"render_mode`": `"default`", `"render_sides`": `"auto`", `"z_offset`": 0, `"frame_time`": 1, `"frame_order_type`": `"loop`", `"frame_order`": `"`", `"frame_interpolate`": false, `"visible`": true, `"internal`": true, `"saved`": false, `"uuid`": `"$texUuid`", `"relative_path`": `"`", `"source`": `"data:image/png;base64,$b64`" }"
$bb += "`t],"
$bb += "`t`"animations`": ["
$bb += "`t`t{"
$bb += "`t`t`t`"uuid`": `"$bodyUuid`","
$bb += "`t`t`t`"name`": `"switch`","
$bb += "`t`t`t`"loop`": `"once`","
$bb += "`t`t`t`"override`": false,"
$bb += "`t`t`t`"length`": 0.25,"
$bb += "`t`t`t`"snapping`": 20,"
$bb += "`t`t`t`"selected`": false,"
$bb += "`t`t`t`"anim_time_update`": `"`","
$bb += "`t`t`t`"blend_weight`": `"`","
$bb += "`t`t`t`"start_delay`": `"`","
$bb += "`t`t`t`"loop_delay`": `"`","
$bb += "`t`t`t`"animators`": {"
$bb += "`t`t`t`t`"$rootUuid`": { `"name`": `"$daoName`", `"type`": `"bone`", `"rotation_global`": false, `"quaternion_interpolation`": false, `"keyframes`": ["
$bb += "`t`t`t`t`t{ `"channel`": `"rotation`", `"data_points`": [{ `"x`": `"0`", `"y`": `"0`", `"z`": `"0`" }], `"uuid`": `"$(NewUuid 911)`", `"time`": 0, `"color`": -1, `"interpolation`": `"linear`" },"
$bb += "`t`t`t`t`t{ `"channel`": `"rotation`", `"data_points`": [{ `"x`": `"0`", `"y`": `"0`", `"z`": `"0`" }], `"uuid`": `"$(NewUuid 912)`", `"time`": 0.125, `"color`": -1, `"interpolation`": `"linear`" },"
$bb += "`t`t`t`t`t{ `"channel`": `"rotation`", `"data_points`": [{ `"x`": `"0`", `"y`": `"0`", `"z`": `"180`" }], `"uuid`": `"$(NewUuid 913)`", `"time`": 0.25, `"color`": -1, `"interpolation`": `"linear`" }"
$bb += "`t`t`t`t] },"
$bb += "`t`t`t`t`"$sheathUuid`": { `"name`": `"$sheathGroup`", `"type`": `"bone`", `"rotation_global`": false, `"quaternion_interpolation`": false, `"keyframes`": ["
$bb += "`t`t`t`t`t{ `"channel`": `"position`", `"data_points`": [{ `"x`": `"0`", `"y`": `"-8`", `"z`": `"0`" }], `"uuid`": `"$(NewUuid 921)`", `"time`": 0, `"color`": -1, `"interpolation`": `"linear`" },"
$bb += "`t`t`t`t`t{ `"channel`": `"position`", `"data_points`": [{ `"x`": `"0`", `"y`": `"0`", `"z`": `"0`" }], `"uuid`": `"$(NewUuid 922)`", `"time`": 0.125, `"color`": -1, `"interpolation`": `"linear`" },"
$bb += "`t`t`t`t`t{ `"channel`": `"position`", `"data_points`": [{ `"x`": `"0`", `"y`": `"0`", `"z`": `"0`" }], `"uuid`": `"$(NewUuid 923)`", `"time`": 0.25, `"color`": -1, `"interpolation`": `"linear`" }"
$bb += "`t`t`t`t] }"
$bb += "`t`t`t`t}"
$bb += "`t`t`t}"
$bb += "`t`t}"
$bb += "`t],"
$bb += "`t`"display`": {}"
$bb += '}'

$bbPath = Join-Path $projectDir 'katana.bbmodel'
$text = ($bb -join $nl)
$check = $text | ConvertFrom-Json
if ($check.meta.model_format -ne 'modded_entity') { Write-Output 'ABORT: bbmodel format wrong'; exit 1 }
[System.IO.File]::WriteAllText($bbPath, $text, (New-Object System.Text.UTF8Encoding($false)))
Write-Output ''
Write-Output ('blockbench project: models\katana.bbmodel  (modded_entity, ' + (@($check.elements)).Count + ' cubes, ' + (@($check.groups)).Count + ' bones, animation "' + $check.animations[0].name + '" ' + $check.animations[0].length + 's)')
Write-Output 'palette copy: models\katana_texture0.png'

# ---------------------------------------------------------------- Blockbench project
# Bedrock entity format: it carries bones and can export .geo.json / .animation.json,
# unlike Blockbench's Java item format which cannot export skeletal animation.
$animLength = 0.25
$elements = @()
$cubeUuids = @{}
$idx = 0
foreach ($part in $parts) {
    $u = '00000000-0000-0000-0000-' + $idx.ToString('000000000000')
    $cubeUuids[$idx] = $u
    $uv = Uv $part.patch
    $faces = [ordered]@{}
    foreach ($fk in $faceOrder) { $faces[$fk] = [ordered]@{ uv = $uv; texture = 0 } }
    $elements += [ordered]@{
        name = $part.name; box_uv = $false; rescale = $false; locked = $false
        render_order = 'default'; allow_mirror_modeling = $true
        from = $part.from; to = $part.to; autouv = 0; color = 0
        origin = $part.from; faces = $faces; type = 'cube'; uuid = $u
    }
    $idx++
}
$rootUuid = '00000000-0000-0000-0000-000000000100'
$bodyUuid = '00000000-0000-0000-0000-000000000101'
$sheathUuid = '00000000-0000-0000-0000-000000000102'
$bodyCubes = @()
$sheathCubes = @()
for ($i = 0; $i -lt $idx; $i++) {
    if ($parts[$i].group -eq 'sheath') { $sheathCubes += $cubeUuids[$i] } else { $bodyCubes += $cubeUuids[$i] }
}
$groups = @(
    [ordered]@{ name = $bladeGroup; uuid = $bodyUuid; origin = @(8, 4.6, 8); color = 0; children = $bodyCubes },
    [ordered]@{ name = $sheathGroup; uuid = $sheathUuid; origin = @(8, 10, 8); color = 0; children = $sheathCubes }
)
$outliner = @([ordered]@{
        name = $daoName; uuid = $rootUuid; origin = $grip; color = 0
        children = @(
            [ordered]@{ name = $bladeGroup; uuid = $bodyUuid; origin = @(8, 4.6, 8); color = 0; children = $bodyCubes },
            [ordered]@{ name = $sheathGroup; uuid = $sheathUuid; origin = @(8, 10, 8); color = 0; children = $sheathCubes }
        )
    })

$pngBytes = [System.IO.File]::ReadAllBytes($texPath)
$b64 = [Convert]::ToBase64String($pngBytes)
$animators = [ordered]@{
    $rootUuid = [ordered]@{
        name = $daoName; type = 'bone'; rotation_global = $false; quaternion_interpolation = $false
        keyframes = @(
            [ordered]@{ channel = 'rotation'; data_points = @([ordered]@{ x = '0'; y = '0'; z = '0' }); uuid = '00000000-0000-0000-0000-000000000201'; time = 0; color = -1; interpolation = 'linear' },
            [ordered]@{ channel = 'rotation'; data_points = @([ordered]@{ x = '0'; y = '0'; z = '0' }); uuid = '00000000-0000-0000-0000-000000000202'; time = 0.125; color = -1; interpolation = 'linear' },
            [ordered]@{ channel = 'rotation'; data_points = @([ordered]@{ x = '0'; y = '0'; z = '180' }); uuid = '00000000-0000-0000-0000-000000000203'; time = $animLength; color = -1; interpolation = 'linear' }
        )
    }
    $sheathUuid = [ordered]@{
        name = $sheathGroup; type = 'bone'; rotation_global = $false; quaternion_interpolation = $false
        keyframes = @(
            [ordered]@{ channel = 'position'; data_points = @([ordered]@{ x = '0'; y = '-8'; z = '0' }); uuid = '00000000-0000-0000-0000-000000000211'; time = 0; color = -1; interpolation = 'linear' },
            [ordered]@{ channel = 'position'; data_points = @([ordered]@{ x = '0'; y = '0'; z = '0' }); uuid = '00000000-0000-0000-0000-000000000212'; time = 0.125; color = -1; interpolation = 'linear' },
            [ordered]@{ channel = 'position'; data_points = @([ordered]@{ x = '0'; y = '0'; z = '0' }); uuid = '00000000-0000-0000-0000-000000000213'; time = $animLength; color = -1; interpolation = 'linear' }
        )
    }
}

$bb = [ordered]@{
    meta = [ordered]@{ format_version = '4.5'; model_format = 'bedrock_entity'; box_uv = $false }
    name = 'katana'
    parent = ''
    bedrock_animation_mode = 'entity'
    ambientocclusion = $false
    front_gui_light = $false
    visible_box = @(1, 1, 0)
    variable_placeholders = ''
    resolution = [ordered]@{ width = 16; height = 16 }
    elements = $elements
    groups = $groups
    outliner = $outliner
    textures = @([ordered]@{
            path = ''; name = 'katana_3d'; folder = 'item'; namespace = 'sephiria_fantasy'
            id = '0'; particle = $true; render_mode = 'default'; visible = $true
            mode = 'bitmap'; saved = $false; uuid = '00000000-0000-0000-0000-000000000301'
            source = ('data:image/png;base64,' + $b64)
            uv_width = 16; uv_height = 16; width = 16; height = 16
        })
    animations = @([ordered]@{
            uuid = '00000000-0000-0000-0000-000000000400'; name = 'switch'; loop = 'once'
            override = $false; length = $animLength; snapping = 20; selected = $false
            group_name = ''; scope = 0; anim_time_update = ''; blend_weight = ''
            loop_delay = ''; start_delay = ''; animators = $animators
        })
}
$bbPath = Join-Path $root 'models\katana.bbmodel'
$bbJson = $bb | ConvertTo-Json -Depth 30
[System.IO.File]::WriteAllText($bbPath, $bbJson, (New-Object System.Text.UTF8Encoding($false)))
Write-Output ('Blockbench project (bedrock_entity, bones + switch animation): models\katana.bbmodel')

