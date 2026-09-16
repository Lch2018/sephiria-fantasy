# Bake the bbmodel "reload" animation into per-frame game models.
#
# For each sample time t the animated nodes (cube 弹匣, groups 弦 and 弩) contribute an
# affine transform; every cube's box is re-expressed so that the game's model format
# (a box rotated about one origin, `rotation` euler in Rx*Ry*Rz order, same as
# Blockbench) reproduces the animated pose exactly:
#
#   R, t  = composited affine of the cube (rest rotation + its own animation + ancestors)
#   O     = the cube's origin after the same transform
#   box   = rest box + (O - rest origin)        (absorbs the screw component)
#   rotation = euler(R) about O
#
# Output: models/item/colossal_crossbow_reload_<i>.json  (children of the in-hand model)
# ASCII-only on purpose.
param(
    [int]$Frames = 11,
    [double]$TimeFrom = 0.0,
    [double]$TimeTo = 1.0,
    [switch]$MagazineOnly
)

$root = Split-Path -Parent $PSScriptRoot
$bbPath = Join-Path $root 'models\colossal_crossbow.bbmodel'
$itemDir = Join-Path $root 'src\main\resources\assets\sephiria\models\item'
$inHandPath = Join-Path $itemDir 'colossal_crossbow_in_hand.json'
$inv = [System.Globalization.CultureInfo]::InvariantCulture

# ---------- small 3x3 matrix helpers (column-vector convention, R = Rx*Ry*Rz) ----------
function MatMul($a, $b) {
    $r = New-Object double[] 9
    for ($i = 0; $i -lt 3; $i++) {
        for ($j = 0; $j -lt 3; $j++) {
            $s = 0.0
            for ($k = 0; $k -lt 3; $k++) { $s += $a[$i * 3 + $k] * $b[$k * 3 + $j] }
            $r[$i * 3 + $j] = $s
        }
    }
    return , $r
}
function MatVec($m, $v) {
    $r = New-Object double[] 3
    for ($i = 0; $i -lt 3; $i++) {
        $r[$i] = $m[$i * 3 + 0] * $v[0] + $m[$i * 3 + 1] * $v[1] + $m[$i * 3 + 2] * $v[2]
    }
    return , $r
}
function MatT($m) { return , @($m[0], $m[3], $m[6], $m[1], $m[4], $m[7], $m[2], $m[5], $m[8]) }
function EulerMat($e) {
    $x = [Math]::PI * $e[0] / 180.0
    $y = [Math]::PI * $e[1] / 180.0
    $z = [Math]::PI * $e[2] / 180.0
    $ca = [Math]::Cos($x); $sa = [Math]::Sin($x)
    $cb = [Math]::Cos($y); $sb = [Math]::Sin($y)
    $cc = [Math]::Cos($z); $sc = [Math]::Sin($z)
    # R = Rx*Ry*Rz
    $m = New-Object double[] 9
    $m[0] = $cb * $cc; $m[1] = -$cb * $sc; $m[2] = $sb
    $m[3] = $sa * $sb * $cc + $ca * $sc; $m[4] = -$sa * $sb * $sc + $ca * $cc; $m[5] = -$sa * $cb
    $m[6] = -$ca * $sb * $cc + $sa * $sc; $m[7] = $ca * $sb * $sc + $sa * $cc; $m[8] = $ca * $cb
    return , $m
}
function MatEuler($m) {
    $sb = [Math]::Max(-1.0, [Math]::Min(1.0, $m[2]))
    $b = [Math]::Asin($sb)
    if ([Math]::Abs([Math]::Abs($sb) - 1.0) -lt 1e-6) {
        # gimbal lock: fold the z rotation into x
        $a = [Math]::Atan2((-1.0 * $m[7]), $m[4])
        $c = 0.0
    } else {
        $a = [Math]::Atan2((-1.0 * $m[5]), $m[8])
        $c = [Math]::Atan2((-1.0 * $m[1]), $m[0])
    }
    $out = New-Object double[] 3
    $out[0] = $a * 180.0 / [Math]::PI
    $out[1] = $b * 180.0 / [Math]::PI
    $out[2] = $c * 180.0 / [Math]::PI
    return , $out
}
function FmtNum($v) {
    $r = [Math]::Round([double]$v, 5)
    if ([Math]::Abs($r) -lt 0.00001) { return '0' }
    if ($r -eq [Math]::Floor($r)) { return ([long]$r).ToString($inv) }
    return $r.ToString('0.#####', $inv)
}
function FmtNums($arr) {
    $parts = @()
    foreach ($v in $arr) { $parts += (FmtNum $v) }
    return '[' + ($parts -join ', ') + ']'
}
function Num($v) { return [double]$v }

