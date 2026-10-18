# Apply the slates described in tools/slate-generator/out/slates.json to the mod sources.
#
# Flow: the user designs slates in the Slate Generator GUI -> pastes the key text -> the assistant
# saves it as tools/slate-generator/inbox/keys.txt, runs tools/slate-generator/parse-keys.ps1,
# fills id / nameEn / descEn in out/slates.json, and finally runs this script.
#
# This script patches, for every slate (each step is skipped when it is already there):
#   src/main/java/com/sephiria/slate/Slates.java         the pattern constant
#   src/main/java/com/sephiria/registry/ModItems.java    the item + registerSlate() call
#   src/main/java/com/sephiria/registry/ModCreativeTabs.java  the slates creative tab entry
#   src/main/java/com/sephiria/artifact/ArtifactLoot.java     the slate loot pool (chests / mining)
#   assets/.../lang/zh_cn.json + en_us.json              name + flavor keys
#   assets/.../items/<id>.json + models/item/<id>.json   the item model definitions
#   tools/check-resources.ps1                            the id list the checks walk
#   tools/import-artifact-icons.ps1                      a job for the icon (+ the 'slates' root)
# and then runs the icon import for the new slates (16x16 cut from the inbox reference).
#
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

param(
    [string]$File = '',
    [switch]$SkipIcons
)

$ErrorActionPreference = 'Stop'

$generatorDir = $PSScriptRoot
$root = Split-Path -Parent (Split-Path -Parent $generatorDir)

if (-not $File) { $File = Join-Path $generatorDir 'out\slates.json' }

if (-not (Test-Path $File)) {
    throw "slate draft not found: $File (run parse-keys.ps1 first)"
}

$rarityJava = @{ common = 'COMMON'; advanced = 'ADVANCED'; rare = 'RARE'; legendary = 'LEGENDARY'; eternal = 'ETERNAL' }

# the chinese labels the generated java comments use live in labels.zh.json: this script stays
# ASCII-only on purpose (PS 5.1 reads a non-BOM .ps1 as GBK and would mangle inline chinese)
$labelsPath = Join-Path $generatorDir 'labels.zh.json'

if (-not (Test-Path $labelsPath)) { throw "missing labels file: $labelsPath" }

