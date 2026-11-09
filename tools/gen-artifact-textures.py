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
        icon = process(BASE / filename)
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
