#!/usr/bin/env python3
"""Draws the built-in generic car: a brandless side view, its paint mask and pack.json.
Everything is drawn here, so the APK ships no photo or logo of a real make. Needs Pillow and numpy.
Usage: make_generic_car.py launcher/app/src/main/assets/packs/generico"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

REF_W, REF_H = 920, 350  # pack units (ref_width)
SS = 4                   # supersampling
OUT_W = 640
TOP, BOTTOM_CROP = 60, 340  # the part of the canvas that is kept
FRONT, REAR = (200, 268), (722, 268)
TIRE, RIM, ARCH = 66, 46, 78
BOTTOM = 300  # rocker line
SILVER = np.array([236, 239, 244], np.float32)


def bez(p0, p1, p2, p3, n=24):
    return [((1 - t) ** 3 * p0[0] + 3 * (1 - t) ** 2 * t * p1[0] + 3 * (1 - t) * t * t * p2[0] + t ** 3 * p3[0],
             (1 - t) ** 3 * p0[1] + 3 * (1 - t) ** 2 * t * p1[1] + 3 * (1 - t) * t * t * p2[1] + t ** 3 * p3[1])
            for t in (i / n for i in range(n + 1))]


def arc(c, r, a0, a1, n=48):
    return [(c[0] + r * math.cos(a), c[1] + r * math.sin(a)) for a in (a0 + (a1 - a0) * i / n for i in range(n + 1))]


def arch_span(c):
    """Where the wheel arch meets the rocker: right x, left x, and the angle of the right end."""
    dx = math.sqrt(ARCH ** 2 - (BOTTOM - c[1]) ** 2)
    return c[0] + dx, c[0] - dx, math.atan2(BOTTOM - c[1], dx)


def body_outline():
    pts = bez((44, BOTTOM), (30, 296), (24, 280), (24, 258))
    pts += bez((24, 258), (23, 236), (26, 218), (36, 208))
    pts += bez((36, 208), (44, 200), (60, 196), (80, 194))
    pts += bez((80, 194), (160, 184), (240, 174), (306, 166))
    pts += bez((306, 166), (372, 132), (420, 100), (468, 86))
    pts += bez((468, 86), (540, 74), (620, 72), (680, 78))
    pts += bez((680, 78), (730, 84), (770, 92), (796, 102))
    pts += bez((806, 108), (828, 124), (846, 146), (858, 166))
    pts += bez((858, 166), (876, 174), (890, 186), (893, 200))
    pts += bez((893, 200), (898, 222), (898, 248), (896, 266))
    pts += bez((896, 266), (894, 286), (886, 296), (870, BOTTOM))
    for c in (REAR, FRONT):
        rx, _, a = arch_span(c)
        pts += [(rx, BOTTOM)] + arc(c, ARCH, a, -math.pi - a)
    return pts


def glass_outline():
    pts = bez((330, 176), (372, 146), (414, 112), (470, 96))
    pts += bez((470, 96), (540, 86), (620, 84), (680, 90))
    pts += bez((680, 90), (712, 94), (736, 100), (752, 108))
    pts += bez((764, 150), (640, 158), (470, 168), (330, 176))
    return pts


def canvas(draw_fn):
    im = Image.new("L", (REF_W * SS, REF_H * SS), 0)
    draw_fn(ImageDraw.Draw(im))
    return np.asarray(im, dtype=np.float32) / 255


def sc(pts):
    return [(x * SS, y * SS) for x, y in pts]


def line(pts, width):
    return canvas(lambda d: d.line(sc(pts), fill=255, width=max(1, round(width * SS)), joint="curve"))


def poly(pts):
    return canvas(lambda d: d.polygon(sc(pts), fill=255))


def disk(c, r):
    return canvas(lambda d: d.ellipse(((c[0] - r) * SS, (c[1] - r) * SS, (c[0] + r) * SS, (c[1] + r) * SS), fill=255))


def rrect(cx, cy, hw, hh):
    return canvas(lambda d: d.rounded_rectangle(((cx - hw) * SS, (cy - hh) * SS, (cx + hw) * SS, (cy + hh) * SS),
                                                radius=hh * SS, fill=255))


def along(x, p0, p1):
    return p0[1] + (p1[1] - p0[1]) * (x - p0[0]) / (p1[0] - p0[0])


def paint_luminance(X, Y):
    """Bright upper surfaces, a crisp shoulder crease, darker toward the rocker: what PaintMath recolors."""
    shoulder = along(X, (110, 200), (880, 186))
    below = np.clip((Y - shoulder) / np.maximum(BOTTOM - shoulder, 1), 0, 1)
    lum = np.where(Y < shoulder, 0.74 - 0.06 * np.clip((shoulder - Y) / 60, 0, 1), 0.66 - 0.30 * below ** 1.2)
    lum += 0.26 * np.exp(-((Y - (shoulder - 3)) / 2.2) ** 2) - 0.10 * np.exp(-((Y - (shoulder + 6)) / 5) ** 2)
    lum += 0.05 * np.exp(-((Y - 250) / 10) ** 2) - 0.05 * np.exp(-((Y - 234) / 8) ** 2)
    lower = along(X, (290, 262), (650, 256))
    side = (X > 270) & (X < 660)
    lum += np.where(side, 0.10 * np.exp(-((Y - lower) / 2) ** 2) - 0.05 * np.exp(-((Y - lower - 5) / 4) ** 2), 0)
    lum *= 1 - 0.18 * np.clip((140 - X) / 120, 0, 1) - 0.14 * np.clip((X - 820) / 80, 0, 1)
    for c in (FRONT, REAR):  # shadow on the arch lip, a little flare above it
        r = np.hypot(X - c[0], Y - c[1])
        lum *= 1 - 0.40 * np.clip((ARCH + 5 - r) / 5, 0, 1) * (r >= ARCH)
        lum += 0.07 * np.exp(-((r - (ARCH + 8)) / 2.5) ** 2) * (Y < c[1])
    gaps = np.clip(line(bez((318, 172), (306, 212), (300, 250), (298, BOTTOM - 2)), 1.6)
                   + line([(566, 165), (570, 230), (572, BOTTOM - 2)], 1.6)
                   + line(bez((706, 156), (700, 182), (688, 200), (664, 208)), 1.6)
                   + line(bez((784, 98), (806, 124), (830, 158), (848, 194)), 1.4), 0, 1)
    lum *= 1 - 0.65 * gaps
    for hx, hy in ((480, 197), (650, 192)):  # door handles
        handle = rrect(hx, hy, 17, 3.5)
        lum = lum * (1 - handle) + 0.78 * handle
        lum *= 1 - 0.5 * line([(hx - 15, hy + 5), (hx + 15, hy + 5)], 1.4)
    return np.clip(lum, 0.05, 0.97)


def rim(c, X, Y):
    """A plain six-spoke silver rim of its own."""
    dx, dy = X - c[0], Y - c[1]
    r = np.hypot(dx, dy)
    a = np.arctan2(dy, dx)
    rel = np.abs(((a + math.pi / 2) % (2 * math.pi / 6)) - math.pi / 6)  # 0 on each spoke's axis
    half = (5.5 - 2.5 * np.clip((r - 10) / (RIM - 14), 0, 1)) / np.maximum(r, 1)
    out = np.zeros(r.shape + (3,), np.float32)
    out[r <= RIM - 3] = [30, 31, 34]
    out[(r <= RIM - 3) & (rel < half) & (r > 9)] = [190, 194, 200]
    out[(r <= RIM) & (r > RIM - 3)] = [206, 210, 216]
    out[r <= 11] = [150, 154, 160]
    out[r <= 5] = [70, 72, 76]
    out *= (1 + 0.18 * np.clip(-(dx + dy) / (RIM * 1.4), -1, 1))[..., None]
    return np.clip(out, 0, 255), (r <= RIM).astype(np.float32)


def draw():
    h, w = REF_H * SS, REF_W * SS
    Y, X = np.mgrid[0:h, 0:w].astype(np.float32) / SS
    body = poly(body_outline())
    rgb = SILVER * paint_luminance(X, Y)[..., None]
    alpha = body.copy()
    paint = body.copy()

    def over(m, color):
        m = np.clip(m, 0, 1)[..., None]
        rgb[:] = rgb * (1 - m) + np.asarray(color, np.float32) * m

    # Glass with a soft sky reflection, its black trim and pillars.
    glass = poly(glass_outline())
    gy = np.clip((Y - 90) / 85, 0, 1)
    streak = np.exp(-(((X - 0.9 * Y) - 360) / 40) ** 2) * 0.35 + np.exp(-(((X - 0.9 * Y) - 560) / 18) ** 2) * 0.2
    over(glass, np.stack([46 - 28 * gy, 52 - 32 * gy, 60 - 36 * gy], -1) + streak[..., None] * [120, 128, 138])
    over((poly([(558, 86), (576, 86), (574, 163), (558, 164)]) + poly([(694, 88), (703, 89), (709, 154), (700, 155)])) * glass,
         [16, 17, 19])
    over(line(glass_outline() + [glass_outline()[0]], 1.8) * body, [20, 21, 23])
    paint *= 1 - glass

    # Side mirror: painted cap on a black base.
    mirror = poly(bez((334, 174), (336, 160), (350, 152), (372, 152)) + [(378, 162)]
                  + bez((378, 162), (370, 172), (354, 176), (334, 174)))
    over(mirror, SILVER * np.clip(0.80 - 0.35 * (Y - 150) / 24, 0.3, 0.85)[..., None])
    mbase = poly([(330, 178), (336, 168), (352, 172), (344, 180)])
    over(mbase, [18, 19, 21])
    alpha = np.maximum(alpha, np.maximum(mirror, mbase))
    paint = np.clip(np.maximum(paint, mirror) - mbase, 0, 1)

    # Lights and dark plastic: never repainted.
    head = poly(bez((28, 216), (32, 206), (44, 200), (80, 196)) + [(150, 187), (178, 186)]
                + bez((178, 186), (160, 197), (110, 205), (60, 212)) + [(32, 222)])
    over(head, [88, 96, 108])
    over((disk((72, 207), 6) + disk((100, 202), 5)) * head, [36, 40, 48])
    over(line(bez((46, 204), (90, 197), (140, 190), (168, 188)), 2.4) * head, [250, 252, 255])
    tail = poly([(842, 170), (860, 170)] + bez((860, 170), (880, 178), (890, 190), (893, 202)) + [(893, 214), (868, 206), (846, 186)])
    over(tail, [150, 16, 24])
    over(line(bez((852, 178), (870, 181), (884, 190), (890, 202)), 2.0) * tail, [232, 70, 72])
    fx = arch_span(FRONT)[0]
    rx = arch_span(REAR)[1]
    plastic = np.clip(poly([(fx - 4, 286), (rx + 4, 284), (rx + 2, BOTTOM + 1), (fx - 2, BOTTOM + 1)])
                      + poly(bez((24, 272), (60, 278), (100, 282), (126, 284)) + [(124, BOTTOM + 1), (44, BOTTOM + 1)]
                             + bez((44, BOTTOM + 1), (30, 294), (24, 284), (24, 272)))
                      + poly([(796, 284), (866, 282), (896, 276), (890, 294), (870, BOTTOM + 1), (796, BOTTOM + 1)]), 0, 1) * body
    over(plastic, [24, 25, 27])
    paint *= 1 - np.clip(head + tail + plastic, 0, 1)

    # Wheels: the well (only inside the arch), the tire and the rim.
    for c in (FRONT, REAR):
        well = disk(c, ARCH - 1) * (Y <= BOTTOM)
        tire = disk(c, TIRE)
        r = np.hypot(X - c[0], Y - c[1])
        over(well, [9, 9, 11])
        over(tire, np.repeat((26 + 14 * np.exp(-((r - (TIRE - 9)) / 4) ** 2) + 6 * (Y < c[1]))[..., None], 3, -1))
        rim_rgb, rim_a = rim(c, X, Y)
        over(rim_a, rim_rgb)
        alpha = np.maximum(alpha, np.maximum(well, tire))
        paint *= 1 - np.clip(well + tire, 0, 1)
    return rgb, alpha, paint


def save(out, rgb, alpha, paint):
    sl = slice(TOP * SS, BOTTOM_CROP * SS)
    size = (OUT_W, round((BOTTOM_CROP - TOP) * OUT_W / REF_W))
    rgba = np.dstack([np.clip(rgb[sl], 0, 255), np.clip(alpha[sl], 0, 1) * 255]).astype(np.uint8)
    # Premultiplied while scaling, so the edges don't pick up the black behind the transparent pixels.
    Image.fromarray(rgba, "RGBA").convert("RGBa").resize(size, Image.LANCZOS).convert("RGBA") \
        .save(os.path.join(out, "base.webp"), lossless=True, method=6)
    Image.fromarray((np.clip(paint[sl], 0, 1) * 255).astype(np.uint8), "L").resize(size, Image.LANCZOS) \
        .convert("RGB").save(os.path.join(out, "paint.webp"), quality=90, method=6)


def option(oid, es, en, pt, color=None):
    o = {"id": oid, "name": es, "name_en": en, "name_pt": pt}
    if color:
        o["color"] = color
    return o


PAINTS = [("original", "Plata", "Silver", "Prata"), ("negro", "Negro", "Black", "Preto", "#16171A"),
          ("blanco", "Blanco", "White", "Branco", "#F2F2F0"), ("gris", "Gris", "Grey", "Cinza", "#4A4D52"),
          ("rojo", "Rojo", "Red", "Vermelho", "#B5121B"), ("azul", "Azul", "Blue", "Azul", "#143C8C"),
          ("verde", "Verde", "Green", "Verde", "#123A2C"), ("naranja", "Naranja", "Orange", "Laranja", "#D9571A"),
          ("amarillo", "Amarillo", "Yellow", "Amarelo", "#D8A514")]
WHEELS = [("original", "Originales", "Original", "Originais"), ("5-rayos", "5 rayos", "5-spoke", "5 raios"),
          ("5-doble-rayo", "5 doble rayo", "5 double-spoke", "5 raios duplos"),
          ("7-doble-rayo", "7 doble rayo", "7 double-spoke", "7 raios duplos"),
          ("10-rayos", "10 rayos", "10-spoke", "10 raios"),
          ("5-rayos-antracita", "5 rayos antracita", "5-spoke anthracite", "5 raios grafite"),
          ("5-doble-negra", "5 doble negra", "5 double-spoke black", "5 raios duplos pretos")]


def main(out):
    os.makedirs(out, exist_ok=True)
    save(out, *draw())
    pack = {
        "id": os.path.basename(os.path.normpath(out)), "brand": "",
        "title": "Mi auto", "title_en": "My car", "title_pt": "Meu carro", "ref_width": REF_W,
        "defaults": {"body": "", "engine": "", "year": ""},
        "paint": {"gain": 1.4, "gamma": 1.35, "spec": 0.24},
        "paints": [option(*p) for p in PAINTS],
        "wheels": [option(*w) for w in WHEELS],
        # The crop only moves things up: x stays in pack units.
        "wheel_centers": [{"cx": c[0], "cy": c[1] - TOP, "rx": RIM, "ry": RIM, "angle": 0} for c in (FRONT, REAR)],
    }
    with open(os.path.join(out, "pack.json"), "w", encoding="utf-8") as f:
        json.dump(pack, f, ensure_ascii=False, indent=1)
        f.write("\n")


if __name__ == "__main__":
    main(sys.argv[1])
