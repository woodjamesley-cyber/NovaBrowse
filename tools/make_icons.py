#!/usr/bin/env python3
"""Build NovaBrowse launcher icon bitmaps from the generated brand artwork."""
import os
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "tools", "icons", "source.png")
RES = os.path.join(ROOT, "app", "src", "main", "res")

DENSITIES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
LEGACY = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def rounded(img, radius_ratio=0.225):
    size = img.size[0]
    mask = Image.new("L", (size * 4, size * 4), 0)
    d = ImageDraw.Draw(mask)
    d.rounded_rectangle((0, 0, size * 4 - 1, size * 4 - 1), radius=int(size * 4 * radius_ratio), fill=255)
    mask = mask.resize((size, size), Image.LANCZOS)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def circle(img):
    size = img.size[0]
    mask = Image.new("L", (size * 4, size * 4), 0)
    d = ImageDraw.Draw(mask)
    d.ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
    mask = mask.resize((size, size), Image.LANCZOS)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def main():
    src = Image.open(SRC).convert("RGBA")
    side = min(src.size)
    src = src.crop(((src.width - side) // 2, (src.height - side) // 2,
                    (src.width + side) // 2, (src.height + side) // 2))

    for density, px in DENSITIES.items():
        folder = os.path.join(RES, f"mipmap-{density}")
        os.makedirs(folder, exist_ok=True)
        # Adaptive foreground: artwork sits inside the 66dp safe zone of the 108dp layer.
        art = src.resize((int(px * 0.80), int(px * 0.80)), Image.LANCZOS)
        fg = Image.new("RGBA", (px, px), (0, 0, 0, 0))
        fg.paste(rounded(art, 0.23), ((px - art.width) // 2, (px - art.height) // 2), rounded(art, 0.23))
        fg.save(os.path.join(folder, "ic_launcher_foreground.png"))

        legacy_px = LEGACY[density]
        legacy = rounded(src.resize((legacy_px, legacy_px), Image.LANCZOS), 0.225)
        legacy.save(os.path.join(folder, "ic_launcher.png"))
        circle(src.resize((legacy_px, legacy_px), Image.LANCZOS)).save(
            os.path.join(folder, "ic_launcher_round.png"))

    # Full-resolution brand art used in-app (about screen / extension store header).
    drawable = os.path.join(RES, "drawable-nodpi")
    os.makedirs(drawable, exist_ok=True)
    src.resize((512, 512), Image.LANCZOS).save(os.path.join(drawable, "nova_brand_mark.png"))
    src.save(os.path.join(drawable, "nova_brand_full.png"))
    print("icons written")


if __name__ == "__main__":
    main()
