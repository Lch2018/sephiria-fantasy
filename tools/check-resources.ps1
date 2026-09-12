# Static consistency check for SEPHIRIA resources.
# Verifies that every item id has an item-model-definition, a model, a texture,
# and matching lang entries in both en_us and zh_cn.
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

$root = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $root 'src\main\resources\assets\sephiria'

$items = @('default_sword_and_shield', 'steel_greatsword', 'dagger', 'colossal_crossbow', 'blade', 'quarterstaff')
$branches = @('sword_and_shield', 'greatsword', 'dagger', 'crossbow', 'katana', 'staff')

$errors = 0
function Fail($msg) { Write-Output ("FAIL: " + $msg); $script:errors++ }
function Ok($msg)   { Write-Output ("ok  : " + $msg) }
# PS 5.1 reads BOM-less files as ANSI/GBK, so always decode JSON as UTF-8 explicitly.
# Keep this file pure ASCII: a non-ASCII byte here can swallow the next line.
function ReadJson($path) { return ([System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8) | ConvertFrom-Json) }

foreach ($id in $items) {
    $defPath = Join-Path $assets "items\$id.json"
    $modelPath = Join-Path $assets "models\item\$id.json"
    $texPath = Join-Path $assets "textures\item\$id.png"

    if (-not (Test-Path $defPath)) { Fail "missing item model definition: items/$id.json" }
    if (-not (Test-Path $modelPath)) { Fail "missing model: models/item/$id.json" ; continue }
    if (-not (Test-Path $texPath)) { Fail "missing texture: textures/item/$id.png" }

    # model definition -> model file
    if (Test-Path $defPath) {
        $def = ReadJson $defPath
        $target = $def.model.model
        if ($target -ne "sephiria:item/$id") { Fail "items/$id.json points at '$target', expected 'sephiria:item/$id'" }
    }

    # model -> texture file
    $model = ReadJson $modelPath
    $layer0 = $model.textures.layer0
    $texName = ($layer0 -split ':')[-1] -replace '^item/', ''
    if ($texName -ne $id) { Fail "models/item/$id.json layer0 is '$layer0', expected 'sephiria:item/$id'" }

    # our weapons inherit a shared base model that carries the hand display transforms
    $expectedParent = if ($id -eq 'colossal_crossbow') { 'sephiria:item/crossbow' } else { 'sephiria:item/weapon' }
    if ($model.parent -ne $expectedParent) { Fail "models/item/$id.json parent is '$($model.parent)', expected '$expectedParent'" }
}

# base models must exist and define all four hand contexts (this is what keeps the weapons
# held at the same angle as the vanilla weapon of the same type)
foreach ($base in @('weapon', 'crossbow')) {
    $basePath = Join-Path $assets "models\item\$base.json"
    if (-not (Test-Path $basePath)) { Fail "missing base model models/item/$base.json"; continue }

    $baseJson = ReadJson $basePath
    $contexts = $baseJson.display.PSObject.Properties.Name
    foreach ($ctx in @('thirdperson_righthand', 'thirdperson_lefthand', 'firstperson_righthand', 'firstperson_lefthand')) {
        if ($contexts -notcontains $ctx) { Fail "models/item/$base.json has no display.$ctx" }
    }
    if ($baseJson.parent -ne 'minecraft:item/generated') { Fail "models/item/$base.json parent is '$($baseJson.parent)', expected 'minecraft:item/generated'" }
    foreach ($ctx in $contexts) {
        $rot = $baseJson.display.$ctx.rotation
        $scale = $baseJson.display.$ctx.scale
        if ($null -eq $rot -or $rot.Count -ne 3) { Fail "models/item/$base.json display.$ctx.rotation malformed" }
        if ($null -eq $scale -or $scale.Count -ne 3) { Fail "models/item/$base.json display.$ctx.scale malformed" }
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
    Ok "lang/$lang.json checked ($($keys.Count) keys)"
}

if (-not (Test-Path (Join-Path $assets 'icon.png'))) { Fail "missing icon.png" }

# the lang format string must keep two placeholders
foreach ($lang in @('en_us', 'zh_cn')) {
    $json = ReadJson (Join-Path $assets "lang\$lang.json")
    $fmt = $json.'tooltip.sephiria.branch'
    $count = ([regex]::Matches($fmt, '%s')).Count
    if ($count -ne 2) { Fail "lang/$lang.json tooltip.sephiria.branch has $count placeholders, expected 2" }
}

Write-Output ""
if ($errors -eq 0) { Write-Output "ALL RESOURCE CHECKS PASSED" } else { Write-Output ("$errors PROBLEM(S) FOUND") }
exit $errors
