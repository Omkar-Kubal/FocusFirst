# Material Design 3 — Detailed Component Specifications

*Extended reference. Load this file when you need exact dimensions, states, and spec details beyond the SKILL.md summary.*

---

## Table of Contents
1. [Buttons — All States & Sizes](#1-buttons--all-states--sizes)
2. [Top App Bars — All 4 Variants](#2-top-app-bars--all-4-variants)
3. [Navigation Components — Full Specs](#3-navigation-components--full-specs)
4. [Cards — Full Anatomy](#4-cards--full-anatomy)
5. [Text Fields — All States](#5-text-fields--all-states)
6. [Dialogs — Full Specs](#6-dialogs--full-specs)
7. [Bottom Sheets — Full Specs](#7-bottom-sheets--full-specs)
8. [Chips — All Types and States](#8-chips--all-types-and-states)
9. [Lists — All Sizes](#9-lists--all-sizes)
10. [Menus — Anatomy](#10-menus--anatomy)
11. [Sliders — Full Specs](#11-sliders--full-specs)
12. [Snackbars — Full Specs](#12-snackbars--full-specs)
13. [Progress Indicators — All Types](#13-progress-indicators--all-types)
14. [Tabs — Full Specs](#14-tabs--full-specs)
15. [Date & Time Pickers](#15-date--time-pickers)
16. [Search](#16-search)
17. [Badges](#17-badges)
18. [Dividers](#18-dividers)
19. [Icon Buttons](#19-icon-buttons)
20. [Component State Color Mapping](#20-component-state-color-mapping)

---

## 1. Buttons — All States & Sizes

### Anatomy (all button types)
```
[Leading icon 18dp] [Label Large text] [Trailing icon 18dp — rare]
├── Height: 40dp visual / 48dp touch
├── Corner radius: Full (~20dp)
├── Min width: 48dp (icon-only), flexible (labeled)
└── Horizontal padding: 24dp (label only) | 16dp (icon+label) | 12dp (icon-only)
```

### State Colors per Button Type

| Button | Container | Label/Icon | Hovered Container | Pressed Container | Disabled Container | Disabled Content |
|---|---|---|---|---|---|---|
| Filled | Primary | On-Primary | Primary + 8% on-primary | Primary + 12% on-primary | On-Surface 12% | On-Surface 38% |
| Tonal | Secondary-Container | On-Secondary-Container | + 8% on-secondary-container | + 12% | On-Surface 12% | On-Surface 38% |
| Elevated | Surface-Container-Low | Primary | Surface-Container-Low + 8% | + 12% | On-Surface 12% | On-Surface 38% |
| Outlined | None | Primary | Primary 8% overlay | Primary 12% | — (transparent) | On-Surface 38% |
| Text | None | Primary | Primary 8% overlay | Primary 12% | — | On-Surface 38% |

### FAB States

| State | Elevation | Color Change |
|---|---|---|
| Resting | 6dp (Level 3) | — |
| Hover | 8dp (Level 4) | + 8% on-primary-container |
| Pressed | 6dp (no change) | + 12% on-primary-container |
| Focused | 6dp | + 12% on-primary-container |

FAB container color: **Primary Container**. Icon color: **On Primary Container**.

---

## 2. Top App Bars — All 4 Variants

### Small Top App Bar (most common)
```
Height: 64dp
├── Leading: Navigation icon (24dp icon, 48dp touch) — 4dp from left
├── Title: Title Large (22sp), Center or leading-aligned
├── Trailing: Up to 3 action icons (24dp each, 48dp touch), 4dp from right
└── Background: Surface (0dp elevation resting)
```

On scroll: Background → Surface Container (Level 2, 3dp elevation)

### Center-Aligned Top App Bar
```
Height: 64dp
├── Leading: Navigation icon
├── Title: Title Large, horizontally centered
├── Trailing: 1 action icon
└── Usage: Simple screens, detail views
```

### Medium Top App Bar
```
Total height: 112dp (two-row layout)
├── Row 1 (64dp): Leading nav icon + trailing action icons
├── Row 2 (48dp): Headline Small (24sp) title, 16dp leading padding
└── Title collapses to small bar on scroll
```

On scroll: collapses to Small Top App Bar. Background: Surface → Surface Container.

### Large Top App Bar
```
Total height: 152dp
├── Row 1 (64dp): Leading nav icon + trailing action icons
├── Row 2 (88dp): Headline Medium (28sp) title, 16dp leading padding
└── Title collapses to small bar on scroll
```

Expanded title font: Headline Medium (28sp, 36sp line height)
Collapsed title font: Title Large (22sp, 28sp line height)

### Scrolling Behavior
- Small: Stays, changes color on scroll (no title scroll)
- Center/Medium/Large: Title scrolls up, bar compresses to Small size

---

## 3. Navigation Components — Full Specs

### Navigation Bar (Bottom) — Complete Spec
```
Height: 80dp total
├── Container: Surface Container, full-width
├── Active Indicator pill: 64dp wide × 32dp tall, rounded-full
├── Icon: 24dp, centered in indicator
├── Label: Label Medium (12sp), below indicator, 4dp gap
├── Items: 3–5 (3–4 optimal)
├── Item width: equally distributed
└── Content padding: 12dp top, 16dp bottom
```

States:
- Inactive: Outlined icon, On-Surface-Variant color
- Active: Filled icon, On-Secondary-Container on Secondary-Container indicator
- Hover: + 8% overlay
- Pressed: + 12% overlay

### Navigation Rail — Complete Spec
```
Width: 80dp
├── Container: Surface
├── FAB (optional): 56dp × 56dp, at top, 8dp margin from rail edges, 4dp gap to first item
├── Items: 3–7 destinations
├── Item container: 56dp height
├── Active indicator: 56dp wide × 32dp tall (pill), centered horizontally
├── Icon: 24dp
├── Label: Label Medium (12sp), below indicator, 4dp gap
└── Top/bottom padding: 12dp
```

### Navigation Drawer — Complete Spec
```
Width: 360dp (standard/modal)
Modal: Overlays content with scrim (Black 40%)
Standard: Permanent, pushes content
├── Header area: 56dp+ height, optional logo/title/account
├── Section label: Label Medium, 12dp top, 16dp left/right
├── List item height: 56dp
│   ├── Active indicator: full width pill, 56dp × 48dp, 12dp horizontal padding
│   ├── Icon: 24dp, 16dp from left edge
│   ├── Label: Label Large (14sp)
│   └── Trailing: badge or count
├── Divider: between sections
└── Corner radius (modal): Large (16dp) right corners only
```

---

## 4. Cards — Full Anatomy

### Card Layout Rules
```
Card (all types):
├── Corner radius: 12dp (Medium shape)
├── Touch ripple: starts from touch point
├── Padding (standard content): 16dp all sides
├── No horizontal dividers inside cards (use spacing instead)
└── Cards should never stack directly — minimum 8dp gap

Media (image/video):
├── Edge-to-edge at top (no corner clipping needed — card clips)
├── Standard ratios: 16:9, 1:1, 4:3
└── Below media content padding: 16dp

Header area:
├── Avatar: 40dp × 40dp (if included)
├── Title: Title Medium (16sp) or Body Large
├── Subtitle: Body Medium (14sp), On-Surface-Variant
├── Padding: 16dp, 16dp between avatar and text
└── Action menu: trailing 24dp icon, 48dp touch

Action area (footer):
├── Text buttons: right-aligned
├── Between buttons: 8dp gap
└── Padding: 8dp top, 8dp right, 8dp bottom

Expanded/Image card:
└── Image: 16:9, text overlay possible using scrim gradient
```

---

## 5. Text Fields — All States

### Filled Text Field States

| State | Container | Label | Indicator | Helper |
|---|---|---|---|---|
| Enabled | Surface Variant | On-Surface-Variant | On-Surface-Variant (1dp) | On-Surface-Variant |
| Hovered | Surface Variant + 8% | On-Surface | On-Surface | — |
| Focused | Surface Variant | Primary (floating small) | Primary (2dp) | On-Surface-Variant |
| Error | Surface Variant | Error (floating) | Error (2dp) | Error |
| Disabled | On-Surface 4% | On-Surface 38% | On-Surface 38% (dashed) | — |

### Outlined Text Field States

| State | Outline | Label | Text | Helper |
|---|---|---|---|---|
| Enabled | Outline (1dp) | On-Surface-Variant | On-Surface | On-Surface-Variant |
| Focused | Primary (2dp) | Primary (floating) | On-Surface | — |
| Error | Error (2dp) | Error (floating) | On-Surface | Error |
| Disabled | On-Surface 12% (dashed) | On-Surface 38% | On-Surface 38% | — |

### Text Field Anatomy
```
Height: 56dp
├── Leading icon (optional): 24dp, 16dp from left, 12dp from input
├── Label (floating): Label Small when active, Body Large when inactive
│   └── Floating position: 8dp from top of container
├── Input text: Body Large (16sp), 16dp left padding
├── Trailing icon/clear/error icon: 24dp, 12dp from right
├── Supporting text (helper/error): Body Small (12sp), 4dp below container
└── Character counter: Body Small, trailing align with container right edge
```

---

## 6. Dialogs — Full Specs

### Basic Dialog
```
Container width: 280–560dp (typically 312dp on phones)
Container min-height: 192dp
Corner radius: 28dp (Extra Large)
Padding: 24dp all sides

├── Optional Icon: 24dp, centered, 24dp below top edge
├── Title: Headline Small (24sp), 24dp from top (or 16dp below icon)
│   └── Alignment: Center (with icon) or Left (without)
├── Body text: Body Medium (14sp), 16dp top gap from title
│   └── Scrollable if content exceeds available height
└── Action buttons:
    ├── Right-aligned row
    ├── Text buttons (standard) or Filled/Outlined (for emphasis)
    ├── 8dp gap between buttons
    └── 24dp right / 24dp bottom padding
```

Scrim: Black 40% over background

### Full-Screen Dialog
```
Fills entire screen (no corner radius)
├── Top App Bar: Close (×) on left, confirm action button on right
│   └── Bar: Surface, Title Large
├── Content: Scrollable body
└── Use when: Multi-step flow, complex forms, cannot dismiss easily
```

---

## 7. Bottom Sheets — Full Specs

### Modal Bottom Sheet
```
Background: Surface (no tonal overlay needed — already floating)
Corner radius: 28dp top-left and top-right only
Drag handle: 32dp × 4dp, Surface-Variant color, centered, 22dp from top edge
Scrim: Black 40%

Heights:
├── Peek (partially visible): configurable (e.g., 256dp)
├── Expanded: up to ~90% screen height
└── Hidden: off-screen

Content padding:
├── Top: 48dp (below handle area)
├── Sides: 16dp
└── Bottom: 32dp (+ system navigation bar height)
```

### Standard Bottom Sheet
```
Same shape, no scrim, persistent behind app content
├── Can be dragged open/closed
└── App content dims slightly (no hard scrim)
```

---

## 8. Chips — All Types and States

### Chip Anatomy
```
Height: 32dp
Corner radius: 8dp (Small shape)
Horizontal padding: 8dp (no icon), 4dp left + 8dp right (with leading icon)
Icon: 18dp (leading), 18dp (trailing — input chip only)
Label: Label Large (14sp)
```

### State Colors

| Chip Type | Default Container | Selected Container | Hovered | Pressed |
|---|---|---|---|---|
| Assist | Surface | — | + 8% overlay | + 12% |
| Filter | Surface | Secondary Container | + 8% | + 12% |
| Input | Surface | Secondary Container | + 8% | + 12% |
| Suggestion | Surface | — | + 8% | + 12% |

**Elevated variant:** Surface Container + 1dp shadow

### Filter Chip States
- Unselected: Outlined, no fill
- Selected: Secondary Container fill, On-Secondary-Container text, optional checkmark icon

---

## 9. Lists — All Sizes

### List Item Anatomy

**1-Line List Item (56dp)**
```
├── Leading (optional): 40dp × 40dp (icon, avatar, image, checkbox)
│   └── Leading indent: 16dp from edge
├── Headline: Body Large (16sp), On-Surface
│   └── Text indent: 16dp from edge (or 72dp if leading item present)
└── Trailing (optional): 24dp icon, text, switch, checkbox, radio
    └── Trailing indent: 16dp from edge
```

**2-Line List Item (72dp)**
```
├── Same leading/trailing as 1-line
├── Headline: Body Large (16sp)
└── Supporting text: Body Medium (14sp), On-Surface-Variant, 1 line max
```

**3-Line List Item (88dp)**
```
├── Leading: aligned to top
├── Headline: Body Large (16sp), max 1 line
└── Supporting text: Body Medium (14sp), up to 2 lines, On-Surface-Variant
```

### List Section Headers
- Label: Label Large (14sp), On-Surface-Variant
- Padding: 24dp top, 16dp left, 8dp bottom
- Optional Divider above header

---

## 10. Menus — Anatomy

### Dropdown Menu
```
Container: Surface Container (elevation Level 2)
Corner radius: Extra Small (4dp)
Min width: 112dp | Max width: 280dp
├── List item height: 48dp
├── Item padding: 12dp top/bottom, 12dp left/right
├── Label: Body Large (16sp)
├── Leading icon (optional): 24dp, 12dp gap to label
├── Trailing icon/shortcut: 24dp or text, right-aligned
└── Divider: 1dp Outline Variant, between groups
```

### Exposed Dropdown Menu
```
Looks like a text field (filled or outlined variant):
├── Height: 56dp
├── Text: Body Large (16sp)
├── Trailing icon: Dropdown arrow (24dp)
└── Menu appears below field, same width
```

---

## 11. Sliders — Full Specs

### Anatomy
```
Track height: 4dp (both inactive and active portions)
Active portion: Primary color
Inactive portion: Surface Container Highest
Corner radius: Full (pill)
Thumb: 20dp diameter, Primary color
Thumb border (unselected): 2dp, On-Surface-Variant
Touch target around thumb: 48dp × 48dp
Value label (drag): appears above thumb
├── Background: Primary
├── Text: On-Primary, Label Medium (12sp)
└── Label container: ~28dp tall, pill shape
Tick marks (discrete): 2dp diameter, on track
```

### Range Slider
```
Two thumbs — identical specs
Active track: between two thumbs (Primary)
Inactive track: outside both thumbs (Surface Container Highest)
Min gap between thumbs: 8dp of track space
```

---

## 12. Snackbars — Full Specs

```
Container:
├── Background: Inverse Surface
├── Corner radius: Extra Small (4dp)
├── Width: 288dp min, 640dp max (or match screen width on small screens)
├── Height: 48dp (single line) | flexible (multi-line)
└── Elevation: Level 3 (6dp)

Content:
├── Message: Body Medium (14sp), Inverse On-Surface
├── Left padding: 16dp
├── Right padding: 16dp (no action) | 8dp (with action)
└── Top/bottom padding: 14dp (single line), 12dp (multi-line)

Action button (optional):
├── Color: Inverse Primary
├── Style: Text button (Label Large, 14sp)
├── Padding: 8dp left from message
└── Max 1 action button

Behavior:
├── Auto-dismiss: 4s (short message), 10s (long message)
├── Manual dismiss: optional × button (Inverse On-Surface)
├── Only ONE snackbar at a time
├── Appear from bottom, slide up animation
└── Do NOT interrupt critical user tasks
```

---

## 13. Progress Indicators — All Types

### Linear Progress Indicator
```
Track height: 4dp
Corner radius: Full (2dp)
Active track: Primary color
Inactive track: Surface Container Highest
Animation (indeterminate): Expanding/contracting line, left-to-right
Width: Fill container (or defined width)
Placement: Below top app bar, above content, inline
```

### Circular Progress Indicator
```
Sizes: 16dp (small) | 24dp (medium) | 48dp (large/default)
Stroke widths: 3dp (16dp size) | 4dp (24dp+)
Color: Primary
Background track: Surface Container Highest (optional)
Animation (indeterminate): Rotating arc, pulsing length
```

### Loading Indicator (M3 Expressive)
```
Shape-morphing animation between abstract forms
Duration: ~1s loop
Color: Primary
Replaces indeterminate circular indicator for waits < 5s

ContainedLoadingIndicator:
├── Background: Primary Container (or Secondary Container)
├── Indicator color: On-Primary-Container
└── Used inside buttons or containers
```

---

## 14. Tabs — Full Specs

### Primary Tabs
```
Height: 48dp
Icon + Label variant: 72dp

├── Indicator: 3dp thick, primary color, spans full item width
├── Active label: On-Surface (or Primary), Label Large (14sp)
├── Inactive label: On-Surface-Variant, Label Large (14sp)
├── Icon: 24dp (active: filled, inactive: outlined)
├── Horizontal padding: 16dp per item
└── Min item width: 90dp | Max: 360dp
Scrollable: When content won't fit (≥ 5 tabs)
```

### Secondary Tabs
```
Height: 48dp (same)
├── Indicator: Same 3dp height but styled differently
├── Use: In-page content filtering (not navigation)
└── Typically appear below Primary tabs if both needed
```

---

## 15. Date & Time Pickers

### Date Picker (Dialog)
```
Width: 360dp
Corner radius: Extra Large (28dp)
Header: 
├── "Select date" label: Label Large
├── Selected date: Display Small (36sp)
└── Background: Primary Container
Calendar:
├── Year/month nav: Title Small + chevron icons
├── Day cells: 40dp × 40dp
├── Selected day: Primary circle (40dp)
├── Today: Outlined circle
└── Body text: Body Large for day numbers
Action buttons: Text buttons, right-aligned
```

### Date Picker (Inline/Docked)
```
Same calendar UI, no dialog wrapper
No header or actions, embedded in form
Width: 328–360dp
```

### Date Range Picker
```
Selected range: primary-colored span between start/end
Start/end day: Primary filled circles
Middle days: Primary Container rectangle
```

### Time Picker
```
Dial variant:
├── Clock face: 256dp diameter
├── Active time hand: Primary
├── AM/PM toggle: Text button style

Keyboard input variant: Two text inputs (hour:minute)

Both: 24dp padding, dialog wrapper with action buttons
```

---

## 16. Search

### Search Bar
```
Height: 56dp
Corner radius: Full (28dp)
Container: Surface Container High
├── Leading: search icon (24dp) or nav icon
├── Input: Body Large (16sp)
├── Trailing: avatar/profile icon or clear (×)
└── Horizontal padding: 16dp

On focus: expands to Search View
```

### Search View (Full-Screen or Docked)
```
Docked (appears inline, expands content below):
├── Same bar at top
└── Results list below

Full-screen (replaces content):
├── Leading: back arrow
├── Search bar full width
└── Results fill remaining screen
```

---

## 17. Badges

### Small Badge (dot)
```
Size: 6dp × 6dp
Color: Error
Position: Top-right of icon
No text
Use: Unread notification indicator (count unknown)
```

### Large Badge (with count)
```
Height: 16dp
Min-width: 16dp
Corner radius: Full
Color: Error
Text: Label Small (11sp), On-Error
Padding: 4dp horizontal
Max: "999+"
Position: Top-right of icon, offset by -4dp from corner
```

---

## 18. Dividers

### Horizontal Divider
```
Height: 1dp
Color: Outline Variant
Width: Full container width
Optional: Left/right inset (16dp typical for list dividers)
```

### Vertical Divider
```
Width: 1dp
Height: Full container height
Color: Outline Variant
Use: Side-by-side layout separation
```

---

## 19. Icon Buttons

### Variants and Specs
```
All icon buttons:
├── Visual size: 40dp × 40dp
├── Touch target: 48dp × 48dp (padding 4dp each side)
└── Icon: 24dp

Standard (no container):
├── Icon color: On-Surface-Variant (inactive) | Primary (active)
└── Hover: On-Surface-Variant + 8% Surface Variant

Filled:
├── Container: Primary
├── Icon: On-Primary
└── Toggle: Filled → Tonal on selected

Filled Tonal:
├── Container: Secondary Container
├── Icon: On-Secondary-Container
└── Good for mid-emphasis toolbar actions

Outlined:
├── No fill, 1dp Outline border
├── Icon: On-Surface-Variant
└── Good on colored surfaces needing visual weight
```

---

## 20. Component State Color Mapping

### Universal State Layer System

Every interactive component applies an **overlay of the "on" color** on interaction:

```
Component surface color: X
Content color ("on" color): Y

State overlay = Y color at specified alpha applied ON TOP of X:
- Hover:   Y at 8%
- Focus:   Y at 12%
- Pressed: Y at 12%
- Dragged: Y at 16%

Example: Filled Button
- Container: Primary (#6750A4)
- Content: On-Primary (#FFFFFF)
- Hovered → Primary + White 8% overlay
- Pressed → Primary + White 12% overlay
```

### Disabled State Rules
```
Disabled container:  On-Surface at 12% (very transparent)
Disabled content:    On-Surface at 38% (very muted)
Disabled outline:    On-Surface at 12%
Never apply interaction overlays to disabled components
Never use error color on disabled states
```

### Focus Ring (Keyboard/External Navigation)
```
Ring: 3dp width, On-Surface color
Offset: 2dp from component boundary
Corner: Matches component corner radius + 2dp
Use: High-visibility focus for keyboard and TV navigation
```

---

*End of detailed component specifications.*
*For overall system guidelines, color system, typography, and layout, see SKILL.md*