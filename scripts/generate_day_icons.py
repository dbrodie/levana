#!/usr/bin/env python3
# /// script
# requires-python = ">=3.9"
# dependencies = ["Pillow"]
# ///
"""
Generate per-day launcher icon foreground PNGs + adaptive-icon XMLs.

Run from the project root:
    uv run scripts/generate_day_icons.py

Output
------
  app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_day_NN_fg.png
  app/src/main/res/mipmap-anydpi-v26/ic_launcher_day_NN.xml   (30 files)
  app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml          (updated → day-01)
  app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml    (updated → day-01)

Each foreground PNG is transparent and contains:
  - Gold moon crescent (left-centre)
  - White calendar box + dark header + gold hanging tab (right-centre)
  - Hebrew day letter centred in the calendar box content area

The adaptive-icon XMLs combine these with the background drawable at runtime.

Viewport: 108 × 108 dp.  Scale factor s = sizePx / 108.

Tuning
------
Adjust LETTER_CX_FRAC, LETTER_CY_FRAC, LETTER_SIZE_FRAC and re-run if the
letter needs repositioning within the calendar box.
"""

from __future__ import annotations

import math
import os
from pathlib import Path
from textwrap import dedent

from PIL import Image, ImageDraw, ImageFont

# ---------------------------------------------------------------------------
# Hebrew day strings for days 1-30
# Matches HebrewDateFormatter.formatHebrewNumber() with geresh / gershayim
# stripped, exactly as HebrewDateTileService does.
# ---------------------------------------------------------------------------
HEBREW_DAYS: list[str] = [
    "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט", "י",
    "יא", "יב", "יג", "יד", "טו", "טז", "יז", "יח", "יט", "כ",
    "כא", "כב", "כג", "כד", "כה", "כו", "כז", "כח", "כט", "ל",
]

# ---------------------------------------------------------------------------
# Density → foreground PNG size in pixels (108 dp base)
# ---------------------------------------------------------------------------
DENSITIES: list[tuple[str, int]] = [
    ("mdpi",    108),
    ("hdpi",    162),
    ("xhdpi",   216),
    ("xxhdpi",  324),
    ("xxxhdpi", 432),
]

# ---------------------------------------------------------------------------
# Layout constants (108 dp viewport; multiply by s = sizePx/108 to scale)
# ---------------------------------------------------------------------------
# Moon crescent — outer circle minus shifted inner circle
MOON_OUT_CX, MOON_OUT_CY, MOON_OUT_R = 40.0, 52.0, 24.0
MOON_IN_DX,  MOON_IN_R               = 7.0,  20.0   # inner centre = (cx+DX, cy)

# Calendar box  x=44..82, y=34..78, corner r=4
BOX_X, BOX_Y, BOX_W, BOX_H, BOX_R = 44.0, 34.0, 38.0, 44.0, 4.0
HDR_H = 13.0   # dark header height

# Hanging tab (centred on top edge of box), 8×8 dp, corner r=2
TAB_W, TAB_H, TAB_R = 8.0, 8.0, 2.0

# Letter — centre of the box content area (below the header)
LETTER_CX   = BOX_X + BOX_W / 2.0          # 63.0
LETTER_CY   = BOX_Y + HDR_H + (BOX_H - HDR_H) / 2.0  # 62.5
LETTER_SIZE = (BOX_H - HDR_H) * 0.65       # ≈ 20.1 dp

# Colours (RGBA)
GOLD    = (249, 168, 37, 255)
WHITE   = (255, 255, 255, 255)
DARK    = (55, 71, 79, 255)     # header / text contrast
NAVY    = (13, 21, 37, 255)     # letter colour


# ---------------------------------------------------------------------------
# Geometry helpers
# ---------------------------------------------------------------------------

def circle_mask(size: int, cx: float, cy: float, r: float) -> Image.Image:
    """Return a grayscale mask image with a filled circle."""
    mask = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(mask)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=255)
    return mask


def crescent(draw: ImageDraw.ImageDraw, size: int, color: tuple) -> None:
    """Draw the gold moon crescent using Area subtraction via two masks."""
    s = size / 108.0

    outer = circle_mask(size,
                        MOON_OUT_CX * s, MOON_OUT_CY * s, MOON_OUT_R * s)
    inner = circle_mask(size,
                        (MOON_OUT_CX + MOON_IN_DX) * s, MOON_OUT_CY * s,
                        MOON_IN_R * s)

    # crescent_mask = outer AND NOT inner
    from PIL import ImageChops
    not_inner = ImageChops.invert(inner)
    crescent_mask = ImageChops.multiply(outer, not_inner)

    # Paint the crescent colour onto a temp layer, then paste with mask
    layer = Image.new("RGBA", (size, size), color)
    draw._image.paste(layer, mask=crescent_mask)


