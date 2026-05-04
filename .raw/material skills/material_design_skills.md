---
name material-design
description 
  Comprehensive Material Design 3 (M3  Material You) UIUX knowledge base for Android app development.
  Use this skill whenever the user asks about Android UI design, Material Design components, color schemes,
  typography, layout, elevation, motion, accessibility, or building screens and interfaces with Jetpack Compose
  or Android Views following Google's official M3 guidelines. Trigger this skill for questions like
  what size should my button be, how do I design a bottom navigation bar, what colors should I use,
  how do cards work in Material 3, how do I implement dynamic color, what's the correct padding for
  a dialog, how do I pick a typography style, what shape should my FAB be, how do I design for
  tablets in Android, or any Android UIUX design question. Always use this skill before designing or
  generating Android UI code or layouts.
---

# Material Design 3 (M3) — Complete Android UIUX Reference

Material Design 3 (also called Material You) is Google's design system for Android. It centers on
personalization (dynamic color from wallpaper), adaptability (responsive across phones, tablets,
foldables), and expressiveness (spring physics, shape morphing, rich typography hierarchy).

 Quick Reference Sections
 - [Color System](#1-color-system) — tokens, roles, tonal palettes, dynamic color
 - [Typography](#2-typography) — type scale, sizes, line heights, letter spacing
 - [Shape System](#3-shape-system) — corner radii, shape scale, morphing
 - [Elevation](#4-elevation) — tonal elevation, shadow levels, surface colors
 - [Layout & Spacing](#5-layout--spacing) — window size classes, grids, margins, canonical layouts
 - [Motion](#6-motion) — easing, durations, spring physics, transitions
 - [Components Reference](#7-components-reference) — every major M3 component with specs
 - [Accessibility](#8-accessibility) — contrast, touch targets, semantics
 - [Design Tokens](#9-design-tokens--theming) — token structure, theming in Compose
 - [Detailed Component Specs](referencescomponents.md) — extended per-component specs

---

## 1. Color System

### 1.1 Core Concept Tonal Palettes

Every M3 color scheme is derived from 5 key colors, each generating a 13-tone tonal palette
at tones `0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 95, 99, 100`.

Tone 0 = pure black. Tone 100 = pure white. Tone 40 is the standard primary in light themes.
Tone 80 is the standard primary in dark themes.

5 Key Colors
 Key Color  Role  Chroma Target 
---------
 Primary  Main brand color, main CTAs  ~48 
 Secondary  Supporting, filter chips  ~16 
 Tertiary  Accent contrast, highlights  ~24 
 Neutral  Surfaces, backgrounds  ~4 
 Neutral Variant  Outlines, muted surfaces  ~8 

### 1.2 Color Roles (29 total)

Each key color spawns 4 roles. Pattern `[color]`, `on-[color]`, `[color]-container`, `on-[color]-container`.

Accent Roles
 Role  Light Tone  Dark Tone  Usage 
------------
 primary  40  80  Key components, active states, FAB 
 on-primary  100  20  Texticons on primary 
 primary-container  90  30  Less prominent filled components 
 on-primary-container  10  90  Texticons on primary-container 
 secondary  40  80  Less prominent components, chips 
 on-secondary  100  20  Texticons on secondary 
 secondary-container  90  30  Filter chips, selected states 
 on-secondary-container  10  90  Text on secondary-container 
 tertiary  40  80  Contrasting accent, balance 
 on-tertiary  100  20  Text on tertiary 
 tertiary-container  90  30  Highlighted containers 
 on-tertiary-container  10  90  Text on tertiary-container 

Surface & Neutral Roles
 Role  Light  Dark  Usage 
------------
 surface  98  6  Default backgrounds 
 on-surface  10  90  Primary text on surface 
 surface-variant  90  30  Contained elements, chips 
 on-surface-variant  30  80  Secondary text, icons 
 surface-container-lowest  100  4  Cards in light mode 
 surface-container-low  96  10  Subtle containers 
 surface-container  94  12  Default containers 
 surface-container-high  92  17  Emphasized containers 
 surface-container-highest  90  22  Highest contrast containers 
 inverse-surface  20  90  Snackbars 
 inverse-on-surface  95  20  Text on inverse-surface 
 inverse-primary  80  40  Text in snackbar action 
 outline  50  60  Borders, text field outlines 
 outline-variant  80  30  Subtle dividers 
 scrim  0  0  Modal scrim overlays 

Semantic Roles
 Role  Usage 
------
 error  on-error  error-container  on-error-container  Error states, destructive actions 

### 1.3 Dynamic Color (Material You)

Available on Android 12+ (API 31+). Extracts 5 seed colors from wallpaper to generate a full color scheme.

```kotlin
val dynamicColor = Build.VERSION.SDK_INT = Build.VERSION_CODES.S
val colors = when {
    dynamicColor && darkTheme - dynamicDarkColorScheme(LocalContext.current)
    dynamicColor && !darkTheme - dynamicLightColorScheme(LocalContext.current)
    darkTheme - DarkColorScheme
    else - LightColorScheme
}
MaterialTheme(colorScheme = colors) {  content  }
```

RULE Always provide a static fallback color scheme for API  31. Never rely solely on dynamic color.

### 1.4 Color Usage Rules

- NEVER put `primary` text on `primary` background — use `on-primary`
- NEVER use surface colors for interactive elements — use primarysecondarytertiary
- DO use `surface-container` variants for cards and sheets, not raw `surface`
- DO use `primary-container` for selectedactive chips, not `primary`
- Maintain minimum 4.51 contrast ratio for body text, 31 for large text (18sp+) and UI components
- Dark theme use tones 80 for accent colors, not 40 (too dark on dark backgrounds)

---

## 2. Typography

### 2.1 Type Scale (15 Styles)

Default typeface Roboto. Only Regular (400) and Medium (500) weights are used by default.

 Token  Size (sp)  Weight  Line Height (sp)  Letter Spacing (sp)  Usage 
------------------
 Display Large  57  Regular  64  -0.25  Hero text, splash screens 
 Display Medium  45  Regular  52  0  Large marketing text 
 Display Small  36  Regular  44  0  Sub-hero labels 
 Headline Large  32  Regular  40  0  Section headers, dialogs 
 Headline Medium  28  Regular  36  0  App bar titles (largemedium bars) 
 Headline Small  24  Regular  32  0  Card headers, list headers 
 Title Large  22  Regular  28  0  Toolbar titles, top app bar 
 Title Medium  16  Medium  24  0.15  Navigation labels, important items 
 Title Small  14  Medium  20  0.1  Tab labels, subtitle 
 Body Large  16  Regular  24  0.5  Default body text, text fields 
 Body Medium  14  Regular  20  0.25  Secondary body text 
 Body Small  12  Regular  16  0.4  Captions, helper text 
 Label Large  14  Medium  20  0.1  Button text, chip labels 
 Label Medium  12  Medium  16  0.5  Navigation bar labels, badges 
 Label Small  11  Medium  16  0.5  Annotation, footnotes 

### 2.2 When to Use Each Role

- Display Only for largest text on screen. Short, impactful, hero moments.
- Headline Short labels for top-of-page areas. App bars use Headline MediumSmall.
- Title Medium-emphasis, shorter text. Navigation, dialogs, list section headers.
- Body Long-form readable text. Body Large for primary content.
- Label Text inside components (buttons, chips, tabs). Never use for paragraph text.

### 2.3 Custom Typography in Compose

```kotlin
val AppTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = MyBrandFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = MyBodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
     All 15 styles available
)
MaterialTheme(typography = AppTypography) {  ...  }
```

RULE Button text is sentence case, NEVER ALL CAPS in M3.

---

## 3. Shape System

### 3.1 Shape Scale

M3 has 5 shape tiers + circle + rectangle

 Token  Corner Radius  Default Components 
---------
 Extra Small  4dp  Tooltip, snackbar, menu item 
 Small  8dp  Chip, text field, small card 
 Medium  12dp  Card (default), outlined text field 
 Large  16dp  Navigation drawer, large card, FAB 
 Extra Large  28dp  Bottom sheet, large dialog 
 Full (Circle)  50%  FAB (small), icon button, switch 
 None  0dp  Full-width banners 

### 3.2 Component Default Shapes

 Component  Shape Token  Radius 
---------
 Button (all types)  Full  Pill-shaped (50% radius) 
 FAB Small  Large  16dp 
 FAB Regular  Large  16dp 
 FAB Large  Extra Large  28dp 
 Extended FAB  Large  16dp 
 Card (elevatedfilledoutlined)  Medium  12dp 
 Chip (all types)  Small  8dp 
 Dialog  Extra Large  28dp 
 Bottom Sheet  Extra Large (top only)  28dp top corners 
 Navigation Bar  None (bottom)  0dp 
 Navigation Drawer  Large (right side)  16dp right corners 
 Top App Bar  None  0dp 
 Text Field  Extra Small (top only)  4dp top corners 
 Snackbar  Extra Small  4dp 

### 3.3 Shape Morphing (M3 Expressive)

M3 Expressive adds 35 new shape variants including polygon, star, and irregular forms. Shape
morphing enables animated state-changes (e.g., square → circle on selection). Use
`ShapeDefaults.`-prefixed tokens in Compose with `@OptIn(ExperimentalMaterial3ExpressiveApiclass)`.

---

## 4. Elevation

### 4.1 Tonal Elevation (Primary Method in M3)

M3 uses tonal color overlays instead of heavy shadows for elevation. The primary color tints the
surface at increasing levels. Shadows are secondary and subtle.

 Level  dp  Tonal Overlay Alpha  Usage 
------------
 Level 0  0dp  0%  Surface, backgrounds 
 Level 1  1dp  5% (Surface Container Low)  Cards, navigation bar 
 Level 2  3dp  8% (Surface Container)  FAB resting state 
 Level 3  6dp  11% (Surface Container High)  Dialogs, navigation drawer 
 Level 4  8dp  12%  Snackbar 
 Level 5  12dp  14% (Surface Container Highest)  Top app bar scrolled 

### 4.2 Component Elevations

 Component  Resting Elevation  PressedHover Elevation 
---------
 Filled Button  0dp  1dp 
 Elevated Button  1dp  2dp 
 Outlined Button  0dp  1dp 
 FAB  6dp  8dp 
 Extended FAB  6dp  8dp 
 Card (elevated)  1dp  4dp 
 Card (filled)  0dp  1dp 
 Card (outlined)  0dp  1dp 
 Navigation Bar  2dp  — 
 Top App Bar (default)  0dp  — 
 Top App Bar (scrolled)  3dp  — 
 Bottom Sheet  1dp  — 
 Dialog  3dp  — 
 Snackbar  3dp  — 
 Menu  2dp  — 

### 4.3 In Compose

```kotlin
Surface(
    tonalElevation = 6.dp,     Tonal color overlay
    shadowElevation = 2.dp     Physical shadow (secondary)
) {  content  }

 Surface container colors map to elevations automatically
containerColor = MaterialTheme.colorScheme.surfaceContainerHigh   ~Level 3
```

---

## 5. Layout & Spacing

### 5.1 Window Size Classes (Breakpoints)

 Class  Width Range  Typical Device  Navigation Component 
------------
 Compact   600dp  Phone portrait  Navigation Bar (bottom) 
 Medium  600–840dp  Phone landscape, small tablet, foldable  Navigation Rail 
 Expanded  840–1200dp  Tablet, large foldable open  Navigation Rail or Drawer 
 Large  1200–1600dp  Desktop window  Navigation Drawer 
 Extra-Large   1600dp  Large monitor  Navigation Drawer 

Implementation in Compose
```kotlin
val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
val useNavRail = windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT
```

### 5.2 Spacing System

M3 uses a 4dp base grid. All spacing values are multiples of 4dp.

 Token  Value  Usage 
---------
 space-0  0dp  No space 
 space-1  4dp  Inline gap between icon and label 
 space-2  8dp  Compact padding, dense lists 
 space-3  12dp  Component internal padding 
 space-4  16dp  Standard content padding, margins 
 space-5  20dp  Card internal padding 
 space-6  24dp  Section spacing, dialog padding 
 space-8  32dp  Large section breaks 
 space-10  40dp  Hero section padding 
 space-12  48dp  Screen-level vertical padding 

### 5.3 Margins & Gutters

 Window Class  Margin (leftright)  Gutter (between columns)  Columns 
------------
 Compact  16dp  16dp  4 
 Medium  24dp  24dp  12 
 Expanded  24–200dp (centered)  24dp  12 

### 5.4 Canonical Layouts

Three pre-built layout patterns for all screen sizes

List-Detail
- Compact Full-screen list → navigate to detail
- MediumExpanded List pane left (fixed) + detail pane right (flexible)
- Use Email, contacts, file browsers
- Implementation `ListDetailPaneScaffold` in Compose

Supporting Pane
- Compact Main content full-screen, supporting pane in bottom sheet
- MediumExpanded Main content left, supporting pane right (~30% width)
- Use Document editors, settings with preview, shopping carts
- Implementation `SupportingPaneScaffold` in Compose

Feed
- Compact Single-column card list
- Medium 2-column grid
- Expanded 3+ column grid or featured card + grid
- Use Social media, news, photos, catalogs

### 5.5 Pane Strategies

Three adaptation strategies as screen size changes
1. ShowHide — Show extra panes when space is available
2. Reposition — Move elements (e.g., FAB → Nav Rail at medium+)
3. ResizeReflow — Change element proportions, wrap columns

---

## 6. Motion

### 6.1 Easing Curves

 Curve  CSS Cubic-Bezier  Usage 
---------
 Emphasized  (0.2, 0.0, 0.0, 1.0)  Most transitions enteringon screen 
 Emphasized Decelerate  (0.05, 0.7, 0.1, 1.0)  Elements entering screen 
 Emphasized Accelerate  (0.3, 0.0, 0.8, 0.15)  Elements exiting screen 
 Standard  (0.2, 0.0, 0.0, 1.0)  Functional, utility animations 
 Standard Decelerate  (0.0, 0.0, 0.0, 1.0)  Elements decelerating to rest 
 Standard Accelerate  (0.3, 0.0, 1.0, 1.0)  Elements accelerating to exit 

### 6.2 Duration Tokens

 Token  Duration  Usage 
---------
 Short 1  50ms  Ripple effect start 
 Short 2  100ms  Checkbox, radio check 
 Short 3  150ms  FAB collapse, icon change 
 Short 4  200ms  Simple property change 
 Medium 1  250ms  Chip expandcollapse 
 Medium 2  300ms  Standard transition (mobile) 
 Medium 3  350ms  Card expansion 
 Medium 4  400ms  Navigation animation 
 Long 1  450ms  Full-screen transition 
 Long 2  500ms  Large complex transition 
 Long 3  550ms  Large area transitions 
 Long 4  600ms  Maximum standard 
 Extra Long 1–4  700–1000ms  Specialloading only 

Rules
- Mobile standard 300ms with Emphasized easing
- Exit animations should be shorter than enter (less attention needed)
- Desktop 150–200ms (faster, simpler)
- Avoid animations exceeding 400ms for interactive feedback

### 6.3 Spring Physics (M3 Expressive)

Physics-based motion for more natural, interruptible animations. Two preset motion schemes

 Scheme  Feel  Damping  Best For 
------------
 Expressive  Bouncy, playful, overshoot  Low damping  Hero moments, key CTAs 
 Standard  Subdued, minimal bounce  High damping  Utility, navigation 

Spring speeds by component size
- Fast spring Small components (switches, checkboxes, buttons)
- Default spring Mid-size components (cards, sheets)
- Slow spring Full-screen transitions

Types within each speed
- Spatial spring For positionsize (x, y — CAN overshoot)
- Effects spring For color, opacity (CANNOT overshoot  100%)

### 6.4 Transition Patterns

 Pattern  Animation  Usage 
---------
 Container Transform  Source element morphs into destination  Card → Detail, FAB → dialog 
 Shared Axis X  Slide leftright + fade  Forwardback navigation 
 Shared Axis Y  Slide updown + fade  Vertical hierarchy (tabs) 
 Shared Axis Z  Scale + fade  Changing content in-place 
 Fade Through  Cross-fade via 0 opacity  Unrelated destinations 
 Fade  Simple opacity  Appearingdisappearing elements 

---

## 7. Components Reference

 For detailed specs (exact dimensions, padding, states) see `referencescomponents.md`

### 7.1 Buttons

5 types, chosen by emphasis level (highest → lowest)

 Type  Container  Usage 
---------
 Elevated  Surface + shadow  Secondary action when no fill available 
 Filled  Primary color  Highest emphasis, one per screen section 
 Filled Tonal  Secondary container  Medium emphasis, secondary actions 
 Outlined  Transparent + border  Medium emphasis, secondary CTAs 
 Text  None  Lowest emphasis, inline actions 

Sizes & Specs (all button types)
- Height 40dp (standard)
- Minimum width 48dp
- Corner radius Full (pill) — approximately 20dp radius
- Horizontal padding 24dp (with label only), 16dp (with icon + label)
- Icon size 18dp (leading), positioned 8dp from label
- Label text Label Large (14sp, Medium weight, sentence case)
- Vertical touch target minimum 48dp (even if visual is 40dp)

Icon Button
- Size 40dp × 40dp visual  48dp × 48dp touch target
- Icon 24dp
- Variants standard, filled, filled tonal, outlined

Segmented Button
- Height 40dp
- Segments 2–5
- Use for mutually exclusive options (radio replacement) or multi-select (checkbox replacement)

### 7.2 Floating Action Button (FAB)

 Variant  Size  Corner Radius  Icon  Usage 
---------------
 FAB Small  40dp × 40dp  Large (16dp)  24dp  Compact space, secondary action 
 FAB Regular  56dp × 56dp  Large (16dp)  24dp  Primary action on screen 
 FAB Large  96dp × 96dp  Extra Large (28dp)  36dp  Hero screens, large touch targets 
 Extended FAB  56dp height, flexible width  Large (16dp)  24dp  When action needs a label 

Extended FAB internal padding 16dp left (icon side), 20dp right, 16dp topbottom, 12dp icon–label gap.

Positioning Bottom-right (default). Bottom-center for large screens. Above navigation bar 16dp gap.

### 7.3 Cards

3 variants, all with 12dp Medium shape

 Type  Elevation  Container Color  Shadow 
------------
 Elevated  1dp (Level 1)  Surface Container Low  Yes (subtle) 
 Filled  0dp (Level 0)  Surface Container Highest  No 
 Outlined  0dp (Level 0)  Surface  No — outline only 

Card anatomy
- Content padding 16dp all sides
- Imagemedia edge-to-edge (no padding)
- Header padding 16dp, with 16dp between icon and title
- Action area padding 8dp top, 8dp right
- Footer buttons Text buttons, 8dp gap

### 7.4 Navigation Components

Navigation Bar (Bottom)
- Height 80dp (with labels)
- Items 3–5 destinations
- Icon 24dp, active indicator pill 64dp × 32dp
- Label Label Medium (12sp)
- Active state filled icon + colored indicator pill
- Inactive outlined icon, muted color
- Color Surface Container (background)

Navigation Rail (Medium+ screens)
- Width 80dp
- Items 3–7 destinations
- FAB optional, top of rail
- FAB gap from first item 8dp
- Item height 56dp, icon 24dp

Navigation Drawer
- Width 360dp (modalstandard)
- Item height 56dp
- Item horizontal padding 12dp
- Active indicator full-width pill, 56dp × 48dp
- Header optional, 64dp height
- Section divider Divider component

Top App Bar (4 variants)

 Variant  Height  Title Style  Usage 
------------
 Small  64dp  Title Large (22sp)  Standard screen 
 Center-Aligned  64dp  Title Large centered  Simple screens 
 Medium  112dp  Headline Small (24sp) in lower section  Content-rich screens 
 Large  152dp  Headline Medium (28sp) in lower section  Feature screens 

All bars Leading icon (24dp, 48dp touch target), trailing icons (24dp), 16dp horizontal content padding.

### 7.5 Text Fields

2 variants

 Variant  Container  Usage 
---------
 Filled  Surface Variant bg, no outline  Primary, most forms 
 Outlined  Transparent + border  When bg contrast is needed 

Specs (both)
- Height 56dp
- Corner radius Extra Small top (4dp) — filled; Small all (4dp) — outlined
- Label text (floating) Label Small (11sp) when focused
- Input text Body Large (16sp) — NOTE 16sp not 14sp
- Helper text Body Small (12sp)
- Icon 24dp, 16dp from edge
- Horizontal padding 16dp
- Error indicator Error color outlineindicator, error icon optional

### 7.6 Chips

4 types, all 32dp height, Small shape (8dp)

 Type  Usage  Behavior 
---------
 Assist  Quick actions in context  Non-toggle, like a button 
 Filter  Narrow content  Toggleable (selectedunselected) 
 Input  Represent entered values (tags)  Removable with X 
 Suggestion  Offer contextual suggestions  Non-toggle 

- Horizontal padding 8dp (without icon), 8dp left + 4dp after icon
- Label Label Large (14sp)
- Leading icon 18dp
- Trailing icon (input chip) 18dp close icon
- Selected filter chip filled with Secondary Container color
- Elevated chips subtle shadow (1dp)

### 7.7 Dialogs

2 types

Basic Dialog
- Container Surface, Extra Large shape (28dp)
- Width Fixed, between 280dp–560dp
- Padding 24dp all sides
- Title Headline Small (24sp)
- Body Body Medium (14sp)
- Button area Row of Text or Filled buttons, right-aligned, 8dp gap
- Icon (optional) 24dp, centered above title

Full-Screen Dialog
- Used when content requires full screen or is a flow
- Top app bar with Close (X) and action
- No rounded corners (full screen)

### 7.8 Bottom Sheet

2 types

Modal Bottom Sheet
- Corner radius Extra Large top (28dp)
- Handledrag indicator 32dp × 4dp pill, centered, 16dp from top
- Scrim Black 40% opacity
- Peek height varies (typically 256dp)
- Full height up to 90% of screen

Standard Bottom Sheet
- Persistent, no scrim
- Same shapehandle

### 7.9 Sliders

4 configurations

 Type  Usage 
------
 Continuous  Any value in range 
 Discrete  Specific values only (tick marks) 
 Centered  Value relative to center point 
 Range  Select start and end values 

Specs
- Track height 4dp (inactive), 4dp (active)
- Thumb size 20dp diameter
- Touch target 48dp × 48dp around thumb
- Tick mark 2dp diameter (discrete only)
- Value label appears above thumb on drag, Label Medium (12sp)
- Track corner radius Full (pill)

### 7.10 Switches, Checkboxes, Radio Buttons

Switch
- Track 52dp × 32dp, Full shape
- Thumb 16dp (unselected), 24dp (selected)
- Touch target 52dp × 32dp minimum (use 48dp height wrapper)
- Selected Primary color track
- Supports optional icon on thumb (24dp)

Checkbox
- Box 18dp × 18dp
- Touch target 48dp × 48dp
- States unchecked (outlined), checked (filled primary), indeterminate (dash)
- Corner radius Extra Small (2dp)

Radio Button
- Outer circle 20dp diameter
- Touch target 48dp × 48dp
- Inner dot (selected) 10dp

### 7.11 Progress Indicators

Circular Progress
- Sizes 16dp (small), 24dp (medium), 48dp (large, default)
- Track width 3dp (small), 4dp (mediumlarge)
- Color Primary

Linear Progress
- Height 4dp track
- Corner radius Full (pill ends)
- Color Primary on Surface Container track

Loading Indicator (M3 Expressive — NEW)
- Replacement for short waits ( 5s)
- Morphs between shapes for visual engagement
- `LoadingIndicator` (standalone) or `ContainedLoadingIndicator` (in colored container)

### 7.12 Lists & List Items

Standard list item heights
- 1-line 56dp
- 2-line 72dp
- 3-line 88dp

Anatomy
- Leading item (iconavatarimage) 40dp × 40dp, 16dp from edge
- Content padding 16dp horizontal
- Headline Body Large (16sp)
- Supporting text Body Medium (14sp), on-surface-variant
- Trailing item 24dp icon or metadata

Dividers 1dp, Outline Variant color, optional padding

### 7.13 Snackbars

- Height 48dp (single line), taller for 2-line
- Width 288dp–640dp, centered or leading
- Corner radius Extra Small (4dp)
- Background Inverse Surface color
- Text Inverse On Surface color, Body Medium
- Action button Inverse Primary color, Label Large, text-only (no fill)
- Duration 4s (short), 10s (long), indefinite (requires action)
- Only one snackbar at a time

### 7.14 Tabs

2 variants

Primary Tabs
- Height 48dp
- Active indicator Full-width underline, 3dp height, Primary color
- Label Label Large or icon or both
- Scrollable when 5+ tabs

Secondary Tabs
- Similar but used for within-page filtering
- No underline, just text color change

### 7.15 Tooltips

2 types

Plain Tooltip
- Background Inverse Surface
- Text Inverse On Surface, Body Small
- Corner radius Extra Small (4dp)
- Padding 4dp vertical, 8dp horizontal

Rich Tooltip
- Background Surface Container
- Has title (Label Large), body text (Body Small), optional action button
- Corner radius Extra Small (4dp)
- Max width 320dp

### 7.16 Menus

Dropdown Menu
- Corner radius Extra Small (4dp)
- Item height 48dp
- Item padding 12dp vertical, 12dp horizontal
- Icon 24dp, trailing or leading
- Background Surface Container (elevation 2dp)
- Width min 112dp, max 280dp

Exposed Dropdown
- Looks like a text field with trailing dropdown arrow
- Uses text field height (56dp)
- Menu appears below text field

---

## 8. Accessibility

### 8.1 Touch Targets

Minimum 48dp × 48dp touch target for ALL interactive elements, even if visual size is smaller.
- Buttons visual 40dp → add 4dp padding each side for 48dp target
- Icons visual 24dp → wrap in 48dp × 48dp clickable area
- Checkboxesradios 18–20dp visual → 48dp touch target via padding

### 8.2 Color Contrast

 Context  Minimum Ratio 
------
 Normal text ( 18sp regular,  14sp bold)  4.51 
 Large text (≥ 18sp regular, ≥ 14sp bold)  31 
 UI components (borders, icons, non-text)  31 
 Disabled states  No requirement (but visually distinct) 
 Decorative elements  No requirement 

M3's on-color pairs (e.g., `on-primary` on `primary`) are guaranteed to meet 4.51.

### 8.3 State Layers (Interaction States)

All interactive elements use color overlays on interaction

 State  Alpha Overlay 
------
 Hover  8% of on-color 
 Pressed  12% of on-color 
 Focused  12% of on-color 
 Dragged  16% of on-color 
 Disabled  38% opacity (container) or 12% on surface 

### 8.4 Semantics in Compose

```kotlin
 Always provide contentDescription for icons
Icon(
    imageVector = Icons.Default.Edit,
    contentDescription = Edit profile   NOT null for standalone icons
)

 Use role for custom components
Modifier.semantics { role = Role.Button }

 For decorative iconsimages
contentDescription = null   explicitly null = skip in accessibility tree
```

---

## 9. Design Tokens & Theming

### 9.1 Token Hierarchy

```
Reference Tokens (md.ref.)      → Raw values (e.g., md.ref.palette.primary40 = #476810)
    ↓ assigned to
System Tokens (md.sys.)         → Semantic roles (e.g., md.sys.color.primary)
    ↓ applied to
Component Tokens (md.comp.)     → Component-specific (e.g., md.comp.button.container.color)
```

### 9.2 Complete MaterialTheme in Compose

```kotlin
@Composable
fun AppTheme(
    darkTheme Boolean = isSystemInDarkTheme(),
    dynamicColor Boolean = true,
    content @Composable () - Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT = Build.VERSION_CODES.S - {
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        }
        darkTheme - DarkColorScheme    your custom dark ColorScheme
        else - LightColorScheme        your custom light ColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

 Accessing tokens in components
val color = MaterialTheme.colorScheme.primary
val type = MaterialTheme.typography.bodyLarge
val shape = MaterialTheme.shapes.medium
```

### 9.3 CSS Custom Properties (Material Web)

```css
root {
  --md-sys-color-primary #6750A4;
  --md-sys-color-on-primary #FFFFFF;
  --md-sys-typescale-body-medium-size 0.875rem;
  --md-sys-typescale-body-medium-line-height 1.25rem;
  --md-ref-typeface-brand 'Your Brand Font';
  --md-ref-typeface-plain system-ui;
}
```

---

## 10. Quick Decision Reference

### What navigation component
- Phone portrait (Compact) → Navigation Bar (bottom, 3–5 items)
- Phone landscape  small tablet (Medium) → Navigation Rail (left side, 3–7 items)
- Tablet  desktop (Expanded+) → Navigation Drawer (persistent left, or modal)

### What button type
- Primary CTA (one per section) → Filled Button
- Secondary action, need some weight → Filled Tonal
- Secondary action, need contrast → Outlined
- Inline action, lowest priority → Text
- Action on elevated surface → Elevated
- Floating primary action → FAB (regular for most, large for hero, extended if label needed)

### What card type
- Clickable item on surface → Elevated Card (subtle shadow differentiates it)
- Item in a feedgrid → Filled Card (no shadow, contained feel)
- Needs to stand out from similar cards → Outlined Card (border, clean)

### What type style
- Page title  hero → Display or Headline Large
- Screen section header → Headline Small or Title Large
- List item primary → Body Large
- List item secondary → Body Medium
- Buttonchip text → Label Large
- Nav bar label → Label Medium
- Captionhelper → Body Small or Label Small

### Spacing quick rules
- Between cards in a feed 8dp gutters
- Internal card padding 16dp
- Screen edge margins 16dp (compact), 24dp (medium+)
- Between icon and label inline 8dp
- Dialog padding 24dp all sides
- Between dialog buttons 8dp
- Between sections 24dp
- Bottom nav to content auto (nav bar is separate surface)

---

## 11. M3 Expressive (Latest Update)

M3 Expressive is the evolution of M3, backed by 46 studies and 18,000+ participants. Key additions

- Spring physics motion — bouncy, retargetable animations (two schemes Expressive, Standard)
- Shape morphing — shapes animate between states
- 35 new shape variants — polygons, stars, irregular forms
- Emphasized typography styles — emphasized displayheadline variants
- New components
  - `LoadingIndicator`  `ContainedLoadingIndicator` — replaces circular progress for short waits
  - `FloatingToolbar` — replaces Bottom App Bar
  - `ButtonGroup`  `SplitButton` — grouped actions
  - FAB Menu — expandable FAB with multiple actions
- Larger buttons by default — improves usability for older users, motor impairments
- Users locate key elements 4× faster in Expressive designs vs standard

All expressive APIs require `@OptIn(ExperimentalMaterial3ExpressiveApiclass)`

---

For detailed per-component specs including all states and edge cases, see `referencescomponents.md`