# ---------- load the model source ----------
$bb = [System.IO.File]::ReadAllText($bbPath, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
$anim = @($bb.animations)[0]

# group hierarchy: uuid -> { name, origin, parent, cubes }
$groups = @{}
foreach ($g in @($bb.groups)) { $groups[$g.uuid] = @{ name = $g.name; origin = @((Num ($g.origin[0])), (Num ($g.origin[1])), (Num ($g.origin[2]))); parent = $null } }
$cubeParent = @{}
$cubeUuids = @{}
function Walk($node, $parentUuid) {
    if ($node -is [string]) {
        $cubeParent[$node] = $parentUuid
        $cubeUuids[$node] = $true
    } else {
        $u = $node.uuid
        if ($groups.ContainsKey($u)) { $groups[$u].parent = $parentUuid }
        foreach ($c in @($node.children)) { Walk $c $u }
    }
}
foreach ($node in @($bb.outliner)) { Walk $node $null }

# animation channels per target uuid.
# Note: animator keys are uuids, but after a copy/paste of the model an animator can point
# at a uuid that no longer exists (the magazine did). Resolve each animator to the current
# element/group with the same NAME first, and only fall back to the raw uuid.
$byName = @{}
foreach ($g in @($bb.groups)) { $byName[[string]$g.name] = $g.uuid }
foreach ($c in @($bb.elements)) { $byName[[string]$c.name] = $c.uuid }

$channels = @{}
# "弹匣" built from code points so this script stays ASCII-only (PS 5.1 reads it as GBK)
$magazineName = ([string][char]0x5F39) + ([string][char]0x5323)
foreach ($p in $anim.animators.PSObject.Properties) {
    $target = [string]$p.Name
    if ($byName.ContainsKey([string]$p.Value.name)) { $target = $byName[[string]$p.Value.name] }
    if ($MagazineOnly -and [string]$p.Value.name -ne $magazineName) { continue }
    if ($channels.ContainsKey($target)) { continue }
    $list = @{}
    foreach ($kf in @($p.Value.keyframes)) {
        $ch = [string]$kf.channel
        if (-not $list.ContainsKey($ch)) { $list[$ch] = @() }
        $dp = @($kf.data_points)[0]
        $list[$ch] += , @{ t = (Num ($kf.time)); v = @((Num ($dp.x)), (Num ($dp.y)), (Num ($dp.z))) }
    }
    $channelNames = @($list.Keys)
    foreach ($ch in $channelNames) { $list[$ch] = @($list[$ch] | Sort-Object { $_.t }) }
    $channels[$target] = $list
}
$animLength = Num $anim.length

function SampleChannel($uuid, $channel, $t) {
    if (-not $channels.ContainsKey($uuid)) { return @(0.0, 0.0, 0.0) }
    $keys = $channels[$uuid]
    if (-not $keys.ContainsKey($channel)) { return @(0.0, 0.0, 0.0) }
    $kf = $keys[$channel]
    if ($kf.Count -eq 1) { return $kf[0].v }
    if ($t -le $kf[0].t) { return $kf[0].v }
    if ($t -ge $kf[$kf.Count - 1].t) { return $kf[$kf.Count - 1].v }
    for ($i = 0; $i -lt $kf.Count - 1; $i++) {
        if ($t -ge $kf[$i].t -and $t -le $kf[$i + 1].t) {
            $span = $kf[$i + 1].t - $kf[$i].t
            $f = 0.0
            if ($span -gt 0) { $f = ($t - $kf[$i].t) / $span }
            $out = New-Object double[] 3
            for ($c = 0; $c -lt 3; $c++) {
                $v0 = [double]($kf[$i].v[$c])
                $v1 = [double]($kf[$i + 1].v[$c])
                $out[$c] = $v0 + ($v1 - $v0) * $f
            }
            return , $out
        }
    }
    return , (New-Object double[] 3)
}

# node affine: p -> R*(p - O) + O + T
function NodeAffine($uuid, $origin, $t) {
    $rot = SampleChannel $uuid 'rotation' $t
    $pos = SampleChannel $uuid 'position' $t
    $R = EulerMat $rot
    $RO = MatVec $R $origin
    $tt = New-Object double[] 3
    for ($i = 0; $i -lt 3; $i++) { $tt[$i] = $origin[$i] + $pos[$i] - $RO[$i] }
    return @{ R = $R; t = $tt }
}
function AffineCompose($outer, $inner) {
    # outer(inner(p))
    $R = MatMul $outer.R $inner.R
    $t = MatVec $outer.R $inner.t
    $tt = New-Object double[] 3
    for ($i = 0; $i -lt 3; $i++) { $tt[$i] = $t[$i] + $outer.t[$i] }
    return @{ R = $R; t = $tt }
}

# element table from the game model (keeps faces/uv exactly as shipped)
$inHand = [System.IO.File]::ReadAllText($inHandPath, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
$faceOrder = @('north', 'east', 'south', 'west', 'up', 'down')

# map bbmodel cubes to their rest data by matching from/to
$restByKey = @{}
foreach ($c in @($bb.elements)) {
    $key = ((@($c.from)) -join ',') + '|' + ((@($c.to)) -join ',')
    $restRot = @(0.0, 0.0, 0.0)
    if ($null -ne $c.rotation) { $restRot = @((Num ($c.rotation[0])), (Num ($c.rotation[1])), (Num ($c.rotation[2]))) }
    $restOrigin = @((Num ($c.from[0])), (Num ($c.from[1])), (Num ($c.from[2])))
    if ($null -ne $c.origin) { $restOrigin = @((Num ($c.origin[0])), (Num ($c.origin[1])), (Num ($c.origin[2]))) }
    $rest = @{ uuid = $c.uuid; from = @((Num ($c.from[0])), (Num ($c.from[1])), (Num ($c.from[2]))); to = @((Num ($c.to[0])), (Num ($c.to[1])), (Num ($c.to[2]))); origin = $restOrigin; rot = $restRot }
    $restByKey[$key] = $rest
}

Write-Output ('groups=' + $groups.Count + '  animated nodes=' + $channels.Count + '  frames=' + $Frames + '  range=' + $TimeFrom + '..' + $TimeTo + 's')

for ($f = 0; $f -lt $Frames; $f++) {
    $t = $TimeFrom + ($TimeTo - $TimeFrom) * $f / ($Frames - 1)

    # per-cube affine for this frame, keyed by the rest from|to
    $affines = @{}
    foreach ($key in $restByKey.Keys) {
        $rest = $restByKey[$key]
        # rest rotation, applied about the cube's own origin (not the world origin)
        $Rrest = EulerMat $rest.rot
        $ROrest = MatVec $Rrest $rest.origin
        $trest = New-Object double[] 3
        for ($i = 0; $i -lt 3; $i++) { $trest[$i] = $rest.origin[$i] - $ROrest[$i] }
        $X = @{ R = $Rrest; t = $trest }
        if ($channels.ContainsKey($rest.uuid)) {
            $own = NodeAffine $rest.uuid $rest.origin $t
            $X = AffineCompose $own $X
        }
        $parent = $cubeParent[$rest.uuid]
        while ($null -ne $parent) {
            $g = $groups[$parent]
            $A = NodeAffine $parent $g.origin $t
            $X = AffineCompose $A $X
            $parent = $g.parent
        }
        $affines[$key] = $X
    }

    $lines = @()
    foreach ($el in @($inHand.elements)) {
        $key = ((@($el.from)) -join ',') + '|' + ((@($el.to)) -join ',')
        $rest = $restByKey[$key]
        $X = $affines[$key]

        # the rotation origin must be the cube's origin AFTER the transform: X(O) = R*O + t
        $rotOrigin = MatVec $X.R $rest.origin
        $originFinal = New-Object double[] 3
        for ($i = 0; $i -lt 3; $i++) { $originFinal[$i] = $rotOrigin[$i] + $X.t[$i] }
        $from = New-Object double[] 3
        $to = New-Object double[] 3
        for ($i = 0; $i -lt 3; $i++) {
            $shift = $originFinal[$i] - $rest.origin[$i]
            $from[$i] = $rest.from[$i] + $shift
            $to[$i] = $rest.to[$i] + $shift
        }

        $euler = MatEuler $X.R
        $rotated = ([Math]::Abs($euler[0]) + [Math]::Abs($euler[1]) + [Math]::Abs($euler[2])) -gt 0.001

        $parts = @('{ "from": ' + (FmtNums $from) + ', "to": ' + (FmtNums $to))
        if ($rotated) {
            $parts += ', "rotation": { "origin": ' + (FmtNums $originFinal) + ', "x": ' + (FmtNum $euler[0]) + ', "y": ' + (FmtNum $euler[1]) + ', "z": ' + (FmtNum $euler[2]) + ' }'
        }
        $faceTexts = @()
        foreach ($fk in $faceOrder) {
            $face = $el.faces.$fk
            if ($null -eq $face) { continue }
            $faceTexts += '"' + $fk + '": { "texture": "' + $face.texture + '", "uv": ' + (FmtNums @($face.uv)) + ' }'
        }
        $parts += ', "faces": { ' + ($faceTexts -join ', ') + ' } }'
        $lines += ("`t`t" + ($parts -join ''))
    }

    $out = '{' + "`n" +
    "`t`"parent`": `"sephiria:item/colossal_crossbow_in_hand`",`n" +
    "`t`"elements`": [`n" + ($lines -join ",`n") + "`n`t]`n" + '}' + "`n"
    $check = $out | ConvertFrom-Json
    if ((@($check.elements)).Count -ne @($inHand.elements).Count) { Write-Output ('ABORT frame ' + $f); exit 1 }
    [System.IO.File]::WriteAllText((Join-Path $itemDir ('colossal_crossbow_reload_' + $f + '.json')), $out, (New-Object System.Text.UTF8Encoding($false)))
    Write-Output ('  frame ' + $f + '  t=' + (FmtNum $t) + 's  elements=' + (@($check.elements).Count))
}
Write-Output 'DONE'
