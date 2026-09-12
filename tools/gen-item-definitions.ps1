# Writes the six item model definitions.
#
# GUI-like contexts always keep the flat sprite; what happens while held depends on the weapon:
#   - plain weapons  : one voxel model
#   - sword and shield: the shield in the off hand, the sword in the main hand
#   - crossbow       : vanilla-style charge states driven by charge_type / crossbow pull
#   - katana         : sheathed, drawing and drawn states driven by the use duration
# ASCII-only on purpose: Windows PowerShell 5.1 reads non-ASCII .ps1 as GBK.

$root = Split-Path -Parent $PSScriptRoot
$dir = Join-Path $root 'src\main\resources\assets\sephiria\items'

function Write-Item($id, $json) {
    [IO.File]::WriteAllText((Join-Path $dir ($id + '.json')), $json, (New-Object System.Text.UTF8Encoding($false)))
    Write-Output ("wrote " + $id + '.json')
}

$guiWhen = '"when": [ "gui", "ground", "fixed", "on_shelf" ]'

# ---- plain weapons -------------------------------------------------------------------
foreach ($id in @('steel_greatsword', 'dagger', 'quarterstaff')) {
    Write-Item $id @"
{
	"model": {
		"type": "minecraft:select",
		"property": "minecraft:display_context",
		"cases": [
			{
				$guiWhen,
				"model": { "type": "minecraft:model", "model": "sephiria:item/$id" }
			}
		],
		"fallback": { "type": "minecraft:model", "model": "sephiria:item/${id}_in_hand" }
	}
}
"@
}

# ---- sword and shield: shield in the off hand, sword in the main hand -----------------
Write-Item 'default_sword_and_shield' @"
{
	"model": {
		"type": "minecraft:select",
		"property": "minecraft:display_context",
		"cases": [
			{
				$guiWhen,
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
"@

# ---- crossbow: vanilla charge states --------------------------------------------------
Write-Item 'colossal_crossbow' @"
{
	"model": {
		"type": "minecraft:select",
		"property": "minecraft:display_context",
		"cases": [
			{
				$guiWhen,
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
"@

# ---- katana: sheathed / drawing / drawn ----------------------------------------------
Write-Item 'blade' @"
{
	"model": {
		"type": "minecraft:select",
		"property": "minecraft:display_context",
		"cases": [
			{
				$guiWhen,
				"model": { "type": "minecraft:model", "model": "sephiria:item/blade" }
			}
		],
		"fallback": {
			"type": "minecraft:condition",
			"property": "minecraft:using_item",
			"on_true": {
				"type": "minecraft:range_dispatch",
				"property": "minecraft:use_duration",
				"entries": [
					{ "threshold": 6, "model": { "type": "minecraft:model", "model": "sephiria:item/katana_in_hand" } }
				],
				"fallback": { "type": "minecraft:model", "model": "sephiria:item/katana_drawing" }
			},
			"on_false": { "type": "minecraft:model", "model": "sephiria:item/katana_sheathed" }
		}
	}
}
"@
