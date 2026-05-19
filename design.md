# design.md

## 1. Visual Identity & Theme (MediaCore V4.0 Spec)

### 1.1. Landing & Authentication Screen
- **Theme:** Light Theme with a technical blueprint / grid feeling.
- **Background:** Crisp white with light gray grid-line background pattern (16dp cell width/height, line thickness 1dp, color `#F0F0F0`).
- **Typography:**
  - App Name: "MEDIACORE V4.0" (SCREAMING_SNAKE_CASE, medium weight, charcoal color, side padding).
  - Main Title: "HD VIDEO CALL" (Bold, large sans-serif, deep black).
  - Subtitle: "HIGH-BITRATE SECURE LINK • ENCRYPTED" (Medium size, semi-bold, gray color).
  - Left Accent Bar: Solid black vertical bar (thickness 4dp, height spanning both lines of Title and Subtitle) placed to the left of the text block.
- **Landing Card:**
  - Color: White surface (`#FFFFFF`), rounded borders (4dp), thin gray outline (`#E0E0E0`).
  - Layout: Column with distinct vertical margins.
  - Labels: "ROOM ENVIRONMENT" and "OPERATOR IDENTITY" in small uppercase gray text.
  - Input Fields: Large text size, bordered style, placeholder text in uppercase. Includes a custom right-aligned icon:
    - Room field: Door/Entry outline icon.
    - Operator field: ID card outline icon.
  - CTA Button: Large rectangular black button (`#000000`) containing bold uppercase white text "INITIALIZE CONNECTION" and an arrow icon pointing right (`->`). Full width.

---

### 1.2. Active Call Grid View
- **Theme:** Dark Mode, military/industrial secure overlay theme.
- **Background:** Jet Black (`#000000`) or the raw remote camera stream.
- **Header Bar:**
  - Left: Shield icon followed by "SESSION_ID: 0X4F2A" in white mono text.
  - Right: Outlined profile/operator icon.
- **Stream Grid:**
  - Remote video stream fills the viewport.
  - Mirrored locally (PiP): Floating self-view overlay in the top-right.
- **Floating self-view overlay (PiP):**
  - Rectangular card with absolute aspect ratio (approx. 4:5 or 3:4).
  - Border: Thin gray/white border.
  - Header overlay: "AES-256" and "60.0 FPS" in tiny white mono text at the top-right corner.
  - Footer overlay: "[YOU]" label in white bold text inside a transparent black card at the bottom-left corner of the PiP.
- **Status Overlay Indicators:**
  - Top-Left: "REMOTE PARTICIPANT" labeled with a red square status icon. Below it, a transparent pill showing connection specs: "LATENCY: 24MS | RESOLUTION: [etc.]".
  - Bottom-Left: "NETWORK STABILITY INDEX" label, followed by 4 vertical indicator bars. Active bars are white, inactive are gray.
  - Bottom-Right: "SECURE LINK ESTABLISHED" in white uppercase text. Underneath it, "TUNNEL: 09-XF-44-22" in small gray mono text.
- **Controls Dock (Very Bottom):**
  - Full-width, solid black panel (`#000000`).
  - Contains five equally spaced control buttons:
    1. **MUTE:** White micro-icon (Microphone) with label "MUTE".
    2. **VIDEO:** Highlighted state. White background card, black video camera icon, and "VIDEO" label.
    3. **SHARE:** White icon (Upload/Share screen) with label "SHARE".
    4. **GRID:** White icon (Grid/Layout) with label "GRID".
    5. **END CALL:** Rectangular red background card (`#B71C1C`) with white phone-down icon inside.

---

## 2. Component Design & Layout Primitives (Compose Implementation guidelines)
- **Grid Background Component:** Implement a custom Compose `Modifier.drawBehind` to paint a clean background grid pattern.
- **CTA Button Click Animation:** Custom interactive scaling down to 0.96f on tap.
- **Picture-in-Picture Draggability:** State-driven offset modifier to track user drag gestures on the local camera PiP.
