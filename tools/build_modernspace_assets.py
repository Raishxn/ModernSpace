#!/usr/bin/env python3
"""
ModernSpace - Asset & Logo Suite Generator
Generates 100% original, authorial Minecraft pixel art assets:
- logo.png: 512x512 crisp static logo (transparent / clean)
- logo.gif: 512x512 32-frame smooth animated loop (swirling nether portal, hovering enchanted book, drifting runes)
- logo.svg: 1000x1000 crisp pixel vector SVG
- banner.png: 1200x380 GitHub README header banner (dimensional blueprint void, 3D MC title, platform, badges)

Theme:
Personal pocket dimension customization (layers, lots/streets, portal altar, floating book).
Strictly pocket-realm Minecraft aesthetic — no outer-space galaxies, rockets, or sci-fi planets.
Clean design without the heavy circular dome from GTNA.
"""

import os
import math
import shutil
import numpy as np
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SCRATCH = os.path.join(ROOT, "build/asset_scratch")
os.makedirs(SCRATCH, exist_ok=True)

# 1. Ensure Minecraft textures are extracted from gradle cache
GRADLE_CACHE_JAR = "/home/raishxn/.gradle/caches/neoformruntime/artifacts/minecraft_1.20.1_client.jar"
NEEDED_TEXTURES = [
    "assets/minecraft/textures/block/obsidian.png",
    "assets/minecraft/textures/block/nether_portal.png",
    "assets/minecraft/textures/block/dirt.png",
    "assets/minecraft/textures/block/stone.png",
    "assets/minecraft/textures/block/bedrock.png",
    "assets/minecraft/textures/block/grass_block_top.png",
    "assets/minecraft/textures/block/grass_block_side.png",
    "assets/minecraft/textures/font/ascii.png"
]

import zipfile
with zipfile.ZipFile(GRADLE_CACHE_JAR, 'r') as z:
    for tex in NEEDED_TEXTURES:
        fname = os.path.basename(tex)
        out_f = os.path.join(SCRATCH, fname)
        if not os.path.exists(out_f):
            with open(out_f, 'wb') as f:
                f.write(z.read(tex))

# Load textures
obs = Image.open(f'{SCRATCH}/obsidian.png').convert('RGBA')
portal_sheet = Image.open(f'{SCRATCH}/nether_portal.png').convert('RGBA')
dirt = Image.open(f'{SCRATCH}/dirt.png').convert('RGBA')
stone = Image.open(f'{SCRATCH}/stone.png').convert('RGBA')
bedrock = Image.open(f'{SCRATCH}/bedrock.png').convert('RGBA')
grass_top_gray = Image.open(f'{SCRATCH}/grass_block_top.png').convert('RGBA')
grass_side = Image.open(f'{SCRATCH}/grass_block_side.png').convert('RGBA')
font_sheet = Image.open(f'{SCRATCH}/ascii.png').convert('RGBA')

# Plains biome grass tint: (115, 185, 90)
grass_arr = np.array(grass_top_gray).astype(float)
for y in range(16):
    for x in range(16):
        v = grass_arr[y, x, 0] / 255.0
        grass_arr[y, x, 0] = np.clip(v * 115, 0, 255)
        grass_arr[y, x, 1] = np.clip(v * 185, 0, 255)
        grass_arr[y, x, 2] = np.clip(v * 90, 0, 255)
grass_top = Image.fromarray(grass_arr.astype(np.uint8))


