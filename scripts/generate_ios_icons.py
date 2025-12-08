#!/usr/bin/env python3
"""
Generate iOS app icons in all required sizes.
"""

import os
from pathlib import Path
from PIL import Image, ImageDraw

def draw_globe_icon(size, output_path):
    """Draw a globe icon with purple parallel and meridian lines."""
    # Create image with transparent background
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    center = size // 2
    radius = int(size * 0.45)
    
    # Background circle (dark)
    draw.ellipse(
        [center - radius, center - radius, center + radius, center + radius],
        fill=(26, 26, 46, 255),  # Dark background
        outline=(139, 92, 246, 255),  # Purple outline
        width=max(2, size // 128)
    )
    
    # Draw meridians (vertical curved lines)
    purple = (139, 92, 246, 255)  # #8b5cf6
    purple_light = (167, 139, 250, 200)  # #a78bfa with alpha
    
    # Main meridians
    for offset in [-radius * 0.3, 0, radius * 0.3]:
        points = []
        for y in range(0, size, size // 20):
            x = center + offset + int((y - center) ** 2 / (size * 2)) * (1 if offset < 0 else -1)
            points.append((x, y))
        if len(points) > 1:
            for i in range(len(points) - 1):
                draw.line([points[i], points[i+1]], fill=purple, width=max(2, size // 170))
    
    # Secondary meridians
    for offset in [-radius * 0.6, radius * 0.6]:
        points = []
        for y in range(0, size, size // 20):
            x = center + offset + int((y - center) ** 2 / (size * 2.5)) * (1 if offset < 0 else -1)
            points.append((x, y))
        if len(points) > 1:
            for i in range(len(points) - 1):
                draw.line([points[i], points[i+1]], fill=purple_light, width=max(1, size // 200))
    
    # Draw parallels (horizontal curved lines)
    # Main parallels
    for y_offset in [-radius * 0.4, 0, radius * 0.4]:
        y = center + int(y_offset)
        rx = int(radius * 0.85)
        ry = int(radius * 0.25)
        # Draw ellipse
        bbox = [center - rx, y - ry, center + rx, y + ry]
        draw.ellipse(bbox, outline=purple, width=max(2, size // 170))
    
    # Secondary parallels
    for y_offset in [-radius * 0.7, radius * 0.7]:
        y = center + int(y_offset)
        rx = int(radius * 0.7)
        ry = int(radius * 0.2)
        bbox = [center - rx, y - ry, center + rx, y + ry]
        draw.ellipse(bbox, outline=purple_light, width=max(1, size // 200))
    
    # Save image
    img.save(output_path, 'PNG')
    print(f"Generated: {output_path} ({size}x{size})")

def main():
    """Generate iOS icons in all required sizes."""
    base_dir = Path(__file__).parent.parent
    ios_icons_dir = base_dir / "truth-ios-client/TruthTraining/Assets.xcassets/AppIcon.appiconset"
    ios_icons_dir.mkdir(parents=True, exist_ok=True)
    
    # iOS icon sizes from Contents.json
    icon_sizes = [
        # iPhone
        (40, "2x", "iphone", "20x20"),  # 40x40
        (60, "3x", "iphone", "20x20"),  # 60x60
        (58, "2x", "iphone", "29x29"),  # 58x58
        (87, "3x", "iphone", "29x29"),  # 87x87
        (80, "2x", "iphone", "40x40"),  # 80x80
        (120, "3x", "iphone", "40x40"),  # 120x120
        (120, "2x", "iphone", "60x60"),  # 120x120
        (180, "3x", "iphone", "60x60"),  # 180x180
        # iPad
        (20, "1x", "ipad", "20x20"),  # 20x20
        (40, "2x", "ipad", "20x20"),  # 40x40
        (29, "1x", "ipad", "29x29"),  # 29x29
        (58, "2x", "ipad", "29x29"),  # 58x58
        (40, "1x", "ipad", "40x40"),  # 40x40
        (80, "2x", "ipad", "40x40"),  # 80x80
        (152, "2x", "ipad", "76x76"),  # 152x152
        (167, "2x", "ipad", "83.5x83.5"),  # 167x167
        # iOS Marketing
        (1024, "1x", "ios-marketing", "1024x1024"),  # 1024x1024
    ]
    
    print("Generating iOS icons...")
    for size, scale, idiom, size_str in icon_sizes:
        filename = f"icon_{idiom}_{size_str}_{scale}.png"
        output_path = ios_icons_dir / filename
        draw_globe_icon(size, output_path)
    
    print(f"\nAll iOS icons generated in: {ios_icons_dir}")
    print("Note: You need to update Contents.json to reference these files.")

if __name__ == "__main__":
    main()

