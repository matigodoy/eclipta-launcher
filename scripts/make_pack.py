#!/usr/bin/env python3
"""Builds a vehicle pack's image layers from a cutout photo (transparent background).

Usage: make_pack.py cutout.png scripts/packs/<id>.json launcher/app/src/main/assets/packs/<id>
Needs Pillow and numpy. Re-encoding drops EXIF. Polygons are in `ref_width` pixels:
  exclude = never painted (glass, lights, grille, wheels)
  blank   = solid fill (ALWAYS cover the real license plate)
  darken  = dim and blur (windows that reflect other things)
Outputs base.webp (photo) and paint.webp (white = body paint).
"""
import json
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

WIDTH = 640


def shape_mask(size, scale, item):
    m = Image.new("L", size, 0)
    d = ImageDraw.Draw(m)
    if "poly" in item:
        d.polygon([(x * scale, y * scale) for x, y in item["poly"]], fill=255)
    else:
        cx, cy, rx, ry = [v * scale for v in item["ellipse"]]
        d.ellipse((cx - rx, cy - ry, cx + rx, cy + ry), fill=255)
    return m


def main(src, cfg_path, out):
    cfg = json.load(open(cfg_path))
    im = Image.open(src).convert("RGBA")
    bbox = im.getchannel("A").point(lambda v: 255 if v > 20 else 0).getbbox()
    im = im.crop(bbox)
    scale_px = WIDTH / im.width
    im = im.resize((WIDTH, round(im.height * scale_px)), Image.LANCZOS)
    scale = WIDTH / cfg["ref_width"]

    for b in cfg.get("blank", []):
        m = shape_mask(im.size, scale, b).filter(ImageFilter.GaussianBlur(0.8))
        alpha = im.getchannel("A")
        fill = Image.new("RGBA", im.size, tuple(b["color"]) + (255,))
        im = Image.composite(fill, im, m)
        im.putalpha(alpha)
    for b in cfg.get("darken", []):
        m = shape_mask(im.size, scale, b).filter(ImageFilter.GaussianBlur(1.5))
        alpha = im.getchannel("A")
        dim = im.convert("L").filter(ImageFilter.GaussianBlur(b.get("blur", 3))).point(lambda v: int(v * b["factor"]))
        im = Image.composite(Image.merge("RGBA", (dim, dim, dim, alpha)), im, m)
        im.putalpha(alpha)

    a = np.asarray(im).astype(np.float32) / 255
    luma = a[..., 0] * 0.299 + a[..., 1] * 0.587 + a[..., 2] * 0.114
    sat = a[..., :3].max(axis=2) - a[..., :3].min(axis=2)
    p = cfg["paint"]
    mask = np.clip((luma - p["min_luma"] + 0.08) / 0.16, 0, 1) if p["min_luma"] > 0 else np.ones_like(luma)
    mask *= np.clip((p["max_sat"] - sat) / 0.08, 0, 1)
    mask *= a[..., 3]
    excl = Image.new("L", im.size, 0)
    for item in cfg["exclude"]:
        excl = Image.fromarray(np.maximum(np.asarray(excl), np.asarray(shape_mask(im.size, scale, item))))
    excl = np.asarray(excl.filter(ImageFilter.GaussianBlur(1.0))).astype(np.float32) / 255
    mask *= 1 - excl
    paint = Image.fromarray((mask * 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(0.6))

    os.makedirs(out, exist_ok=True)
    im.save(os.path.join(out, "base.webp"), quality=86, method=6, exact=False)
    Image.merge("RGB", (paint, paint, paint)).save(os.path.join(out, "paint.webp"), quality=75, method=6)
    print(im.size, os.path.getsize(os.path.join(out, "base.webp")), os.path.getsize(os.path.join(out, "paint.webp")))


if __name__ == "__main__":
    main(*sys.argv[1:4])
