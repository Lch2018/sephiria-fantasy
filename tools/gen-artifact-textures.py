"""把桌面的神器图标处理成 16×16 透明物品贴图，并生成物品定义/模型 JSON。

源图：外框 #222034、内部背景 #32313E、中间是原作的神器像素图（自带深色边）。
处理规则（用户 2026-11-09 口径）：
  - 去掉与画布边缘连通的外框 #222034；
  - 去掉内部背景 #32313E；
  - 保留图像自带的深色边，不再额外补描边；
  - 裁到内容后等比缩进 16×16 并居中，不越界。
"""

from __future__ import annotations

import json
from pathlib import Path

from PIL import Image, ImageDraw

BASE = Path(r"C:\Users\28237\.zcode\cli\image-cache\session_855df452020fe2e95669613fb9ab60a1")
DESKTOP = Path(r"C:\Users\28237\Desktop\赛菲莉亚素材\赛菲莉娅神器")
TEXTURES = Path("src/main/resources/assets/sephiria_fantasy/textures/item")
ITEMS = Path("src/main/resources/assets/sephiria_fantasy/items")
MODELS = Path("src/main/resources/assets/sephiria_fantasy/models/item")
SHEET = Path("tools/artifact-textures-sheet.png")

ICONS = {
    "broken_sapphire": "image-43febc1ff752e48a6b19707f6c336c5e.png",
    "sharpshooter_crank": "image-8528a3f30173c1e7109724b66a34c5b0.png",
    "sturdy_reload_crate": "image-e726e5df0f7829db4dc50e828e0b68d9.png",
    "masters_sword_guard": "image-da47984eb7cc8c01745166b395d82283.png",
    "rainbow_feather": "image-5b5ec5e344d64df3a35caa5444bd099d.png",
    "absolute_ring": "image-9d573dd5f3292701deffdaa5ec17dea4.png",
    "wings": "image-a6cf79eee972441f44df2e310c92cdd8.png",
    "beiruts_scythe": "image-7c9493f7921b8d8e9de1244c315a617c.png",
    "barts_monocle": "image-a79d6d574607a62174f5591af5759684.png",
    "black_scale": "image-44cda4dea2335965abee972a6edced80.png",
    "fire_bolt": "image-54908545213259ce7a7d380c39eaab61.png",
    "ice_bolt": "image-804ea0aa3049381be60678a24a9cf419.png",
    "lightning_bolt": "image-bf7b60f73fc2c3983f422dd55b70c82f.png",
    "flame_arrow": "image-ced7e4534279e05e788ca2e3c071e49b.png",
    "frost_knife": "image-ce54c09d4e02b717d53c7d0de9adcc7c.png",
    "lightning_boomerang": "image-e8994d1edc23d15c11bf9886539178e7.png",
    "fire_circus": "image-11c99559133a7bdd8f4d219e397cb96b.png",
    "arrow_rain": "image-fe21ae384465a76ccd92fbeee76707b0.png",
    "thunder_armor": "image-e368dbeec547f59a63e9afac3470dc27.png",
    "meteor_shower": "image-b9f47fb07974887f78f6ef23ed52cf1d.png",
    "ice_storm": "image-09a05255d42ef036face3306c131a18a.png",
    "thunder_verdict": "image-2770d68e0f8db6ae4a5bb7b40e6a9da6.png",
    "magicians_coin": "image-163f3402336e191c39f2cb41d21c63a5.png",
    "ray_star_fragment": "image-906946b6d6b7d288db41f64d4695448d.png",
    "mouse_magician_pendant": "image-476b356ade8299ac58a8f17196df5323.png",
    # 学院第二批（2026-11-13）：素材直接来自桌面文件夹
    "pearl_powder": DESKTOP / "珍珠粉.png",
    "orb_of_desire": DESKTOP / "渴望宝珠.png",
    "crabapple": DESKTOP / "海棠果.png",
    "double_horn_pipe": DESKTOP / "双角管.png",
    "tuning_fork": DESKTOP / "音叉.png",
    "jewel_armor": DESKTOP / "宝石盔甲.png",
    "glowing_hourglass": DESKTOP / "发光的沙漏.png",
    "battle_mage_gloves": DESKTOP / "战斗法师的手套.png",
    "meditation_book": DESKTOP / "冥想书.png",
    "academy_pen": DESKTOP / "学院钢笔.png",
    "library_sun": DESKTOP / "书库之阳.png",
    "empty_hilt": DESKTOP / "空剑柄.png",
    "leilis_pocket_watch": DESKTOP / "莱莉的怀表.png",
    "wilted_flower": DESKTOP / "枯萎的花.png",
    "endodite_paperweight": DESKTOP / "恩多迪特的镇纸.png",
    "scarecrow_doll": DESKTOP / "稻草人偶.png",
    "star_ruby": DESKTOP / "星红宝石.png",
    "star_sea_sapphire": DESKTOP / "星海蓝宝石.png",
    "water_bag": DESKTOP / "水袋.png",
    "lure_bait": DESKTOP / "诱惑的鱼饵.png",
    "obras_blood": DESKTOP / "奥布拉斯之血.png",
    "broken_root": DESKTOP / "破碎的树根.png",
    "heart_carrot": DESKTOP / "心形胡萝卜.png",
    "white_branch": DESKTOP / "白色树枝.png",
    "stone_spindle": DESKTOP / "石纺锤.png",
    "full_fruit_bucket": DESKTOP / "饱满果实桶.png",
    "jolo_watering_can": DESKTOP / "乔洛的浇水壶.png",
    "water_spirit": DESKTOP / "水之精灵.png",
    "plitvice_water_drop": DESKTOP / "普利特维采的水滴.png",
    "cloth_armor": DESKTOP / "布甲.png",
    "shield": DESKTOP / "护盾.png",
    "shield_earring": DESKTOP / "盾牌耳环.png",
    "broken_mirror": DESKTOP / "破碎的镜子.png",
    "thorn_shell": DESKTOP / "荆棘壳.png",
    "serens_letter": DESKTOP / "瑟伦潦草的信.png",
    "pot_lid": DESKTOP / "锅盖.png",
    "academy_chainmail": DESKTOP / "学院锁子甲.png",
    "lizard_plate_armor": DESKTOP / "蜥蜴板甲.png",
    "small_mirror": DESKTOP / "小镜子.png",
    "white_edge_bread": DESKTOP / "白边面包.png",
    "beast_heart": DESKTOP / "野兽之心.png",
    "shield_bag": DESKTOP / "盾牌包.png",
    "rock_elephant": DESKTOP / "岩象.png",
}

