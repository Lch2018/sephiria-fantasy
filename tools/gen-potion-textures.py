"""把桌面的 75×75 药水图标处理成 16×16 透明物品贴图。

源图：外框 #222034、内部背景 #36263A、中间是原作的药水像素图。
处理规则（用户 2026-11-09 口径）：
  - 直接去掉所有 #222034（黑色边框）与 #36263A（背景）；
  - 不再额外补描边；
  - 裁到内容后等比缩进 16×16 并居中，不越界。

用法：python tools/gen-potion-textures.py
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image

SRC = Path(r"C:\Users\28237\Desktop\赛菲莉亚素材\药水")
DST = Path("src/main/resources/assets/sephiria_fantasy/textures/item")
SHEET = Path("tools/potion-textures-sheet.png")

IDS = {
    "马迪朗的平静.png": "madiran_calm",
    "格里塞特的浅淡.png": "grisette_pale",
    "贵兹的合奏.png": "gewurz_ensemble",
    "内比奥罗的执着.png": "nebbiolo_persistence",
    "乌尼·布兰的雾霭.png": "ugni_blanc_mist",
    "Tempranillo的热情.png": "tempranillo_passion",
    "马尔贝克的浓郁.png": "malbec_richness",
    "蘑菇汤.png": "mushroom_soup",
    "马德拉的坚韧.png": "madeira_fortitude",
    "萨格兰蒂诺的忍耐.png": "sagrantino_patience",
    "普萨的留白.png": "poulsard_whitespace",
    "黎明之露.png": "dawn_dew",
}

OUTER = (34, 32, 52)   # 源图外框 #222034
INNER = (54, 38, 58)   # 药水源图内部背景 #36263A


def remove_background(image: Image.Image) -> Image.Image:
    """直接清掉源图里的 #222034 黑色边框与 #36263A 背景。"""
    rgba = image.convert("RGBA")
    px = rgba.load()
    width, height = rgba.size

    for y in range(height):
        for x in range(width):
            if px[x, y][:3] in (OUTER, INNER):
                px[x, y] = (0, 0, 0, 0)

    return rgba


def process(path: Path) -> Image.Image:
    icon = remove_background(Image.open(path))
    icon = icon.crop(icon.getbbox())
    icon.thumbnail((15, 15), Image.Resampling.NEAREST)

    canvas = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    canvas.paste(icon, ((16 - icon.width) // 2, (16 - icon.height) // 2), icon)
    return canvas


def main() -> None:
    DST.mkdir(parents=True, exist_ok=True)
    sheet = Image.new("RGBA", (16 * 6, 16 * 2), (245, 245, 245, 255))

    for index, (name, item_id) in enumerate(IDS.items()):
        icon = process(SRC / name)
        icon.save(DST / f"{item_id}.png")
        sheet.alpha_composite(icon, ((index % 6) * 16, (index // 6) * 16))

    sheet.resize((sheet.width * 8, sheet.height * 8), Image.Resampling.NEAREST).save(SHEET)
    print("wrote", len(IDS), "textures to", DST)


if __name__ == "__main__":
    main()
