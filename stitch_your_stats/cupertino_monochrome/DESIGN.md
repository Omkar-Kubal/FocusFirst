# Design System Strategy: The High-Precision Light Theory

## 1. Overview & Creative North Star
**Creative North Star: The Ethereal Archive**
This design system is not a standard "flat" UI; it is an exercise in high-precision editorial layering. It moves away from the "boxy" nature of traditional apps by treating the interface as a series of translucent, stacked planes of light. The goal is to create an experience that feels as curated as a high-end fashion lookbook—minimalist, authoritative, and impossibly clean. 

We break the "template" look through **tonal depth**. Instead of using lines to separate thoughts, we use light. The aesthetic relies on the tension between pure black (`primary`) and the varying temperatures of white and silver to guide the eye.

## 2. Colors & Tonal Architecture
The palette is strictly monochrome but achieves "soul" through the meticulous use of Material Design-inspired surface tiers.

### The "No-Line" Rule
**Explicit Instruction:** You are prohibited from using 1px solid borders to define sections. A "borderless" interface feels more expansive and premium. Boundaries must be defined solely through:
1.  **Background Color Shifts:** Placing a `surface-container-lowest` card on a `surface-container-low` background.
2.  **Subtle Tonal Transitions:** Using the hierarchy of `surface` to `surface-dim` to create natural breaks in content.

### Surface Hierarchy & Nesting
Treat the UI as a physical stack of fine paper.
*   **Base Layer (`surface` / `#f9f9fe`):** The canvas.
*   **Floating Elements (`surface-container-lowest` / `#ffffff`):** Used for primary cards to make them pop against the off-white base.
*   **Recessed Elements (`surface-container-high` / `#e8e8ed`):** Used for input fields or secondary utility areas to "sink" them into the page.

### The "Glass & Gradient" Rule
To achieve the requested "high-precision" feel, use **Glassmorphism** for all persistent navigation (top bars/bottom tabs). 
*   **Formula:** `surface` at 70% opacity + `backdrop-blur: 20px`.
*   **Signature Texture:** For primary CTAs, do not use a flat black. Apply a subtle radial gradient from `primary` (#000000) to `primary-container` (#3b3b3b) at a 45-degree angle. This prevents the black from feeling "dead" and adds a microscopic level of polish.

---

## 3. Typography: Editorial Authority
We utilize **Inter** not as a system font, but as a typographic tool for hierarchy.

*   **Display (lg/md/sm):** Used for hero moments. Tracking should be set to `-0.02em` to create a tight, Swiss-style tension.
*   **Headline & Title:** These are your anchors. Always use `primary` (#000000) for these to ensure high-contrast readability.
*   **Body (lg/md):** Use `on-surface-variant` (#474747) for long-form text to reduce eye strain, reserving pure black for emphasis.
*   **Label (md/sm):** Set in uppercase with `+0.05em` letter spacing when used for categories to provide an architectural feel.

---

## 4. Elevation & Depth: Tonal Layering
Traditional shadows are often "dirty." In this system, depth is a result of light physics.

*   **The Layering Principle:** Place a `#ffffff` card (`surface-container-lowest`) on a `#f3f3f8` (`surface-container-low`) background. The 4% difference in luminosity is enough to define the edge without a single pixel of stroke.
*   **Ambient Shadows:** For floating modals, use a "Shadow Tint." Instead of `rgba(0,0,0,0.2)`, use `rgba(26, 28, 31, 0.06)` (a tint of our `on-surface` color) with a blur of `40px` and a `12px` Y-offset.
*   **The "Ghost Border" Fallback:** If a layout is failing accessibility on specific displays, use a 1px border with `outline-variant` (#c6c6c6) at **15% opacity**. It should be felt, not seen.
*   **Glassmorphism Depth:** Navigation bars must allow the content to scroll underneath them. The `surface-variant` color should be used as a subtle "shine" on the top edge of glass elements.

---

## 5. Components: The Primitive Set

### Buttons
*   **Primary:** Solid `primary` (#000000) with `on-primary` (#e2e2e2) text. 12px radius. No shadow.
*   **Secondary:** `surface-container-highest` background with `primary` text.
*   **Tertiary:** Ghost style. No background, pure `primary` text, underlined only on hover.

### Cards & Lists
*   **The Divider Ban:** Do not use horizontal lines between list items. Use **8px of vertical whitespace** and a transition to a slightly different surface tone (`surface-container-low`) on tap/hover.
*   **Nesting:** Small "Selection Chips" should live inside `surface-container-highest` wells to create a sense of containment.

### Input Fields
*   **Style:** Soft-filled. Use `surface-container-high` as the background. On focus, the background shifts to `surface-container-lowest` (pure white) with a `ghost border`.

### Specialized Component: The "Precision Scrubber"
For iOS-inspired systems, use a custom slider/scrubber that uses a `primary` track and a `surface-container-lowest` thumb with a 10% `ambient shadow`.

---

## 6. Do's and Don'ts

### Do:
*   **DO** use whitespace as a structural element. If in doubt, add `spacing-8`.
*   **DO** use `surface-bright` for highlights in dark images or headers.
*   **DO** maintain the 12px (`md` roundedness) corner radius religiously across all containers.

### Don't:
*   **DON'T** use 100% black for secondary text; it creates "vibration." Use `on-surface-variant`.
*   **DON'T** use standard "Drop Shadows" from software defaults. They look amateur in a high-precision system.
*   **DON'T** use pure blue for links. Use `primary` (Black) with a medium-weight underline to maintain the monochrome editorial aesthetic.

### Accessibility Note
While we are chasing a "Soft Minimalist" aesthetic, ensure that all `on-surface` text meets a 4.5:1 contrast ratio against its respective `surface` tier. Use the `error` (#ba1a1a) token sparingly but clearly for destructive actions.