OUTER = (34, 32, 52)   # 源图外框 #222034
INNER = (50, 49, 62)   # 神器源图内部背景 #32313E


def remove_background(image: Image.Image) -> Image.Image:
    """去掉与边缘连通的 #222034 外框，再去掉所有 #32313E 内部背景。"""
    rgba = image.convert("RGBA")
    px = rgba.load()
    width, height = rgba.size

    frame = Image.new("L", rgba.size, 0)
    fp = frame.load()

    for y in range(height):
        for x in range(width):
            if px[x, y][:3] == OUTER:
                fp[x, y] = 255

    for x in range(width):
        for y in (0, height - 1):
            if fp[x, y] == 255:
                ImageDraw.floodfill(frame, (x, y), 128)
    for y in range(height):
        for x in (0, width - 1):
            if fp[x, y] == 255:
                ImageDraw.floodfill(frame, (x, y), 128)

    for y in range(height):
        for x in range(width):
            if fp[x, y] == 128 or px[x, y][:3] == INNER:
                px[x, y] = (0, 0, 0, 0)

    return rgba


def process(path: Path) -> Image.Image:
    icon = remove_background(Image.open(path))
    icon = icon.crop(icon.getbbox())
    icon.thumbnail((14, 14), Image.Resampling.NEAREST)

    canvas = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    canvas.paste(icon, ((16 - icon.width) // 2, (16 - icon.height) // 2), icon)
    return canvas


def main() -> None:
    TEXTURES.mkdir(parents=True, exist_ok=True)
    ITEMS.mkdir(parents=True, exist_ok=True)
    MODELS.mkdir(parents=True, exist_ok=True)

    sheet = Image.new("RGBA", (16 * len(ICONS), 16), (245, 245, 245, 255))

    for index, (item_id, filename) in enumerate(ICONS.items()):
        icon = process(filename if isinstance(filename, Path) else BASE / filename)
        icon.save(TEXTURES / f"{item_id}.png")
        sheet.alpha_composite(icon, (index * 16, 0))

        item_def = {"model": {"type": "minecraft:model", "model": f"sephiria_fantasy:item/{item_id}"}}
        model = {"parent": "minecraft:item/generated",
                 "textures": {"layer0": f"sephiria_fantasy:item/{item_id}"}}
        (ITEMS / f"{item_id}.json").write_text(
            json.dumps(item_def, ensure_ascii=False, indent="\t") + "\n", encoding="utf-8")
        (MODELS / f"{item_id}.json").write_text(
            json.dumps(model, ensure_ascii=False, indent="\t") + "\n", encoding="utf-8")

    sheet.resize((sheet.width * 8, 128), Image.Resampling.NEAREST).save(SHEET)
    print("wrote", len(ICONS), "artifact textures")


if __name__ == "__main__":
    main()