$labels = [IO.File]::ReadAllText($labelsPath, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
$rarityZh = @{
    common = $labels.common; advanced = $labels.advanced; rare = $labels.rare
    legendary = $labels.legendary; eternal = $labels.eternal
}
$rotateZh = @{ yes = $labels.rotatable; no = $labels.fixed }

function Read-Text([string]$path) {
    return [IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
}

function Test-Bom([string]$path) {
    # the patched files must keep their byte-order mark: check-resources.ps1 HAS one (PS 5.1 reads a
    # BOM-less non-ascii .ps1 as GBK and the mangled chinese comments eat the next code line - this
    # exact failure cost a debugging round on 2026-10-18)
    $bytes = [IO.File]::ReadAllBytes($path)

    return ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF)
}

function Write-Lines([string]$path, $lines, [bool]$bom) {
    $encoding = New-Object System.Text.UTF8Encoding($bom)
    [IO.File]::WriteAllText($path, (($lines -join "`n") + "`n"), $encoding)
}

function Read-Lines([string]$path) {
    $lines = @([IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8) -split "`n")

    for ($i = 0; $i -lt $lines.Count; $i++) { $lines[$i] = $lines[$i].TrimEnd("`r") }

    # drop the empty tail left by the final newline - Write-Lines adds exactly one back
    if ($lines.Count -gt 1 -and $lines[$lines.Count - 1] -eq '') {
        $lines = @($lines[0..($lines.Count - 2)])
    } elseif ($lines.Count -eq 1 -and $lines[0] -eq '') {
        $lines = @()
    }

    return $lines
}

function Escape-Json([string]$value) {
    $text = $value.Replace('\', '\\').Replace('"', '\"').Replace("`r", '').Replace("`n", '\n')
    return $text
}

# ---- load + validate the draft ----
$draft = [IO.File]::ReadAllText($File, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
$slates = @($draft)
$problems = New-Object System.Collections.ArrayList

foreach ($s in $slates) {
    if (-not $s.id) { [void]$problems.Add($s.name + ': id is empty') }
    elseif ($s.id -notmatch '^[a-z][a-z0-9_]*$') { [void]$problems.Add($s.name + ': id must be lower_snake_case (' + $s.id + ')') }

    if (-not $s.nameEn) { [void]$problems.Add($s.name + ': nameEn is empty') }
    # the flavor is optional now: v2 keys have no desc (the slate description is generated in
    # EffectLines), only old v1 keys carry an author written flavor - then its english is required
    if ($s.desc -and -not $s.descEn) { [void]$problems.Add($s.name + ': descEn is empty') }
    if (-not $rarityJava.ContainsKey($s.rarity)) { [void]$problems.Add($s.name + ': bad rarity ' + $s.rarity) }
    if (@($s.cells).Count -eq 0) { [void]$problems.Add($s.name + ': no cells') }
}

if ($problems.Count -gt 0) {
    foreach ($problem in $problems) { Write-Output ('MISSING: ' + $problem) }
    throw 'the draft is incomplete - fill the fields above in ' + $File + ' and run again'
}

# ---- file texts (read once, patch in memory, write once) ----
$slatesJava = Join-Path $root 'src\main\java\com\sephiria\slate\Slates.java'
$modItems = Join-Path $root 'src\main\java\com\sephiria\registry\ModItems.java'
$modTabs = Join-Path $root 'src\main\java\com\sephiria\registry\ModCreativeTabs.java'
$lootJava = Join-Path $root 'src\main\java\com\sephiria\artifact\ArtifactLoot.java'
$checkScript = Join-Path $root 'tools\check-resources.ps1'
$importScript = Join-Path $root 'tools\import-artifact-icons.ps1'
$langDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\lang'
$defDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\items'
$modelDir = Join-Path $root 'src\main\resources\assets\sephiria_fantasy\models\item'

$slatesLines = Read-Lines $slatesJava
$modItemsLines = Read-Lines $modItems
$modTabsLines = Read-Lines $modTabs
$lootLines = Read-Lines $lootJava
$checkLines = Read-Lines $checkScript
$importLines = Read-Lines $importScript
$zhLines = Read-Lines (Join-Path $langDir 'zh_cn.json')
$enLines = Read-Lines (Join-Path $langDir 'en_us.json')

$checkBom = Test-Bom $checkScript
$importBom = Test-Bom $importScript
$slatesBom = Test-Bom $slatesJava
$modItemsBom = Test-Bom $modItems
$modTabsBom = Test-Bom $modTabs
$lootBom = Test-Bom $lootJava
$zhBom = Test-Bom (Join-Path $langDir 'zh_cn.json')
$enBom = Test-Bom (Join-Path $langDir 'en_us.json')

$iconIds = New-Object System.Collections.ArrayList
$applied = New-Object System.Collections.ArrayList

function Insert-Before($lines, [string]$pattern, $block) {
    # returns the new array, inserting $block before the first line matching $pattern
    $out = New-Object System.Collections.ArrayList
    $done = $false

    foreach ($line in $lines) {
        if (-not $done -and $line -match $pattern) {
            foreach ($b in $block) { [void]$out.Add($b) }
            $done = $true
        }

        [void]$out.Add($line)
    }

    if (-not $done) { throw "anchor not found in the target file: $pattern" }

    return $out.ToArray()
}

function Insert-After($lines, [string]$pattern, $block) {
    $out = New-Object System.Collections.ArrayList
    $done = $false

    foreach ($line in $lines) {
        [void]$out.Add($line)

        if (-not $done -and $line -match $pattern) {
            foreach ($b in $block) { [void]$out.Add($b) }
            $done = $true
        }
    }

    if (-not $done) { throw "anchor not found in the target file: $pattern" }

    return $out.ToArray()
}

function Insert-After-Last($lines, [string]$pattern, $block) {
    # needed when several slates arrive at once: the first insert moves the plain anchor (for
    # example 'Slate.HANDSHAKE, true);' stops being the last declaration), so anchor on the LAST
    # line that matches instead - the new entries then keep the file's order
    $target = -1

    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match $pattern) { $target = $i }
    }

    if ($target -lt 0) { throw "anchor not found in the target file: $pattern" }

    $out = New-Object System.Collections.ArrayList

    for ($i = 0; $i -lt $lines.Count; $i++) {
        [void]$out.Add($lines[$i])

        if ($i -eq $target) {
            foreach ($b in $block) { [void]$out.Add($b) }
        }
    }

    return $out.ToArray()
}

function Insert-LangKeys($lines, $entries) {
    # insert before the closing brace (the last non-empty line); keep the trailing comma valid
    $entries = @($entries)

    # the last inserted key must NOT keep its trailing comma (that would leave "{ ..., }")
    if ($entries.Count -gt 0) {
        $last = $entries[$entries.Count - 1].TrimEnd()

        if ($last.EndsWith(',')) { $last = $last.Substring(0, $last.Length - 1) }

        $entries[$entries.Count - 1] = $last
    }

    $out = New-Object System.Collections.ArrayList
    $lastKey = -1

    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i].TrimStart().StartsWith('"')) { $lastKey = $i }
    }

    if ($lastKey -lt 0) { throw 'lang file has no key lines' }

    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($i -eq $lastKey) {
            $line = $lines[$i]

            if (-not $line.TrimEnd().EndsWith(',')) { $line = $line.TrimEnd() + ',' }

            [void]$out.Add($line)

            foreach ($entry in $entries) { [void]$out.Add($entry) }

            continue
        }

        [void]$out.Add($lines[$i])
    }

    return $out.ToArray()
}

