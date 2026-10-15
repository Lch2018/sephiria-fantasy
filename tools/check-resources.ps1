# Static consistency check for SEPHIRIA resources.
#
# Structure-agnostic on purpose: item definitions are walked for model references, every
# referenced model must exist, every element model must be well formed, every palette texture
# must be present, and every parent chain must resolve inside the mod. Plus the lang keys.
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

$root = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $root 'src\main\resources\assets\sephiria_fantasy'
$modelsDir = Join-Path $assets 'models\item'
$texDir = Join-Path $assets 'textures\item'

$items = @('default_sword_and_shield', 'steel_greatsword', 'dagger', 'colossal_crossbow', 'crossbow_bolt', 'blade', 'quarterstaff', 'charm_of_strength', 'artifact_tab_icon', 'warriors_proof', 'slate_of_future', 'enchant_coin', 'slate_tab_icon', 'dice', 'artifact_chest', 'slate_chest', 'upgrade_chest', 'shield_textbook', 'sword_textbook', 'wind_score', 'pressure_bandage', 'golden_cloak', 'wanderer_necklace', 'projection_sword', 'colorless_cube', 'silver_plate', 'encouragement_banner', 'haste_grimoire', 'red_dew', 'longing_amulet', 'fault_probe', 'deft_amulet', 'pinwheel', 'specimen_beak', 'evergreen_cloak', 'resistance_band', 'warm_stone', 'slate_of_oath', 'slate_of_belief', 'slate_of_entrance', 'slate_of_competition', 'slate_of_rally', 'slate_of_wave', 'slate_of_double_star', 'slate_of_handshake', 'regeneration_potion', 'apple_juice', 'trappist_sacred', 'big_dice_potion', 'vampire_lord_oath',
    # the magic-tech / dark-cloud artifact batches: an icon texture without these two definition
    # files renders as a missing model in game, so every artifact added since batch 10 is listed
    'magic_carrot', 'sharp_flint', 'resonance_stone', 'lightning_struck_branch', 'electric_bug',
    'qilin_horn', 'electric_amulet', 'sande_earrings', 'thunder_verdict', 'storm_compass',
    'firefly', 'typhoon_score', 'stone_flower', 'sapote_fruit', 'lightning_rod',
    'pointy_acorn', 'thunder_stone', 'cloudseed_arrow', 'mast_model', 'raven_tablet',
    'solis_fracto', 'solis_parvo', 'sun_sword',
    # batch 20 (glacier): the amulet / claw / ring / sealed tegas
    'wisdom_amulet', 'blue_claw', 'proshium_ring', 'sealed_tegas',
    # batch 21 (frost armament): the scabbard / hammer / conch
    'exorcism_scabbard', 'blizzard_hammer', 'magic_conch',
    # batch 22 (frost armament, second wave): wings / heart / volspa / bow
    'ice_wings', 'frozen_heart', 'volspa', 'ice_bow',
    # batch 23 (eternal eclipse): the sun-sword + frost-armament bond artifact
    'eternal_eclipse',
    # the three hidden carriers the frost projectiles are drawn with (ice-blue recolours)
    'ice_hammer', 'ice_spear', 'ice_arrow')

# 方块：每个都要有 blockstates/<id>.json → models/block/<model>.json、贴图齐全、方块物品的定义文件，
# 以及中英各一条 block.sephiria_fantasy.<id> 名字键（见 1c 那一段）
$blocks = @('root_retreat', 'wish_pool')

# Extra MODELS that no item definition is named after: the eternal-eclipse batch recolours existing
# art instead of adding items (the carriers' definitions switch to them by component, the eclipse's
# definition selects between them by component), so they are checked as models, not as items - see
# section 1b. Their textures are what the in-game look depends on, so both files must be there.
$extraModels = @('flame_hammer', 'flame_spear', 'flame_arrow',
    'eternal_eclipse_flame', 'eternal_eclipse_frost', 'sun_sword_ice')
$branches = @('sword_and_shield', 'greatsword', 'dagger', 'crossbow', 'katana', 'staff')

$errors = 0
function Fail($msg) { Write-Output ("FAIL: " + $msg); $script:errors++ }
function Ok($msg)   { Write-Output ("ok  : " + $msg) }
function ReadText($path) { return [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8) }
function ReadJson($path) { return (ReadText $path | ConvertFrom-Json) }
function Get-JarNames($jar, $pattern) {
    # group 1 of every jar entry matching $pattern (1.21.4+ keeps item definitions under
    # assets/minecraft/items/, so plain vanilla item ids are checkable without the game)
    $names = New-Object System.Collections.Generic.List[string]
    $zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
    try {
        foreach ($entry in $zip.Entries) {
            $m = [regex]::Match($entry.FullName, $pattern)
            if ($m.Success) { $names.Add($m.Groups[1].Value) }
        }
    }
    finally { $zip.Dispose() }
    return $names
}