def make_iso_block(top_img, left_img, right_img, height_px=16, top_light=1.0, left_light=0.80, right_light=0.62):
    """Renders a standard 2:1 dimetric isometric Minecraft block from textures."""
    W = 32
    H = 16 + height_px
    arr = np.zeros((H, W, 4), dtype=np.uint8)
    
    top_pixels = np.array(top_img.convert('RGBA'))
    left_pixels = np.array(left_img.convert('RGBA'))
    right_pixels = np.array(right_img.convert('RGBA'))

    # Top rhombus face (32x16)
    for y in range(16):
        if y < 8:
            x_min = 15 - (y * 2 + 1)
            x_max = 16 + (y * 2 + 1)
        else:
            dy = 15 - y
            x_min = 15 - (dy * 2 + 1)
            x_max = 16 + (dy * 2 + 1)
        x_min = max(0, x_min)
        x_max = min(32, x_max)
        for x in range(x_min, x_max):
            nx = (x - 15.5) / 16.0
            ny = (y - 7.5) / 8.0
            u = int(np.clip(((nx + ny) / 2.0 + 0.5) * 16.0, 0, 15))
            v = int(np.clip(((-nx + ny) / 2.0 + 0.5) * 16.0, 0, 15))
            c = top_pixels[v, u].astype(float)
            c[:3] *= top_light
            arr[y, x] = c.astype(np.uint8)

    # Left face
    for x in range(16):
        top_y = 8 + x // 2
        for dy in range(height_px):
            y = top_y + dy
            u = x
            v = int(dy * 16.0 / height_px)
            v = np.clip(v, 0, 15)
            c = left_pixels[v, u].astype(float)
            c[:3] *= left_light
            arr[y, x] = c.astype(np.uint8)

    # Right face
    for x in range(16, 32):
        top_y = 15 - (x - 16) // 2
        for dy in range(height_px):
            y = top_y + dy
            u = x - 16
            v = int(dy * 16.0 / height_px)
            v = np.clip(v, 0, 15)
            c = right_pixels[v, u].astype(float)
            c[:3] *= right_light
            arr[y, x] = c.astype(np.uint8)

    return Image.fromarray(arr)


def draw_enchanted_book():
    """Renders the open floating enchanted book with leather binding, gold corners, parchment and SGA runes."""
    b_arr = np.zeros((14, 22, 4), dtype=np.uint8)
    C_LEATHER = [125, 29, 29, 255]
    C_LEATHER_DARK = [75, 16, 16, 255]
    C_GOLD = [245, 190, 20, 255]
    C_PAGE = [255, 245, 210, 255]
    C_PAGE_SHADOW = [215, 200, 160, 255]
    C_RUNE_PURPLE = [192, 132, 252, 255]
    C_RUNE_CYAN = [125, 211, 252, 255]

    for x in range(2, 20):
        b_arr[11, x] = C_LEATHER_DARK
        b_arr[12, x] = C_LEATHER_DARK
    b_arr[11, 2] = C_GOLD; b_arr[12, 2] = C_GOLD
    b_arr[11, 19] = C_GOLD; b_arr[12, 19] = C_GOLD

    for x in range(2, 11):
        t = (x - 2) / 8.0
        top_y = int(3 + 2 * (1.0 - math.sin(t * math.pi / 2)))
        for y in range(top_y, 11):
            if y == top_y:
                b_arr[y, x] = C_PAGE
            elif y == 10:
                b_arr[y, x] = C_LEATHER
            else:
                b_arr[y, x] = C_PAGE if x > 3 else C_PAGE_SHADOW

    for x in range(11, 20):
        t = (x - 11) / 8.0
        top_y = int(3 + 2 * math.sin(t * math.pi / 2))
        for y in range(top_y, 11):
            if y == top_y:
                b_arr[y, x] = C_PAGE
            elif y == 10:
                b_arr[y, x] = C_LEATHER
            else:
                b_arr[y, x] = C_PAGE if x < 18 else C_PAGE_SHADOW

    b_arr[5, 10] = C_LEATHER; b_arr[5, 11] = C_LEATHER
    b_arr[6, 10] = C_PAGE_SHADOW; b_arr[6, 11] = C_PAGE_SHADOW

    b_arr[6, 4] = C_RUNE_PURPLE; b_arr[6, 6] = C_RUNE_PURPLE; b_arr[6, 8] = C_RUNE_CYAN
    b_arr[8, 5] = C_RUNE_PURPLE; b_arr[8, 7] = C_RUNE_PURPLE
    b_arr[6, 13] = C_RUNE_CYAN; b_arr[6, 15] = C_RUNE_PURPLE; b_arr[6, 17] = C_RUNE_PURPLE
    b_arr[8, 14] = C_RUNE_PURPLE; b_arr[8, 16] = C_RUNE_PURPLE
    return Image.fromarray(b_arr)