foreach ($s in $slates) {
    $id = $s.id
    $upper = $id -replace '^slate_of_', ''
    $const = $upper.ToUpperInvariant()
    $rotText = $rotateZh.no
    $rotJava = 'false'

    if ($s.rotatable) {
        $rotText = $rotateZh.yes
        $rotJava = 'true'
    }

    $changed = New-Object System.Collections.ArrayList

    # a) Slates.java - the pattern constant
    if ((Read-Text $slatesJava).Contains('Cell> ' + $const + ' =')) {
        [void]$changed.Add('Slates.java: already there')
    } else {
        $block = New-Object System.Collections.ArrayList
        [void]$block.Add('')
        [void]$block.Add("`t/** " + ($labels.docPattern -f $s.name, $rarityZh[$s.rarity], $rotText, $s.desc) + ' */')
        [void]$block.Add("`tpublic static final List<PatternSlateItem.Cell> " + $const + " = List.of(")
        $temp = New-Object System.Collections.ArrayList
        $count = 0

        foreach ($cell in $s.cells) {
            $count++
            $suffix = ','

            if ($count -eq @($s.cells).Count) { $suffix = ');' }

            [void]$temp.Add("`t`t`tnew PatternSlateItem.Cell(" + $cell[0] + ', ' + $cell[1] + ', ' + $cell[2] + ')' + $suffix)
        }

        $block = @($block) + @($temp)
        $slatesLines = Insert-Before $slatesLines 'private Slates\(\) \{' $block
        [void]$changed.Add('Slates.java: + ' + $const)
    }

    # b) ModItems.java - the item + registerSlate() call
    if ((Read-Text $modItems).Contains('Item ' + $id.ToUpperInvariant() + ' = registerSlate(')) {
        [void]$changed.Add('ModItems.java: already there')
    } else {
        $itemConst = $id.ToUpperInvariant()
        # NOTE: the concatenation must stay in parentheses - an unparenthesised 'a' + $x inside an
        # array literal is parsed as several elements (the classic PS 5.1 trap this repo has hit
        # twice: see the handbook's JSON section). The declaration wraps before Slates.* to match
        # the hand-written ones.
        $block = @(
            '',
            ("`t/** " + ($labels.docItem -f $s.name, $rarityZh[$s.rarity], $s.desc) + ' */'),
            ("`tpublic static final Item " + $itemConst + ' = registerSlate(' + [char]34 + $id + [char]34 +
                ', ArtifactRarity.' + $rarityJava[$s.rarity] + ','),
            ("`t`t`tSlates." + $const + ', ' + $rotJava + ');')
        )
        # the anchor is the last slate declaration's closing line (the private helper below keeps its
        # own javadoc, so new declarations must stay above that javadoc)
        $modItemsLines = Insert-After-Last $modItemsLines 'Slates\.[A-Z_]+, (true|false)\);\s*$' $block
        [void]$changed.Add('ModItems.java: + ' + $itemConst)
    }

    # c) ModCreativeTabs.java - the slates tab
    if ((Read-Text $modTabs).Contains('ModItems.' + $id.ToUpperInvariant() + ');')) {
        [void]$changed.Add('ModCreativeTabs.java: already there')
    } else {
        $block = @(("`t`t`t`t`t`toutput.accept(ModItems." + $id.ToUpperInvariant() + ');'))
        $modTabsLines = Insert-After-Last $modTabsLines 'output\.accept\(ModItems\.SLATE_OF_[A-Z_]+\);' $block
        [void]$changed.Add('ModCreativeTabs.java: + accept')
    }

    # d) ArtifactLoot.java - the loot pool (chests / mining / wish pool read it)
    #    the guard tests the token with its following separator: every generated entry but the last
    #    one is followed by ',' so a plain 'ModItems.X)' test re-inserts it on the next run
    $lootToken = 'ModItems.' + $id.ToUpperInvariant()

    if ((Read-Text $lootJava).Contains($lootToken + ',') -or (Read-Text $lootJava).Contains($lootToken + ')')) {
        [void]$changed.Add('ArtifactLoot.java: already there')
    } else {
        # the anchor is the closing of slates()'s List.of(...): anchoring on the hand-written
        # SLATE_OF_HANDSHAKE stops matching as soon as one generated slate is in there
        $text = $lootLines -join "`n"
        $functionStart = $text.IndexOf('private static List<Item> slates()')

        if ($functionStart -lt 0) { throw 'anchor not found: ArtifactLoot.slates()' }

        $listStart = $text.IndexOf('return List.of(', $functionStart)

        if ($listStart -lt 0) { throw 'anchor not found: ArtifactLoot slates() List.of' }

        $close = $text.IndexOf(');', $listStart)

        if ($close -lt 0) { throw 'anchor not found: ArtifactLoot slates() closing' }

        $insertion = ',' + "`n" + "`t`t`t`tModItems." + $id.ToUpperInvariant()
        $text = $text.Substring(0, $close) + $insertion + $text.Substring($close)
        $lootLines = @($text -split "`n")
        [void]$changed.Add('ArtifactLoot.java: + pool')
    }

    # e) lang keys
    $zhKey = 'item.sephiria_fantasy.' + $id
    $enKey = $zhKey

    if ((Read-Text (Join-Path $langDir 'zh_cn.json')).Contains('"' + $zhKey + '"')) {
        [void]$changed.Add('lang: already there')
    } else {
        $zhEntries = @(
            ('    "' + $zhKey + '":  "' + (Escape-Json $s.name) + '",'),
            ('    "artifact.sephiria_fantasy.' + $id + '.flavor":  "' + (Escape-Json $s.desc) + '",')
        )
        $enEntries = @(
            ('    "' + $enKey + '":  "' + (Escape-Json $s.nameEn) + '",'),
            ('    "artifact.sephiria_fantasy.' + $id + '.flavor":  "' + (Escape-Json $s.descEn) + '",')
        )
        $zhLines = Insert-LangKeys $zhLines $zhEntries
        $enLines = Insert-LangKeys $enLines $enEntries
        [void]$changed.Add('lang: + name/flavor')
    }

    # f) check-resources.ps1 - the id list
    if ((Read-Text $checkScript).Contains("'" + $id + "'")) {
        [void]$changed.Add('check-resources.ps1: already there')
    } else {
        $out = New-Object System.Collections.ArrayList

        foreach ($line in $checkLines) {
            [void]$out.Add($line.Replace("'slate_of_handshake', ", "'slate_of_handshake', '" + $id + "', "))
        }

        $checkLines = $out.ToArray()
        [void]$changed.Add('check-resources.ps1: + id')
    }

    # g) item model definitions
    New-Item -ItemType Directory -Force -Path $defDir, $modelDir | Out-Null

    $defFile = Join-Path $defDir ($id + '.json')

    if (-not (Test-Path $defFile)) {
        $defLines = @('{', '  "model": {', '    "type": "minecraft:model",',
                ('    "model": "sephiria_fantasy:item/' + $id + '"'), '  }', '}')
        Write-Lines $defFile $defLines $false
        [void]$changed.Add('items/' + $id + '.json')
    }

    $modelFile = Join-Path $modelDir ($id + '.json')

    if (-not (Test-Path $modelFile)) {
        $modelLines = @('{', '  "parent": "minecraft:item/generated",', '  "textures": {',
                ('    "layer0": "sephiria_fantasy:item/' + $id + '"'), '  }', '}')
        Write-Lines $modelFile $modelLines $false
        [void]$changed.Add('models/item/' + $id + '.json')
    }

    # h) the icon: copy it to an ascii name inside the inbox + queue an import job
    if ($s.icon -and $s.icon -ne '-') {
        $source = Join-Path $generatorDir ($s.icon -replace '/', '\')

        if (-not (Test-Path $source)) {
            [void]$changed.Add('ICON MISSING: ' + $s.icon)
        } elseif ($SkipIcons) {
            [void]$changed.Add('icon skipped (-SkipIcons)')
        } else {
            $ext = [System.IO.Path]::GetExtension($source)
            if (-not $ext) { $ext = '.png' }

            $asciiName = $id + $ext
            Copy-Item -LiteralPath $source -Destination (Join-Path $generatorDir ('inbox\' + $asciiName)) -Force
            [void]$iconIds.Add($id)

            # the guards read the IN-MEMORY lines, not the file: a batch of several slates edits the
            # same array repeatedly while the file is only written once at the end (checking the file
            # made every slate re-insert the root block - 2026-10-18)
            if (($importLines -join "`n").Contains("name = '" + $id + "'")) {
                [void]$changed.Add('import job: already there')
            } else {
                # the 'slates' root has to exist once
                if (-not ($importLines -join "`n").Contains('$slatesRef')) {
                    $importLines = Insert-After $importLines "\`$eclipseRef = Join-Path" @(
                        '',
                        '# the Slate Generator inbox (tools/slate-generator/inbox): the user drops slate art',
                        '# there through the GUI, then apply-slates.ps1 copies it to <id>.png before importing.',
                        "`$slatesRef = Join-Path `$root 'tools\slate-generator\inbox'"
                    )
                    $replaced = New-Object System.Collections.ArrayList

                    foreach ($line in $importLines) {
                        [void]$replaced.Add($line.Replace(
                            "`$sourceRoot = if (`$job.root -eq 'eclipse')",
                            "`$sourceRoot = if (`$job.root -eq 'slates') { `$slatesRef } elseif (`$job.root -eq 'eclipse')"))
                    }

                    $importLines = $replaced.ToArray()
                }

                # the job goes inside the $jobs array: right before the ')' that closes it
                $closeIndex = -1

                for ($i = 0; $i -lt $importLines.Count; $i++) {
                    if ($importLines[$i] -match '^\$skipped = @\(\)') { break }
                    if ($importLines[$i] -match '^\)\s*$') { $closeIndex = $i }
                }

                if ($closeIndex -lt 0) { throw 'could not find the end of the $jobs array in import-artifact-icons.ps1' }

                $jobLines = @(
                    '',
                    ('    # slate batch (' + $id + '): cut from the Slate Generator inbox reference'),
                    ("    @{ name = '" + $id + "'; root = 'slates'; source = '" + $asciiName + "';"),
                    "       target = 16; dir = 'item' }"
                )
                $importLines = @($importLines[0..($closeIndex - 1)]) + $jobLines + @($importLines[$closeIndex..($importLines.Count - 1)])
                [void]$changed.Add('import job: + ' + $id)
            }
        }
    } else {
        [void]$changed.Add('no icon (key says -) - the icon job has to be added later')
    }

    [void]$applied.Add($s.name + ' [' + $id + '] -> ' + ($changed -join ', '))
}

# ---- write everything back ----
Write-Lines $slatesJava $slatesLines $slatesBom
Write-Lines $modItems $modItemsLines $modItemsBom
Write-Lines $modTabs $modTabsLines $modTabsBom
Write-Lines $lootJava $lootLines $lootBom
Write-Lines $checkScript $checkLines $checkBom
Write-Lines (Join-Path $langDir 'zh_cn.json') $zhLines $zhBom
Write-Lines (Join-Path $langDir 'en_us.json') $enLines $enBom
Write-Lines $importScript $importLines $importBom

Write-Output ('applied ' + $slates.Count + ' slate(s):')

foreach ($line in $applied) { Write-Output ('  ' + $line) }

# ---- cut the 16x16 icons ----
if ($iconIds.Count -gt 0) {
    Write-Output ''
    Write-Output ('running the icon import for: ' + ($iconIds -join ', '))
    & powershell -NoProfile -ExecutionPolicy Bypass -File $importScript -Only ($iconIds -join ',')
} else {
    Write-Output ''
    Write-Output 'no icons to cut.'
}

Write-Output ''
Write-Output 'next: gradlew.bat compileJava  then  gradlew.bat modelDecodeCheck  then  tools/check-resources.ps1'