function Get-Refs($text) {
    $refs = New-Object System.Collections.Generic.List[string]
    foreach ($m in [regex]::Matches($text, 'sephiria_fantasy:item/([A-Za-z0-9_]+)')) { $refs.Add($m.Groups[1].Value) }
    return $refs
}

# 0) the mod icon fabric.mod.json points at must exist and be square
$iconPath = Join-Path $assets 'icon.png'
if (-not (Test-Path $iconPath)) { Fail 'missing assets/sephiria_fantasy/icon.png (the mod-list icon)' }
else {
    Add-Type -AssemblyName System.Drawing
    $icon = [System.Drawing.Bitmap]::FromFile($iconPath)
    if ($icon.Width -ne $icon.Height) { Fail "icon.png is not square ($($icon.Width)x$($icon.Height))" }
    elseif ($icon.Width -lt 64) { Fail "icon.png is smaller than 64x64 ($($icon.Width)x$($icon.Height))" }
    else { Ok "icon.png $($icon.Width)x$($icon.Height)" }
    $icon.Dispose()
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

# 1b) extra models (no item definition of their own): the file and its palette texture must exist
foreach ($name in $extraModels) {
    $modelPath = Join-Path $modelsDir ($name + '.json')
    if (-not (Test-Path $modelPath)) { Fail "missing models/item/$name.json"; continue }
    $json = ReadJson $modelPath
    $layer = $json.textures.layer0
    if (-not $layer) { Fail "models/item/$name.json has no textures.layer0"; continue }
    if ($layer -notlike 'sephiria_fantasy:item/*') { Fail "models/item/$name.json layer0 '$layer' is not a sephiria_fantasy:item texture"; continue }
    $sprite = Join-Path $texDir (((($layer -split '/')[-1]) -replace '\.png$', '') + '.png')
    if (-not (Test-Path $sprite)) { Fail "models/item/$name.json layer0 $layer is missing"; continue }
    Ok "models/item/$name.json -> $layer"
}

# 1c) blocks: the blockstate must resolve, its model's textures must exist, and the block needs its
#     item definition + a block name key in both languages (a block without items/<id>.json renders
#     as a missing model in the inventory)
foreach ($id in $blocks) {
    $statePath = Join-Path $assets "blockstates\$id.json"
    if (-not (Test-Path $statePath)) { Fail "missing blockstates/$id.json"; continue }

    # 注意：方块状态文件用**正则**取模型名，不能走 ConvertFrom-Json——没有属性的方块
    # （只有空气那一种状态）变体键是空字符串 ""，而 PowerShell 5.1 的 ConvertFrom-Json
    # 遇到空属性名直接报「参数 name 的值无效」（原版方块就是这么写的，游戏读得没问题）
    $stateText = ReadText $statePath
    $modelRefs = New-Object System.Collections.Generic.List[string]

    foreach ($m in [regex]::Matches($stateText, '"model"\s*:\s*"([^"]+)"')) {
        $modelRefs.Add($m.Groups[1].Value)
    }

    if ($modelRefs.Count -eq 0) { Fail "blockstates/$id.json references no model" }
    foreach ($ref in ($modelRefs | Select-Object -Unique)) {
        if ($ref -notlike 'sephiria_fantasy:block/*') { Fail "blockstates/$id.json model '$ref' is not a sephiria_fantasy:block model"; continue }
        $modelName = $ref.Substring('sephiria_fantasy:block/'.Length)
        $modelPath = Join-Path $assets "models\block\$modelName.json"
        if (-not (Test-Path $modelPath)) { Fail "blockstates/$id.json -> models/block/$modelName.json is missing"; continue }

        $model = ReadJson $modelPath

        # every texture the model references (layer/palette keys) must exist as a png
        foreach ($tex in $model.textures.PSObject.Properties) {
            $value = $tex.Value
            if ($value -notlike 'sephiria_fantasy:*') { continue }
            $rel = $value.Substring('sephiria_fantasy:'.Length) + '.png'
            $texPath = Join-Path (Join-Path $assets 'textures') ($rel -replace '/', '\')
            if (-not (Test-Path $texPath)) { Fail "models/block/$modelName.json texture $value is missing" }
        }

        # same uv guard as the item models: 0..16 only (Blockbench pixel space leaks in easily)
        if ($null -ne $model.elements) {
            foreach ($el in @($model.elements)) {
                foreach ($side in $el.faces.PSObject.Properties.Name) {
                    $uv = @($el.faces.$side.uv)
                    if ($uv.Count -ne 4) { Fail "models/block/$modelName.json: $side face without uv"; break }
                    foreach ($v in $uv) {
                        if ($v -lt 0 -or $v -gt 16) { Fail "models/block/$modelName.json: $side uv $v outside 0..16"; break }
                    }
                }
            }
        }

        Ok "blockstates/$id.json -> models/block/$modelName.json"
    }

    # 方块物品：定义文件 + 名字键（贴图走 textures/block，不在 $items 那套里）
    $defPath = Join-Path $assets "items\$id.json"
    if (-not (Test-Path $defPath)) { Fail "missing items/$id.json (the block's item)" }

    foreach ($lang in @('en_us', 'zh_cn')) {
        $keys = (ReadJson (Join-Path $assets "lang\$lang.json")).PSObject.Properties.Name
        if ($keys -notcontains "block.sephiria_fantasy.$id") { Fail "lang/$lang.json missing key block.sephiria_fantasy.$id" }
    }
}

# 2) every model file: valid geometry, existing palette, resolvable parent
foreach ($file in Get-ChildItem $modelsDir -Filter *.json) {
    $name = $file.BaseName
    $text = ReadText $file.FullName
    $json = $text | ConvertFrom-Json

    if ($json.parent -like 'sephiria_fantasy:item/*') {
        $parentName = $json.parent.Substring('sephiria_fantasy:item/'.Length)
        if (-not (Test-Path (Join-Path $modelsDir ($parentName + '.json')))) {
            Fail "models/item/$name.json parent '$($json.parent)' is missing"
        }
    }

    if ($null -ne $json.elements) {
        $elements = @($json.elements)
        foreach ($el in $elements) {
            if ((@($el.from)).Count -ne 3 -or (@($el.to)).Count -ne 3) { Fail "models/item/$name.json: malformed element"; break }
            foreach ($side in $el.faces.PSObject.Properties.Name) {
                $uv = @($el.faces.$side.uv)
                if ($uv.Count -ne 4) { Fail "models/item/$name.json: $side face without uv"; break }
                # UVs live in a 0..16 space regardless of texture size; Blockbench pixel
                # coordinates (e.g. 0..32 for a 32x32 palette) must be converted first,
                # otherwise the game samples outside the sprite and the item fails to bake.
                foreach ($v in $uv) {
                    if ($v -lt 0 -or $v -gt 16) { Fail "models/item/$name.json: $side uv $v outside 0..16 (pixel-space uv leaked in?)"; break }
                }
            }
        }
        $tex = $json.textures.'0'
        if ($tex -like 'sephiria_fantasy:item/*') {
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
    if ($json.parent -notlike 'sephiria_fantasy:item/*') { continue }
    $baseName = $json.parent.Substring('sephiria_fantasy:item/'.Length)
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
        if ($keys -notcontains "item.sephiria_fantasy.$id") { Fail "lang/$lang.json missing key item.sephiria_fantasy.$id" }
    }
    foreach ($b in $branches) {
        if ($keys -notcontains "sephiria_fantasy.branch.$b") { Fail "lang/$lang.json missing key sephiria_fantasy.branch.$b" }
    }
    # 提示框的格式（颜色、斜杠分隔、括号说明）都靠这几个键拼，占位符数量写错就会显示成 %s
    $placeholders = @{
        'tooltip.sephiria_fantasy.tip.label'         = 1
        'tooltip.sephiria_fantasy.tip.attack_state'  = 1
        'tooltip.sephiria_fantasy.tip.desc'          = 2
        'tooltip.sephiria_fantasy.part.damage'       = 2
        'tooltip.sephiria_fantasy.part.attack_speed' = 2
        'tooltip.sephiria_fantasy.part.range'        = 2
        'tooltip.sephiria_fantasy.part.cooldown'     = 1
        'tooltip.sephiria_fantasy.part.distance'     = 1
    }
    foreach ($key in $placeholders.Keys) {
        $line = $json.$key
        if (-not $line) { Fail "lang/$lang.json missing key $key"; continue }
        if (([regex]::Matches($line, '%s')).Count -ne $placeholders[$key]) {
            Fail "lang/$lang.json $key needs $($placeholders[$key]) placeholders"
        }
    }
    # 每一条文案里的 % 都必须是原版认识的三形式之一：%%（字面百分号）、%s、%N$s。
    # 裸一个 %（比如「+33%」「× 10%」）会让 TranslatableContents 抛 TranslatableFormatException，
    # **整行退回没格式化的原文**——连该替换的 %s 也一起原样显示（火红的夕阳当初就是这么坏的：
    # 「太阳剑投掷时有 %s%% 概率增大，那一发伤害 +33%」）。
    $barePercent = 0
    foreach ($prop in $json.PSObject.Properties) {
        $value = [string]$prop.Value
        $cursor = 0
        while ($cursor -lt $value.Length) {
            $at = $value.IndexOf('%', $cursor)
            if ($at -lt 0) { break }
            $rest = $value.Substring($at)
            $m = [regex]::Match($rest, '^%(?:\d+\$)?(?:[A-Za-z]|%)')
            if (-not $m.Success) {
                Fail "lang/$lang.json $($prop.Name) has a bare % - write it as %% (literal percent)"
                $barePercent++
                break
            }
            $cursor = $at + $m.Length
        }
    }
    if ($barePercent -gt 0) { Write-Output ("      affected keys: " + $barePercent) }

    Ok "lang/$lang.json checked ($($keys.Count) keys)"
}

if (-not (Test-Path (Join-Path $assets 'icon.png'))) { Fail "missing icon.png" }

# 5) data: crafting recipes, the tags they reference, and the recipe-book unlocks.
#    A recipe that fails to load is completely silent in game -- the pattern simply never
#    matches -- so the shape is checked here: pattern symbols must be declared, declared
#    symbols must be used, the result must be one of this mod's items, and every referenced
#    tag file must exist. Vanilla ids are checked against the Minecraft jars in the loom
#    cache when they are there, and skipped otherwise (the cache path differs per machine).
$dataDir = Join-Path $root 'src\main\resources\data\sephiria_fantasy'
$recipeDir = Join-Path $dataDir 'recipe'
$tagDir = Join-Path $dataDir 'tags\item'
$weaponItems = @('default_sword_and_shield', 'steel_greatsword', 'dagger', 'colossal_crossbow', 'blade', 'quarterstaff')

$vanillaItems = $null
$vanillaItemTags = $null
$clientJar = Join-Path $env:USERPROFILE '.gradle\caches\fabric-loom\26.2\minecraft-client.jar'
$serverJar = Join-Path $env:USERPROFILE '.gradle\caches\fabric-loom\26.2\minecraft-extracted_server.jar'
if ((Test-Path $clientJar) -and (Test-Path $serverJar)) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $vanillaItems = Get-JarNames $clientJar '^assets/minecraft/items/(.+)\.json$'
    $vanillaItemTags = Get-JarNames $serverJar '^data/minecraft/tags/item/(.+)\.json$'
    Ok ("vanilla id lists loaded (" + $vanillaItems.Count + " items, " + $vanillaItemTags.Count + " item tags)")
} else {
    Write-Output "note: Minecraft jars not in the loom cache -- vanilla id checks skipped"
}

# Returns the problem with one ingredient ("minecraft:iron_ingot" or "#minecraft:saplings"),
# or $null when it resolves.
function Test-Ingredient($value) {
    $tagged = $value.StartsWith('#')
    if ($tagged) { $value = $value.Substring(1) }

    $parts = $value.Split(':', 2)
    if ($parts.Count -ne 2) { return "$value has no namespace" }

    if ($parts[0] -eq 'minecraft') {
        if ($tagged) {
            if ($null -ne $vanillaItemTags -and -not $vanillaItemTags.Contains($parts[1])) { return "unknown vanilla item tag #$value" }
        } elseif ($null -ne $vanillaItems -and -not $vanillaItems.Contains($parts[1])) {
            return "unknown vanilla item $value"
        }
    } elseif ($parts[0] -eq 'sephiria_fantasy') {
        if ($tagged) {
            if (-not (Test-Path (Join-Path $tagDir ($parts[1] + '.json')))) { return "no tags/item/$($parts[1]).json for #$value" }
        } elseif (($items + $blocks) -notcontains $parts[1]) {
            return "unknown mod item $value"
        }
    } else {
        return "$value is in an unknown namespace"
    }

    return $null
}

$crafted = New-Object System.Collections.Generic.List[string]
foreach ($file in Get-ChildItem $recipeDir -Filter *.json) {
    $name = $file.BaseName
    $json = ReadJson $file.FullName

    if ($json.type -ne 'minecraft:crafting_shaped' -and $json.type -ne 'minecraft:crafting_shapeless') {
        Fail "recipe/$name.json: unsupported type '$($json.type)'"; continue
    }

    # 结果可以是本模组的物品，也可以是方块物品（$blocks 那一批）
    $result = $json.result.id
    $known = $items + $blocks
    if ($result -notlike 'sephiria_fantasy:*' -or $known -notcontains $result.Substring('sephiria_fantasy:'.Length)) {
        Fail "recipe/$name.json: result '$result' is not one of this mod's items"
    }
    $crafted.Add($result)

    # 无序配方（crafting_shapeless）：没有图案，ingredients 就是材料表——
    # 每一项要么是一个 id / #标签，要么是一个「可替换」的数组（数组里每一项都要合法）
    if ($json.type -eq 'minecraft:crafting_shapeless') {
        $slots = 0
        foreach ($ingredient in @($json.ingredients)) {
            $slots++
            if ($ingredient -is [string]) {
                $problem = Test-Ingredient $ingredient
                if ($problem) { Fail "recipe/$name.json: $problem" }
            } else {
                foreach ($option in @($ingredient)) {
                    $problem = Test-Ingredient $option
                    if ($problem) { Fail "recipe/$name.json: $problem" }
                }
            }
        }
        if ($slots -eq 0) { Fail "recipe/$name.json: no ingredients" }
        if ($slots -gt 9) { Fail "recipe/$name.json: $slots ingredients (max 9)" }
        Ok ("recipe/$name.json -> $result [shapeless: $slots ingredient(s)]")
        continue
    }

    $symbols = @($json.key.PSObject.Properties.Name)
    $pattern = @($json.pattern)
    if ($pattern.Count -eq 0 -or $pattern.Count -gt 3) { Fail "recipe/$name.json: pattern has $($pattern.Count) rows" }

    $used = New-Object System.Collections.Generic.HashSet[string]
    foreach ($row in $pattern) {
        if ($row.Length -ne $pattern[0].Length) { Fail "recipe/$name.json: pattern rows differ in width" }
        if ($row.Length -gt 3) { Fail "recipe/$name.json: row '$row' is wider than 3" }
        foreach ($ch in $row.ToCharArray()) {
            # empty cells are plain spaces; '.' is NOT special to the recipe codec and makes
            # the whole recipe fail to load (silently, apart from a log line)
            if ($ch -eq '.') { Fail "recipe/$name.json: '.' is not an empty cell, use a space"; continue }
            if ($ch -ne ' ') { [void]$used.Add([string]$ch) }
        }
    }

    foreach ($s in $used) {
        if ($symbols -notcontains $s) { Fail "recipe/$name.json: pattern uses '$s' but key does not declare it" }
    }
    foreach ($s in $symbols) {
        if (-not $used.Contains($s)) { Fail "recipe/$name.json: key '$s' is never used by the pattern" }
        $problem = Test-Ingredient $json.key.$s
        if ($problem) { Fail "recipe/$name.json: $problem" }
    }

    Ok ("recipe/$name.json -> $result [" + ($pattern -join '/') + "]")
}

foreach ($id in $weaponItems) {
    if (-not $crafted.Contains("sephiria_fantasy:$id")) { Fail "no recipe crafts sephiria_fantasy:$id" }
}

foreach ($file in Get-ChildItem $tagDir -Filter *.json) {
    $json = ReadJson $file.FullName
    foreach ($value in @($json.values)) {
        # A tag may also carry an object entry ({"id": ..., "required": ...}); only plain ids are used here.
        if ($value -isnot [string]) { Fail "tags/item/$($file.Name): object entries are not checked" ; continue }
        $problem = Test-Ingredient $value
        if ($problem) { Fail "tags/item/$($file.Name): $problem" }
    }
    Ok ("tags/item/" + $file.Name + " (" + @($json.values).Count + " values)")
}

$advancementDir = Join-Path $dataDir 'advancement'
foreach ($file in Get-ChildItem $advancementDir -Recurse -Filter *.json) {
    $json = ReadJson $file.FullName
    # 解锁条目指向不存在的合成表时游戏只记一条日志，玩家那边就是"配方书里没有"
    # display-only entries (tab roots, weapon index) carry no rewards -> nothing to unlock
    if ($null -eq $json.rewards) { Ok ("advancement/" + $file.Name + " (display only)"); continue }
    foreach ($recipe in @($json.rewards.recipes)) {
        if (-not $crafted.Contains($recipe)) { Fail ("advancement/" + $file.Name + ": unlocks '$recipe' which no recipe provides") }
    }
    Ok ("advancement/" + $file.Name + " unlocks " + @($json.rewards.recipes).Count + " recipe(s)")
}

Write-Output ""
if ($errors -eq 0) { Write-Output "ALL RESOURCE CHECKS PASSED" } else { Write-Output ("$errors PROBLEM(S) FOUND") }
exit $errors