def get_char_glyph(ch):
    """Extracts an 8x8 character glyph from vanilla ascii.png."""
    c = ord(ch)
    if c >= 256: return None, 6
    row = c // 16; col = c % 16
    glyph = font_sheet.crop((col * 8, row * 8, (col + 1) * 8, (row + 1) * 8))
    arr = np.array(glyph)
    if ch == ' ': return glyph, 4
    non_empty = np.where(arr[:, :, 3] > 0)
    if len(non_empty[1]) == 0: return glyph, 4
    w = np.max(non_empty[1]) + 2
    return glyph, w


def render_mc_text(text, color=(255, 255, 255), shadow_color=(63, 63, 63), scale=1):
    """Renders text with authentic Minecraft font glyphs, kerning and drop shadows."""
    total_w = 0
    glyphs = []
    for ch in text:
        g, w = get_char_glyph(ch)
        glyphs.append((g, w, ch))
        total_w += w
        
    img = Image.new('RGBA', (total_w + 3, 11), (0, 0, 0, 0))
    if shadow_color:
        cur_x = 1
        for g, w, ch in glyphs:
            if ch != ' ':
                arr = np.array(g).copy()
                mask = arr[:, :, 3] > 0
                arr[mask, 0] = shadow_color[0]
                arr[mask, 1] = shadow_color[1]
                arr[mask, 2] = shadow_color[2]
                img.alpha_composite(Image.fromarray(arr), (cur_x, 1))
            cur_x += w
            
    cur_x = 0
    for g, w, ch in glyphs:
        if ch != ' ':
            arr = np.array(g).copy()
            mask = arr[:, :, 3] > 0
            arr[mask, 0] = color[0]
            arr[mask, 1] = color[1]
            arr[mask, 2] = color[2]
            img.alpha_composite(Image.fromarray(arr), (cur_x, 0))
        cur_x += w
        
    if scale > 1:
        img = img.resize((img.width * scale, img.height * scale), Image.Resampling.NEAREST)
    return img


# Pre-render standard blocks
block_grass = make_iso_block(grass_top, grass_side, grass_side, height_px=14)
block_dirt = make_iso_block(dirt, dirt, dirt, height_px=8)
block_stone = make_iso_block(stone, stone, stone, height_px=6)
block_bedrock = make_iso_block(bedrock, bedrock, bedrock, height_px=4)
book_img = draw_enchanted_book()


