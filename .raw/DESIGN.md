# Design System: Toki Focus Timer
**Project ID:** Local bitmap reference from `ChatGPT Image May 2, 2026, 08_53_11 PM.png`

---

> This document defines both **Dark Mode** (default) and **Light Mode** variants. All typography, spacing, layout, shape language, stroke weights, and component structure are identical between modes. Only the color palette and its role assignments change.

---

## 1. Visual Theme & Atmosphere

### Dark Mode
Toki is a midnight-minimal productivity interface with a calm, premium, instrument-like presence. The screen uses a true black canvas, sparse white typography, thin graphite outlines, and a few soft status accents to keep attention on the timer. The atmosphere is focused, quiet, tactile, and slightly futuristic without becoming decorative.

### Light Mode
Toki Light is a daylight-clean variant of the same instrument. The screen uses a pure white canvas, near-black typography, thin warm-gray outlines, and the same sparse red/cyan symbolic accents. The atmosphere is crisp, airy, and paper-like — premium in a different register without becoming soft or pastel. The UI retains its calm utility mood; brightness replaces depth but never adds noise.

The UI should feel like a polished mobile utility rather than a marketing screen: dense enough for repeated daily use, but spacious around the central timer so the session state feels ceremonial. Every control is rounded, low-contrast, and intentionally restrained until selected.

## 2. Color Palette & Roles