def rounded_rect(
    draw: ImageDraw.ImageDraw, x: float, y: float, w: float, h: float,
    r: float, color: tuple, clip_bottom: float | None = None
) -> None:
    """Draw a filled rounded rectangle, optionally clipped to clip_bottom y."""
    x0, y0, x1, y1 = x, y, x + w, y + h
    if clip_bottom is not None:
        y1 = min(y1, clip_bottom)
    draw.rounded_rectangle([x0, y0, x1, y1], radius=r, fill=color)


# ---------------------------------------------------------------------------
# Build one foreground image
# ---------------------------------------------------------------------------

def make_foreground(hebrew_day: str, size: int) -> Image.Image:
    s = size / 108.0
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # 1. Moon crescent
    crescent(draw, size, GOLD)

    # 2. Calendar box body (white)
    rounded_rect(draw,
                 BOX_X * s, BOX_Y * s, BOX_W * s, BOX_H * s,
                 BOX_R * s, WHITE)

    # 3. Dark header band — rounded top, flat bottom at HDR_H
    # Draw as a rounded rect then overwrite the bottom corners with a flat rect
    hdr_y1 = (BOX_Y + HDR_H) * s
    rounded_rect(draw,
                 BOX_X * s, BOX_Y * s, BOX_W * s, (HDR_H + BOX_R) * s,
                 BOX_R * s, DARK, clip_bottom=hdr_y1)
    # Fill bottom strip of the header (flat, covers lower arc of above rrect)
    draw.rectangle([BOX_X * s, (BOX_Y + BOX_R) * s,
                    (BOX_X + BOX_W) * s, hdr_y1],
                   fill=DARK)

    # 4. Gold hanging tab centred on top edge
    tx = (BOX_X + (BOX_W - TAB_W) / 2.0) * s
    ty = (BOX_Y - TAB_H / 2.0) * s
    rounded_rect(draw, tx, ty, TAB_W * s, TAB_H * s, TAB_R * s, GOLD)

    # 5. Hebrew letter — centred in the content area
    font_px = max(8, round(LETTER_SIZE * s))
    font = _load_hebrew_font(font_px)

    # Use getbbox for accurate measurement
    bbox = font.getbbox(hebrew_day)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    lx = LETTER_CX * s - tw / 2.0 - bbox[0]
    ly = LETTER_CY * s - th / 2.0 - bbox[1]
    draw.text((lx, ly), hebrew_day[::-1], font=font, fill=NAVY)

    return img


def _load_hebrew_font(size: int) -> ImageFont.FreeTypeFont:
    """Try system fonts known to include Hebrew glyphs."""
    candidates = [
        "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
        "/System/Library/Fonts/Helvetica.ttc",
        "/Library/Fonts/Arial.ttf",
        "/System/Library/Fonts/SFNS.ttf",
        "/System/Library/Fonts/SFNSDisplay.ttf",
    ]
    for path in candidates:
        if os.path.exists(path):
            try:
                return ImageFont.truetype(path, size)
            except Exception:
                continue
    return ImageFont.load_default(size=size)


# ---------------------------------------------------------------------------
# Adaptive-icon XML template
# ---------------------------------------------------------------------------

def day_icon_xml(fg_name: str) -> str:
    return dedent(f"""\
        <?xml version="1.0" encoding="utf-8"?>
        <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
            <background android:drawable="@drawable/ic_launcher_background" />
            <foreground android:drawable="@mipmap/{fg_name}" />
        </adaptive-icon>
        """)

LAUNCHER_XML_TEMPLATE = day_icon_xml  # same format


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> None:
    res_dir   = Path("app/src/main/res")
    anydpi    = res_dir / "mipmap-anydpi-v26"
    anydpi.mkdir(parents=True, exist_ok=True)

    for day_idx, hebrew_day in enumerate(HEBREW_DAYS):
        day_num = day_idx + 1
        day_str = f"{day_num:02d}"
        fg_name = f"ic_launcher_day_{day_str}_fg"

        # 1. PNG foregrounds at each density
        for qualifier, size_px in DENSITIES:
            out_dir = res_dir / f"mipmap-{qualifier}"
            out_dir.mkdir(exist_ok=True)
            img = make_foreground(hebrew_day, size_px)
            img.save(out_dir / f"{fg_name}.png")

        # 2. Per-day adaptive-icon XML
        (anydpi / f"ic_launcher_day_{day_str}.xml").write_text(
            day_icon_xml(fg_name), encoding="utf-8"
        )

        print(f"Day {day_str} ({hebrew_day}) ✓")

    # 3. Update default ic_launcher → day-01 foreground (shows א)
    default_xml = day_icon_xml("ic_launcher_day_01_fg")
    (anydpi / "ic_launcher.xml").write_text(default_xml, encoding="utf-8")
    (anydpi / "ic_launcher_round.xml").write_text(default_xml, encoding="utf-8")

    total_pngs = len(HEBREW_DAYS) * len(DENSITIES)
    print(f"\nUpdated ic_launcher.xml + ic_launcher_round.xml → day 01 (א)")
    print(f"Done — {total_pngs} PNGs + {len(HEBREW_DAYS)} XMLs generated.")


if __name__ == "__main__":
    main()