def render_platform_scene(frame=0, total_frames=32, background_style="transparent"):
    """
    Renders 128x128 native pixel art:
    - background_style:
        'transparent' (default, pure clean isolated Minecraft 3D voxel island)
        'rounded' (minimal modern rounded square dark plate with subtle 1px border)
        'simple_circle' (thin minimal 1px circle outline)
    - 3x3 customizable pocket dimension platform showing 4 flat layers:
      Bedrock, Stone, Dirt, Grass with lot grid accents
    - Personal Space Portal Altar (Obsidian + swirling Nether Portal)
    - Floating Enchanted Book with SGA runes drifting upward
    """
    CANVAS_SIZE = 128
    canvas = Image.new('RGBA', (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)

    if background_style == "rounded":
        # Minimal modern rounded badge/plate: dark obsidian slate base, subtle 1px border
        draw.rounded_rectangle([4, 4, 123, 123], radius=16, fill=(20, 18, 28, 255), outline=(58, 44, 82, 255), width=2)
        draw.rounded_rectangle([6, 6, 121, 121], radius=14, outline=(126, 34, 206, 70), width=1)
    elif background_style == "simple_circle":
        # Simple minimal 1px circle
        cx, cy, r = 64, 64, 58
        for y in range(128):
            for x in range(128):
                d = math.hypot(x - cx, y - cy)
                if d <= r:
                    if d > r - 1.5:
                        canvas.putpixel((x, y), (147, 51, 234, 255))
                    else:
                        canvas.putpixel((x, y), (20, 18, 28, 245))

    portal_idx = frame % 32
    portal_tex = portal_sheet.crop((0, portal_idx * 16, 16, (portal_idx + 1) * 16))
    block_portal = make_iso_block(portal_tex, obs, obs, height_px=11)

    # Perfectly centered: CX_PLAT = 64, CY_PLAT = 38 (bounding box 16, 14, 112, 115)
    CX_PLAT = 64
    CY_PLAT = 38

    for sum_xz in range(5):
        for gx in range(3):
            gz = sum_xz - gx
            if 0 <= gz < 3:
                bx = CX_PLAT - 16 + (gx - gz) * 16
                by_base = CY_PLAT + (gx + gz) * 8
                canvas.alpha_composite(block_bedrock, (bx, by_base + 26))
                canvas.alpha_composite(block_stone, (bx, by_base + 20))
                canvas.alpha_composite(block_dirt, (bx, by_base + 14))
                canvas.alpha_composite(block_grass, (bx, by_base))

    # Center portal altar at (gx=1, gz=1)
    portal_bx = CX_PLAT - 16
    portal_by = CY_PLAT + 16 - 11

    # Soft shadow under portal
    for dy in range(3):
        for dx in range(-4, 5):
            sx = CX_PLAT + dx
            sy = CY_PLAT + 16 + dy
            if 0 <= sx < CANVAS_SIZE and 0 <= sy < CANVAS_SIZE:
                pr, pg, pb, pa = canvas.getpixel((sx, sy))
                canvas.putpixel((sx, sy), (int(pr*0.65), int(pg*0.65), int(pb*0.65), pa))

    canvas.alpha_composite(block_portal, (portal_bx, portal_by))

    # Floating enchanted book
    t_bob = math.sin(2.0 * math.pi * frame / total_frames)
    book_bob = int(round(1.5 * t_bob))
    book_x = CX_PLAT - 11
    book_y = portal_by - 15 + book_bob
    canvas.alpha_composite(book_img, (book_x, book_y))

    # Floating SGA runes / particles
    rune_defs = [
        (CX_PLAT - 7, book_y - 3, (192, 132, 252, 240)),
        (CX_PLAT + 6, book_y - 6, (125, 211, 252, 240)),
        (CX_PLAT - 1, book_y - 9, (244, 114, 182, 230)),
        (CX_PLAT + 9, book_y - 2, (192, 132, 252, 200)),
        (CX_PLAT - 10, book_y + 2, (125, 211, 252, 180)),
    ]
    for idx, (rx, ry, col) in enumerate(rune_defs):
        phase = (frame + idx * 7) % total_frames
        drift = int(round(phase * 0.35))
        px = rx + int(round(math.sin(phase * 0.35) * 1.5))
        py = ry - drift
        if 0 <= px < CANVAS_SIZE and 0 <= py < CANVAS_SIZE:
            canvas.putpixel((px, py), col)
            if idx % 2 == 0 and px + 1 < CANVAS_SIZE:
                canvas.putpixel((px + 1, py), col)

    return canvas


def build_banner():
    """Generates 1200x380 GitHub README header banner."""
    BW, BH = 1200, 380
    banner = Image.new('RGBA', (BW, BH), (15, 14, 22, 255))
    draw = ImageDraw.Draw(banner)
    
    # 1. Subtle Void Gradient (Obsidian to Amethyst Deep)
    for y in range(BH):
        t = y / float(BH)
        r = int(14 + 8 * (1.0 - t))
        g = int(12 + 6 * (1.0 - t))
        b = int(22 + 15 * (1.0 - t))
        draw.line([(0, y), (BW, y)], fill=(r, g, b, 255))

    # 2. Subtle Blueprint / Lot Dimension Grid Lines
    grid_col = (147, 51, 234, 20)
    grid_col_cyan = (56, 189, 248, 16)
    for gy in range(0, BH, 24):
        draw.line([(0, gy), (BW, gy)], fill=grid_col, width=1)
    for gx in range(0, BW, 24):
        draw.line([(gx, 0), (gx, BH)], fill=grid_col, width=1)
        
    for gx in range(0, BW, 120):
        draw.line([(gx, 0), (gx, BH)], fill=grid_col_cyan, width=1)
    for gy in range(0, BH, 120):
        draw.line([(0, gy), (BW, gy)], fill=grid_col_cyan, width=1)

    # 3. Outer border frame
    draw.rectangle([0, 0, BW-1, BH-1], outline=(126, 34, 206, 180), width=2)
    draw.rectangle([2, 2, BW-3, BH-3], outline=(56, 189, 248, 80), width=1)

    # 4. Render Isometric Platform on the Left (clean transparent)
    plat_raw = render_platform_scene(0, 32, background_style="transparent")
    plat_scaled = plat_raw.resize((312, 312), Image.Resampling.NEAREST)
    banner.alpha_composite(plat_scaled, (50, 34))

    # 5. Right Side Typography:
    title_img = render_mc_text("MODERNSPACE", color=(255, 255, 255), shadow_color=(35, 20, 55), scale=8)
    t_arr = np.array(title_img)
    for ty in range(t_arr.shape[0]):
        ratio = ty / float(t_arr.shape[0])
        for tx in range(t_arr.shape[1]):
            if t_arr[ty, tx, 0] == 255 and t_arr[ty, tx, 1] == 255 and t_arr[ty, tx, 2] == 255:
                t_arr[ty, tx, 0] = int(240 - 50 * ratio)
                t_arr[ty, tx, 1] = int(245 - 110 * ratio)
                t_arr[ty, tx, 2] = 255
    title_tinted = Image.fromarray(t_arr)

    TITLE_X = 405
    TITLE_Y = 55
    banner.alpha_composite(title_tinted, (TITLE_X, TITLE_Y))

    sub_img = render_mc_text("Personal Dimensions for Modern Minecraft", color=(226, 232, 240), shadow_color=(30, 25, 45), scale=3)
    banner.alpha_composite(sub_img, (TITLE_X + 4, TITLE_Y + 95))

    desc_img = render_mc_text("Place a portal, design your world (layers, biomes, sky, streets) & step into it.", color=(167, 139, 250), shadow_color=(25, 18, 38), scale=2)
    banner.alpha_composite(desc_img, (TITLE_X + 4, TITLE_Y + 140))

    # Badges arranged in 2 organized rows with ample margins
    badges_row1 = [
        ("FORGE 1.20.1", (56, 189, 248)),
        ("GTNH PORT", (244, 114, 182)),
    ]
    badges_row2 = [
        ("CUSTOM LAYERS & BIOMES", (168, 85, 247)),
        ("STREETS & LOTS GRID", (74, 222, 128)),
    ]
    
    # Draw Row 1
    bx = TITLE_X + 4
    by = TITLE_Y + 180
    for b_text, b_col in badges_row1:
        b_img = render_mc_text(b_text, color=b_col, shadow_color=(20, 15, 30), scale=2)
        pw = b_img.width + 16
        ph = b_img.height + 10
        draw.rectangle([bx, by, bx + pw, by + ph], fill=(26, 22, 38, 220), outline=b_col, width=1)
        banner.alpha_composite(b_img, (bx + 8, by + 5))
        bx += pw + 12

    # Draw Row 2
    bx = TITLE_X + 4
    by = TITLE_Y + 225
    for b_text, b_col in badges_row2:
        b_img = render_mc_text(b_text, color=b_col, shadow_color=(20, 15, 30), scale=2)
        pw = b_img.width + 16
        ph = b_img.height + 10
        draw.rectangle([bx, by, bx + pw, by + ph], fill=(26, 22, 38, 220), outline=b_col, width=1)
        banner.alpha_composite(b_img, (bx + 8, by + 5))
        bx += pw + 12

    return banner


def export_svg(im_128, out_svg_path):
    """Exports crisp vector SVG from 128x128 pixel art."""
    w, h = im_128.size
    rects = []
    for y in range(h):
        for x in range(w):
            r, g, b, a = im_128.getpixel((x, y))
            if a > 0:
                hex_col = f"#{r:02x}{g:02x}{b:02x}"
                rects.append(f'<rect x="{x}" y="{y}" width="1" height="1" fill="{hex_col}" />')
    svg_content = f"""<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="1000" height="1000" shape-rendering="crispEdges">
  <g>
    {''.join(rects)}
  </g>
</svg>"""
    with open(out_svg_path, 'w', encoding='utf-8') as f:
        f.write(svg_content)


def main():
    print("Building ModernSpace Asset & Logo Suite (Clean Edition)...")
    
    # 1. Generate Static Logo (Clean transparent default)
    f0_trans = render_platform_scene(0, 32, background_style="transparent")
    f0_trans_512 = f0_trans.resize((512, 512), Image.Resampling.NEAREST)
    
    logo_png_path = os.path.join(ROOT, "logo.png")
    f0_trans_512.save(logo_png_path, format="PNG")
    print(f"Generated static logo: {logo_png_path} (512x512 transparent)")
    
    # Also save alternative plate version
    f0_plate = render_platform_scene(0, 32, background_style="rounded")
    f0_plate_512 = f0_plate.resize((512, 512), Image.Resampling.NEAREST)
    logo_plate_path = os.path.join(ROOT, "logo_plate.png")
    f0_plate_512.save(logo_plate_path, format="PNG")
    print(f"Generated alternative plate logo: {logo_plate_path} (512x512)")

    # Deploy in-game Forge logo and icon
    mc_logo_path = os.path.join(ROOT, "src/main/resources/logo.png")
    mc_icon_path = os.path.join(ROOT, "src/main/resources/icon.png")
    root_icon_path = os.path.join(ROOT, "icon.png")
    os.makedirs(os.path.dirname(mc_logo_path), exist_ok=True)
    shutil.copyfile(logo_png_path, mc_logo_path)
    shutil.copyfile(logo_png_path, mc_icon_path)
    shutil.copyfile(logo_png_path, root_icon_path)
    print(f"Deployed mod icons: {mc_icon_path}, {mc_logo_path}, {root_icon_path}")

    # 2. Generate Vector SVG
    logo_svg_path = os.path.join(ROOT, "logo.svg")
    export_svg(f0_trans, logo_svg_path)
    print(f"Generated vector SVG: {logo_svg_path} (1000x1000 crispEdges)")

    # 3. Generate Animated GIF (32 frames loop on clean transparent background)
    TOTAL_FRAMES = 32
    frames_512 = []
    print(f"Rendering {TOTAL_FRAMES} frames for animated GIF...")
    for f in range(TOTAL_FRAMES):
        frame_128 = render_platform_scene(f, TOTAL_FRAMES, background_style="transparent")
        frame_512 = frame_128.resize((512, 512), Image.Resampling.NEAREST)
        frames_512.append(frame_512)
        
    logo_gif_path = os.path.join(ROOT, "logo.gif")
    frames_512[0].save(
        logo_gif_path,
        save_all=True,
        append_images=frames_512[1:],
        duration=50, # 20 fps smooth
        loop=0,
        disposal=2
    )
    print(f"Generated animated logo: {logo_gif_path} ({os.path.getsize(logo_gif_path)} bytes)")

    # 4. Generate GitHub Banner
    banner_img = build_banner()
    banner_path = os.path.join(ROOT, "banner.png")
    banner_img.save(banner_path, format="PNG")
    print(f"Generated GitHub header banner: {banner_path} (1200x380)")

    print("\nAll ModernSpace assets generated successfully!")


if __name__ == "__main__":
    main()
