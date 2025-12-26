# PWA Icons

This directory contains icons for the PostgreSQL Console PWA.

## Generating PNG Icons

To generate the required PNG icons from the SVG source, you can use ImageMagick or librsvg:

### Using ImageMagick

```bash
cd src/main/resources/META-INF/resources/icons

for size in 72 96 128 144 152 192 384 512; do
  convert -background none -resize ${size}x${size} icon.svg icon-${size}.png
done
```

### Using librsvg (rsvg-convert)

```bash
cd src/main/resources/META-INF/resources/icons

for size in 72 96 128 144 152 192 384 512; do
  rsvg-convert -w ${size} -h ${size} icon.svg -o icon-${size}.png
done
```

## Required Sizes

The manifest.json references the following icon sizes:
- 72x72 - Android home screen (legacy)
- 96x96 - Android shortcuts
- 128x128 - Chrome Web Store
- 144x144 - Windows tiles
- 152x152 - iOS home screen
- 192x192 - Android home screen (standard)
- 384x384 - Android splash screen
- 512x512 - Android splash screen (high DPI)

## Fallback

The manifest also includes the SVG icon which provides resolution-independent rendering on supported browsers.
