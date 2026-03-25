# Design System Specification: The Focused Void

## 1. Overview & Creative North Star

### Creative North Star: The Focused Void
This design system is built on the philosophy of "The Focused Void." In an era of digital clutter, this system acts as a high-end sanctuary. It moves beyond the standard iOS "utility" look into a bespoke editorial experience. By utilizing deep blacks, extreme translucency, and high-contrast typography, we create an environment where the only thing that matters is the passage of time.

We break the "template" look through **intentional asymmetry** and **tonal depth**. Rather than a rigid, centered grid, we use wide margins and staggered element placements to create a rhythmic, sophisticated flow. This is not just a timer; it is a premium instrument for deep work.

---

## 2. Colors & Surface Logic

The palette is strictly monochrome, relying on the interplay of light and shadow rather than hue to convey meaning.

### Surface Hierarchy & Nesting
We define depth through **Tonal Layering** rather than borders. Treat the UI as a series of nested obsidian sheets.
- **Base Layer:** `surface` (#131313) or pure black (#000000) for the deepest immersion.
- **Sectioning:** Use `surface_container_low` (#1B1B1B) for large structural areas.
- **Active Cards:** Use `surface_container_high` (#2A2A2A) to bring interactive elements forward.

### The "No-Line" Rule
**Explicit Instruction:** 1px solid borders are strictly prohibited for sectioning. Boundaries must be defined solely through background color shifts. A `surface_container_low` card sitting on a `surface` background is sufficient to define a shape. This creates a "soft" interface that feels integrated and expensive.

### The "Glass & Gradient" Rule
To elevate the system above a standard flat UI, all floating elements (modals, navigation bars, and "Quick Action" buttons) must use **Glassmorphism**:
- **Background:** `primary` (#FFFFFF) at 5–10% opacity.
- **Effect:** `backdrop-filter: blur(20px)`.
- **Signature Texture:** Apply a subtle linear gradient to main CTAs, transitioning from `primary` (#FFFFFF) to `primary_container` (#D4D4D4) at a 15-degree angle. This provides a metallic, tactile "soul" to the monochrome buttons.

---

## 3. Typography

The system utilizes **Inter** (as a high-fidelity alternative to SF) to drive a sophisticated editorial feel. 

- **The Hero (Display-LG):** 3.5rem. Used exclusively for the timer countdown. Letter-spacing should be set to `-0.02em` to feel tight and authoritative.
- **Headlines (Headline-MD):** 1.75rem. Used for section titles. These should be set in Semi-Bold to provide a strong anchor against the black void.
- **Body & Labels:** 
    - `body-lg` (1rem) for primary interactions.
    - `label-md` (0.75rem) with `+0.05em` tracking for secondary metadata (e.g., "SESSIONS COMPLETED"). This increased tracking adds a premium "watchmaker" aesthetic to small text.

---

## 4. Elevation & Depth

We move away from the "drop shadow" era. Elevation is achieved through light emission and translucency.

- **The Layering Principle:** Stack `surface-container` tiers. Place a `surface_container_highest` (#353535) element inside a `surface_container_low` (#1B1B1B) area to create natural, soft lift.
- **Ambient Shadows:** For floating elements only, use an extra-diffused shadow: `box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4)`. The shadow color must never be grey; it must be a darker tint of the background to simulate natural light occlusion.
- **The "Ghost Border" Fallback:** If a container lacks sufficient contrast against its parent, use a "Ghost Border": `outline_variant` (#474747) at 15% opacity. This creates a suggestion of a boundary without the harshness of a solid line.

---

## 5. Components

### Buttons
- **Primary:** Full rounded (`rounded-full`). Background: `primary` (#FFFFFF), Text: `on_primary` (#1A1C1C).
- **Secondary (Glass):** Full rounded. Background: `surface_bright` (#393939) at 20% opacity with `backdrop-blur`.
- **Tertiary:** No background. Text: `secondary` (#C6C6CB). Use for "Cancel" or low-priority actions.

### Cards & Lists
- **Rule:** Forbid the use of divider lines.
- **List Implementation:** Separate list items using `spacing-4` (1rem). Each item should reside in its own `surface_container_low` container with a `rounded-lg` (1rem) corner radius.
- **Asymmetry:** In detail views, stagger card widths (e.g., a 100% width card followed by two 48% width cards) to create an editorial layout rather than a repetitive list.

### Input Fields
- **Style:** Minimalist underline or translucent block. 
- **Inactive:** `surface_container_highest` (#353535) at 40% opacity.
- **Focus:** `primary` (#FFFFFF) "Ghost Border" (20% opacity) with a subtle glow using `primary` at 5% opacity.

### The Focus Timer (Signature Component)
The timer is the centerpiece. Use `display-lg` typography. Surround the digits with a `rounded-full` progress ring that uses a `primary` to `secondary` gradient at 2px thickness.

---

## 6. Do's and Don'ts

### Do:
- **Use Breathing Room:** Use `spacing-12` (3rem) and `spacing-16` (4rem) to separate major sections. White space is a design element, not "empty" space.
- **Prioritize Legibility:** Ensure `on_background` (#E2E2E2) is used for all long-form reading text to reduce eye strain against the pure black background.
- **Leverage Inter's Weights:** Use Thin (100) or ExtraLight (200) for large background decorative numbers to add depth.

### Don't:
- **Don't use 100% Opacity Dividers:** They break the immersion of the "void."
- **Don't use Standard Shadows:** Avoid small, dark, high-opacity shadows that look like "web 2.0" styles.
- **Don't Center Everything:** Center-aligned text is for beginners. Use left-aligned editorial stacks for a more professional, intentional feel.
- **Don't use Pure White for secondary text:** Always use `on_surface_variant` (#C6C6C6) or `secondary` (#C6C6CB) to maintain a clear visual hierarchy.