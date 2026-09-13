# Writes the six item model definitions.
#
#   melee weapons : the flat sprite everywhere (art plus the calibrated hand display transforms);
#                   the 3D voxel models in models/item/*_in_hand.json are still shipped but unused
#   crossbow      : vanilla crossbow's state machine (charge_type, crossbow pull), driven by the
#                   vanilla models with recoloured textures
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

$root = Split-Path -Parent $PSScriptRoot
$dir = Join-Path $root 'src\main\resources\assets\sephiria\items'

function Write-Item($id, $json) {
    [IO.File]::WriteAllText((Join-Path $dir ($id + '.json')), $json, (New-Object System.Text.UTF8Encoding($false)))
    Write-Output ("wrote " + $id + '.json')
}

$flat = @'
{
	"model": {
		"type": "minecraft:model",
		"model": "sephiria:item/__ID__"
	}
}
'@

foreach ($id in @('default_sword_and_shield', 'steel_greatsword', 'dagger', 'quarterstaff', 'blade')) {
    Write-Item $id ($flat.Replace('__ID__', $id))
}

Write-Item 'colossal_crossbow' {
	"model": {
		"type": "minecraft:select",
		"property": "minecraft:display_context",
		"cases": [
			{
				"when": [ "gui", "ground", "fixed", "on_shelf" ],
				"model": { "type": "minecraft:model", "model": "sephiria:item/colossal_crossbow" }
			}
		],
		"fallback": {
			"type": "minecraft:select",
			"property": "minecraft:charge_type",
			"cases": [
				{ "when": "arrow", "model": { "type": "minecraft:model", "model": "sephiria:item/crossbow_loaded_in_hand" } },
				{ "when": "rocket", "model": { "type": "minecraft:model", "model": "sephiria:item/crossbow_loaded_in_hand" } }
			],
			"fallback": {
				"type": "minecraft:condition",
				"property": "minecraft:using_item",
				"on_true": {
					"type": "minecraft:range_dispatch",
					"property": "minecraft:crossbow/pull",
					"entries": [
						{ "threshold": 0.58, "model": { "type": "minecraft:model", "model": "sephiria:item/crossbow_pulling_1" } },
						{ "threshold": 1.0, "model": { "type": "minecraft:model", "model": "sephiria:item/crossbow_pulling_2" } }
					],
					"fallback": { "type": "minecraft:model", "model": "sephiria:item/crossbow_pulling_0" }
				},
				"on_false": { "type": "minecraft:model", "model": "sephiria:item/colossal_crossbow_in_hand" }
			}
		}
	}
}
