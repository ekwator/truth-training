# Application Icons

This document describes the application icons for all platforms in the Truth Training project.

## Icon Design

All application icons feature a **purple globe** design with:
- **Background**: Dark blue (#1a1a2e)
- **Lines**: Purple parallel and meridian lines (#8b5cf6, #a78bfa)
- **Style**: Minimalist globe representation with curved lines

## Platform-Specific Icons

### Desktop (Tauri - Linux, Windows, macOS)

**Location**: `ui/desktop/src-tauri/icons/`

**Files**:
- `icon.svg` - Source SVG file (512x512)
- `icon.png` - 32x32 RGBA PNG (Linux/macOS)
- `icon-512.png` - 512x512 RGBA PNG (AppImage, DMG)
- `icon.ico` - 256x256 ICO (Windows installer)

**Configuration**: Referenced in `tauri.conf.json`:
```json
"icon": [
  "icons/icon.png",
  "icons/icon-512.png",
  "icons/icon.ico"
]
```

### Android

**Location**: `truth-android-client/app/src/main/res/`

**Files**:
- `drawable/ic_launcher_foreground.xml` - Vector drawable with purple globe
- `mipmap-anydpi-v26/ic_launcher.xml` - Adaptive icon configuration
- `values/colors.xml` - Background color (#1a1a2e)

**Format**: Vector drawable (XML) for scalable icons on all Android devices.

### iOS

**Location**: `truth-ios-client/TruthTraining/Assets.xcassets/AppIcon.appiconset/`

**Files**: 17 PNG files in various sizes:
- iPhone: 20x20, 29x29, 40x40, 60x60 (2x and 3x scales)
- iPad: 20x20, 29x29, 40x40, 76x76, 83.5x83.5 (1x and 2x scales)
- iOS Marketing: 1024x1024

**Configuration**: `Contents.json` references all icon files.

## Generating Icons

### Desktop Icons

Run the icon generation script:
```bash
python3 scripts/generate_icons.py
```

This creates:
- `icon.png` (32x32)
- `icon-512.png` (512x512)
- `icon.ico` (256x256, via ImageMagick)

### iOS Icons

Run the iOS icon generation script:
```bash
python3 scripts/generate_ios_icons.py
```

This creates all 17 required PNG files for iOS.

### Android Icons

Android uses vector drawable (`ic_launcher_foreground.xml`), which is manually created and doesn't require generation.

## Requirements

**Dependencies**:
- Python 3 with PIL/Pillow: `pip install Pillow`
- ImageMagick (for ICO conversion): `sudo apt-get install imagemagick`

## Color Scheme

- **Background**: `#1a1a2e` (Dark blue)
- **Primary Purple**: `#8b5cf6` (Violet-500)
- **Secondary Purple**: `#a78bfa` (Violet-400)
- **Outline**: `#6a0dad` (Dark violet)

## Notes

- All icons use RGBA format for transparency support
- Desktop icons must be RGBA (not RGB) for proper transparency
- Android adaptive icons use vector drawable for scalability
- iOS icons are generated in all required sizes for App Store submission

