# Writes the six item model definitions: the flat sprite for GUI-like contexts and the voxel
# model while held, selected by display_context (same approach vanilla uses for spears).
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

$root = Split-Path -Parent $PSScriptRoot
$dir = Join-Path $root 'src\main\resources\assets\sephiria\items'

$ids = @(
    'default_sword_and_shield',
    'steel_greatsword',
    'dagger',
    'colossal_crossbow',
    'blade',
    'quarterstaff'
)

$template = @'
{
	"model": {
		"type": "minecraft:select",
		"property": "minecraft:display_context",
		"cases": [
			{
				"when": [ "gui", "ground", "fixed", "on_shelf" ],
				"model": { "type": "minecraft:model", "model": "sephiria:item/__ID__" }
			}
		],
		"fallback": { "type": "minecraft:model", "model": "sephiria:item/__ID___in_hand" }
	}
}
'@

foreach ($id in $ids) {
    $json = $template.Replace('__ID__', $id)
    $path = Join-Path $dir ($id + '.json')
    [IO.File]::WriteAllText($path, $json, (New-Object System.Text.UTF8Encoding($false)))
    Write-Output ("wrote " + $id + '.json')
}
