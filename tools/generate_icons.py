"""Generate SSHCommander launcher icons.

Design: black rounded-square background + white "terminal" glyph
(rounded-rect window frame, ">" prompt chevron, cursor bar) — matching the
Android vector drawable ic_launcher_foreground.xml so all densities agree.

Outputs:
  - androidApp mipmap PNGs (mdpi..xxxhdpi) for pre-API-26 devices
  - desktopApp icon.ico (multi-size, for Windows jpackage + window icon)
"""
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ANDROID_MIPMAPS = {
    "mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192,
}


def draw_icon(size: int) -> Image.Image:
    """Draw the black/white terminal icon at `size` px."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Black rounded-square background
    r = size * 0.22
    d.rounded_rectangle([0, 0, size - 1, size - 1], radius=r, fill=(0, 0, 0, 255))

    # Work in a virtual 960x960 viewport scaled to the icon.
    scale = size / 960.0

    def P(x, y):
        return (x * scale, y * scale)

    sw = max(1, round(36 * scale))

    # Terminal window frame: rounded rect outline (white)
    x0, y0 = 160, 260
    x1, y1 = 800, 760
    d.rounded_rectangle(
        [P(x0, y0)[0], P(x0, y0)[1], P(x1, y1)[0], P(x1, y1)[1]],
        radius=(80 * scale),
        outline=(255, 255, 255, 255),
        width=sw,
    )

    # ">" prompt chevron (two diagonal segments) in upper-left
    cx, cy = 300, 470
    d.line([P(cx, cy - 90), P(cx + 110, cy), P(cx, cy + 90)],
           fill=(255, 255, 255, 255), width=sw)
    # '_' underscore under chevron
    d.line([P(cx - 10, cy + 130), P(cx + 120, cy + 130)],
           fill=(255, 255, 255, 255), width=sw)

    # Cursor bar (bottom-right of window, like a terminal block cursor)
    d.line([P(560, 640), P(660, 640)],
           fill=(255, 255, 255, 255), width=sw * 2)

    return img


def main():
    # Android mipmaps
    for folder, px in ANDROID_MIPMAPS.items():
        out_dir = os.path.join(ROOT, "androidApp", "src", "main", "res", f"mipmap-{folder}")
        os.makedirs(out_dir, exist_ok=True)
        img = draw_icon(px)
        img.save(os.path.join(out_dir, "ic_launcher.png"), "PNG")
        img.save(os.path.join(out_dir, "ic_launcher_round.png"), "PNG")
        print("wrote", out_dir)

    # Desktop .ico — Windows prefers a single ICO with multiple sizes.
    ico_dir = os.path.join(ROOT, "desktopApp", "src", "main", "resources")
    os.makedirs(ico_dir, exist_ok=True)
    sizes = [16, 24, 32, 48, 64, 128, 256]
    ico_path = os.path.join(ico_dir, "icon.ico")
    master = draw_icon(256)
    imgs = [draw_icon(s) for s in sizes]
    master.save(
        ico_path,
        format="ICO",
        sizes=[(s, s) for s in sizes],
        append_images=imgs[:-1],
    )
    print("wrote", ico_path)

    # Also a 256 PNG for the window icon at runtime
    png_path = os.path.join(ico_dir, "icon.png")
    master.save(png_path, "PNG")
    print("wrote", png_path)

    # Legacy `app` module raster mipmaps (pre-API-26) — same palette.
    legacy = {
        "mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192,
    }
    for folder, px in legacy.items():
        out_dir = os.path.join(ROOT, "app", "src", "main", "res", f"mipmap-{folder}")
        os.makedirs(out_dir, exist_ok=True)
        img = draw_icon(px)
        img.save(os.path.join(out_dir, "ic_launcher.webp"), "WEBP")
        img.save(os.path.join(out_dir, "ic_launcher_round.webp"), "WEBP")
        print("wrote (legacy)", out_dir)


if __name__ == "__main__":
    main()
