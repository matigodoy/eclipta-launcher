#!/usr/bin/env python3
"""Checks every vehicle pack: valid pack.json, required layers, size limits. Standard library only."""
import json
import os
import re
import sys

MAX_BYTES = 200 * 1024
REQUIRED = ("title", "brand", "ref_width", "defaults", "paints", "wheels")


def check(path):
    errors = []
    name = os.path.basename(path)
    if not re.fullmatch(r"[a-z0-9][a-z0-9-]{0,63}", name):
        errors.append("folder name must be lowercase letters, digits and dashes")
    try:
        pack = json.load(open(os.path.join(path, "pack.json"), encoding="utf-8"))
    except (OSError, ValueError) as e:
        return [f"pack.json: {e}"]
    for key in REQUIRED:
        if key not in pack:
            errors.append(f"pack.json: missing {key}")
    if pack.get("id", name) != name:
        errors.append("pack.json: id must match the folder name")
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9 -]{0,23}", str(pack.get("brand", "x"))):
        errors.append("pack.json: bad brand")
    for f in ("logo.webp", "sting.webp"):
        if os.path.exists(os.path.join(path, f)):
            errors.append(f"{f}: third-party logos and animations are not accepted in the repo (see docs/08-packs.md)")
    if "plate" in pack and len(pack["plate"]) != 8:
        errors.append("pack.json: plate needs 8 numbers")
    for p in pack.get("paints", []):
        if "color" in p and not re.fullmatch(r"#[0-9A-Fa-f]{6}", p["color"]):
            errors.append(f"pack.json: bad color {p['color']}")
    for f in ("base.webp", "paint.webp"):
        full = os.path.join(path, f)
        if not os.path.isfile(full):
            errors.append(f"missing {f}")
        elif os.path.getsize(full) > MAX_BYTES:
            errors.append(f"{f} is over {MAX_BYTES // 1024} KB")
    return errors


def main(root):
    failed = False
    for name in sorted(os.listdir(root)):
        path = os.path.join(root, name)
        if not os.path.isdir(path):
            continue
        errors = check(path)
        print(("FAIL " if errors else "ok   ") + name)
        for e in errors:
            print("  - " + e)
        failed = failed or bool(errors)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
