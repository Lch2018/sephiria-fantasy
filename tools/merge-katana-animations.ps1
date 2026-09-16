# Merge the katana's side-exported animations into the main animation file.
#
# The Blockbench project exports animations either all together or one at a time, so the
# sheathed attack animation arrives as its own file (katana1.animation.json). GeckoLib
# looks animations up by name inside the file returned from getAnimationResource, so the
# extra file has to be merged into that one file.
#
# Blockbench also marks exported animations as loop:true by default; an attack animation
# must play once, so the merged copy is forced to loop:false.
#
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

$root = Split-Path -Parent $PSScriptRoot
$export = Join-Path $root 'tmp_fix\export'
$resAnims = Join-Path $root 'src\main\resources\assets\sephiria\geckolib\animations'
$keep = Join-Path $root 'models'

$mainPath = Join-Path $resAnims 'katana.animation.json'
$extra = Join-Path $export 'katana1.animation.json'

if (-not (Test-Path $extra)) { throw "missing side export: $extra" }

# archive the author's file next to the model, so the source is traceable
Copy-Item $extra (Join-Path $keep 'katana1.animation.json') -Force

$main = Get-Content $mainPath -Raw -Encoding UTF8 | ConvertFrom-Json
$add = Get-Content $extra -Raw -Encoding UTF8 | ConvertFrom-Json

foreach ($prop in $add.animations.PSObject.Properties) {
    $prop.Value.loop = $false
    $main.animations | Add-Member -NotePropertyName $prop.Name -NotePropertyValue $prop.Value -Force
    Write-Output ('merged ' + $prop.Name)
}

$json = $main | ConvertTo-Json -Depth 24
[System.IO.File]::WriteAllText($mainPath, $json, (New-Object System.Text.UTF8Encoding($false)))

# verify: everything must still parse, and both states' loops must be present
$check = Get-Content $mainPath -Raw -Encoding UTF8 | ConvertFrom-Json
Write-Output '--- animations now in katana.animation.json ---'
foreach ($prop in $check.animations.PSObject.Properties) {
    $bones = ($prop.Value.bones.PSObject.Properties | ForEach-Object { $_.Name }) -join ','
    Write-Output ('  ' + $prop.Name.PadRight(32) + ' len=' + $prop.Value.animation_length + '  loop=' + $prop.Value.loop + '  bones=' + $bones)
}
