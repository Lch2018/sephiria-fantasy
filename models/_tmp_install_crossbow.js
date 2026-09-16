// Install the user's 3D crossbow into the mod:
//   colossal_crossbow_in_hand  (idle, self-contained: elements + display + texture)
//   crossbow_pulling_0/1/2     (string drawn +1.2/+2.4/+3.5, bolt hidden)
//   crossbow_loaded_in_hand    (string drawn +3.5, bolt visible)
// Plus: simplify the sword-and-shield item definition (nothing can render empty anymore).
const fs = require('fs');
const root = 'E:/workspace/Zcode/mcmode';
const itemDir = root + '/src/main/resources/assets/sephiria/models/item';
const texDir = root + '/src/main/resources/assets/sephiria/textures/item';

const bb = JSON.parse(fs.readFileSync(root + '/models/colossal_crossbow.bbmodel', 'utf8'));
const vec = a => `[ ${a.join(', ')} ]`.replace(/\[ ([\d.eE+-]+), ([\d.eE+-]+), ([\d.eE+-]+) \]/, '[$1, $2, $3]');
const v3 = a => `[${a.join(', ')}]`;
const tex = 'sephiria:item/colossal_crossbow_3d';

// vanilla crossbow hand transforms
const display = {
	thirdperson_righthand: { rotation: [-90, 0, -60], translation: [2, 0.1, -3], scale: [0.9, 0.9, 0.9] },
	thirdperson_lefthand: { rotation: [-90, 0, 30], translation: [2, 0.1, -3], scale: [0.9, 0.9, 0.9] },
	firstperson_righthand: { rotation: [-90, 0, -55], translation: [1.13, 3.2, 1.13], scale: [0.68, 0.68, 0.68] },
	firstperson_lefthand: { rotation: [-90, 0, 35], translation: [1.13, 3.2, 1.13], scale: [0.68, 0.68, 0.68] },
};

const STRING = ['弦左', '弦右'];
const BOLT = ['箭尖', '箭杆'];

const elementLine = (e, dz) => {
	const from = [e.from[0], e.from[1], e.from[2] + dz];
	const to = [e.to[0], e.to[1], e.to[2] + dz];
	const faces = Object.entries(e.faces)
		.map(([k, f]) => `"${k}": { "texture": "#0", "uv": ${v3(f.uv)} }`)
		.join(', ');
	let rot = '';
	if (e.rotation && e.rotation.some(v => v !== 0)) {
		const i = e.rotation.findIndex(v => v !== 0);
		// MC only accepts 0/±22.5/±45 per axis: snap anything else (the ±20 strings) to the nearest legal value.
		const legal = [-45, -22.5, 0, 22.5, 45];
		const raw = e.rotation[i];
		const angle = legal.reduce((a, b) => Math.abs(b - raw) < Math.abs(a - raw) ? b : a);
		rot = `, "rotation": { "origin": ${v3(e.origin)}, "axis": "${['x', 'y', 'z'][i]}", "angle": ${angle} }`;
	}
	return `\t\t{ "from": ${v3(from)}, "to": ${v3(to)}${rot}, "faces": { ${faces} } }`;
};

const modelHeader = () => [
	'{', '\t"gui_light": "front",', '\t"ambientocclusion": false,',
	'\t"textures": {', `\t\t"0": "${tex}",`, `\t\t"particle": "${tex}"`, '\t},',
];
const displayBlock = () => [
	'\t"display": {',
	...Object.entries(display).map(([slot, t], i, arr) =>
		`\t\t"${slot}": { "rotation": ${vec(t.rotation)}, "translation": ${vec(t.translation)}, "scale": ${vec(t.scale)} }${i < arr.length - 1 ? ',' : ''}`),
	'\t},',
];

function writeModel(name, stage) {
	// stage: 0 idle, 1..3 pulling, 4 loaded
	const dz = stage === 0 ? 0 : [0, 1.2, 2.4, 3.5][stage];
	const showBolt = stage === 4;
	const els = bb.elements
		.filter(e => showBolt || !BOLT.includes(e.name))
		.map(e => elementLine(e, STRING.includes(e.name) ? dz : 0));
	const body = [...modelHeader(), ...(stage === 0 ? displayBlock() : ['\t"parent": "sephiria:item/colossal_crossbow_in_hand",']), '\t"elements": [', els.join(',\n'), '\t]', '}', ''];
	fs.writeFileSync(`${itemDir}/${name}.json`, body.join('\n'));
	console.log(name, '->', els.length, 'elements', stage === 0 ? '(idle, own display)' : `(string +${dz}, bolt ${showBolt ? 'shown' : 'hidden'})`);
}

writeModel('colossal_crossbow_in_hand', 0);
writeModel('crossbow_pulling_0', 1);
writeModel('crossbow_pulling_1', 2);
writeModel('crossbow_pulling_2', 3);
writeModel('crossbow_loaded_in_hand', 4);

// texture
const src = (bb.textures[0].source || '').match(/^data:image\/png;base64,(.+)$/);
if (src) {
	fs.writeFileSync(texDir + '/colossal_crossbow_3d.png', Buffer.from(src[1], 'base64'));
	console.log('texture -> colossal_crossbow_3d.png', Buffer.from(src[1], 'base64').length, 'bytes');
}

// sword & shield: simplest possible split, nothing can render empty
const swordShield = {
	model: {
		type: 'minecraft:select',
		property: 'minecraft:display_context',
		cases: [
			{ when: ['gui', 'ground', 'fixed', 'on_shelf'], model: { type: 'minecraft:model', model: 'sephiria:item/default_sword_and_shield' } },
			{ when: ['firstperson_lefthand', 'thirdperson_lefthand'], model: { type: 'minecraft:model', model: 'sephiria:item/shield_in_hand' } }
		],
		fallback: { type: 'minecraft:model', model: 'sephiria:item/sword_in_hand' }
	}
};
fs.writeFileSync(root + '/src/main/resources/assets/sephiria/items/default_sword_and_shield.json', JSON.stringify(swordShield, null, '\t') + '\n');
console.log('sword&shield: simplified split (main=sword, off=shield, no empty model)');
