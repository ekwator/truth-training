#!/usr/bin/env python3
"""
Generate purple globe icons for all platforms.
Creates icons with purple parallel and meridian lines.
"""

import os
import sys
from pathlib import Path

try:
    from PIL import Image, ImageDraw
    PIL_AVAILABLE = True
except ImportError:
    PIL_AVAILABLE = False
    print("Warning: PIL/Pillow not available. Install with: pip install Pillow")

def draw_globe_icon(size, output_path):
    """Draw a globe icon with purple parallel and meridian lines."""
    if not PIL_AVAILABLE:
        print(f"Error: Cannot generate {output_path} - PIL not available")
        return False
    
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
    return True

def main():
    """Generate icons for all platforms."""
    base_dir = Path(__file__).parent.parent
    
    # Desktop icons (Tauri)
    desktop_icons_dir = base_dir / "ui/desktop/src-tauri/icons"
    desktop_icons_dir.mkdir(parents=True, exist_ok=True)
    
    print("Generating Desktop icons...")
    draw_globe_icon(32, desktop_icons_dir / "icon.png")
    draw_globe_icon(512, desktop_icons_dir / "icon-512.png")
    
    # Generate ICO for Windows (using ImageMagick if available)
    if os.system("which convert > /dev/null 2>&1") == 0:
        os.system(f"convert {desktop_icons_dir}/icon-512.png -resize 256x256 {desktop_icons_dir}/icon.ico")
        print(f"Generated: {desktop_icons_dir}/icon.ico (256x256)")
    else:
        # Fallback: create ICO from PNG
        draw_globe_icon(256, desktop_icons_dir / "icon-temp.png")
        if os.system(f"convert {desktop_icons_dir}/icon-temp.png {desktop_icons_dir}/icon.ico 2>/dev/null") == 0:
            (desktop_icons_dir / "icon-temp.png").unlink()
            print(f"Generated: {desktop_icons_dir}/icon.ico (256x256)")
        else:
            print(f"Warning: Could not generate ICO file. Please convert {desktop_icons_dir}/icon-512.png manually.")
    
    print("\nIcon generation complete!")
    print(f"Desktop icons: {desktop_icons_dir}")
    print("\nNote: Android and iOS icons need to be updated manually:")
    print("  - Android: Update ic_launcher_foreground.xml")
    print("  - iOS: Add PNG files to AppIcon.appiconset/")

if __name__ == "__main__":
    main()