### Dark Mode
* **Absolute Night Black (#000000):** Primary app background. Creates the deep, distraction-free base for the timer experience.
* **Soft White (#F7F7F7):** Primary foreground for the logo text, active timer numerals, selected pills, icon strokes, and the large play button surface.
* **Pure Black Ink (#050505):** Text or icon color used on white selected controls, especially the active duration pill and play glyph.
* **Charcoal Glass (#111111):** Subtle card and navigation surfaces. Used where containers need to separate from the black background without becoming visually heavy.
* **Pressed Graphite (#1F1F1F):** Active segmented-control fill and soft interactive backgrounds.
* **Fine Graphite Stroke (#2A2A2A):** Hairline borders for pills, cards, the bottom navigation, and inactive controls.
* **Dim Graphite Track (#242424):** Timer progress background ring and secondary progress bars.
* **Muted Silver Text (#A6A6A6):** Secondary labels, inactive duration text, card labels, and inactive navigation labels.
* **Disabled Smoke (#777777):** De-emphasized mode text such as inactive "Flow" and subdued tab labels.
* **Pomodoro Red (#FF2727):** Tiny brand/status accent on the logo and tomato icon. Use sparingly for presence, not for broad warning states.
* **Wave Cyan (#28A9E8):** Flow mode emoji/icon accent. Keep as a small symbolic color rather than a system-wide blue.

### Light Mode
* **Pure Canvas White (#FFFFFF):** Primary app background. Creates the bright, distraction-free base.
* **Ink Black (#0D0D0D):** Primary foreground for the logo text, active timer numerals, selected pills, icon strokes, and the large play button surface.
* **Soft White Fill (#F7F7F7):** Text or icon color used on dark selected controls, especially the active duration pill and play glyph.
* **Cloud Surface (#F2F2F2):** Subtle card and navigation surfaces. Used where containers need to separate from the white background without becoming visually heavy.
* **Pressed Ash (#E4E4E4):** Active segmented-control fill and soft interactive backgrounds.
* **Fine Ash Stroke (#D4D4D4):** Hairline borders for pills, cards, the bottom navigation, and inactive controls.
* **Pale Ash Track (#E8E8E8):** Timer progress background ring and secondary progress bars.
* **Muted Slate Text (#7A7A7A):** Secondary labels, inactive duration text, card labels, and inactive navigation labels.
* **Disabled Fog (#BBBBBB):** De-emphasized mode text such as inactive "Flow" and subdued tab labels.
* **Pomodoro Red (#FF2727):** Unchanged. Same tiny brand/status accent on the logo and tomato icon. Use sparingly.
* **Wave Cyan (#28A9E8):** Unchanged. Flow mode emoji/icon accent. Keep as a small symbolic color.

## 3. Typography Rules
Use a modern geometric sans serif with rounded terminals, similar to SF Pro Display, Inter, or Geist. Typography should be bold only where the interface needs immediate recognition. These rules apply identically to both dark and light modes; only the token colors differ.

* **App title:** Heavy weight, large mobile title scale, tight but readable spacing.
* **Timer numerals:** Extra-bold, oversized, centered, and highly legible. The numerals are the visual anchor of the screen.
* **Mode and control labels:** Semibold, compact, and clear. Labels inside pills should be large enough to feel tappable.
* **Metadata labels:** Uppercase, medium weight, muted color, and wide letter spacing. Examples include "FOCUS", "DAILY GOAL", and "STREAK".
* **Navigation labels:** Uppercase, compact, medium weight. Dark: active in Soft White, inactive in Muted Silver Text. Light: active in Ink Black, inactive in Muted Slate Text.

Avoid negative letter spacing. Reserve wide tracking for small uppercase labels only.

## 4. Component Stylings

All structure, placement, sizing, and radius values are identical in both modes. Only fill, stroke, and text colors swap to their mode-appropriate tokens.

### Dark Mode
* **Header:** Settings button — transparent round control with Fine Graphite Stroke border, white gear icon.
* **Logo mark:** White circular glyph on black, small Pomodoro Red dot near top-right.
* **Mode switch:** Fine Graphite Stroke outline. Active segment: Pressed Graphite fill, Soft White label, tomato icon. Inactive: transparent/black fill, Disabled Smoke text, cyan wave icon.
* **Duration selector:** Inactive: Charcoal Glass fill, Fine Graphite Stroke border, Muted Silver Text. Active: Soft White pill, Pure Black Ink text, no border.
* **Circular timer:** Dim Graphite Track, Soft White active arc, rounded caps, round handle.
* **Secondary action pills:** Fine Graphite Stroke border, white icons, Soft White text.
* **Primary play button:** Soft White circle, Pure Black Ink play glyph.
* **Metric cards:** Charcoal Glass fill, Fine Graphite Stroke border. Uppercase muted labels, large white values, muted supporting text.
* **Progress mini bar:** Dim Graphite Track; slightly lighter fill when progress exists.
* **Bottom navigation:** Charcoal Glass fill, Fine Graphite Stroke border. Active: Soft White icon + label + short white underline. Inactive: Muted Silver Text.

### Light Mode
* **Header:** Settings button — transparent round control with Fine Ash Stroke border, Ink Black gear icon.
* **Logo mark:** Ink Black circular glyph on white, same Pomodoro Red dot near top-right.
* **Mode switch:** Fine Ash Stroke outline. Active segment: Pressed Ash fill, Ink Black label, tomato icon. Inactive: transparent/white fill, Disabled Fog text, cyan wave icon.
* **Duration selector:** Inactive: Cloud Surface fill, Fine Ash Stroke border, Muted Slate Text. Active: Ink Black pill, Soft White Fill text, no border.
* **Circular timer:** Pale Ash Track, Ink Black active arc, rounded caps, round handle.
* **Secondary action pills:** Fine Ash Stroke border, Ink Black icons, Ink Black text.
* **Primary play button:** Ink Black circle, Soft White Fill play glyph.
* **Metric cards:** Cloud Surface fill, Fine Ash Stroke border. Uppercase muted labels, large Ink Black values, Muted Slate Text supporting text.
* **Progress mini bar:** Pale Ash Track; slightly darker fill when progress exists.
* **Bottom navigation:** Cloud Surface fill, Fine Ash Stroke border. Active: Ink Black icon + label + short black underline. Inactive: Muted Slate Text.

## 5. Layout Principles
Design for a tall mobile viewport with a centered, single-column composition and generous horizontal margins. The screen should feel vertically sequenced: brand header, mode controls, duration controls, timer, session metadata controls, primary action, progress cards, then bottom navigation.

Use consistent edge margins around 24-28px on mobile. Keep large vertical breathing room around the circular timer, while allowing the top controls and bottom cards to remain compact. Align paired controls and cards on a two-column grid with equal widths and even gutters.

The central timer circle should dominate the screen width, roughly 80-85% of the viewport, and must remain visually centered. Controls should never overlap or crowd the circle; the interface depends on calm spacing.

## 6. Shape, Radius & Stroke Language
The design is built almost entirely from circles, pill shapes, and softly rounded rectangles.

* **Circular controls:** Logo, settings button, progress ring, and play button.
* **Pill controls:** Segmented mode switch, duration chips, secondary action buttons, and bottom navigation.
* **Rounded cards:** Metric cards use rounded corners that are softer than standard utility cards but less round than pills.
* **Stroke weight:** Borders and rings should be thin, about 1-2px for containers and 6-8px for the timer ring. Strokes are low-contrast except when showing active progress.

## 7. Depth & Elevation
Depth is created through contrast, borders, and tonal fills rather than shadows. The UI should feel flat and precise, with only whisper-soft inner tonal variation on cards and the bottom navigation. Avoid heavy drop shadows, glows, gradients, or decorative background effects. This principle is identical in both modes.

**Dark Mode:** Selected states use fill inversion — white surface with black text for duration and play, or charcoal fill with white text for mode selection. Inactive states stay outlined and quiet.

**Light Mode:** Selected states use the same fill inversion logic reversed — black surface with white text for duration and play, or pale ash fill with black text for mode selection. Inactive states stay outlined and quiet. The inversion principle is the same; the polarity swaps.

## 8. Interaction States

### Dark Mode
* **Active:** White fill or bright white stroke, black text where needed, clear icon contrast.
* **Inactive:** Transparent or charcoal fill, graphite border, muted text.
* **Pressed:** Slightly brighten the border and deepen the fill without adding glow.
* **Disabled:** Disabled Smoke text, low-contrast outlines.
* **Progress:** Soft White arc over Dim Graphite Track, rounded caps, visible circular handle.

### Light Mode
* **Active:** Ink Black fill or crisp black stroke, white text where needed, clear icon contrast.
* **Inactive:** Transparent or Cloud Surface fill, ash border, muted slate text.
* **Pressed:** Slightly darken the border and deepen the fill without adding shadow or glow.
* **Disabled:** Disabled Fog text, low-contrast outlines.
* **Progress:** Ink Black arc over Pale Ash Track, rounded caps, visible circular handle.

## 9. Iconography
Use simple, rounded, high-legibility line icons. The app uses playful emoji-like symbolic accents only in the mode switch: tomato for Pomodoro and wave for Flow. Do not expand this into a colorful icon system; the rest of the UI remains monochrome.

**Dark Mode:** Icons are white in active contexts, muted gray in inactive navigation.
**Light Mode:** Icons are Ink Black in active contexts, Muted Slate Text in inactive navigation. Same shapes, same sizes; only color swaps.

## 10. Prompting Notes For New Screens

**Dark Mode:** Describe Toki as a dark, premium, mobile-first focus timer with a black canvas, white typographic hierarchy, graphite outlines, pill controls, circular progress motifs, and sparse red/cyan symbolic accents. Preserve the calm utility mood, the heavy central numerals, and the contrast inversion used for selected controls.

**Light Mode:** Describe Toki Light as a bright, premium, mobile-first focus timer with a pure white canvas, near-black typographic hierarchy, warm ash outlines, pill controls, circular progress motifs, and the same sparse red/cyan symbolic accents. Preserve the calm utility mood, the heavy central numerals, and the contrast inversion used for selected controls — the polarity flips but the logic is identical.
