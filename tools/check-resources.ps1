# Static consistency check for SEPHIRIA resources.
#
# Structure-agnostic on purpose: item definitions are walked for model references, every
# referenced model must exist, every element model must be well formed, every palette texture
# must be present, and every parent chain must resolve inside the mod. Plus the lang keys.
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

$root = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $root 'src\main\resources\assets\sephiria'
$modelsDir = Join-Path $assets 'models\item'
$texDir = Join-Path $assets 'textures\item'

$items = @('default_sword_and_shield', 'steel_greatsword', 'dagger', 'colossal_crossbow', 'blade', 'quarterstaff')
$branches = @('sword_and_shield', 'greatsword', 'dagger', 'crossbow', 'katana', 'staff')

$errors = 0
function Fail($msg) { Write-Output ("FAIL: " + $msg); $script:errors++ }
function Ok($msg)   { Write-Output ("ok  : " + $msg) }
function ReadText($path) { return [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8) }
function ReadJson($path) { return (ReadText $path | ConvertFrom-Json) }

function Get-Refs($text) {
    $refs = New-Object System.Collections.Generic.List[string]
    foreach ($m in [regex]::Matches($text, 'sephiria:item/([A-Za-z0-9_]+)')) { $refs.Add($m.Groups[1].Value) }
    return $refs
}

# 1) item definitions: every referenced model exists
foreach ($id in $items) {
    $defPath = Join-Path $assets "items\$id.json"
    if (-not (Test-Path $defPath)) { Fail "missing items/$id.json"; continue }
    if (-not (Test-Path (Join-Path $texDir ($id + '.png')))) { Fail "missing sprite textures/item/$id.png" }

    $refs = Get-Refs (ReadText $defPath)
    if ($refs.Count -eq 0) { Fail "items/$id.json references no model" }
    foreach ($r in ($refs | Select-Object -Unique)) {
        if (-not (Test-Path (Join-Path $modelsDir ($r + '.json')))) { Fail "items/$id.json -> models/item/$r.json is missing" }
    }
    Ok ("items/$id.json -> " + (($refs | Select-Object -Unique) -join ', '))
}

# 2) every model file: valid geometry, existing palette, resolvable parent
foreach ($file in Get-ChildItem $modelsDir -Filter *.json) {
    $name = $file.BaseName
    $text = ReadText $file.FullName
    $json = $text | ConvertFrom-Json

    if ($json.parent -like 'sephiria:item/*') {
        $parentName = $json.parent.Substring('sephiria:item/'.Length)
        if (-not (Test-Path (Join-Path $modelsDir ($parentName + '.json')))) {
            Fail "models/item/$name.json parent '$($json.parent)' is missing"
        }
    }

    if ($null -ne $json.elements) {
        $elements = @($json.elements)
        foreach ($el in $elements) {
            if ((@($el.from)).Count -ne 3 -or (@($el.to)).Count -ne 3) { Fail "models/item/$name.json: malformed element"; break }
            foreach ($side in $el.faces.PSObject.Properties.Name) {
                if ((@($el.faces.$side.uv)).Count -ne 4) { Fail "models/item/$name.json: $side face without uv"; break }
            }
        }
        $tex = $json.textures.'0'
        if ($tex -like 'sephiria:item/*') {
            $texFile = Join-Path $texDir (($tex -split '/')[-1] + '.png')
            if (-not (Test-Path $texFile)) { Fail "models/item/$name.json palette $tex is missing" }
        }
    }
}

# 3) flat weapon models must reach a base that carries the hand display transforms
foreach ($id in $items) {
    $path = Join-Path $modelsDir ($id + '.json')
    if (-not (Test-Path $path)) { continue }
    $json = ReadJson $path
    if ($json.parent -notlike 'sephiria:item/*') { continue }
    $baseName = $json.parent.Substring('sephiria:item/'.Length)
    $basePath = Join-Path $modelsDir ($baseName + '.json')
    if (-not (Test-Path $basePath)) { continue }
    $base = ReadJson $basePath
    $contexts = $base.display.PSObject.Properties.Name
    foreach ($ctx in @('thirdperson_righthand', 'thirdperson_lefthand', 'firstperson_righthand', 'firstperson_lefthand')) {
        if ($contexts -notcontains $ctx) { Fail "models/item/$baseName.json (base of $id) has no display.$ctx" }
    }
    Ok ("$id inherits hand transforms from models/item/$baseName.json")
}

# 4) lang files
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
    if (([regex]::Matches($json.'tooltip.sephiria.branch', '%s')).Count -ne 2) { Fail "lang/$lang.json tooltip needs 2 placeholders" }
    Ok "lang/$lang.json checked ($($keys.Count) keys)"
}

if (-not (Test-Path (Join-Path $assets 'icon.png'))) { Fail "missing icon.png" }

Write-Output ""
if ($errors -eq 0) { Write-Output "ALL RESOURCE CHECKS PASSED" } else { Write-Output ("$errors PROBLEM(S) FOUND") }
exit $errors
