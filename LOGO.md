# MemosApp Logo Specification

## Concept

The logo is inspired by the Google Foobar challenge logo, adapted into a circular form. A large pink circle carries a 47-degree angular notch on the right side, split into black (inside the circle) and teal (extending beyond).

## Colors

| Element         | Hex       | Usage                                  |
|-----------------|-----------|----------------------------------------|
| Pink            | `#EE67A4` | Main circle fill                       |
| Black           | `#231F20` | Wedge section inside the circle        |
| Teal            | `#35BEB8` | Wedge section outside the circle       |
| Background      | `#1F1F1F` | Launcher icon background               |

## Geometry (Circle Variant — Primary)

All measurements relative to a 108x108dp Android adaptive icon viewport, center at (54, 54).

### Base Shape

- **Pink circle**: radius = 28, centered at (54, 54)

### 47-Degree Wedge

The wedge is an annular sector — it does NOT originate from the center. It spans between two imaginary concentric circles:

- **Imaginary inner circle**: radius = 20 (where the wedge starts)
- **Imaginary outer circle**: radius = 35 (where the wedge ends)
- **Wedge angle**: 47 degrees, centered on the horizontal-right axis (east)
- **Half-angle**: 23.5 degrees above and below the horizontal

### Wedge Sections

1. **Black section** — the portion of the wedge that falls inside the pink circle
   - Annular sector from r=20 to r=28
   - Creates a dark notch visible against the pink fill

2. **Teal section** — the portion of the wedge that extends beyond the pink circle
   - Annular sector from r=28 to r=35
   - Creates a colored protrusion outside the main circle

### Key Coordinates

Using center (54, 54), half-angle 23.5 degrees:

```
cos(23.5°) = 0.91706
sin(23.5°) = 0.39875
```

| Point                     | X      | Y      |
|---------------------------|--------|--------|
| Inner upper (r=20)        | 72.34  | 46.02  |
| Inner lower (r=20)        | 72.34  | 61.98  |
| Circle upper (r=28)       | 79.68  | 42.84  |
| Circle lower (r=28)       | 79.68  | 65.16  |
| Outer upper (r=35)        | 86.10  | 40.04  |
| Outer lower (r=35)        | 86.10  | 67.96  |

### Construction Steps

1. Draw a filled circle at (54, 54) with radius 28, color `#EE67A4`
2. Draw the black annular sector:
   - Path from inner-upper → circle-upper → arc along r=28 to circle-lower → inner-lower → arc along r=20 back
3. Draw the teal annular sector:
   - Path from circle-upper → outer-upper → arc along r=35 to outer-lower → circle-lower → arc along r=28 back

### Android Vector Drawable Paths

```xml
<!-- Pink circle -->
<path android:fillColor="#EE67A4"
    android:pathData="M26,54 A28,28 0 1,1 82,54 A28,28 0 1,1 26,54 Z" />

<!-- Black annular sector (r=20 to r=28) -->
<path android:fillColor="#231F20"
    android:pathData="M72.34,46.02 L79.68,42.84 A28,28 0 0,1 79.68,65.16
                      L72.34,61.98 A20,20 0 0,0 72.34,46.02 Z" />

<!-- Teal annular sector (r=28 to r=35) -->
<path android:fillColor="#35BEB8"
    android:pathData="M79.68,42.84 L86.1,40.04 A35,35 0 0,1 86.1,67.96
                      L79.68,65.16 A28,28 0 0,0 79.68,42.84 Z" />
```

## Geometry (Triangle Variant — Alternate)

Three concentric equilateral triangles pointing upward, with a 47-degree wedge cutting through the right edge.

### Triangle Circumradii (200x200 SVG, center at 100,100)

| Ring           | Outer R | Inner R | Fill         |
|----------------|---------|---------|--------------|
| Outer ring     | 80      | 65      | Pink         |
| Middle ring    | 55      | 40      | Pink         |
| Inner triangle | 30      | —       | Pink (solid) |
| Gap            | 65→55   | —       | Background   |
| Gap            | 40→30   | —       | Background   |

### Wedge Intersections

The 47-degree wedge (centered on horizontal-right) intersects each triangle's right edge. The intersection points are computed by solving the parametric line-line intersection of the wedge rays with each triangle edge.

- **Black section**: wedge intersection with the middle ring (R=40 to R=55)
- **Teal section**: wedge intersection with the outer ring (R=65 to R=80)
- **Inner triangle**: stays fully pink

## Adaptive Icon Layers

| File                        | Purpose                     |
|-----------------------------|-----------------------------|
| `ic_launcher_foreground.xml`| Colored logo (pink/black/teal) |
| `ic_launcher_background.xml`| Solid `#1F1F1F` background  |
| `ic_launcher_monochrome.xml`| White silhouette for themed icons |

### Safe Zone

Android adaptive icons use a 108dp canvas. The recommended safe zone is a 66dp diameter circle (radius 33 from center). All critical logo elements should fall within this zone.

- Pink circle (r=28): within safe zone
- Teal extension (r=35): at safe zone boundary, may be slightly clipped on some launchers — this is intentional as it creates a "bleeding edge" effect

## Files

- `logo-circle.svg` — Circle variant preview
- `logo-triangle.svg` — Triangle variant preview
- `androidApp/src/main/res/drawable/ic_launcher_foreground.xml` — Production circle logo
- `androidApp/src/main/res/drawable/ic_launcher_monochrome.xml` — Themed icon silhouette
- `androidApp/src/main/res/drawable/ic_launcher_background.xml` — Dark background
