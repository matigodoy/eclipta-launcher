#!/usr/bin/env python3
"""Draws the shared wheel overlays (front-on rims, warped per pack by the app). Needs Pillow.
Usage: make_wheels.py launcher/app/src/main/assets/wheels"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFilter

S = 720  # supersampled
OUT = 180
R = S / 2


def spoke(d, ang, w0, w1, r0, r1, col):
    c, s = math.cos(ang), math.sin(ang)
    nx, ny = -s, c
    pts = [(R + c * r0 + nx * w0, R + s * r0 + ny * w0), (R + c * r1 + nx * w1, R + s * r1 + ny * w1),
           (R + c * r1 - nx * w1, R + s * r1 - ny * w1), (R + c * r0 - nx * w0, R + s * r0 - ny * w0)]
    d.polygon(pts, fill=col)


def rim(name, draw_spokes, metal=(196, 200, 206), dark=(26, 27, 30)):
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse((4, 4, S - 4, S - 4), fill=metal + (255,))  # lip
    d.ellipse((34, 34, S - 34, S - 34), fill=(tuple(int(v * 0.72) for v in metal)) + (255,))
    d.ellipse((50, 50, S - 50, S - 50), fill=dark + (255,))  # barrel
    draw_spokes(d, metal)
    hub = 66
    d.ellipse((R - hub, R - hub, R + hub, R + hub), fill=tuple(int(v * 0.85) for v in metal) + (255,))
    d.ellipse((R - 34, R - 34, R + 34, R + 34), fill=(70, 72, 76, 255))
    for i in range(5):  # lug nuts
        a = i * 2 * math.pi / 5 - math.pi / 2
        x, y = R + math.cos(a) * 50, R + math.sin(a) * 50
        d.ellipse((x - 7, y - 7, x + 7, y + 7), fill=(50, 52, 56, 255))
    # soft top-left light so it doesn't read flat
    lt = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(lt).ellipse((60, 30, S - 200, S - 260), fill=(255, 255, 255, 46))
    lt = lt.filter(ImageFilter.GaussianBlur(60))
    mask = im.getchannel("A")
    im = Image.alpha_composite(im, Image.composite(lt, Image.new("RGBA", (S, S), (0, 0, 0, 0)), mask))
    im.resize((OUT, OUT), Image.LANCZOS).save(os.path.join(sys.argv[1], name + ".webp"), quality=88, method=6)


def five(d, m):
    for i in range(5):
        a = i * 2 * math.pi / 5 - math.pi / 2
        spoke(d, a, 46, 30, 40, 318, m + (255,))


def double(d, m):
    for i in range(5):
        a = i * 2 * math.pi / 5 - math.pi / 2
        for o in (-0.15, 0.15):
            spoke(d, a + o, 22, 15, 40, 318, m + (255,))


def turbine(d, m):
    for i in range(10):
        a = i * 2 * math.pi / 10
        spoke(d, a, 14, 26, 40, 318, m + (255,))


def multi(d, m):
    for i in range(7):
        a = i * 2 * math.pi / 7 - math.pi / 2
        for o in (-0.12, 0.12):
            spoke(d, a + o * 0.7, 18, 12, 40, 318, m + (255,))


if __name__ == "__main__":
    os.makedirs(sys.argv[1], exist_ok=True)
    rim("5-rayos", five)
    rim("5-doble-rayo", double)
    rim("10-rayos", turbine)
    rim("7-doble-rayo", multi)
    rim("5-rayos-antracita", five, metal=(92, 95, 100))
    rim("5-doble-negra", double, metal=(44, 45, 48))
