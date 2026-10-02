# Vita3K UI Transformation Plan for "Nes Emulator ArDev"

This plan faithfully implements the clean, modern Vita3K design shown in the user's screenshots, tailored specifically for **Nes Emulator ArDev**.

---

## 1. Main Screen (Library Screen - Vita3K Style)

Based on **Screenshot 1**:
- **Header Bar**:
  - Title: **Nes Emulator ArDev** in bold white typography.
  - Subtitle: **v1.0.0 (Pro 60 FPS)** in muted gray.
  - Action icons on the right: Search (🔍), Settings (⚙️), Filter/Sort (☰), and More (⋮).
- **Game List Layout**:
  - Clean OLED black background (`#0D0E12`).
  - Square game icon with subtle rounded corners (`48x48 dp`).
  - Game Title in bold white (`15sp`).
  - Sub-row containing:
    - Game ID / Mapper code (e.g., `NES-MMC3`, `NES-AXROM`, `NES-NROM`).
    - Vita3K-style status badge:
      - `[ Playable ]` (Green rounded pill `#2E7D32`)
      - `[ Ingame ]` or `[ 60 FPS ]` (Amber/Orange rounded pill `#D97706`).
- **Floating Action Button (FAB)**:
  - Deep amber/orange circular button (`#D97706` / `#E65100`) at the bottom right with a white `+` icon to import `.nes` ROMs.

---

## 2. Settings Screen (Vita3K Style)

Based on **Screenshot 2**:
- **Header**:
  - Back arrow (←), "Settings" in bold, Search (🔍), More (⋮).
- **Horizontal Category Tabs**:
  - Filter chips with icons: `Core`, `CPU`, `Graphics` (Active with amber highlight), `Audio`, `Controls`, `Save States`.
- **Card-Based Settings Components**:
  - Rounded dark cards (`#1B1D22`).
  - Setting title, subtitle, and (ℹ️) info button.
  - **Segmented Choice Pills**:
    - Unselected: Dark gray pill (`#232630`) with white text.
    - Selected: Active Vita3K Amber/Orange (`#D97706`) with bold text.
    - Video Filters: `Nearest`, `Bilinear`, `CRT Scanlines`.
    - Aspect Ratio: `Original (4:3)`, `Square (1:1)`, `Stretch`.
    - Controller Size: `Compact`, `Standard`, `Large`.
  - **Amber Switch Toggles**:
    - Switches styled with amber/orange track and thumb.

---

## 3. In-Game Controller Overlay (Vita3K Minimalist White Outline)

Based on **Screenshot 3**:
- **Minimalist White Line Art Overlay**:
  - Ultra-clean transparent overlay with crisp white 1.8dp outlines (adjustable opacity).
- **Left Side**:
  - Top: Rounded rectangle `[ L ]` button (Quick Load).
  - Center: Sleek 4-way D-Pad outline with cross arrows and center diamond.
  - Bottom Left: Circular analog-style ring / thumb-pad.
  - Bottom Center-Left: Rounded pill `[ SELECT ]`.
- **Right Side**:
  - Top: Rounded rectangle `[ R ]` button (Quick Save).
  - Action Cluster:
    - Primary NES buttons: `[ B ]` and `[ A ]` in clean circular outlines.
    - Turbo buttons: `[ TB ]` and `[ TA ]` for 30Hz rapid fire.
  - Bottom Right: Circular analog-style ring.
  - Bottom Center-Right: Rounded pill `[ START ]`.
- **Center Controls**:
  - Top: Circle `(F)` button (Fast-Forward 2x toggle).
  - Bottom: Circle logo `(ArDev)` button to trigger the in-game Vita3K menu / save state drawer.

---

## 4. Execution Steps & Verification

1. **Theme & Colors**:
   - Define Vita3K color constants (`Vita3kDark`, `Vita3kCard`, `Vita3kAmber`, `Vita3kGreen`).
2. **Library Screen**:
   - Update `LibraryScreen.kt` to reproduce Screenshot 1 with "Nes Emulator ArDev" branding, mapper badges, and amber FAB.
3. **Settings Screen**:
   - Implement the tabbed category settings screen replicating Screenshot 2.
4. **In-Game Overlay & Screen**:
   - Update `VirtualController.kt` and `GameScreen.kt` to match the minimalist white outline style of Screenshot 3.
5. **Verification**:
   - Run `compile_applet` and unit tests (`gradle :app:testDebugUnitTest`).
   - Rebuild and export `apks/NES_Emulator_By_ArDev_v1.0.apk`.
