"""Frame a phone screenshot for Google Play: 1080x1920 (9:16), caption on top, status and
navigation bars cropped off. Needs Pillow and the Lato font.

    python3 frame.py in.jpg out.png "Caption line 1|line 2" [top bottom]

top/bottom are the fractions of the height to crop (defaults: status bar and navigation bar);
use a larger bottom, e.g. 0.165, to also drop the home-screen dock."""
import sys
from PIL import Image, ImageDraw, ImageFilter, ImageFont

W, H = 1080, 1920
BG = (0x0F, 0x5F, 0x5C)
INK = (255, 255, 255)
FONT = ImageFont.truetype("/usr/share/fonts/truetype/lato/Lato-Black.ttf", 66)

src, out, caption = sys.argv[1], sys.argv[2], sys.argv[3].split("|")
shot = Image.open(src).convert("RGB")
# Crop the status bar and the navigation bar (fractions of height, so any resolution works).
top = float(sys.argv[4]) if len(sys.argv) > 4 else 0.047
bottom = float(sys.argv[5]) if len(sys.argv) > 5 else 0.05
shot = shot.crop((0, round(shot.height * top), shot.width, round(shot.height * (1 - bottom))))

canvas = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(canvas)
y = 110
for line in caption:
    w = d.textlength(line, font=FONT)
    d.text(((W - w) / 2, y), line, font=FONT, fill=INK)
    y += 84

# Phone-like card: scale to fit below the caption, rounded corners, soft shadow.
area_top = 110 + 84 * len(caption) + 60
max_h, max_w = H - area_top - 70, W - 200
scale = min(max_h / shot.height, max_w / shot.width)
shot = shot.resize((round(shot.width * scale), round(shot.height * scale)), Image.LANCZOS)
x0 = (W - shot.width) // 2
radius = 36
mask = Image.new("L", shot.size, 0)
ImageDraw.Draw(mask).rounded_rectangle((0, 0, shot.width - 1, shot.height - 1), radius, fill=255)

shadow = Image.new("L", (W, H), 0)
ImageDraw.Draw(shadow).rounded_rectangle(
    (x0, area_top + 14, x0 + shot.width, area_top + shot.height + 14), radius, fill=110)
shadow = shadow.filter(ImageFilter.GaussianBlur(24))
canvas.paste(Image.new("RGB", (W, H), (0x06, 0x33, 0x31)), (0, 0), shadow)
canvas.paste(shot, (x0, area_top), mask)
canvas.save(out, optimize=True)
print(out, canvas.size, "shot", shot.size)
