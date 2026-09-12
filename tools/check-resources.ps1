# Static consistency check for SEPHIRIA resources.
#  - every item definition is a display_context switch whose GUI case uses the flat sprite
#  - every model referenced anywhere in a definition exists, and element models are well formed
#  - lang files carry every key
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

$root = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $root 'src\main\resources\assets\sephiria'
$modelsDir = Join-Path $assets 'models\item'
$texturesDir = Join-Path $assets 'textures\item'

$items = @('default_sword_and_shield', 'steel_greatsword', 'dagger', 'colossal_crossbow', 'blade', 'quarterstaff')
$branches = @('sword_and_shield', 'greatsword', 'dagger', 'crossbow', 'katana', 'staff')

$errors = 0
function Fail($msg) { Write-Output ("FAIL: " + $msg); $script:errors++ }
function Ok($msg)   { Write-Output ("ok  : " + $msg) }
function ReadJson($path) { return ([System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8) | ConvertFrom-Json) }

# collect every "sephiria:item/xxx" reference by scanning the raw JSON text (recursing through
# the parsed object graph is unreliable and slow in PowerShell 5.1)
function Get-ModelRefs($path) {
    $text = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
    $refs = New-Object System.Collections.Generic.List[string]
    foreach ($m in [regex]::Matches($text, 'sephiria:item/([A-Za-z0-9_]+)')) {
        $refs.Add($m.Groups[1].Value)
    }
    return $refs
}

function Check-Model($name) {
    $path = Join-Path $modelsDir ($name + '.json')
    if (-not (Test-Path $path)) { Fail ("model models/item/$name.json is referenced but missing"); return }

    $json = ReadJson $path
    if ($null -ne $json.elements) {
        $elements = @($json.elements)
        if ($elements.Count -lt 1) { Fail "models/item/$name.json has no elements" }
        foreach ($el in $elements) {
            if ((@($el.from)).Count -ne 3 -or (@($el.to)).Count -ne 3) { Fail "models/item/$name.json: malformed element"; break }
            $faces = $el.faces.PSObject.Properties.Name
            if ($faces.Count -lt 1) { Fail "models/item/$name.json: element without faces"; break }
            foreach ($side in $faces) {
                if ($el.faces.$side.texture -ne '#0') { Fail "models/item/$name.json: $side face without palette texture"; break }
                if ((@($el.faces.$side.uv)).Count -ne 4) { Fail "models/item/$name.json: $side face without uv"; break }
            }
        }
        # palette texture named after the model must exist
        $tex = $json.textures.'0'
        if ($tex -like 'sephiria:item/*') {
            $texFile = Join-Path $texturesDir (($tex -split '/')[-1] + '.png')
            if (-not (Test-Path $texFile)) { Fail ("missing palette texture for models/item/$name.json (" + $tex + ")") }
        }
    }
}

foreach ($id in $items) {
    $defPath = Join-Path $assets "items\$id.json"
    if (-not (Test-Path $defPath)) { Fail "missing items/$id.json"; continue }
    if (-not (Test-Path (Join-Path $texturesDir ($id + '.png')))) { Fail "missing sprite textures/item/$id.png" }

    $def = ReadJson $defPath
    if ($def.model.type -ne 'minecraft:select') { Fail "items/$id.json is not a select model"; continue }
    if ($def.model.property -ne 'minecraft:display_context') { Fail "items/$id.json selects on '$($def.model.property)'"; continue }

    $flat = @($def.model.cases)[0]
    $flatWhen = @($flat.when)
    foreach ($ctx in @('gui', 'ground', 'fixed', 'on_shelf')) {
        if ($flatWhen -notcontains $ctx) { Fail "items/$id.json does not use the flat icon for '$ctx'" }
    }
    $flatModel = $flat.model.model
    if ($flatModel -ne "sephiria:item/$id") { Fail "items/$id.json GUI case points at '$flatModel'" }

    $refs = Get-ModelRefs $defPath
    foreach ($r in ($refs | Select-Object -Unique)) { Check-Model $r }
    Ok ("items/$id.json -> " + (($refs | Select-Object -Unique) -join ', '))
}

# display bases used by the hand models
foreach ($base in @('weapon', 'crossbow', 'weapon_3d', 'crossbow_3d', 'shield_3d')) {
    $basePath = Join-Path $modelsDir ($base + '.json')
    if (-not (Test-Path $basePath)) { Fail "missing base model models/item/$base.json"; continue }
    $json = ReadJson $basePath
    $contexts = $json.display.PSObject.Properties.Name
    foreach ($ctx in @('thirdperson_righthand', 'thirdperson_lefthand', 'firstperson_righthand', 'firstperson_lefthand')) {
        if ($contexts -notcontains $ctx) { Fail "models/item/$base.json has no display.$ctx" }
    }
    Ok "base model models/item/$base.json ($($contexts.Count) display contexts)"
}

# lang files
foreach ($lang in @('en_us', 'zh_cn')) {
    $langPath = Join-Path $assets "lang\$lang.json"
    if (-not (Test-Path $langPath)) { Fail "missing lang/$lang.json"; continue }
    $json = ReadJson $langPath
    $keys = $json.PSObject.Properties.Name
    foreach ($id in $items) {
        if ($keys -notcontains "item.sephiria.$id") { Fail "lang/$lang.json missing key item.sephiria.$id" }
    }
    foreach ($b in $branches) {
        if ($keys -notcontains "sephiria.branch.$b") { Fail "lang/$lang.json missing key sephiria.branch.$b" }
    }
    foreach ($k in @('itemGroup.sephiria.weapons', 'tooltip.sephiria.branch')) {
        if ($keys -notcontains $k) { Fail "lang/$lang.json missing key $k" }
    }
    if (([regex]::Matches($json.'tooltip.sephiria.branch', '%s')).Count -ne 2) { Fail "lang/$lang.json tooltip needs 2 placeholders" }
    Ok "lang/$lang.json checked ($($keys.Count) keys)"
}

if (-not (Test-Path (Join-Path $assets 'icon.png'))) { Fail "missing icon.png" }

Write-Output ""
if ($errors -eq 0) { Write-Output "ALL RESOURCE CHECKS PASSED" } else { Write-Output ("$errors PROBLEM(S) FOUND") }
exit $errors
