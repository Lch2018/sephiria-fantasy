# Static consistency check for SEPHIRIA resources.
#  - every item id has: an item definition, a flat icon model, an in-hand voxel model,
#    a sprite texture and a palette texture
#  - the item definition switches models on display_context (flat in GUI, voxel in hand)
#  - lang files carry every key
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

$root = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $root 'src\main\resources\assets\sephiria'

$items = @('default_sword_and_shield', 'steel_greatsword', 'dagger', 'colossal_crossbow', 'blade', 'quarterstaff')
$branches = @('sword_and_shield', 'greatsword', 'dagger', 'crossbow', 'katana', 'staff')

$errors = 0
function Fail($msg) { Write-Output ("FAIL: " + $msg); $script:errors++ }
function Ok($msg)   { Write-Output ("ok  : " + $msg) }
# PS 5.1 reads BOM-less files as ANSI/GBK, so always decode JSON as UTF-8 explicitly.
function ReadJson($path) { return ([System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8) | ConvertFrom-Json) }

function Test-File($path, $label) {
    if (-not (Test-Path $path)) { Fail ("missing " + $label); return $false }
    return $true
}

foreach ($id in $items) {
    $defPath = Join-Path $assets "items\$id.json"
    $flatPath = Join-Path $assets "models\item\$id.json"
    $voxelPath = Join-Path $assets "models\item\${id}_in_hand.json"
    $spritePath = Join-Path $assets "textures\item\$id.png"
    $palettePath = Join-Path $assets "textures\item\${id}_3d.png"

    if (-not (Test-File $defPath "items/$id.json")) { continue }
    if (-not (Test-File $flatPath "models/item/$id.json")) { continue }
    if (-not (Test-File $voxelPath "models/item/${id}_in_hand.json")) { continue }
    if (-not (Test-File $spritePath "textures/item/$id.png")) { continue }
    if (-not (Test-File $palettePath "textures/item/${id}_3d.png")) { continue }

    # item definition: flat icon for GUI-ish contexts, voxel model otherwise
    $def = ReadJson $defPath
    if ($def.model.type -ne 'minecraft:select') { Fail "items/$id.json is not a select model" }
    if ($def.model.property -ne 'minecraft:display_context') { Fail "items/$id.json selects on '$($def.model.property)'" }
    $case = $def.model.cases[0]
    if ($case.model.model -ne "sephiria:item/$id") { Fail "items/$id.json GUI case points at '$($case.model.model)'" }
    $flatContexts = @($case.when)
    foreach ($ctx in @('gui', 'ground', 'fixed', 'on_shelf')) {
        if ($flatContexts -notcontains $ctx) { Fail "items/$id.json does not use the flat icon for '$ctx'" }
    }
    if ($def.model.fallback.model -ne "sephiria:item/${id}_in_hand") { Fail "items/$id.json fallback is '$($def.model.fallback.model)'" }

    # flat model -> sprite
    $flat = ReadJson $flatPath
    $layer0 = $flat.textures.layer0
    if ((($layer0 -split ':')[-1] -replace '^item/', '') -ne $id) { Fail "models/item/$id.json layer0 is '$layer0'" }
    $expectedParent = if ($id -eq 'colossal_crossbow') { 'sephiria:item/crossbow' } else { 'sephiria:item/weapon' }
    if ($flat.parent -ne $expectedParent) { Fail "models/item/$id.json parent is '$($flat.parent)', expected '$expectedParent'" }

    # voxel model -> palette texture, and it must actually contain geometry
    $voxel = ReadJson $voxelPath
    $expected3dParent = if ($id -eq 'colossal_crossbow') { 'sephiria:item/crossbow_3d' } else { 'sephiria:item/weapon_3d' }
    if ($voxel.parent -ne $expected3dParent) { Fail "models/item/${id}_in_hand.json parent is '$($voxel.parent)', expected '$expected3dParent'" }
    if ($voxel.textures.'0' -ne "sephiria:item/${id}_3d") { Fail "models/item/${id}_in_hand.json palette is '$($voxel.textures.'0')'" }
    $elementCount = @($voxel.elements).Count
    if ($elementCount -lt 4) { Fail "models/item/${id}_in_hand.json has only $elementCount elements" }
    foreach ($el in $voxel.elements) {
        if ($null -eq $el.from -or $el.from.Count -ne 3 -or $null -eq $el.to -or $el.to.Count -ne 3) {
            Fail "models/item/${id}_in_hand.json: malformed element"
            break
        }
        # hidden faces are dropped on purpose, so only check the faces that are present
        $faces = $el.faces.PSObject.Properties.Name
        if ($faces.Count -lt 1) { Fail "models/item/${id}_in_hand.json: element without any face"; break }
        foreach ($side in $faces) {
            if ($el.faces.$side.texture -ne '#0') { Fail "models/item/${id}_in_hand.json: $side face without palette texture"; break }
            if ((@($el.faces.$side.uv)).Count -ne 4) { Fail "models/item/${id}_in_hand.json: $side face without uv"; break }
        }
    }
}

# base models must exist and define all four hand contexts
foreach ($base in @('weapon', 'crossbow', 'weapon_3d', 'crossbow_3d')) {
    $basePath = Join-Path $assets "models\item\$base.json"
    if (-not (Test-File $basePath "base model models/item/$base.json")) { continue }

    $baseJson = ReadJson $basePath
    $contexts = $baseJson.display.PSObject.Properties.Name
    foreach ($ctx in @('thirdperson_righthand', 'thirdperson_lefthand', 'firstperson_righthand', 'firstperson_lefthand')) {
        if ($contexts -notcontains $ctx) { Fail "models/item/$base.json has no display.$ctx" }
    }
    foreach ($ctx in $contexts) {
        if ((@($baseJson.display.$ctx.rotation)).Count -ne 3) { Fail "models/item/$base.json display.$ctx.rotation malformed" }
        if ((@($baseJson.display.$ctx.scale)).Count -ne 3) { Fail "models/item/$base.json display.$ctx.scale malformed" }
    }
    Ok "base model models/item/$base.json ($($contexts.Count) display contexts)"
}

# lang files
foreach ($lang in @('en_us', 'zh_cn')) {
    $langPath = Join-Path $assets "lang\$lang.json"
    if (-not (Test-File $langPath "lang/$lang.json")) { continue }

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
    $fmt = $json.'tooltip.sephiria.branch'
    if (([regex]::Matches($fmt, '%s')).Count -ne 2) { Fail "lang/$lang.json tooltip.sephiria.branch needs 2 placeholders" }

    Ok "lang/$lang.json checked ($($keys.Count) keys)"
}

if (-not (Test-Path (Join-Path $assets 'icon.png'))) { Fail "missing icon.png" }

Write-Output ""
if ($errors -eq 0) { Write-Output "ALL RESOURCE CHECKS PASSED" } else { Write-Output ("$errors PROBLEM(S) FOUND") }
exit $errors
