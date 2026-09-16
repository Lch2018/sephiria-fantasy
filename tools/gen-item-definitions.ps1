# Writes the six item model definitions.
#
#   sword and shield : one item, sword in the main hand, shield in the off hand
#   other melee      : flat art in gui/ground/fixed/on_shelf, the 3D voxel model in hand
#   blade            : flat art only (its voxel model is not wired up)
#   crossbow         : flat art in gui/ground/fixed/on_shelf, the 3D voxel model in hand
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

$handSelect = @'
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
		"fallback": { "type": "minecraft:model", "model": "sephiria:item/__HAND__" }
	}
}
'@

$swordAndShield = @'
{
	"model": {
		"type": "minecraft:select",
		"property": "minecraft:display_context",
		"cases": [
			{
				"when": [ "gui", "ground", "fixed", "on_shelf" ],
				"model": { "type": "minecraft:model", "model": "sephiria:item/default_sword_and_shield" }
			},
			{
				"when": [ "firstperson_lefthand", "thirdperson_lefthand" ],
				"model": { "type": "minecraft:model", "model": "sephiria:item/shield_in_hand" }
			}
		],
		"fallback": { "type": "minecraft:model", "model": "sephiria:item/sword_in_hand" }
	}
}
'@

$withHand = [ordered]@{
    'steel_greatsword' = 'steel_greatsword_in_hand'
    'dagger'           = 'dagger_in_hand'
    'quarterstaff'     = 'quarterstaff_in_hand'
}
foreach ($id in $withHand.Keys) {
    Write-Item $id ($handSelect.Replace('__ID__', $id).Replace('__HAND__', $withHand[$id]))
}

Write-Item 'default_sword_and_shield' $swordAndShield

# flat art only: the voxel model is not wired into the item yet
Write-Item 'blade' ($flat.Replace('__ID__', 'blade'))

# crossbow: flat art outside the hand; in hand it shows the bolted model. Reloading puts the
# item on cooldown, and the minecraft:cooldown progress drives the baked animation frames
# (colossal_crossbow_reload_0..10 = magazine drop + tumble), so the reload has a visible
# duration without needing a held use action.
$crossbow = @'
{
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
			"type": "minecraft:range_dispatch",
			"property": "minecraft:cooldown",
			"entries": [
				{ "threshold": 0.1, "model": { "type": "minecraft:model", "model": "sephiria:item/colossal_crossbow_reload_9" } },
				{ "threshold": 0.2, "model": { "type": "minecraft:model", "model": "sephiria:item/colossal_crossbow_reload_8" } },
				{ "threshold": 0.3, "model": { "type": "minecraft:model", "model": "sephiria:item/colossal_crossbow_reload_7" } },
				{ "threshold": 0.4, "model": { "type": "minecraft:model", "model": "sephiria:item/colossal_crossbow_reload_6" } },
				{ "threshold": 0.5, "model": { "type": "minecraft:model", "model": "sephiria:item/colossal_crossbow_reload_5" } },
				{ "threshold": 0.6, "model": { "type": "minecraft:model", "model": "sephiria:item/colossal_crossbow_reload_4" } },
				{ "threshold": 0.7, "model": { "type": "minecraft:model", "model": "sephiria:item/colossal_crossbow_reload_3" } },
				{ "threshold": 0.8, "model": { "type": "minecraft:model", "model": "sephiria:item/colossal_crossbow_reload_2" } },
				{ "threshold": 0.9, "model": { "type": "minecraft:model", "model": "sephiria:item/colossal_crossbow_reload_1" } },
				{ "threshold": 1.0, "model": { "type": "minecraft:model", "model": "sephiria:item/colossal_crossbow_reload_0" } }
			],
			"fallback": { "type": "minecraft:model", "model": "sephiria:item/colossal_crossbow_in_hand" }
		}
	}
}
'@
Write-Item 'colossal_crossbow' $crossbow
